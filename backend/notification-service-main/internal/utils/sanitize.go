package utils

import (
	"html"
	"regexp"
	"strings"
)

// SanitizeHTML removes potentially dangerous HTML/script tags but allows safe styling tags
func SanitizeHTML(input string) string {
	sanitized := input
	
	// Remove script, iframe, object, embed tags and their content
	// Use a simpler approach without backreferences
	tags := []string{"script", "iframe", "object", "embed", "form", "input", "button"}
	for _, tag := range tags {
		// Match opening and closing tags with any content in between
		pattern := regexp.MustCompile(`(?i)<` + regexp.QuoteMeta(tag) + `[^>]*>.*?</` + regexp.QuoteMeta(tag) + `>`)
		sanitized = pattern.ReplaceAllString(sanitized, "")
		// Also remove self-closing tags
		selfClosingPattern := regexp.MustCompile(`(?i)<` + regexp.QuoteMeta(tag) + `[^>]*/>`)
		sanitized = selfClosingPattern.ReplaceAllString(sanitized, "")
	}

	// Remove javascript: and data: URLs
	jsPattern := regexp.MustCompile(`(?i)javascript:`)
	sanitized = jsPattern.ReplaceAllString(sanitized, "")
	
	dataPattern := regexp.MustCompile(`(?i)data:`)
	sanitized = dataPattern.ReplaceAllString(sanitized, "")

	// Remove on* event handlers
	eventPattern := regexp.MustCompile(`(?i)\s*on\w+\s*=\s*["'][^"']*["']`)
	sanitized = eventPattern.ReplaceAllString(sanitized, "")

	return sanitized
}

// SanitizePlainText removes any HTML tags from plain text
func SanitizePlainText(input string) string {
	// Remove all HTML tags
	htmlTagPattern := regexp.MustCompile(`<[^>]*>`)
	sanitized := htmlTagPattern.ReplaceAllString(input, "")
	
	// Decode HTML entities
	sanitized = html.UnescapeString(sanitized)
	
	return strings.TrimSpace(sanitized)
}

