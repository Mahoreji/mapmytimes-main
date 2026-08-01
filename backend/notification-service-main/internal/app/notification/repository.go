package notification

import (
	"notification-service/internal/db"
	"time"

	"gorm.io/gorm"
)

type Repository struct {
	db *gorm.DB
}

func NewRepository(db *gorm.DB) *Repository {
	return &Repository{db: db}
}

func (r *Repository) Create(notification *db.Notification) error {
	return r.db.Create(notification).Error
}

func (r *Repository) GetByID(id string) (*db.Notification, error) {
	var notification db.Notification
	err := r.db.Where("id = ?", id).First(&notification).Error
	return &notification, err
}

func (r *Repository) Update(notification *db.Notification) error {
	return r.db.Save(notification).Error
}

func (r *Repository) GetPendingNotifications(limit int) ([]*db.Notification, error) {
	var notifications []*db.Notification
	err := r.db.Where("status = ?", "pending").Limit(limit).Find(&notifications).Error
	return notifications, err
}

func (r *Repository) GetByStatus(status string, limit int) ([]*db.Notification, error) {
	var notifications []*db.Notification
	err := r.db.Where("status = ?", status).Limit(limit).Find(&notifications).Error
	return notifications, err
}

func (r *Repository) UpdateStatus(id string, status string) error {
	deliveryStatus := "PENDING"
	if status == "sent" {
		deliveryStatus = "DELIVERED"
	} else if status == "failed" {
		deliveryStatus = "FAILED"
	}
	return r.db.Model(&db.Notification{}).Where("id = ?", id).Updates(map[string]interface{}{
		"status":          status,
		"delivery_status": deliveryStatus,
	}).Error
}

func (r *Repository) CountByStatus(status string) (int64, error) {
	var count int64
	err := r.db.Model(&db.Notification{}).Where("status = ?", status).Count(&count).Error
	return count, err
}

// ContactForm repository methods
func (r *Repository) CreateContactForm(contactForm *db.ContactForm) error {
	return r.db.Create(contactForm).Error
}

func (r *Repository) GetContactFormByID(id string) (*db.ContactForm, error) {
	var contactForm db.ContactForm
	err := r.db.Where("id = ?", id).First(&contactForm).Error
	return &contactForm, err
}

func (r *Repository) GetRecentNotifications(limit int) ([]*db.Notification, error) {
	var notifications []*db.Notification
	err := r.db.Order("created_at DESC").Limit(limit).Find(&notifications).Error
	return notifications, err
}

func (r *Repository) GetNotificationsByType(notificationType string, limit int) ([]*db.Notification, error) {
	var notifications []*db.Notification
	err := r.db.Where("type = ?", notificationType).Limit(limit).Find(&notifications).Error
	return notifications, err
}

func (r *Repository) GetScheduledNotifications() ([]*db.Notification, error) {
	var notifications []*db.Notification
	err := r.db.Where("scheduled_at IS NOT NULL AND scheduled_at > ?", 0).Find(&notifications).Error
	return notifications, err
}

func (r *Repository) GetNotificationsFiltered(recipients []string, category, notifType string, read *bool, page, limit int) ([]*db.Notification, int64, error) {
	var notifications []*db.Notification
	var total int64

	if len(recipients) == 0 {
		return nil, 0, nil
	}

	query := r.db.Model(&db.Notification{}).Where("recipient IN ?", recipients)
	// Exclude expired notifications
	query = query.Where("expires_at IS NULL OR expires_at > ?", time.Now().Unix())

	if category != "" {
		query = query.Where("category = ?", category)
	}
	if notifType != "" {
		query = query.Where("type = ?", notifType)
	}
	if read != nil {
		query = query.Where("is_read = ?", *read)
	}

	// Get count first
	if err := query.Count(&total).Error; err != nil {
		return nil, 0, err
	}

	// Get paginated results
	offset := (page - 1) * limit
	err := query.Order("created_at DESC").Offset(offset).Limit(limit).Find(&notifications).Error
	return notifications, total, err
}

func (r *Repository) MarkAsRead(id string, readAt int64) error {
	return r.db.Model(&db.Notification{}).Where("id = ?", id).Updates(map[string]interface{}{
		"is_read": true,
		"read_at": readAt,
	}).Error
}

func (r *Repository) MarkAllAsRead(recipients []string, readAt int64) error {
	if len(recipients) == 0 {
		return nil
	}
	return r.db.Model(&db.Notification{}).Where("recipient IN ? AND is_read = ?", recipients, false).Updates(map[string]interface{}{
		"is_read": true,
		"read_at": readAt,
	}).Error
}

func (r *Repository) GetUnreadCount(recipients []string) (int64, error) {
	var count int64
	if len(recipients) == 0 {
		return 0, nil
	}
	err := r.db.Model(&db.Notification{}).Where("recipient IN ? AND is_read = ? AND (expires_at IS NULL OR expires_at > ?)", recipients, false, time.Now().Unix()).Count(&count).Error
	return count, err
}

func (r *Repository) GetNotificationCounts(recipients []string) (total int64, unread int64, read int64, err error) {
	if len(recipients) == 0 {
		return 0, 0, 0, nil
	}
	now := time.Now().Unix()
	err = r.db.Model(&db.Notification{}).Where("recipient IN ? AND (expires_at IS NULL OR expires_at > ?)", recipients, now).Count(&total).Error
	if err != nil {
		return
	}
	err = r.db.Model(&db.Notification{}).Where("recipient IN ? AND is_read = ? AND (expires_at IS NULL OR expires_at > ?)", recipients, false, now).Count(&unread).Error
	if err != nil {
		return
	}
	read = total - unread
	return
}

func (r *Repository) Delete(id string) error {
	return r.db.Transaction(func(tx *gorm.DB) error {
		return tx.Where("id = ?", id).Delete(&db.Notification{}).Error
	})
}

func (r *Repository) DeleteAll(recipients []string) error {
	if len(recipients) == 0 {
		return nil
	}
	return r.db.Transaction(func(tx *gorm.DB) error {
		return tx.Where("recipient IN ?", recipients).Delete(&db.Notification{}).Error
	})
}

