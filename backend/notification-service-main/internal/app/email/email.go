package email

import (
	"encoding/base64"
	"fmt"
	"io"
	"log"
	"notification-service/config"
	"os"
	"strings"
	"time"

	"gopkg.in/gomail.v2"
)

type Attachment struct {
	Content     string
	Filename    string
	ContentType string
}

type Service struct {
	config *config.Config
}

func NewService(cfg *config.Config) *Service {
	return &Service{config: cfg}
}

// SendPlainEmail sends a plain text email
func (s *Service) SendPlainEmail(to, subject, body string) error {
	m := gomail.NewMessage()
	m.SetHeader("From", m.FormatAddress(s.config.EmailUsername, "Map My Tour"))
	m.SetHeader("To", to)
	m.SetHeader("Subject", subject)
	m.SetBody("text/plain", body)
	m.AddAlternative("text/html", GetHTMLTemplateWithTitle(subject, body))

	d := gomail.NewDialer(s.config.EmailHost, s.config.EmailPort, s.config.EmailUsername, s.config.EmailPassword)
	
	// Configure SSL/TLS based on port (matching auth service)
	if s.config.EmailPort == 465 {
		d.SSL = true
	} else {
		d.SSL = false
	}

	return d.DialAndSend(m)
}

// SendHTMLEmail sends an HTML email
func (s *Service) SendHTMLEmail(to, subject, body string) error {
	return s.SendHTMLEmailWithAttachments(to, subject, body, nil)
}

// SendHTMLEmailWithAttachments sends an HTML email with optional attachments
func (s *Service) SendHTMLEmailWithAttachments(to, subject, body string, attachments []Attachment) error {
	// Validate email configuration
	if s.config == nil {
		log.Printf("[EMAIL ERROR] Config is nil!")
		return fmt.Errorf("email service config is nil")
	}
	
	log.Printf("[EMAIL DEBUG] SendHTMLEmail - Config check: Username='%s', Host='%s', Port=%d, To='%s'", 
		s.config.EmailUsername, s.config.EmailHost, s.config.EmailPort, to)
	
	if s.config.EmailHost == "console" || s.config.EmailHost == "mock" {
		log.Printf("[EMAIL MOCK] Bypassing SMTP sending because host is '%s'", s.config.EmailHost)
		htmlBody := body
		if !containsHTML(body) {
			htmlBody = GetHTMLTemplateWithTitle(subject, body)
		}
		fallbackDir := "logs"
		_ = os.MkdirAll(fallbackDir, 0755)
		fallbackFile := fmt.Sprintf("%s/mock_sent_email_%d.html", fallbackDir, time.Now().Unix())
		_ = os.WriteFile(fallbackFile, []byte(htmlBody), 0644)
		log.Printf("[EMAIL MOCK] Mock email written to local file successfully: %s", fallbackFile)
		return nil
	}

	if s.config.EmailUsername == "" || s.config.EmailPassword == "" || s.config.EmailHost == "" {
		log.Printf("[EMAIL ERROR] Email configuration is incomplete!")
		return fmt.Errorf("email configuration is incomplete")
	}
	
	m := gomail.NewMessage()
	m.SetHeader("From", m.FormatAddress(s.config.EmailUsername, "Map My Tour"))
	m.SetHeader("To", to)
	m.SetHeader("Subject", subject)
	
	htmlBody := body
	if !containsHTML(body) {
		htmlBody = GetHTMLTemplateWithTitle(subject, body)
	}
	m.SetBody("text/html", htmlBody)

	// Add attachments
	for _, att := range attachments {
		content, err := base64.StdEncoding.DecodeString(att.Content)
		if err != nil {
			log.Printf("[EMAIL ERROR] Failed to decode attachment %s: %v", att.Filename, err)
			continue
		}

		m.Attach(att.Filename, gomail.SetCopyFunc(func(w io.Writer) error {
			_, err := w.Write(content)
			return err
		}))
	}

	d := gomail.NewDialer(s.config.EmailHost, s.config.EmailPort, s.config.EmailUsername, s.config.EmailPassword)
	if s.config.EmailPort == 465 {
		d.SSL = true
	} else {
		d.SSL = false
	}

	log.Printf("[EMAIL DEBUG] Attempting to connect to SMTP server: %s:%d (SSL=%v, Attachments=%d)", s.config.EmailHost, s.config.EmailPort, d.SSL, len(attachments))
	err := d.DialAndSend(m)
	if err != nil {
		log.Printf("[EMAIL ERROR] Failed to send email to %s: %v", to, err)
		
		// Fallback output
		fallbackDir := "logs"
		_ = os.MkdirAll(fallbackDir, 0755)
		fallbackFile := fmt.Sprintf("%s/failed_sent_email_%d.html", fallbackDir, time.Now().Unix())
		_ = os.WriteFile(fallbackFile, []byte(htmlBody), 0644)
		log.Printf("[EMAIL FALLBACK] Saved failed email to local file for preview: %s", fallbackFile)
		
		return fmt.Errorf("failed to send email to %s: %w", to, err)
	}
	log.Printf("[EMAIL SUCCESS] Email sent successfully to %s", to)
	return nil
}

// SendTemplatedEmail sends an email using a specific template
func (s *Service) SendTemplatedEmail(to, subject, templateName string, data TemplateData) error {
	htmlBody, err := RenderTemplate(templateName, data)
	if err != nil {
		return fmt.Errorf("failed to render template %s: %w", templateName, err)
	}

	if s.config.EmailHost == "console" || s.config.EmailHost == "mock" {
		log.Printf("[EMAIL MOCK] Bypassing SMTP templated sending because host is '%s'", s.config.EmailHost)
		fallbackDir := "logs"
		_ = os.MkdirAll(fallbackDir, 0755)
		fallbackFile := fmt.Sprintf("%s/mock_sent_email_%d.html", fallbackDir, time.Now().Unix())
		_ = os.WriteFile(fallbackFile, []byte(htmlBody), 0644)
		log.Printf("[EMAIL MOCK] Mock templated email written to local file successfully: %s", fallbackFile)
		return nil
	}

	m := gomail.NewMessage()
	m.SetHeader("From", m.FormatAddress(s.config.EmailUsername, "Map My Tour"))
	m.SetHeader("To", to)
	m.SetHeader("Subject", subject)
	m.SetBody("text/html", htmlBody)

	d := gomail.NewDialer(s.config.EmailHost, s.config.EmailPort, s.config.EmailUsername, s.config.EmailPassword)
	if s.config.EmailPort == 465 {
		d.SSL = true
	} else {
		d.SSL = false
	}

	dialErr := d.DialAndSend(m)
	if dialErr != nil {
		log.Printf("[EMAIL ERROR] Failed to send templated email to %s: %v", to, dialErr)
		
		// Fallback output
		fallbackDir := "logs"
		_ = os.MkdirAll(fallbackDir, 0755)
		fallbackFile := fmt.Sprintf("%s/failed_sent_email_%d.html", fallbackDir, time.Now().Unix())
		_ = os.WriteFile(fallbackFile, []byte(htmlBody), 0644)
		log.Printf("[EMAIL FALLBACK] Saved failed templated email to local file for preview: %s", fallbackFile)
		
		return fmt.Errorf("failed to send templated email to %s: %w", to, dialErr)
	}
	return nil
}

// SendEmail - backward compatibility (defaults to plain text)
func (s *Service) SendEmail(to, subject, body string) error {
	return s.SendPlainEmail(to, subject, body)
}

// SendEmailWithAttachment sends email with file attachments
func (s *Service) SendEmailWithAttachment(to, subject, body string, attachmentPaths []string) error {
	m := gomail.NewMessage()
	m.SetHeader("From", m.FormatAddress(s.config.EmailUsername, "Map My Tour"))
	m.SetHeader("To", to)
	m.SetHeader("Subject", subject)
	m.SetBody("text/html", GetHTMLTemplateWithTitle(subject, body))

	// Add attachments
	for _, path := range attachmentPaths {
		m.Attach(path)
	}

	d := gomail.NewDialer(s.config.EmailHost, s.config.EmailPort, s.config.EmailUsername, s.config.EmailPassword)
	
	// Configure SSL/TLS based on port (matching auth service)
	if s.config.EmailPort == 465 {
		d.SSL = true
	} else {
		d.SSL = false
	}

	return d.DialAndSend(m)
}

// SendBulkEmail sends email to multiple recipients
func (s *Service) SendBulkEmail(recipients []string, subject, body string) error {
	for _, recipient := range recipients {
		if err := s.SendHTMLEmail(recipient, subject, body); err != nil {
			return err
		}
	}
	return nil
}

// Helper function to check if string contains HTML
func containsHTML(s string) bool {
	if len(s) == 0 {
		return false
	}
	// Convert to lowercase for case-insensitive matching
	sLower := strings.ToLower(s)
	return strings.Contains(sLower, "<html") ||
		strings.Contains(sLower, "<body") ||
		strings.Contains(sLower, "<div") ||
		strings.Contains(sLower, "<p") ||
		strings.Contains(sLower, "<br") ||
		strings.Contains(sLower, "<h") ||
		strings.Contains(sLower, "<table") ||
		strings.Contains(sLower, "<span") ||
		strings.Contains(sLower, "<ul") ||
		strings.Contains(sLower, "<ol") ||
		strings.Contains(sLower, "<li") ||
		strings.Contains(sLower, "<b") ||
		strings.Contains(sLower, "<strong") ||
		strings.Contains(sLower, "<i") ||
		strings.Contains(sLower, "<em") ||
		strings.Contains(sLower, "<a") ||
		strings.Contains(sLower, "<img") ||
		strings.Contains(sLower, "<section") ||
		strings.Contains(sLower, "<article") ||
		strings.Contains(sLower, "<header") ||
		strings.Contains(sLower, "<footer")
}
