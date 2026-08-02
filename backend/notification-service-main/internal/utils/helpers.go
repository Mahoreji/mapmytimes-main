package utils

import (
	"encoding/json"
	"regexp"
	"strings"
	"time"
)

func ToJSON(v interface{}) string {
	bytes, _ := json.Marshal(v)
	return string(bytes)
}

func FromJSON(data string, v interface{}) error {
	return json.Unmarshal([]byte(data), v)
}

func GetCurrentTimestamp() int64 {
	return time.Now().UnixMilli()
}

func IsValidEmail(email string) bool {
	// More comprehensive email validation
	if len(email) < 3 || len(email) > 254 {
		return false
	}
	
	// Basic email regex pattern
	emailRegex := regexp.MustCompile(`^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$`)
	return emailRegex.MatchString(email)
}

func IsValidPhone(phone string) bool {
	// Remove all non-digit characters except + at the beginning
	cleanPhone := regexp.MustCompile(`[^\d+]`).ReplaceAllString(phone, "")
	
	// Check if phone starts with + and has 10-15 digits
	if strings.HasPrefix(cleanPhone, "+") {
		cleanPhone = cleanPhone[1:] // Remove the + sign
		if len(cleanPhone) >= 10 && len(cleanPhone) <= 15 {
			// Check if all remaining characters are digits
			digitRegex := regexp.MustCompile(`^\d+$`)
			return digitRegex.MatchString(cleanPhone)
		}
	}
	
	// For phones without + prefix, check if it's 10-15 digits
	if len(cleanPhone) >= 10 && len(cleanPhone) <= 15 {
		digitRegex := regexp.MustCompile(`^\d+$`)
		return digitRegex.MatchString(cleanPhone)
	}
	
	return false
}

// IsValidIndianPhone specifically validates Indian phone numbers
func IsValidIndianPhone(phone string) bool {
	// Remove all non-digit characters except + at the beginning
	cleanPhone := regexp.MustCompile(`[^\d+]`).ReplaceAllString(phone, "")
	
	// Indian phone number patterns:
	// +91XXXXXXXXXX (where X is 10 digits)
	// 91XXXXXXXXXX
	// XXXXXXXXXX (10 digits starting with 6,7,8,9)
	
	if strings.HasPrefix(cleanPhone, "+91") {
		number := cleanPhone[3:]
		return len(number) == 10 && regexp.MustCompile(`^[6789]\d{9}$`).MatchString(number)
	}
	
	if strings.HasPrefix(cleanPhone, "91") {
		number := cleanPhone[2:]
		return len(number) == 10 && regexp.MustCompile(`^[6789]\d{9}$`).MatchString(number)
	}
	
	// Direct 10-digit number
	if len(cleanPhone) == 10 {
		return regexp.MustCompile(`^[6789]\d{9}$`).MatchString(cleanPhone)
	}
	
	return false
}

// FormatIndianPhone formats Indian phone number with +91 prefix
func FormatIndianPhone(phone string) string {
	cleanPhone := regexp.MustCompile(`[^\d+]`).ReplaceAllString(phone, "")
	
	if strings.HasPrefix(cleanPhone, "+91") {
		return cleanPhone
	}
	
	if strings.HasPrefix(cleanPhone, "91") {
		return "+" + cleanPhone
	}
	
	if len(cleanPhone) == 10 && regexp.MustCompile(`^[6789]\d{9}$`).MatchString(cleanPhone) {
		return "+91" + cleanPhone
	}
	
	return phone // Return original if can't format
}

// ValidateEmailDomain checks if email domain is valid
func ValidateEmailDomain(email string) bool {
	parts := strings.Split(email, "@")
	if len(parts) != 2 {
		return false
	}
	
	domain := parts[1]
	
	// Check for common valid domains (you can extend this list)
	validDomains := []string{
		"gmail.com", "yahoo.com", "outlook.com", "hotmail.com",
		"mapmytimes.com", "example.com", "test.com",
	}
	
	domain = strings.ToLower(domain)
	for _, validDomain := range validDomains {
		if domain == validDomain {
			return true
		}
	}
	
	// If not in the whitelist, check if it's a valid domain format
	domainRegex := regexp.MustCompile(`^[a-zA-Z0-9][a-zA-Z0-9-]{0,61}[a-zA-Z0-9]?\.[a-zA-Z]{2,}$`)
	return domainRegex.MatchString(domain)
}

// SanitizePhoneNumber removes special characters and formats phone number
func SanitizePhoneNumber(phone string) string {
	// Remove all spaces, dashes, parentheses, etc.
	cleanPhone := regexp.MustCompile(`[^\d+]`).ReplaceAllString(phone, "")
	
	// If it's an Indian number, format it properly
	if IsValidIndianPhone(phone) {
		return FormatIndianPhone(cleanPhone)
	}
	
	return cleanPhone
}

// IsValidDeviceToken validates push notification device tokens
func IsValidDeviceToken(token string) bool {
	// FCM tokens are typically 152-163 characters
	// APNS tokens are typically 64 characters (hex)
	
	if len(token) < 10 {
		return false
	}
	
	// Check for FCM token format (Base64-like)
	fcmRegex := regexp.MustCompile(`^[a-zA-Z0-9_-]+$`)
	if len(token) >= 140 && len(token) <= 170 && fcmRegex.MatchString(token) {
		return true
	}
	
	// Check for APNS token format (hex)
	apnsRegex := regexp.MustCompile(`^[a-fA-F0-9]+$`)
	if len(token) == 64 && apnsRegex.MatchString(token) {
		return true
	}
	
	// For testing purposes, accept simple device identifiers
	if len(token) >= 5 && len(token) <= 50 {
		return true
	}
	
	return false
}
