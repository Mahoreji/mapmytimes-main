package notification

import (
	"encoding/json"
	"notification-service/internal/db"
)

type Attachment struct {
	Content     string `json:"content"` // Base64 encoded content
	Filename    string `json:"filename"`
	ContentType string `json:"content_type"`
}

type NotificationRequest struct {
	Type        string            `json:"type"`
	Recipient   string            `json:"recipient" binding:"required"`
	Subject     string            `json:"subject"`
	Body        string            `json:"body" binding:"required"`
	ScheduledAt *int64            `json:"scheduled_at"`
	Metadata    map[string]string `json:"metadata"`
	Attachments []Attachment      `json:"attachments"`
}

type NotificationResponse struct {
	ID      string `json:"id"`
	Status  string `json:"status"`
	Message string `json:"message"`
}

type BulkNotificationRequest struct {
	Type        string            `json:"type" binding:"required"`
	Recipients  []string          `json:"recipients" binding:"required"`
	Subject     string            `json:"subject"`
	Body        string            `json:"body" binding:"required"`
	ScheduledAt *int64            `json:"scheduled_at"`
	Metadata    map[string]string `json:"metadata"`
}

type EmailTemplateRequest struct {
	Recipient    string            `json:"recipient" binding:"required"`
	Subject      string            `json:"subject" binding:"required"`
	TemplateName string            `json:"template_name" binding:"required"`
	Variables    map[string]string `json:"variables"`
	ScheduledAt  *int64            `json:"scheduled_at"`
}

// Detailed notification response with all fields
type DetailedNotificationResponse struct {
	ID          string            `json:"id"`
	Type        string            `json:"type"`
	Recipient   string            `json:"recipient"`
	Subject     string            `json:"subject"`
	Body        string            `json:"body"`
	Status      string            `json:"status"`
	ScheduledAt *int64            `json:"scheduled_at"`
	CreatedAt   int64             `json:"created_at"`
	UpdatedAt   int64             `json:"updated_at"`
	Metadata    map[string]string `json:"metadata"`
}

// Convert to DB model
func (req *NotificationRequest) ToDBModel() *db.Notification {
	notification := &db.Notification{
		Type:        req.Type,
		Recipient:   req.Recipient,
		Subject:     req.Subject,
		Body:        req.Body,
		ScheduledAt: req.ScheduledAt,
		Status:      "pending",
	}

	// Always ensure metadata is initialized (fix for production)
	if req.Metadata == nil {
		req.Metadata = make(map[string]string)
	}

	// Convert metadata map to JSON string
	metadataBytes, _ := json.Marshal(req.Metadata)
	notification.Metadata = string(metadataBytes)

	return notification
}

// Convert from DB model to simple response
func ToResponse(n *db.Notification) *NotificationResponse {
	return &NotificationResponse{
		ID:      n.ID,
		Status:  n.Status,
		Message: "Notification details retrieved",
	}
}

// Convert from DB model to detailed response
func ToDetailedResponse(n *db.Notification) *DetailedNotificationResponse {
	response := &DetailedNotificationResponse{
		ID:          n.ID,
		Type:        n.Type,
		Recipient:   n.Recipient,
		Subject:     n.Subject,
		Body:        n.Body,
		Status:      n.Status,
		ScheduledAt: n.ScheduledAt,
		CreatedAt:   n.CreatedAt,
		UpdatedAt:   n.UpdatedAt,
	}

	// Parse metadata JSON
	if n.Metadata != "" {
		var metadata map[string]string
		if err := json.Unmarshal([]byte(n.Metadata), &metadata); err == nil {
			response.Metadata = metadata
		}
	}

	return response
}

// Helper function to convert slice of notifications to detailed responses
func ToDetailedResponseList(notifications []*db.Notification) []*DetailedNotificationResponse {
	responses := make([]*DetailedNotificationResponse, len(notifications))
	for i, notification := range notifications {
		responses[i] = ToDetailedResponse(notification)
	}
	return responses
}

// Helper function to convert slice of notifications to simple responses
func ToResponseList(notifications []*db.Notification) []*NotificationResponse {
	responses := make([]*NotificationResponse, len(notifications))
	for i, notification := range notifications {
		responses[i] = ToResponse(notification)
	}
	return responses
}

// ContactFormRequest represents a contact form submission
type ContactFormRequest struct {
	Name     string            `json:"name" binding:"required"`
	Email    string            `json:"email" binding:"required,email"`
	Phone    string            `json:"phone"`
	Subject  string            `json:"subject"`
	Message  string            `json:"message"`
	Source   string            `json:"source"`
	Metadata map[string]string `json:"metadata"`
}

// ContactFormResponse represents the response after submitting a contact form
type ContactFormResponse struct {
	ID      string `json:"id"`
	Message string `json:"message"`
	Status  string `json:"status"`
}
