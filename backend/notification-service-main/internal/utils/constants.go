package utils

const (
	// Notification Types
	NotificationTypeEmail     = "email"
	NotificationTypeEmailHTML     = "email_html"
	NotificationTypeEmailTemplate = "email_template"
	NotificationTypeSMS           = "sms"
	NotificationTypeWhatsApp  = "whatsapp"
	NotificationTypePush      = "push"
	NotificationTypeInstant   = "instant"

	// Notification Status
	StatusPending = "pending"
	StatusSent    = "sent"
	StatusFailed  = "failed"

	// Redis Keys
	RedisNotificationQueue = "notification:queue"
	RedisRetryQueue        = "notification:retry"
	RedisScheduledQueue    = "scheduled_notifications"
)