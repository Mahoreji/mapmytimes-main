package notification

import (
	"context"
	"encoding/json"
	"fmt"
	"html/template"
	"notification-service/config"
	"notification-service/internal/app/email"
	"notification-service/internal/app/push"
	"notification-service/internal/db"
	"notification-service/internal/utils"
	"regexp"
	"sort"
	"strings"
	"time"

	"github.com/go-redis/redis/v8"
	"github.com/hibiken/asynq"
	"github.com/sirupsen/logrus"
	"github.com/twilio/twilio-go"
	openapi "github.com/twilio/twilio-go/rest/api/v2010"
)

// SubmitContactForm handles contact form submissions
func (s *Service) SubmitContactForm(req *ContactFormRequest, ipAddress, userAgent string) (*ContactFormResponse, error) {
	// Sanitize input
	req.Name = utils.SanitizePlainText(req.Name)
	req.Email = utils.SanitizePlainText(req.Email)
	req.Phone = utils.SanitizePlainText(req.Phone)
	if req.Subject != "" {
		req.Subject = utils.SanitizePlainText(req.Subject)
	}
	if req.Message != "" {
		req.Message = utils.SanitizePlainText(req.Message)
	}
	
	// Set default source if not provided
	if req.Source == "" {
		req.Source = "website_contact_form"
	}
	
	// Set default subject if not provided
	if req.Subject == "" {
		req.Subject = "Contact Form Submission"
	}
	
	// Prepare metadata
	if req.Metadata == nil {
		req.Metadata = make(map[string]string)
	}
	metadataBytes, _ := json.Marshal(req.Metadata)
	
	// Create contact form record
	contactForm := &db.ContactForm{
		Name:      req.Name,
		Email:     req.Email,
		Phone:     req.Phone,
		Subject:   req.Subject,
		Message:   req.Message,
		Source:    req.Source,
		Status:    "new",
		IPAddress: ipAddress,
		UserAgent: userAgent,
		Metadata:  string(metadataBytes),
	}
	
	// Save to database
	if err := s.repo.CreateContactForm(contactForm); err != nil {
		s.logger.Error("Failed to create contact form: ", err)
		return nil, fmt.Errorf("failed to save contact form")
	}
	
	s.logger.Info("Contact form submitted: ", contactForm.ID)
	
	// Send notification email to team (async)
	s.logger.Info("Starting goroutine to send contact form notification for ID: ", contactForm.ID)
	go func(cf *db.ContactForm) {
		defer func() {
			if r := recover(); r != nil {
				s.logger.Error("Panic in sendContactFormNotification goroutine: ", r)
			}
		}()
		// Add small delay to ensure database transaction is committed
		time.Sleep(100 * time.Millisecond)
		s.sendContactFormNotification(cf)
	}(contactForm)
	
	// Send auto-reply to customer if enabled
	if s.cfg.ContactFormAutoReply {
		s.logger.Info("Starting goroutine to send auto-reply for contact form ID: ", contactForm.ID)
		go func(cf *db.ContactForm) {
			defer func() {
				if r := recover(); r != nil {
					s.logger.Error("Panic in sendContactFormAutoReply goroutine: ", r)
				}
			}()
			// Add small delay to ensure database transaction is committed
			time.Sleep(100 * time.Millisecond)
			s.sendContactFormAutoReply(cf)
		}(contactForm)
	} else {
		s.logger.Info("Contact form auto-reply is disabled (CONTACT_FORM_AUTO_REPLY=false)")
	}
	
	return &ContactFormResponse{
		ID:      contactForm.ID,
		Message: "Thank you for contacting us! We will get back to you soon.",
		Status:  "submitted",
	}, nil
}

// sendContactFormNotification sends email notification to team
func (s *Service) sendContactFormNotification(cf *db.ContactForm) {
	defer func() {
		if r := recover(); r != nil {
			s.logger.Error("Panic in sendContactFormNotification: ", r)
		}
	}()
	
	recipientEmail := s.cfg.ContactFormRecipientEmail
	if recipientEmail == "" {
		recipientEmail = s.cfg.EmailUsername
	}
	
	if recipientEmail == "" {
		s.logger.Warn("No recipient email configured for contact form notifications")
		return
	}
	
	subject := fmt.Sprintf("New Contact Form Submission: %s", cf.Subject)
	submittedAt := time.Unix(cf.CreatedAt, 0).Format("January 2, 2006 at 3:04 PM")
	
	data := email.TemplateData{
		Subject:     cf.Subject,
		Title:       "New Contact Inquiry",
		Subtitle:    "You have received a new message from the website contact form.",
		StatusBadge: "NEW INQUIRY",
		Name:        cf.Name,
		Email:       cf.Email,
		Phone:       cf.Phone,
		Message:     cf.Message,
		Date:        submittedAt,
	}

	if err := s.emailService.SendTemplatedEmail(recipientEmail, subject, "contact_form_admin.html", data); err != nil {
		s.logger.Error("Failed to send contact form notification email: ", err)
	} else {
		s.logger.Info("✓ Contact form notification email sent successfully to: ", recipientEmail)
	}
}

// sendContactFormAutoReply sends auto-reply to customer
func (s *Service) sendContactFormAutoReply(contactForm *db.ContactForm) {
	subject := fmt.Sprintf("Map My Tour – Inquiry Received - %s", contactForm.Subject)
	submittedAt := time.Unix(contactForm.CreatedAt, 0).Format("January 2, 2006 at 3:04 PM")
	
	data := email.TemplateData{
		Subject:     "Inquiry Received",
		Title:       "Thanks for reaching out!",
		Subtitle:    fmt.Sprintf("Hello %s, we have received your inquiry and our team will get back to you shortly.", contactForm.Name),
		StatusBadge: "INQUIRY RECEIVED",
		Body:        template.HTML(contactForm.Message),
		Date:        submittedAt,
	}

	if err := s.emailService.SendTemplatedEmail(contactForm.Email, subject, "contact_form_reply.html", data); err != nil {
		s.logger.Error("Failed to send contact form auto-reply: ", err)
	} else {
		s.logger.Info("✓ Contact form auto-reply sent successfully to: ", contactForm.Email)
	}
}


type Service struct {
	repo         *Repository
	redisClient  *redis.Client // Kept for rate limiting
	asynqClient  *asynq.Client
	emailService *email.Service
	pushService  *push.Service
	twilioClient *twilio.RestClient
	cfg          *config.Config
	logger       *logrus.Logger
	enricher     *Enricher
}

func NewService(repo *Repository, redisClient *redis.Client, asynqClient *asynq.Client, emailService *email.Service, pushService *push.Service, twilioClient *twilio.RestClient, cfg *config.Config, logger *logrus.Logger) *Service {
	return &Service{
		repo:         repo,
		redisClient:  redisClient,
		asynqClient:  asynqClient,
		emailService: emailService,
		pushService:  pushService,
		twilioClient: twilioClient,
		cfg:          cfg,
		logger:       logger,
		enricher:     NewEnricher(cfg),
	}
}

func (s *Service) prePopulateNotification(n *db.Notification) {
	var metadataMap map[string]string
	if n.Metadata != "" {
		_ = json.Unmarshal([]byte(n.Metadata), &metadataMap)
	}
	if metadataMap == nil {
		metadataMap = make(map[string]string)
	}

	// Resolve the builder based on incoming Type (which could be the event type or channel)
	builder := GetBuilder(n.Type, metadataMap)

	// Set granular EventType
	if n.EventType == "" {
		n.EventType = builder.GetEventType()
	}
	if n.EventType == "" {
		n.EventType = strings.ToUpper(n.Type)
	}

	// Set service-based Type (e.g. TOUR, FLIGHT, HOTEL, SYSTEM, etc.)
	resolvedType := builder.GetType()
	if resolvedType != "" {
		n.Type = resolvedType
	}

	if n.Category == "" {
		n.Category = builder.GetCategory()
	}
	if n.Priority == "" {
		n.Priority = builder.GetPriority()
	}
	if n.DeliveryChannel == "" {
		n.DeliveryChannel = builder.GetDefaultChannel()
	}

	// Default DeliveryStatus to PENDING if not set
	if n.DeliveryStatus == "" {
		n.DeliveryStatus = "PENDING"
	}

	if n.Title == "" {
		n.Title = builder.BuildTitle(metadataMap)
	}
	if n.Message == "" {
		n.Message = builder.BuildMessage(metadataMap)
	}

	// Calculate ExpiresAt (default 24 hours for guest session, 30 days for others)
	if n.ExpiresAt == nil {
		var expires int64
		if IsGuestSession(n.Recipient, metadataMap) {
			expires = time.Now().Add(24 * time.Hour).Unix()
		} else {
			expires = time.Now().AddDate(0, 0, 30).Unix()
		}
		n.ExpiresAt = &expires
	}
	
	s.logger.Infof("Pre-populated notification: id=%s, type=%s, eventType=%s, category=%s, priority=%s, title=%q, deliveryChannel=%s",
		n.ID, n.Type, n.EventType, n.Category, n.Priority, n.Title, n.DeliveryChannel)
}

// IsGuestSession checks if a recipient represents a guest session ID rather than a registered user ID, email, or phone number.
func IsGuestSession(recipient string, metadata map[string]string) bool {
	if recipient == "" {
		return false
	}
	// If it contains '@', it's an email
	if strings.Contains(recipient, "@") {
		return false
	}
	// If it's a phone number (digits and optional +)
	phonePattern := regexp.MustCompile(`^\+?[0-9]+$`)
	if phonePattern.MatchString(recipient) {
		return false
	}
	// If there's a userId/user_id in metadata and it matches recipient, it's a registered user ID
	if uId, ok := metadata["userId"]; ok && uId == recipient {
		return false
	}
	if uId, ok := metadata["user_id"]; ok && uId == recipient {
		return false
	}
	
	// Otherwise, it is likely a guest sessionId or anonymous identifier
	return true
}

func (s *Service) SendNotification(req *NotificationRequest) (*NotificationResponse, error) {
	// Create notification record
	notification := req.ToDBModel()
	s.prePopulateNotification(notification)
	
	if err := s.repo.Create(notification); err != nil {
		s.logger.Error("Failed to create notification: ", err)
		return nil, fmt.Errorf("failed to create notification")
	}
	s.invalidateCache(notification.Recipient)
	s.logger.WithFields(logrus.Fields{
		"action":         "notification_created",
		"notificationId": notification.ID,
		"userId":         notification.Recipient,
	}).Info("Notification created")

	// Queue notification for async processing
	if err := s.queueNotification(notification); err != nil {
		s.logger.Error("Failed to queue notification: ", err)
		return nil, fmt.Errorf("failed to queue notification")
	}

	s.logger.Info("Notification queued successfully: ", notification.ID)

	return &NotificationResponse{
		ID:      notification.ID,
		Status:  "queued",
		Message: "Notification queued successfully",
	}, nil
}

func (s *Service) SendInstantNotification(req *NotificationRequest) (*NotificationResponse, error) {
	// Create notification record
	notification := req.ToDBModel()
	notification.Status = utils.StatusPending
	s.prePopulateNotification(notification)
	
	if err := s.repo.Create(notification); err != nil {
		s.logger.Error("Failed to create instant notification: ", err)
		return nil, fmt.Errorf("failed to create instant notification")
	}
	s.invalidateCache(notification.Recipient)
	s.logger.WithFields(logrus.Fields{
		"action":         "notification_created",
		"notificationId": notification.ID,
		"userId":         notification.Recipient,
	}).Info("Notification created (instant)")

	// Send immediately without queuing - pass attachments directly since they aren't in DB yet
	go s.sendNotificationWithAttachments(notification, req.Attachments)

	s.logger.Info("Instant notification triggered: ", notification.ID)

	return &NotificationResponse{
		ID:      notification.ID,
		Status:  "processing",
		Message: "Instant notification triggered successfully",
	}, nil
}

func (s *Service) queueNotification(notification *db.Notification) error {
	if s.asynqClient == nil {
		return fmt.Errorf("asynq client not available, cannot enqueue notification")
	}
	
	task, err := NewNotificationDeliveryTask(notification.ID)
	if err != nil {
		return err
	}

	var opts []asynq.Option
	if notification.ScheduledAt != nil && *notification.ScheduledAt > time.Now().UnixMilli() {
		// Schedule for later
		scheduledTime := time.UnixMilli(*notification.ScheduledAt)
		opts = append(opts, asynq.ProcessAt(scheduledTime))
	}
	
	// Set max retries from config
	opts = append(opts, asynq.MaxRetry(s.cfg.MaxRetryAttempts))

	info, err := s.asynqClient.Enqueue(task, opts...)
	if err != nil {
		return err
	}
	
	s.logger.Infof("Enqueued notification task: id=%s queue=%s", info.ID, info.Queue)
	return nil
}

// ProcessNotifications is deprecated. Worker runs in a separate asynq Server.
func (s *Service) ProcessNotifications() {
	s.logger.Info("ProcessNotifications called but queue is now managed by Asynq Server")
}

func (s *Service) sendNotification(notification *db.Notification) {
	s.sendNotificationWithAttachments(notification, nil)
}

func (s *Service) sendNotificationWithAttachments(notification *db.Notification, attachments []Attachment) {
	s.logger.Info("Instant processing notification: ", notification.ID)

	// Since this is instant, we just do it once. If it fails, Asynq would handle retries,
	// but this is called directly by SendInstantNotification.
	err := s.sendNotificationOnceWithAttachments(notification, attachments)
	if err != nil {
		s.logger.Error("Failed to send instant notification: ", err)
		if updateErr := s.repo.UpdateStatus(notification.ID, utils.StatusFailed); updateErr != nil {
			s.logger.Error("Failed to update notification status: ", updateErr)
		}
	} else {
		if updateErr := s.repo.UpdateStatus(notification.ID, utils.StatusSent); updateErr != nil {
			s.logger.Error("Failed to update notification status: ", updateErr)
		}
	}
}

func (s *Service) sendNotificationOnce(notification *db.Notification) error {
	return s.sendNotificationOnceWithAttachments(notification, nil)
}

func (s *Service) sendNotificationOnceWithAttachments(notification *db.Notification, attachments []Attachment) error {
	var err error
	
	channel := strings.ToUpper(notification.DeliveryChannel)
	if channel == "" {
		channel = strings.ToUpper(notification.Type)
	}

	switch channel {
	case "EMAIL", "EMAIL_HTML", "EMAIL_TEMPLATE":
		var metadata map[string]string
		if notification.Metadata != "" {
			_ = json.Unmarshal([]byte(notification.Metadata), &metadata)
		}
		if metadata != nil && (metadata["template_name"] != "" || metadata["template"] != "") {
			err = s.sendTemplatedEmailAsync(notification)
		} else if len(attachments) > 0 || strings.Contains(strings.ToLower(notification.Body), "<html") || channel == "EMAIL_HTML" {
			err = s.sendHTMLEmailWithAttachments(notification, attachments)
		} else {
			err = s.sendEmail(notification)
		}
	case "SMS":
		err = s.sendSMS(notification.Recipient, notification.Body)
	case "WHATSAPP":
		err = s.sendWhatsApp(notification.Recipient, notification.Body)
	case "PUSH":
		err = s.pushService.SendPush(notification.Recipient, notification.Subject, notification.Body)
	case "INSTANT":
		err = s.sendInstantMessage(notification)
	default:
		// Fallback to old types if we couldn't match a standard channel string
		switch notification.Type {
		case utils.NotificationTypeEmail:
			err = s.sendEmail(notification)
		case utils.NotificationTypeEmailHTML:
			err = s.sendHTMLEmailWithAttachments(notification, attachments)
		case utils.NotificationTypeEmailTemplate:
			err = s.sendTemplatedEmailAsync(notification)
		case utils.NotificationTypeSMS:
			err = s.sendSMS(notification.Recipient, notification.Body)
		case utils.NotificationTypeWhatsApp:
			err = s.sendWhatsApp(notification.Recipient, notification.Body)
		case utils.NotificationTypePush:
			err = s.pushService.SendPush(notification.Recipient, notification.Subject, notification.Body)
		case utils.NotificationTypeInstant:
			err = s.sendInstantMessage(notification)
		default:
			err = fmt.Errorf("unknown delivery channel: %s (type: %s)", channel, notification.Type)
		}
	}
	return err
}

func (s *Service) sendTemplatedEmailAsync(notification *db.Notification) error {
	var metadata map[string]string
	if err := json.Unmarshal([]byte(notification.Metadata), &metadata); err != nil {
		return fmt.Errorf("failed to parse metadata for templated email: %w", err)
	}

	templateName := metadata["template_name"]
	if templateName == "" {
		templateName = metadata["template"]
	}
	if templateName == "" {
		return fmt.Errorf("template_name missing in metadata")
	}

	data := email.TemplateData{
		Subject:     notification.Subject,
		Title:       notification.Subject,
		BookingRef: func() string {
			if v, ok := metadata["booking_ref"]; ok && v != "" {
				return v
			}
			return metadata["bookingReference"]
		}(),
		ServiceName: func() string {
			if v, ok := metadata["service_name"]; ok && v != "" {
				return v
			}
			return metadata["serviceName"]
		}(),
		BookingDate: func() string {
			if v, ok := metadata["booking_date"]; ok && v != "" {
				return v
			}
			return metadata["bookingDate"]
		}(),
		Amount: func() string {
			if v, ok := metadata["amount"]; ok && v != "" {
				return v
			}
			return metadata["totalAmount"]
		}(),
		Currency:    metadata["currency"],
		ActionUrl: func() string {
			if v, ok := metadata["action_url"]; ok && v != "" {
				return v
			}
			return metadata["actionUrl"]
		}(),
		Name:        func() string {
			if n, ok := metadata["name"]; ok && n != "" {
				return n
			}
			if n, ok := metadata["userName"]; ok && n != "" {
				return n
			}
			if n, ok := metadata["firstName"]; ok && n != "" {
				return n
			}
			return metadata["first_name"]
		}(),
		Email:       metadata["email"],
		Phone:       metadata["phone"],
		Message:     metadata["message"],
		Date:        metadata["date"],
		Subtitle: func() string {
			if v, ok := metadata["subtitle"]; ok && v != "" {
				return v
			}
			return metadata["subTitle"]
		}(),
		AlertType: func() string {
			if v, ok := metadata["alert_type"]; ok && v != "" {
				return v
			}
			return metadata["alertType"]
		}(),
		Description: func() string {
			if v, ok := metadata["description"]; ok && v != "" {
				return v
			}
			return metadata["desc"]
		}(),
		Timestamp: func() string {
			if v, ok := metadata["timestamp"]; ok && v != "" {
				return v
			}
			return metadata["time"]
		}(),
	}
	
	if notification.Body != "" && !strings.HasPrefix(notification.Body, "Templated Email:") {
		data.Body = template.HTML(notification.Body)
	} else {
		if b, ok := metadata["body"]; ok && b != "" {
			data.Body = template.HTML(b)
		} else if b, ok := metadata["verificationCode"]; ok && b != "" {
			data.Body = template.HTML(b)
		} else if b, ok := metadata["otp"]; ok && b != "" {
			data.Body = template.HTML(b)
		} else if b, ok := metadata["code"]; ok && b != "" {
			data.Body = template.HTML(b)
		} else {
			if v, ok := metadata["verification_code"]; ok && v != "" {
				data.Body = template.HTML(v)
			} else {
				data.Body = template.HTML(metadata["verificationCode"])
			}
		}
	}

	return s.emailService.SendTemplatedEmail(notification.Recipient, notification.Subject, templateName, data)
}

func (s *Service) sendEmail(notification *db.Notification) error {
	return s.emailService.SendPlainEmail(notification.Recipient, notification.Subject, notification.Body)
}

func (s *Service) sendHTMLEmail(notification *db.Notification) error {
	return s.sendHTMLEmailWithAttachments(notification, nil)
}

func (s *Service) sendHTMLEmailWithAttachments(notification *db.Notification, attachments []Attachment) error {
	var emailAttachments []email.Attachment
	for _, att := range attachments {
		emailAttachments = append(emailAttachments, email.Attachment{
			Content:     att.Content,
			Filename:    att.Filename,
			ContentType: att.ContentType,
		})
	}
	return s.emailService.SendHTMLEmailWithAttachments(notification.Recipient, notification.Subject, notification.Body, emailAttachments)
}

func (s *Service) sendSMS(recipient, body string) error {
	if s.cfg.TwilioPhoneNumber == "" {
		return fmt.Errorf("TWILIO_PHONE_NUMBER not configured")
	}
	
	params := &openapi.CreateMessageParams{}
	params.SetTo(recipient)
	params.SetFrom(s.cfg.TwilioPhoneNumber)
	params.SetBody(body)

	_, err := s.twilioClient.Api.CreateMessage(params)
	return err
}

func (s *Service) sendWhatsApp(recipient, body string) error {
	if s.cfg.TwilioWhatsAppNumber == "" {
		return fmt.Errorf("TWILIO_WHATSAPP_NUMBER not configured")
	}
	
	params := &openapi.CreateMessageParams{}
	params.SetTo("whatsapp:" + recipient)
	params.SetFrom("whatsapp:" + s.cfg.TwilioWhatsAppNumber)
	params.SetBody(body)

	_, err := s.twilioClient.Api.CreateMessage(params)
	return err
}

func (s *Service) sendInstantMessage(notification *db.Notification) error {
	// For instant messages, we can use multiple channels
	switch notification.Recipient {
	case "email":
		return s.sendEmail(notification)
	case "sms":
		return s.sendSMS(notification.Recipient, notification.Body)
	default:
		// Default to email for instant messages
		return s.sendEmail(notification)
	}
}

func (s *Service) GetNotification(id string) (*db.Notification, error) {
	return s.repo.GetByID(id)
}

func (s *Service) GetNotificationsByStatus(status string, limit int) ([]*db.Notification, error) {
	return s.repo.GetByStatus(status, limit)
}

func (s *Service) RetryFailedNotifications() error {
	failedNotifications, err := s.repo.GetByStatus(utils.StatusFailed, 100)
	if err != nil {
		return err
	}

	for _, notification := range failedNotifications {
		// Reset status to pending
		notification.Status = utils.StatusPending
		if err := s.repo.Update(notification); err != nil {
			s.logger.Error("Failed to update notification for retry: ", err)
			continue
		}

		// Queue for retry
		if err := s.queueNotification(notification); err != nil {
			s.logger.Error("Failed to queue notification for retry: ", err)
			continue
		}

		s.logger.Info("Notification queued for retry: ", notification.ID)
	}

	return nil
}

func (s *Service) GetNotificationStats() (map[string]interface{}, error) {
	stats := make(map[string]interface{})
	
	// Count by status
	pendingCount, _ := s.repo.CountByStatus(utils.StatusPending)
	sentCount, _ := s.repo.CountByStatus(utils.StatusSent)
	failedCount, _ := s.repo.CountByStatus(utils.StatusFailed)
	
	stats["pending"] = pendingCount
	stats["sent"] = sentCount
	stats["failed"] = failedCount
	stats["total"] = pendingCount + sentCount + failedCount
	
	return stats, nil
}

// SendPushToTopic sends a push notification to all subscribers of an FCM topic
func (s *Service) SendPushToTopic(topic, title, body string, data map[string]string) error {
	if err := s.pushService.SendToTopic(topic, title, body, data); err != nil {
		s.logger.Errorf("Failed to send push to topic '%s': %v", topic, err)
		return err
	}
	return nil
}

// SendPushMulticast sends push notifications to a list of device tokens
func (s *Service) SendPushMulticast(tokens []string, title, body string, data map[string]string) (map[string]interface{}, error) {
	result, err := s.pushService.SendMulticast(tokens, title, body, data)
	if err != nil {
		s.logger.Error("Failed to send multicast push: ", err)
		return nil, err
	}
	return map[string]interface{}{
		"successCount": result.SuccessCount,
		"failureCount": result.FailureCount,
		"failedTokens": result.FailedTokens,
	}, nil
}

// SubscribeToPushTopic subscribes device tokens to an FCM topic
func (s *Service) SubscribeToPushTopic(tokens []string, topic string) error {
	return s.pushService.SubscribeToTopic(tokens, topic)
}

// UnsubscribeFromPushTopic unsubscribes device tokens from an FCM topic
func (s *Service) UnsubscribeFromPushTopic(tokens []string, topic string) error {
	return s.pushService.UnsubscribeFromTopic(tokens, topic)
}

// GetPushStatus returns the current state of the FCM push service
func (s *Service) GetPushStatus() map[string]interface{} {
	return map[string]interface{}{
		"fcmEnabled": s.pushService.IsEnabled(),
		"service":    "notification-service",
		"transport":  "FCM (Firebase Cloud Messaging)",
	}
}

// SendTemplatedEmail sends an email using a template
func (s *Service) SendTemplatedEmail(to, subject, templateName string, data email.TemplateData) error {
	return s.emailService.SendTemplatedEmail(to, subject, templateName, data)
}

// GetNotifications retrieves filtered and paginated history, enriching bookings and payments concurrently.
// Uses Redis response caching if available.
func (s *Service) GetNotifications(recipients []string, category, notifType string, read *bool, page, limit int) ([]*NotificationResponseDto, int64, error) {
	ctx := context.Background()

	// Build cache key based on recipients and filter parameters
	readVal := "nil"
	if read != nil {
		if *read {
			readVal = "true"
		} else {
			readVal = "false"
		}
	}
	
	// Sort recipients for deterministic cache key
	sortedRecipients := make([]string, len(recipients))
	copy(sortedRecipients, recipients)
	sort.Strings(sortedRecipients)
	recipientsStr := strings.Join(sortedRecipients, ",")
	
	cacheKey := fmt.Sprintf("notifications:%s:page:%d:limit:%d:cat:%s:type:%s:read:%s", recipientsStr, page, limit, category, notifType, readVal)

	// Try to get from Redis cache
	if s.redisClient != nil {
		val, err := s.redisClient.Get(ctx, cacheKey).Result()
		if err == nil {
			type CachePayload struct {
				Dtos  []*NotificationResponseDto `json:"dtos"`
				Total int64                      `json:"total"`
			}
			var payload CachePayload
			if jsonErr := json.Unmarshal([]byte(val), &payload); jsonErr == nil {
				s.logger.WithField("cacheKey", cacheKey).Info("Notification list cache hit")
				return payload.Dtos, payload.Total, nil
			}
		}
	}

	// Cache miss -> Fetch from database
	notifications, total, err := s.repo.GetNotificationsFiltered(recipients, category, notifType, read, page, limit)
	if err != nil {
		return nil, 0, err
	}

	// Concurrently enrich DTOs to maximize performance and avoid N+1 slow REST calls
	dtos := make([]*NotificationResponseDto, len(notifications))
	if len(notifications) == 0 {
		return dtos, total, nil
	}

	type resultChan struct {
		index int
		dto   *NotificationResponseDto
	}
	ch := make(chan resultChan, len(notifications))

	for i, n := range notifications {
		go func(index int, notif *db.Notification) {
			dto, metadataMap := s.enricher.MapToDto(notif)
			s.enricher.Enrich(dto, metadataMap)
			ch <- resultChan{index: index, dto: dto}
		}(i, n)
	}

	for range notifications {
		res := <-ch
		dtos[res.index] = res.dto
	}

	// Save to Redis cache
	if s.redisClient != nil {
		type CachePayload struct {
			Dtos  []*NotificationResponseDto `json:"dtos"`
			Total int64                      `json:"total"`
		}
		payload := CachePayload{Dtos: dtos, Total: total}
		if bytes, jsonErr := json.Marshal(payload); jsonErr == nil {
			s.redisClient.Set(ctx, cacheKey, string(bytes), 24*time.Hour)
		}
	}

	return dtos, total, nil
}

// MarkAsRead marks a single notification as read and invalidates cache.
func (s *Service) MarkAsRead(id string) error {
	notif, err := s.repo.GetByID(id)
	if err != nil {
		return err
	}
	now := time.Now().Unix()
	err = s.repo.MarkAsRead(id, now)
	if err == nil {
		s.invalidateCache(notif.Recipient)
		s.logger.WithFields(logrus.Fields{
			"action":         "notification_marked_read",
			"notificationId": id,
			"userId":         notif.Recipient,
		}).Info("Notification marked as read")
	}
	return err
}

// MarkAllAsRead marks all notifications for a recipient as read and invalidates cache.
func (s *Service) MarkAllAsRead(recipients []string) error {
	now := time.Now().Unix()
	err := s.repo.MarkAllAsRead(recipients, now)
	if err == nil {
		for _, r := range recipients {
			s.invalidateCache(r)
		}
		s.logger.WithFields(logrus.Fields{
			"action": "all_notifications_marked_read",
			"userId": strings.Join(recipients, ","),
		}).Info("All notifications marked as read")
	}
	return err
}

// GetUnreadCount retrieves the number of unread notifications for a recipient (cached).
func (s *Service) GetUnreadCount(recipients []string) (int64, error) {
	ctx := context.Background()
	
	sortedRecipients := make([]string, len(recipients))
	copy(sortedRecipients, recipients)
	sort.Strings(sortedRecipients)
	recipientsStr := strings.Join(sortedRecipients, ",")
	
	if s.redisClient != nil {
		val, err := s.redisClient.Get(ctx, "notifications-unread:"+recipientsStr).Int64()
		if err == nil {
			return val, nil
		}
	}

	count, err := s.repo.GetUnreadCount(recipients)
	if err != nil {
		return 0, err
	}

	if s.redisClient != nil {
		s.redisClient.Set(ctx, "notifications-unread:"+recipientsStr, count, 24*time.Hour)
	}

	return count, nil
}

// DeleteNotification clears/deletes a specific notification and invalidates cache.
func (s *Service) DeleteNotification(id string) error {
	notif, err := s.repo.GetByID(id)
	if err != nil {
		return err
	}
	err = s.repo.Delete(id)
	if err == nil {
		s.invalidateCache(notif.Recipient)
		s.logger.WithFields(logrus.Fields{
			"action":         "notification_deleted",
			"notificationId": id,
			"userId":         notif.Recipient,
		}).Info("Notification deleted")
	}
	return err
}

// DeleteAllNotifications clears/deletes all notifications for a recipient and invalidates cache.
func (s *Service) DeleteAllNotifications(recipients []string) error {
	err := s.repo.DeleteAll(recipients)
	if err == nil {
		for _, r := range recipients {
			s.invalidateCache(r)
		}
		s.logger.WithFields(logrus.Fields{
			"action": "all_notifications_deleted",
			"userId": strings.Join(recipients, ","),
		}).Info("All notifications deleted")
	}
	return err
}

// GetNotificationCounts returns total, unread, and read notification counts for a recipient (cached).
func (s *Service) GetNotificationCounts(recipients []string) (total int64, unread int64, read int64, err error) {
	ctx := context.Background()

	sortedRecipients := make([]string, len(recipients))
	copy(sortedRecipients, recipients)
	sort.Strings(sortedRecipients)
	recipientsStr := strings.Join(sortedRecipients, ",")

	var cacheTotalFound, cacheUnreadFound bool
	if s.redisClient != nil {
		valTotal, errTotal := s.redisClient.Get(ctx, "notifications-count:"+recipientsStr).Int64()
		if errTotal == nil {
			total = valTotal
			cacheTotalFound = true
		}

		valUnread, errUnread := s.redisClient.Get(ctx, "notifications-unread:"+recipientsStr).Int64()
		if errUnread == nil {
			unread = valUnread
			cacheUnreadFound = true
		}
	}

	if !cacheTotalFound || !cacheUnreadFound {
		t, u, r, dbErr := s.repo.GetNotificationCounts(recipients)
		if dbErr != nil {
			return 0, 0, 0, dbErr
		}

		total = t
		unread = u
		read = r

		if s.redisClient != nil {
			s.redisClient.Set(ctx, "notifications-count:"+recipientsStr, total, 24*time.Hour)
			s.redisClient.Set(ctx, "notifications-unread:"+recipientsStr, unread, 24*time.Hour)
		}
	} else {
		read = total - unread
	}

	return total, unread, read, nil
}

// invalidateCache clears all cached lists and counts associated with a recipient.
func (s *Service) invalidateCache(recipient string) {
	if s.redisClient == nil {
		return
	}
	ctx := context.Background()

	pattern := "notifications:*" + recipient + "*"
	keys, err := s.redisClient.Keys(ctx, pattern).Result()
	if err == nil && len(keys) > 0 {
		s.redisClient.Del(ctx, keys...)
	}

	s.redisClient.Del(ctx, "notifications-count:"+recipient)
	s.redisClient.Del(ctx, "notifications-unread:"+recipient)

	s.logger.WithFields(logrus.Fields{
		"action":    "cache_invalidated",
		"recipient": recipient,
	}).Info("Notification cache invalidated")
}
