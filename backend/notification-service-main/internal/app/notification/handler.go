package notification

import (
	"net/http"
	"notification-service/internal/utils"
	"strconv"

	"github.com/gin-gonic/gin"
	"github.com/sirupsen/logrus"
)

type Handler struct {
	service *Service
	logger  *logrus.Logger
}

func NewHandler(service *Service, logger *logrus.Logger) *Handler {
	return &Handler{
		service: service,
		logger:  logger,
	}
}

// SendNotification - Async notification (returns 200 immediately, processes in background)
func (h *Handler) SendNotification(c *gin.Context) {
	var req NotificationRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		h.logger.Error("Invalid request body: ", err)
		utils.BadRequestResponse(c, "Invalid request body", []string{err.Error()})
		return
	}

	// Sanitize content based on type
	if req.Type == utils.NotificationTypeEmailHTML {
		req.Body = utils.SanitizeHTML(req.Body)
	} else {
		req.Body = utils.SanitizePlainText(req.Body)
	}
	req.Subject = utils.SanitizePlainText(req.Subject)

	// Validate request
	if errors := ValidateNotificationRequest(&req); len(errors) > 0 {
		utils.BadRequestResponse(c, "Validation failed", errors)
		return
	}

	response, err := h.service.SendNotification(&req)
	if err != nil {
		h.logger.Error("Failed to send notification: ", err)
		utils.InternalErrorResponse(c, "Failed to send notification")
		return
	}

	utils.SuccessResponse(c, http.StatusOK, "Notification sent successfully", response)
}

// SendInstantNotification - Immediate notification processing
func (h *Handler) SendInstantNotification(c *gin.Context) {
	var req NotificationRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		h.logger.Error("Invalid request body: ", err)
		utils.BadRequestResponse(c, "Invalid request body", []string{err.Error()})
		return
	}

	// Sanitize content based on type
	if req.Type == utils.NotificationTypeEmailHTML {
		req.Body = utils.SanitizeHTML(req.Body)
	} else {
		req.Body = utils.SanitizePlainText(req.Body)
	}
	req.Subject = utils.SanitizePlainText(req.Subject)

	// Validate request
	if errors := ValidateNotificationRequest(&req); len(errors) > 0 {
		utils.BadRequestResponse(c, "Validation failed", errors)
		return
	}

	response, err := h.service.SendInstantNotification(&req)
	if err != nil {
		h.logger.Error("Failed to send instant notification: ", err)
		utils.InternalErrorResponse(c, "Failed to send instant notification")
		return
	}

	utils.SuccessResponse(c, http.StatusOK, "Instant notification triggered successfully", response)
}

// GetNotification - Get notification by ID
func (h *Handler) GetNotification(c *gin.Context) {
	id := c.Param("id")
	if id == "" {
		utils.BadRequestResponse(c, "Notification ID is required", nil)
		return
	}

	notification, err := h.service.GetNotification(id)
	if err != nil {
		h.logger.Error("Failed to get notification: ", err)
		utils.NotFoundResponse(c, "Notification not found")
		return
	}

	// Convert to detailed response
	detailedResponse := ToDetailedResponse(notification)
	utils.SuccessResponse(c, http.StatusOK, "Notification retrieved successfully", detailedResponse)
}

// GetNotificationsByStatus - Get notifications by status with pagination
func (h *Handler) GetNotificationsByStatus(c *gin.Context) {
	status := c.Param("status")
	if status == "" {
		utils.BadRequestResponse(c, "Status is required", nil)
		return
	}

	limitStr := c.DefaultQuery("limit", "10")
	limit, err := strconv.Atoi(limitStr)
	if err != nil || limit <= 0 {
		limit = 10
	}

	notifications, err := h.service.GetNotificationsByStatus(status, limit)
	if err != nil {
		h.logger.Error("Failed to get notifications by status: ", err)
		utils.InternalErrorResponse(c, "Failed to get notifications")
		return
	}

	// Convert to detailed responses
	detailedResponses := ToDetailedResponseList(notifications)

	utils.SuccessResponse(c, http.StatusOK, "Notifications retrieved successfully", map[string]interface{}{
		"notifications": detailedResponses,
		"count":         len(notifications),
		"status":        status,
	})
}

// RetryFailedNotifications - Retry all failed notifications
func (h *Handler) RetryFailedNotifications(c *gin.Context) {
	err := h.service.RetryFailedNotifications()
	if err != nil {
		h.logger.Error("Failed to retry notifications: ", err)
		utils.InternalErrorResponse(c, "Failed to retry notifications")
		return
	}

	utils.SuccessResponse(c, http.StatusOK, "Failed notifications queued for retry successfully", nil)
}

// GetNotificationStats - Get notification statistics
func (h *Handler) GetNotificationStats(c *gin.Context) {
	stats, err := h.service.GetNotificationStats()
	if err != nil {
		h.logger.Error("Failed to get notification stats: ", err)
		utils.InternalErrorResponse(c, "Failed to get notification stats")
		return
	}

	utils.SuccessResponse(c, http.StatusOK, "Notification stats retrieved successfully", stats)
}

// HealthCheck - Service health check
func (h *Handler) HealthCheck(c *gin.Context) {
	utils.SuccessResponse(c, http.StatusOK, "Notification service is healthy", map[string]string{
		"status":  "healthy",
		"service": "notification-service",
		"version": "1.0.0",
	})
}

// SendEmailHTML - Send HTML email specifically
func (h *Handler) SendEmailHTML(c *gin.Context) {
	var req NotificationRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		h.logger.Error("Invalid request body: ", err)
		utils.BadRequestResponse(c, "Invalid request body", []string{err.Error()})
		return
	}

	// Automatically set type to HTML email
	req.Type = utils.NotificationTypeEmailHTML

	// Sanitize HTML content
	req.Body = utils.SanitizeHTML(req.Body)
	req.Subject = utils.SanitizePlainText(req.Subject)

	// Validate request
	if errors := ValidateNotificationRequest(&req); len(errors) > 0 {
		utils.BadRequestResponse(c, "Validation failed", errors)
		return
	}

	response, err := h.service.SendNotification(&req)
	if err != nil {
		h.logger.Error("Failed to send HTML email: ", err)
		utils.InternalErrorResponse(c, "Failed to send HTML email")
		return
	}

	utils.SuccessResponse(c, http.StatusOK, "HTML email sent successfully", response)
}

// SendEmailPlain - Send plain text email specifically
func (h *Handler) SendEmailPlain(c *gin.Context) {
	var req NotificationRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		h.logger.Error("Invalid request body: ", err)
		utils.BadRequestResponse(c, "Invalid request body", []string{err.Error()})
		return
	}

	// Automatically set type to plain email
	req.Type = utils.NotificationTypeEmail

	// Sanitize plain text content
	req.Body = utils.SanitizePlainText(req.Body)
	req.Subject = utils.SanitizePlainText(req.Subject)

	// Validate request
	if errors := ValidateNotificationRequest(&req); len(errors) > 0 {
		utils.BadRequestResponse(c, "Validation failed", errors)
		return
	}

	response, err := h.service.SendNotification(&req)
	if err != nil {
		h.logger.Error("Failed to send plain email: ", err)
		utils.InternalErrorResponse(c, "Failed to send plain email")
		return
	}

	utils.SuccessResponse(c, http.StatusOK, "Plain email sent successfully", response)
}

// SendSMS - Send SMS specifically
func (h *Handler) SendSMS(c *gin.Context) {
	var req NotificationRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		h.logger.Error("Invalid request body: ", err)
		utils.BadRequestResponse(c, "Invalid request body", []string{err.Error()})
		return
	}

	// Automatically set type to SMS
	req.Type = utils.NotificationTypeSMS

	// Validate request
	if errors := ValidateNotificationRequest(&req); len(errors) > 0 {
		utils.BadRequestResponse(c, "Validation failed", errors)
		return
	}

	response, err := h.service.SendNotification(&req)
	if err != nil {
		h.logger.Error("Failed to send SMS: ", err)
		utils.InternalErrorResponse(c, "Failed to send SMS")
		return
	}

	utils.SuccessResponse(c, http.StatusOK, "SMS sent successfully", response)
}

// SendWhatsApp - Send WhatsApp message specifically
func (h *Handler) SendWhatsApp(c *gin.Context) {
	var req NotificationRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		h.logger.Error("Invalid request body: ", err)
		utils.BadRequestResponse(c, "Invalid request body", []string{err.Error()})
		return
	}

	// Automatically set type to WhatsApp
	req.Type = utils.NotificationTypeWhatsApp

	// Validate request
	if errors := ValidateNotificationRequest(&req); len(errors) > 0 {
		utils.BadRequestResponse(c, "Validation failed", errors)
		return
	}

	response, err := h.service.SendNotification(&req)
	if err != nil {
		h.logger.Error("Failed to send WhatsApp message: ", err)
		utils.InternalErrorResponse(c, "Failed to send WhatsApp message")
		return
	}

	utils.SuccessResponse(c, http.StatusOK, "WhatsApp message sent successfully", response)
}

// SendPushNotification - Send push notification specifically
func (h *Handler) SendPushNotification(c *gin.Context) {
	var req NotificationRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		h.logger.Error("Invalid request body: ", err)
		utils.BadRequestResponse(c, "Invalid request body", []string{err.Error()})
		return
	}

	// Automatically set type to Push
	req.Type = utils.NotificationTypePush

	// Validate request
	if errors := ValidateNotificationRequest(&req); len(errors) > 0 {
		utils.BadRequestResponse(c, "Validation failed", errors)
		return
	}

	response, err := h.service.SendNotification(&req)
	if err != nil {
		h.logger.Error("Failed to send push notification: ", err)
		utils.InternalErrorResponse(c, "Failed to send push notification")
		return
	}

	utils.SuccessResponse(c, http.StatusOK, "Push notification sent successfully", response)
}

// SubmitContactForm - Handle contact form submissions from website
func (h *Handler) SubmitContactForm(c *gin.Context) {
	var req ContactFormRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		h.logger.Error("Invalid contact form request: ", err)
		utils.BadRequestResponse(c, "Invalid request body", []string{err.Error()})
		return
	}

	// Get client IP and user agent
	ipAddress := c.ClientIP()
	userAgent := c.GetHeader("User-Agent")

	// Submit contact form
	response, err := h.service.SubmitContactForm(&req, ipAddress, userAgent)
	if err != nil {
		h.logger.Error("Failed to submit contact form: ", err)
		utils.InternalErrorResponse(c, "Failed to submit contact form")
		return
	}

	utils.SuccessResponse(c, http.StatusOK, "Contact form submitted successfully", response)
}

// SendPushToTopic - Send a push notification to an FCM topic
func (h *Handler) SendPushToTopic(c *gin.Context) {
	var req struct {
		Topic string            `json:"topic" binding:"required"`
		Title string            `json:"title" binding:"required"`
		Body  string            `json:"body" binding:"required"`
		Data  map[string]string `json:"data,omitempty"`
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		utils.BadRequestResponse(c, "Invalid request body", []string{err.Error()})
		return
	}

	if err := h.service.SendPushToTopic(req.Topic, req.Title, req.Body, req.Data); err != nil {
		h.logger.Error("Failed to send topic push: ", err)
		utils.InternalErrorResponse(c, "Failed to send push to topic")
		return
	}

	utils.SuccessResponse(c, http.StatusOK, "Topic push notification sent successfully", map[string]string{"topic": req.Topic})
}

// SendPushMulticast - Send a push notification to multiple device tokens
func (h *Handler) SendPushMulticast(c *gin.Context) {
	var req struct {
		Tokens []string          `json:"tokens" binding:"required,min=1"`
		Title  string            `json:"title" binding:"required"`
		Body   string            `json:"body" binding:"required"`
		Data   map[string]string `json:"data,omitempty"`
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		utils.BadRequestResponse(c, "Invalid request body", []string{err.Error()})
		return
	}

	result, err := h.service.SendPushMulticast(req.Tokens, req.Title, req.Body, req.Data)
	if err != nil {
		h.logger.Error("Failed to send multicast push: ", err)
		utils.InternalErrorResponse(c, "Failed to send multicast push notification")
		return
	}

	utils.SuccessResponse(c, http.StatusOK, "Multicast push notification processed", result)
}

// SubscribeToPushTopic - Subscribe device tokens to a topic
func (h *Handler) SubscribeToPushTopic(c *gin.Context) {
	var req struct {
		Tokens []string `json:"tokens" binding:"required,min=1"`
		Topic  string   `json:"topic" binding:"required"`
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		utils.BadRequestResponse(c, "Invalid request body", []string{err.Error()})
		return
	}

	if err := h.service.SubscribeToPushTopic(req.Tokens, req.Topic); err != nil {
		h.logger.Error("Failed to subscribe to topic: ", err)
		utils.InternalErrorResponse(c, "Failed to subscribe to push topic")
		return
	}

	utils.SuccessResponse(c, http.StatusOK, "Subscribed to topic successfully", map[string]interface{}{
		"topic":       req.Topic,
		"tokenCount":  len(req.Tokens),
	})
}

// UnsubscribeFromPushTopic - Unsubscribe device tokens from a topic
func (h *Handler) UnsubscribeFromPushTopic(c *gin.Context) {
	var req struct {
		Tokens []string `json:"tokens" binding:"required,min=1"`
		Topic  string   `json:"topic" binding:"required"`
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		utils.BadRequestResponse(c, "Invalid request body", []string{err.Error()})
		return
	}

	if err := h.service.UnsubscribeFromPushTopic(req.Tokens, req.Topic); err != nil {
		h.logger.Error("Failed to unsubscribe from topic: ", err)
		utils.InternalErrorResponse(c, "Failed to unsubscribe from push topic")
		return
	}

	utils.SuccessResponse(c, http.StatusOK, "Unsubscribed from topic successfully", map[string]interface{}{
		"topic":       req.Topic,
		"tokenCount":  len(req.Tokens),
	})
}

// GetPushStatus - Check FCM service status
func (h *Handler) GetPushStatus(c *gin.Context) {
	status := h.service.GetPushStatus()
	utils.SuccessResponse(c, http.StatusOK, "Push notification status", status)
}
// SendEmailTemplate - Send email using a template
func (h *Handler) SendEmailTemplate(c *gin.Context) {
	var req EmailTemplateRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		h.logger.Error("Invalid request body: ", err)
		utils.BadRequestResponse(c, "Invalid request body", []string{err.Error()})
		return
	}

	// Convert EmailTemplateRequest to NotificationRequest for standard processing
	if req.Variables == nil {
		req.Variables = make(map[string]string)
	}
	req.Variables["template_name"] = req.TemplateName

	notifReq := NotificationRequest{
		Type:        utils.NotificationTypeEmailTemplate,
		Recipient:   req.Recipient,
		Subject:     req.Subject,
		Body:        "Templated Email: " + req.TemplateName, // Placeholder for DB record
		ScheduledAt: req.ScheduledAt,
		Metadata:    req.Variables,
	}

	response, err := h.service.SendNotification(&notifReq)
	if err != nil {
		h.logger.Error("Failed to queue templated email: ", err)
		utils.InternalErrorResponse(c, "Failed to queue templated email")
		return
	}

	utils.SuccessResponse(c, http.StatusOK, "Templated email queued successfully", response)
}

func (h *Handler) extractRecipients(c *gin.Context) []string {
	var list []string

	// Check query params
	if r := c.Query("recipient"); r != "" {
		list = append(list, r)
	}
	if u := c.Query("userId"); u != "" {
		list = append(list, u)
	}
	if s := c.Query("sessionId"); s != "" {
		list = append(list, s)
	}

	// Check header
	if hs := c.GetHeader("X-Session-Id"); hs != "" {
		list = append(list, hs)
	}

	// Check context variables
	if val, ok := c.Get("userId"); ok {
		if s, ok := val.(string); ok && s != "" {
			list = append(list, s)
		}
	}
	if val, ok := c.Get("userEmail"); ok {
		if s, ok := val.(string); ok && s != "" {
			list = append(list, s)
		}
	}

	// Deduplicate and filter empty
	seen := make(map[string]bool)
	var result []string
	for _, item := range list {
		if item != "" && !seen[item] {
			seen[item] = true
			result = append(result, item)
		}
	}
	return result
}

// GetNotifications - Retrieve filtered and paginated notification history
func (h *Handler) GetNotifications(c *gin.Context) {
	recipients := h.extractRecipients(c)

	category := c.Query("category")
	notifType := c.Query("type")

	var readPtr *bool
	if readStr := c.Query("read"); readStr != "" {
		r := readStr == "true"
		readPtr = &r
	}

	pageStr := c.DefaultQuery("page", "1")
	page, err := strconv.Atoi(pageStr)
	if err != nil || page <= 0 {
		page = 1
	}

	limitStr := c.DefaultQuery("limit", "20")
	limit, err := strconv.Atoi(limitStr)
	if err != nil || limit <= 0 {
		limit = 20
	}

	if len(recipients) == 0 {
		utils.SuccessResponse(c, http.StatusOK, "Notifications retrieved successfully", map[string]interface{}{
			"notifications": []interface{}{},
			"counts": map[string]interface{}{
				"total":  0,
				"unread": 0,
				"read":   0,
			},
			"pagination": map[string]interface{}{
				"page":       page,
				"limit":      limit,
				"total":      0,
				"totalPages": 0,
			},
		})
		return
	}

	dtos, total, err := h.service.GetNotifications(recipients, category, notifType, readPtr, page, limit)
	if err != nil {
		h.logger.Error("Failed to retrieve notifications history: ", err)
		utils.InternalErrorResponse(c, "Failed to retrieve notifications history")
		return
	}

	totalPages := int(total / int64(limit))
	if total%int64(limit) != 0 {
		totalPages++
	}

	// Recalculate counts dynamically
	totalCount, unreadCount, readCount, err := h.service.GetNotificationCounts(recipients)
	if err != nil {
		h.logger.Error("Failed to retrieve notification counts: ", err)
		totalCount, unreadCount, readCount = 0, 0, 0
	}

	utils.SuccessResponse(c, http.StatusOK, "Notifications retrieved successfully", map[string]interface{}{
		"notifications": dtos,
		"counts": map[string]interface{}{
			"total":  totalCount,
			"unread": unreadCount,
			"read":   readCount,
		},
		"pagination": map[string]interface{}{
			"page":       page,
			"limit":      limit,
			"total":      total,
			"totalPages": totalPages,
		},
	})
}

// MarkAsRead - Mark specific notification as read
func (h *Handler) MarkAsRead(c *gin.Context) {
	id := c.Param("id")
	if id == "" {
		utils.BadRequestResponse(c, "Notification ID is required", nil)
		return
	}

	err := h.service.MarkAsRead(id)
	if err != nil {
		h.logger.Error("Failed to mark notification as read: ", err)
		utils.InternalErrorResponse(c, "Failed to mark notification as read")
		return
	}

	utils.SuccessResponse(c, http.StatusOK, "Notification marked as read successfully", nil)
}

// MarkAllAsRead - Mark all notifications for a recipient as read
func (h *Handler) MarkAllAsRead(c *gin.Context) {
	recipients := h.extractRecipients(c)

	if len(recipients) == 0 {
		utils.BadRequestResponse(c, "Recipient or userId parameter is required", nil)
		return
	}

	err := h.service.MarkAllAsRead(recipients)
	if err != nil {
		h.logger.Error("Failed to mark all notifications as read: ", err)
		utils.InternalErrorResponse(c, "Failed to mark notifications as read")
		return
	}

	utils.SuccessResponse(c, http.StatusOK, "All notifications marked as read successfully", nil)
}

// GetUnreadCount - Retrieve unread notification counts for a recipient
func (h *Handler) GetUnreadCount(c *gin.Context) {
	recipients := h.extractRecipients(c)

	if len(recipients) == 0 {
		utils.BadRequestResponse(c, "Recipient or userId parameter is required", nil)
		return
	}

	count, err := h.service.GetUnreadCount(recipients)
	if err != nil {
		h.logger.Error("Failed to retrieve unread notification count: ", err)
		utils.InternalErrorResponse(c, "Failed to retrieve unread notification count")
		return
	}

	utils.SuccessResponse(c, http.StatusOK, "Unread notification count retrieved successfully", map[string]interface{}{
		"unreadCount": count,
	})
}

// DeleteNotification - Clear/Delete specific notification by ID
func (h *Handler) DeleteNotification(c *gin.Context) {
	id := c.Param("id")
	if id == "" {
		utils.BadRequestResponse(c, "Notification ID is required", nil)
		return
	}

	err := h.service.DeleteNotification(id)
	if err != nil {
		h.logger.Error("Failed to delete notification: ", err)
		utils.InternalErrorResponse(c, "Failed to delete notification")
		return
	}

	utils.SuccessResponse(c, http.StatusOK, "Notification cleared successfully", nil)
}

// DeleteAllNotifications - Clear/Delete all notifications for a recipient
func (h *Handler) DeleteAllNotifications(c *gin.Context) {
	recipients := h.extractRecipients(c)

	if len(recipients) == 0 {
		utils.BadRequestResponse(c, "Recipient or userId parameter is required", nil)
		return
	}

	err := h.service.DeleteAllNotifications(recipients)
	if err != nil {
		h.logger.Error("Failed to clear notifications: ", err)
		utils.InternalErrorResponse(c, "Failed to clear notifications")
		return
	}

	utils.SuccessResponse(c, http.StatusOK, "All notifications cleared successfully", nil)
}
