package router

import (
	"fmt"
	"notification-service/config"
	"notification-service/internal/app/email"
	"notification-service/internal/app/notification"
	"notification-service/internal/app/push"
	"notification-service/internal/middleware"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/go-redis/redis/v8"
	"github.com/hibiken/asynq"
	"github.com/sirupsen/logrus"
	"github.com/twilio/twilio-go"
	"gorm.io/gorm"
)

func SetupRouter(db *gorm.DB, redisClient *redis.Client, asynqClient *asynq.Client, asynqServer *asynq.Server, cfg *config.Config, logger *logrus.Logger) *gin.Engine {
	r := gin.New()

	// Set request size limit
	r.MaxMultipartMemory = cfg.MaxRequestSize

	// Middleware
	r.Use(gin.LoggerWithFormatter(func(param gin.LogFormatterParams) string {
		return fmt.Sprintf("%s - [%s] \"%s %s %s %d %s \"%s\" %s\"\n",
			param.ClientIP,
			param.TimeStamp.Format(time.RFC1123),
			param.Method,
			param.Path,
			param.Request.Proto,
			param.StatusCode,
			param.Latency,
			param.Request.UserAgent(),
			param.ErrorMessage,
		)
	}))
	r.Use(gin.Recovery())

	// CORS middleware (secure)
	r.Use(middleware.CORSMiddleware(cfg))

	// Security headers
	r.Use(func(c *gin.Context) {
		c.Header("X-Frame-Options", "SAMEORIGIN")
		c.Header("X-Content-Type-Options", "nosniff")
		c.Header("X-XSS-Protection", "1; mode=block")
		c.Header("Referrer-Policy", "strict-origin-when-cross-origin")
		c.Next()
	})

	// Rate limiting middleware
	r.Use(middleware.RateLimitMiddleware(cfg, redisClient))

	// Initialize services
	emailService := email.NewService(cfg)
	pushService := push.NewService(cfg, logger)
	
	// Initialize Twilio
	twilioClient := twilio.NewRestClientWithParams(twilio.ClientParams{
		Username: cfg.TwilioAccountSID,
		Password: cfg.TwilioAuthToken,
	})

	// Initialize notification components
	notificationRepo := notification.NewRepository(db)
	notificationService := notification.NewService(notificationRepo, redisClient, asynqClient, emailService, pushService, twilioClient, cfg, logger)
	notificationHandler := notification.NewHandler(notificationService, logger)

	// Start background notification processor
	if asynqServer != nil {
		mux := asynq.NewServeMux()
		mux.HandleFunc(notification.TypeNotificationDelivery, notification.NewNotificationProcessor(notificationService).ProcessTask)
		go func() {
			if err := asynqServer.Start(mux); err != nil {
				logger.Fatalf("could not start asynq server: %v", err)
			}
		}()
	} else {
		logger.Warn("Asynq server is nil, notification tasks will not be processed")
	}

	// Root endpoint
	r.GET("/", func(c *gin.Context) {
		c.JSON(200, gin.H{
			"message": "Notification Service API",
			"version": "1.0.0",
			"status":  "healthy",
			"endpoints": gin.H{
				"health":       "/api/v1/health",
				"send":         "/api/v1/notification/send",
				"send_instant": "/api/v1/notification/send/instant",
				"email_html":   "/api/v1/notification/email/html",
				"email_plain":  "/api/v1/notification/email/plain",
				"email_template": "/api/v1/notification/email/template",
				"sms":          "/api/v1/notification/sms",
				"whatsapp":     "/api/v1/notification/whatsapp",
				"push":         "/api/v1/notification/push",
				"stats":        "/api/v1/notification/stats",
			},
		})
	})

	// API routes
	api := r.Group("/api/v1")
	{
		// Health check (public, no auth required)
		api.GET("/health", notificationHandler.HealthCheck)
		
		// Contact form endpoint (public, no auth required for website submissions)
		api.POST("/notification/contact-form", notificationHandler.SubmitContactForm)
		
		// Main notification routes (require authentication)
		notificationGroup := api.Group("/notification")
		notificationGroup.Use(middleware.AuthMiddleware(cfg))
		{
			// Core notification endpoints
			notificationGroup.POST("/send", notificationHandler.SendNotification)
			notificationGroup.POST("/send/instant", notificationHandler.SendInstantNotification)
			notificationGroup.GET("/:id", notificationHandler.GetNotification)
			notificationGroup.GET("/status/:status", notificationHandler.GetNotificationsByStatus)
			notificationGroup.POST("/retry/failed", notificationHandler.RetryFailedNotifications)
			notificationGroup.GET("/stats", notificationHandler.GetNotificationStats)
			
			// New singular endpoints for backward compatibility
			notificationGroup.GET("", notificationHandler.GetNotifications)
			notificationGroup.PATCH("/:id/read", notificationHandler.MarkAsRead)
			notificationGroup.PATCH("/read-all", notificationHandler.MarkAllAsRead)
			notificationGroup.GET("/unread-count", notificationHandler.GetUnreadCount)
			notificationGroup.DELETE("/:id", notificationHandler.DeleteNotification)
			notificationGroup.DELETE("", notificationHandler.DeleteAllNotifications)
			
			// Specific notification type endpoints
			notificationGroup.POST("/email/html", notificationHandler.SendEmailHTML)
			notificationGroup.POST("/email/plain", notificationHandler.SendEmailPlain)
			notificationGroup.POST("/email/template", notificationHandler.SendEmailTemplate)
			notificationGroup.POST("/sms", notificationHandler.SendSMS)
			notificationGroup.POST("/whatsapp", notificationHandler.SendWhatsApp)
			notificationGroup.POST("/push", notificationHandler.SendPushNotification)

			// Advanced Push Notification endpoints (FCM)
			pushGroup := notificationGroup.Group("/push")
			{
				pushGroup.GET("/status", notificationHandler.GetPushStatus)
				pushGroup.POST("/topic", notificationHandler.SendPushToTopic)
				pushGroup.POST("/multicast", notificationHandler.SendPushMulticast)
				pushGroup.POST("/subscribe", notificationHandler.SubscribeToPushTopic)
				pushGroup.POST("/unsubscribe", notificationHandler.UnsubscribeFromPushTopic)
			}
		}

		// Pluralized notifications routes matching requirements
		notificationsGroup := api.Group("/notifications")
		notificationsGroup.Use(middleware.AuthMiddleware(cfg))
		{
			notificationsGroup.GET("", notificationHandler.GetNotifications)
			notificationsGroup.PATCH("/:id/read", notificationHandler.MarkAsRead)
			notificationsGroup.PATCH("/read-all", notificationHandler.MarkAllAsRead)
			notificationsGroup.GET("/unread-count", notificationHandler.GetUnreadCount)
			notificationsGroup.DELETE("/:id", notificationHandler.DeleteNotification)
			notificationsGroup.DELETE("", notificationHandler.DeleteAllNotifications)
		}
	}

	// Pluralized root routes (without /api/v1 prefix) for gateway compatibility
	rootNotificationsGroup := r.Group("/notifications")
	rootNotificationsGroup.Use(middleware.AuthMiddleware(cfg))
	{
		rootNotificationsGroup.GET("", notificationHandler.GetNotifications)
		rootNotificationsGroup.PATCH("/:id/read", notificationHandler.MarkAsRead)
		rootNotificationsGroup.PATCH("/read-all", notificationHandler.MarkAllAsRead)
		rootNotificationsGroup.GET("/unread-count", notificationHandler.GetUnreadCount)
		rootNotificationsGroup.DELETE("/:id", notificationHandler.DeleteNotification)
		rootNotificationsGroup.DELETE("", notificationHandler.DeleteAllNotifications)
	}

	// Singular root routes (without /api/v1 prefix) for gateway compatibility
	rootNotificationGroup := r.Group("/notification")
	rootNotificationGroup.Use(middleware.AuthMiddleware(cfg))
	{
		rootNotificationGroup.GET("", notificationHandler.GetNotifications)
		rootNotificationGroup.PATCH("/:id/read", notificationHandler.MarkAsRead)
		rootNotificationGroup.PATCH("/read-all", notificationHandler.MarkAllAsRead)
		rootNotificationGroup.GET("/unread-count", notificationHandler.GetUnreadCount)
		rootNotificationGroup.DELETE("/:id", notificationHandler.DeleteNotification)
		rootNotificationGroup.DELETE("", notificationHandler.DeleteAllNotifications)
		
		rootNotificationGroup.POST("/send", notificationHandler.SendNotification)
		rootNotificationGroup.POST("/send/instant", notificationHandler.SendInstantNotification)
		rootNotificationGroup.GET("/:id", notificationHandler.GetNotification)
	}

	return r
}
