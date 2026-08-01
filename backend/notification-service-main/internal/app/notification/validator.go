package notification

import (
	"notification-service/internal/utils"
	"strings"
)

func ValidateNotificationRequest(req *NotificationRequest) []string {
	var errors []string

	// Type validation - now handled by the specific endpoint handlers
	if req.Type == "" {
		errors = append(errors, "notification type is required")
		return errors // Return early if type is missing
	}

	validTypes := []string{
		utils.NotificationTypeEmail,
		utils.NotificationTypeEmailHTML,
		utils.NotificationTypeSMS,
		utils.NotificationTypeWhatsApp,
		utils.NotificationTypePush,
		utils.NotificationTypeInstant,
		utils.NotificationTypeEmailTemplate,
	}

	isValidType := false
	for _, validType := range validTypes {
		if req.Type == validType {
			isValidType = true
			break
		}
	}

	// Support registered builders or custom event types containing key terms
	if !isValidType {
		if _, ok := buildersRegistry[req.Type]; ok {
			isValidType = true
		} else {
			t := strings.ToUpper(req.Type)
			if strings.Contains(t, "SUCCESS") || strings.Contains(t, "FAILED") ||
				strings.Contains(t, "CONFIRMED") || strings.Contains(t, "CANCEL") ||
				strings.Contains(t, "REFUND") || strings.Contains(t, "READY") ||
				strings.Contains(t, "OTP") || strings.Contains(t, "ALERT") ||
				strings.Contains(t, "OFFER") || strings.Contains(t, "CREATED") ||
				strings.Contains(t, "REMINDER") || strings.Contains(t, "COMPLETED") ||
				strings.Contains(t, "TOUR") || strings.Contains(t, "FLIGHT") ||
				strings.Contains(t, "HOTEL") || strings.Contains(t, "BUS") ||
				strings.Contains(t, "ACTIVITY") || strings.Contains(t, "SYSTEM") {
				isValidType = true
			}
		}
	}

	if !isValidType {
		errors = append(errors, "invalid notification type. Valid types: email, email_html, sms, whatsapp, push, instant, or rich event types")
	}

	if req.Recipient == "" {
		errors = append(errors, "recipient is required")
	}

	// Validate recipient format based on type
	switch req.Type {
	case utils.NotificationTypeEmail, utils.NotificationTypeEmailHTML:
		if !utils.IsValidEmail(req.Recipient) {
			errors = append(errors, "invalid email address")
		}
		// Email types should have a subject
		if req.Subject == "" {
			errors = append(errors, "subject is required for email notifications")
		}
	case utils.NotificationTypeEmailTemplate:
		if !utils.IsValidEmail(req.Recipient) {
			errors = append(errors, "invalid email address")
		}
		if req.Subject == "" {
			errors = append(errors, "subject is required for templated emails")
		}
	case utils.NotificationTypeSMS, utils.NotificationTypeWhatsApp:
		if !utils.IsValidPhone(req.Recipient) {
			errors = append(errors, "invalid phone number")
		}
	case utils.NotificationTypePush:
		// Push notifications can have various recipient formats (device tokens, user IDs, etc.)
		// So we'll be more lenient here
		if len(req.Recipient) < 3 {
			errors = append(errors, "invalid push notification recipient")
		}
		// Push notifications should have a subject/title
		if req.Subject == "" {
			errors = append(errors, "subject/title is required for push notifications")
		}
	}

	if req.Body == "" {
		errors = append(errors, "message body is required")
	}

	// Validate scheduled time if provided
	if req.ScheduledAt != nil && *req.ScheduledAt <= 0 {
		errors = append(errors, "scheduled_at must be a valid future timestamp")
	}

	return errors
}
