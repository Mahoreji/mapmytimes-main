package db

import (
	"fmt"
	"notification-service/config"
	"time"

	"gorm.io/driver/postgres"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"
)

func InitPostgres(cfg *config.Config) (*gorm.DB, error) {
	// Build DSN with SSL mode
	dsn := fmt.Sprintf("host=%s port=%s user=%s password=%s dbname=%s sslmode=%s",
		cfg.DBHost, cfg.DBPort, cfg.DBUsername, cfg.DBPassword, cfg.DBName, cfg.DBSSLMode)

	// Set log level based on environment
	logLevel := logger.Info
	if cfg.GinMode == "release" {
		logLevel = logger.Silent
	}

	db, err := gorm.Open(postgres.New(postgres.Config{
		DSN:                  dsn,
		PreferSimpleProtocol: true, // Disables prepared statements to prevent 'cached plan must not change result type' errors
	}), &gorm.Config{
		Logger: logger.Default.LogMode(logLevel),
	})

	if err != nil {
		return nil, err
	}

	// Configure connection pool
	sqlDB, err := db.DB()
	if err != nil {
		return nil, err
	}

	// Set connection pool settings
	sqlDB.SetMaxOpenConns(cfg.DBMaxOpenConns)
	sqlDB.SetMaxIdleConns(cfg.DBMaxIdleConns)
	sqlDB.SetConnMaxLifetime(time.Duration(cfg.DBConnMaxLifetime) * time.Second)

	// Test connection
	if err := sqlDB.Ping(); err != nil {
		return nil, fmt.Errorf("failed to ping database: %w", err)
	}

	return db, nil
}

func RunMigrations(db *gorm.DB) error {
	// Auto-migrate your models here
	return db.AutoMigrate(
		&Notification{},
		&ContactForm{},
	)
}

// Notification model
type Notification struct {
	ID              string `gorm:"type:uuid;primary_key;default:gen_random_uuid()" json:"id"`
	Type            string `gorm:"not null" json:"type"` // TOUR, FLIGHT, HOTEL, BUS, ACTIVITY, TRANSFER, VISA, INSURANCE, PAYMENT, WALLET, SYSTEM
	Category        string `json:"category"`
	EventType       string `gorm:"column:event_type" json:"eventType"`
	Priority        string `json:"priority"`
	Title           string `json:"title"`
	Message         string `json:"message"`
	Recipient       string `gorm:"not null" json:"recipient"`
	Subject         string `json:"subject"`
	Body            string `gorm:"not null" json:"body"`
	ScheduledAt     *int64 `json:"scheduled_at"`
	CreatedAt       int64  `gorm:"autoCreateTime" json:"created_at"`
	UpdatedAt       int64  `gorm:"autoUpdateTime" json:"updated_at"`
	Status          string `gorm:"default:'pending'" json:"status"` // pending, sent, failed
	Read            bool   `gorm:"column:is_read;default:false" json:"isRead"`
	ReadAt          *int64 `json:"read_at"`
	IsArchived      bool   `gorm:"column:is_archived;default:false" json:"isArchived"`
	ExpiresAt       *int64 `json:"expires_at"`
	DeliveryChannel string `gorm:"column:delivery_channel" json:"delivery_channel"`
	DeliveryStatus  string `gorm:"column:delivery_status;default:'PENDING'" json:"delivery_status"`
	FailedReason    string `gorm:"column:failed_reason" json:"failed_reason"`
	SentAt          *int64 `json:"sent_at"`
	DeliveredAt     *int64 `json:"delivered_at"`
	Metadata        string `gorm:"type:jsonb" json:"metadata"`
}

func (Notification) TableName() string {
	return "notifications"
}

// ContactForm model for storing contact form submissions
type ContactForm struct {
	ID          string `gorm:"type:uuid;primary_key;default:gen_random_uuid()" json:"id"`
	Name        string `gorm:"not null" json:"name"`
	Email       string `gorm:"not null" json:"email"`
	Phone       string `json:"phone"`
	Subject     string `json:"subject"`
	Message     string `gorm:"type:text" json:"message"`
	Source      string `json:"source"` // website_contact_form, mobile_app, etc.
	Status      string `gorm:"default:'new'" json:"status"` // new, read, replied, archived
	IPAddress   string `json:"ip_address"`
	UserAgent   string `json:"user_agent"`
	Metadata    string `gorm:"type:jsonb" json:"metadata"`
	CreatedAt   int64  `gorm:"autoCreateTime" json:"created_at"`
	UpdatedAt   int64  `gorm:"autoUpdateTime" json:"updated_at"`
}

func (ContactForm) TableName() string {
	return "contact_forms"
}
