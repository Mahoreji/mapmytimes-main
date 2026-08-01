package config

import (
	"fmt"
	"os"
	"strconv"
	"strings"
)

type Config struct {
	// Server
	Port              string
	GinMode           string
	MaxRequestSize    int64
	ReadTimeout       int
	WriteTimeout      int
	IdleTimeout       int

	// Database
	DBHost            string
	DBPort            string
	DBName            string
	DBUsername        string
	DBPassword        string
	DBSSLMode         string
	DBMaxOpenConns    int
	DBMaxIdleConns    int
	DBConnMaxLifetime int

	// Email
	EmailHost     string
	EmailPort     int
	EmailUsername string
	EmailPassword string

	// Contact Form
	ContactFormRecipientEmail string // Email address to receive contact form submissions
	ContactFormAutoReply      bool   // Whether to send auto-reply to customer

	// Twilio
	TwilioAccountSID    string
	TwilioAuthToken     string
	TwilioPhoneNumber   string
	TwilioWhatsAppNumber string

	// Redis
	RedisHost     string
	RedisPort     string
	RedisPassword string
	RedisDB       int

	// Security
	APIKeys              []string
	EnableAuth           bool
	CORSAllowedOrigins   []string
	RateLimitEnabled     bool
	RateLimitRPS         int
	RateLimitBurst       int

	// Retry Configuration
	MaxRetryAttempts int
	RetryBackoffMs   int

	// JWT / HMAC Secret
	JWTSecret        string

	// FCM / Push Notifications
	FCMServiceAccountJSON string // JSON string of service account key (preferred)
	FCMServiceAccountFile string // Path to service account key file

	// External Services
	BookingServiceURL string
	PaymentServiceURL string
}

func Load() *Config {
	emailPort, _ := strconv.Atoi(getEnv("EMAIL_PORT", "465"))
	redisDB, _ := strconv.Atoi(getEnv("REDIS_DB", "0"))
	maxRequestSize, _ := strconv.ParseInt(getEnv("MAX_REQUEST_SIZE", "10485760"), 10, 64) // 10MB default
	readTimeout, _ := strconv.Atoi(getEnv("READ_TIMEOUT", "30"))
	writeTimeout, _ := strconv.Atoi(getEnv("WRITE_TIMEOUT", "30"))
	idleTimeout, _ := strconv.Atoi(getEnv("IDLE_TIMEOUT", "120"))
	
	dbMaxOpenConns, _ := strconv.Atoi(getEnv("DB_MAX_OPEN_CONNS", "100"))
	dbMaxIdleConns, _ := strconv.Atoi(getEnv("DB_MAX_IDLE_CONNS", "5"))
	dbConnMaxLifetime, _ := strconv.Atoi(getEnv("DB_CONN_MAX_LIFETIME", "300"))
	
	rateLimitRPS, _ := strconv.Atoi(getEnv("RATE_LIMIT_RPS", "100"))
	rateLimitBurst, _ := strconv.Atoi(getEnv("RATE_LIMIT_BURST", "200"))
	maxRetryAttempts, _ := strconv.Atoi(getEnv("MAX_RETRY_ATTEMPTS", "3"))
	retryBackoffMs, _ := strconv.Atoi(getEnv("RETRY_BACKOFF_MS", "1000"))
	
	enableAuth := getEnv("ENABLE_AUTH", "false") == "true"
	rateLimitEnabled := getEnv("RATE_LIMIT_ENABLED", "true") == "true"
	
	// Parse API keys (comma-separated)
	apiKeysStr := getEnv("API_KEYS", "")
	var apiKeys []string
	if apiKeysStr != "" {
		apiKeys = strings.Split(apiKeysStr, ",")
		for i := range apiKeys {
			apiKeys[i] = strings.TrimSpace(apiKeys[i])
		}
	}
	
	// Parse CORS allowed origins (comma-separated)
	corsOriginsStr := getEnv("CORS_ALLOWED_ORIGINS", "")
	var corsOrigins []string
	if corsOriginsStr != "" {
		corsOrigins = strings.Split(corsOriginsStr, ",")
		for i := range corsOrigins {
			corsOrigins[i] = strings.TrimSpace(corsOrigins[i])
		}
	} else {
		// Default to empty (no CORS) for security
		corsOrigins = []string{}
	}

	return &Config{
		Port:           getEnv("PORT", "9090"),
		GinMode:        getEnv("GIN_MODE", "release"),
		MaxRequestSize: maxRequestSize,
		ReadTimeout:    readTimeout,
		WriteTimeout:   writeTimeout,
		IdleTimeout:    idleTimeout,

		DBHost:            getEnv("DB_HOST", ""),
		DBPort:            getEnv("DB_PORT", "5432"),
		DBName:            getEnv("DB_NAME", ""),
		DBUsername:        getEnv("DB_USERNAME", ""),
		DBPassword:        getEnv("DB_PASSWORD", ""),
		DBSSLMode:         getEnv("DB_SSL_MODE", "prefer"),
		DBMaxOpenConns:    dbMaxOpenConns,
		DBMaxIdleConns:    dbMaxIdleConns,
		DBConnMaxLifetime: dbConnMaxLifetime,

		EmailHost:     getEnv("EMAIL_HOST", "smtp.zoho.in"),
		EmailPort:     emailPort,
		EmailUsername: getEnv("EMAIL_USERNAME", ""),
		EmailPassword: getEnv("EMAIL_PASSWORD", ""),
		
		ContactFormRecipientEmail: getEnv("CONTACT_FORM_RECIPIENT_EMAIL", getEnv("EMAIL_USERNAME", "")),
		ContactFormAutoReply:      getEnv("CONTACT_FORM_AUTO_REPLY", "true") == "true",

		TwilioAccountSID:    getEnv("TWILIO_ACCOUNT_SID", ""),
		TwilioAuthToken:     getEnv("TWILIO_AUTH_TOKEN", ""),
		TwilioPhoneNumber:   getEnv("TWILIO_PHONE_NUMBER", ""),
		TwilioWhatsAppNumber: getEnv("TWILIO_WHATSAPP_NUMBER", ""),

		RedisHost:     getEnv("REDIS_HOST", "redis"),
		RedisPort:     getEnv("REDIS_PORT", "6379"),
		RedisPassword: getEnv("REDIS_PASSWORD", ""),
		RedisDB:       redisDB,

		APIKeys:            apiKeys,
		EnableAuth:         enableAuth,
		CORSAllowedOrigins: corsOrigins,
		RateLimitEnabled:    rateLimitEnabled,
		RateLimitRPS:        rateLimitRPS,
		RateLimitBurst:     rateLimitBurst,

		MaxRetryAttempts: maxRetryAttempts,
		RetryBackoffMs:   retryBackoffMs,

		JWTSecret:        getEnv("JWT_SECRET", "mapMyTourProductionSecretKey885839!VerySecureAndLongEnoughForHS256AlgorithmRequirement123456789"),

		// FCM / Push Notifications
		FCMServiceAccountJSON: getEnv("FCM_SERVICE_ACCOUNT_JSON", ""),
		FCMServiceAccountFile: getEnv("FCM_SERVICE_ACCOUNT_FILE", ""),

		// External Services
		BookingServiceURL: getEnv("BOOKING_SERVICE_URL", "http://localhost:8089"),
		PaymentServiceURL: getEnv("PAYMENT_SERVICE_URL", "http://localhost:8088"),
	}
}

// Validate checks if all required configuration values are set
func (c *Config) Validate() error {
	var errors []string

	if c.DBHost == "" {
		errors = append(errors, "DB_HOST is required")
	}
	if c.DBName == "" {
		errors = append(errors, "DB_NAME is required")
	}
	if c.DBUsername == "" {
		errors = append(errors, "DB_USERNAME is required")
	}
	if c.DBPassword == "" {
		errors = append(errors, "DB_PASSWORD is required")
	}

	if c.EnableAuth && len(c.APIKeys) == 0 {
		errors = append(errors, "API_KEYS is required when ENABLE_AUTH=true")
	}

	// Warn about email configuration (not required, but recommended for contact form)
	if c.EmailUsername == "" {
		// Don't add as error, just log warning - email might not be needed
	}
	if c.ContactFormRecipientEmail == "" && c.EmailUsername == "" {
		// Don't add as error, contact form will work but emails won't be sent
	}

	if len(errors) > 0 {
		return fmt.Errorf("configuration validation failed: %s", strings.Join(errors, ", "))
	}

	return nil
}

func getEnv(key, defaultValue string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return defaultValue
}
