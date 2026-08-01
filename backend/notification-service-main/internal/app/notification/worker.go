package notification

import (
	"context"
	"encoding/json"
	"github.com/hibiken/asynq"
)

const (
	TypeNotificationDelivery = "notification:deliver"
)

type NotificationDeliveryPayload struct {
	NotificationID string
}

func NewNotificationDeliveryTask(notificationID string) (*asynq.Task, error) {
	payload, err := json.Marshal(NotificationDeliveryPayload{NotificationID: notificationID})
	if err != nil {
		return nil, err
	}
	return asynq.NewTask(TypeNotificationDelivery, payload), nil
}

type NotificationProcessor struct {
	service *Service
}

func NewNotificationProcessor(service *Service) *NotificationProcessor {
	return &NotificationProcessor{service: service}
}

func (p *NotificationProcessor) ProcessTask(ctx context.Context, t *asynq.Task) error {
	var pld NotificationDeliveryPayload
	if err := json.Unmarshal(t.Payload(), &pld); err != nil {
		return err
	}

	// Fetch notification from DB
	notification, err := p.service.repo.GetByID(pld.NotificationID)
	if err != nil {
		return err
	}

	// Send it
	err = p.service.sendNotificationOnce(notification)
	if err != nil {
		p.service.logger.Errorf("Failed to deliver notification %s: %v", notification.ID, err)
		return err // Returning error tells Asynq to retry with backoff
	}

	// Update status on success
	p.service.repo.UpdateStatus(notification.ID, "sent")
	p.service.logger.Infof("Notification %s delivered successfully", notification.ID)
	return nil
}
