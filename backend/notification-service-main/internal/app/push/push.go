package push

import (
	"context"
	"encoding/json"
	"fmt"
	"notification-service/config"
	"os"

	firebase "firebase.google.com/go/v4"
	"firebase.google.com/go/v4/messaging"
	"github.com/sirupsen/logrus"
	"google.golang.org/api/option"
)

// PushRequest represents a full push notification payload
type PushRequest struct {
	// Required: one of Token, Topic, or Condition must be set
	Token     string `json:"token,omitempty"`     // Single device FCM token
	Topic     string `json:"topic,omitempty"`     // Topic (e.g. "booking-updates")
	Condition string `json:"condition,omitempty"` // Condition (e.g. "'dogs' in topics && 'cats' in topics")

	// Notification Payload
	Title    string `json:"title"`
	Body     string `json:"body"`
	ImageURL string `json:"imageUrl,omitempty"`

	// Data payload (key-value pairs sent to the app)
	Data map[string]string `json:"data,omitempty"`

	// Android-specific config
	Android *AndroidConfig `json:"android,omitempty"`

	// APNS (iOS)-specific config
	APNS *APNSConfig `json:"apns,omitempty"`
}

type AndroidConfig struct {
	ChannelID string `json:"channelId,omitempty"`
	Priority  string `json:"priority,omitempty"` // "normal" or "high"
	TTL       string `json:"ttl,omitempty"`      // e.g. "3600s"
	Icon      string `json:"icon,omitempty"`
	Color     string `json:"color,omitempty"` // e.g. "#E96B5B"
	Sound     string `json:"sound,omitempty"`
}

type APNSConfig struct {
	Badge int    `json:"badge,omitempty"`
	Sound string `json:"sound,omitempty"`
}

// MulticastRequest for sending to multiple tokens
type MulticastRequest struct {
	Tokens []string `json:"tokens"`
	Title  string   `json:"title"`
	Body   string   `json:"body"`
	Data   map[string]string `json:"data,omitempty"`
}

// BatchResult tracks success/failure for multicast sends
type BatchResult struct {
	SuccessCount int      `json:"successCount"`
	FailureCount int      `json:"failureCount"`
	FailedTokens []string `json:"failedTokens,omitempty"`
}

type Service struct {
	config    *config.Config
	logger    *logrus.Logger
	fcmClient *messaging.Client
	enabled   bool
}

func NewService(cfg *config.Config, logger *logrus.Logger) *Service {
	svc := &Service{
		config: cfg,
		logger: logger,
	}

	// Initialize Firebase
	if err := svc.initFirebase(); err != nil {
		logger.Warn("Firebase FCM not initialized (push notifications disabled): ", err)
		svc.enabled = false
	} else {
		svc.enabled = true
		logger.Info("✅ Firebase FCM push notification service initialized successfully")
	}

	return svc
}

// initFirebase initializes the Firebase Admin SDK
// Supports both file-based and JSON-string service account credentials
func (s *Service) initFirebase() error {
	ctx := context.Background()
	var app *firebase.App
	var err error

	// Option 1: JSON string in env var FCM_SERVICE_ACCOUNT_JSON
	if credJSON := os.Getenv("FCM_SERVICE_ACCOUNT_JSON"); credJSON != "" {
		s.logger.Info("Initializing Firebase from FCM_SERVICE_ACCOUNT_JSON env var")
		opt := option.WithCredentialsJSON([]byte(credJSON))
		app, err = firebase.NewApp(ctx, nil, opt)
	} else if credFile := os.Getenv("FCM_SERVICE_ACCOUNT_FILE"); credFile != "" {
		// Option 2: Path to service account JSON file
		s.logger.Info("Initializing Firebase from FCM_SERVICE_ACCOUNT_FILE: ", credFile)
		opt := option.WithCredentialsFile(credFile)
		app, err = firebase.NewApp(ctx, nil, opt)
	} else if os.Getenv("GOOGLE_APPLICATION_CREDENTIALS") != "" {
		// Option 3: Standard GOOGLE_APPLICATION_CREDENTIALS environment variable
		s.logger.Info("Initializing Firebase from GOOGLE_APPLICATION_CREDENTIALS")
		app, err = firebase.NewApp(ctx, nil)
	} else {
		return fmt.Errorf("no Firebase credentials configured (set FCM_SERVICE_ACCOUNT_JSON, FCM_SERVICE_ACCOUNT_FILE, or GOOGLE_APPLICATION_CREDENTIALS)")
	}

	if err != nil {
		return fmt.Errorf("failed to initialize Firebase app: %w", err)
	}

	s.fcmClient, err = app.Messaging(ctx)
	if err != nil {
		return fmt.Errorf("failed to get FCM messaging client: %w", err)
	}

	return nil
}

// SendPush is the basic interface used by the notification service dispatcher
func (s *Service) SendPush(token, title, body string) error {
	return s.SendToToken(token, title, body, nil)
}

// SendToToken sends a push notification to a single FCM device token
func (s *Service) SendToToken(token, title, body string, data map[string]string) error {
	if !s.enabled {
		s.logger.Warn("FCM not enabled, skipping push to token: ", token)
		return fmt.Errorf("FCM push notifications are not configured")
	}

	if token == "" {
		return fmt.Errorf("device token is required")
	}

	msg := &messaging.Message{
		Token: token,
		Notification: &messaging.Notification{
			Title: title,
			Body:  body,
		},
		Android: &messaging.AndroidConfig{
			Notification: &messaging.AndroidNotification{
				Title:       title,
				Body:        body,
				Color:       "#E96B5B",
				ChannelID:   "mapmytour_default",
				DefaultSound: true,
			},
			Priority: "high",
		},
		APNS: &messaging.APNSConfig{
			Payload: &messaging.APNSPayload{
				Aps: &messaging.Aps{
					Alert: &messaging.ApsAlert{
						Title: title,
						Body:  body,
					},
					Sound: "default",
					Badge: intPtr(1),
				},
			},
		},
	}

	// Attach custom data if provided
	if len(data) > 0 {
		msg.Data = data
	}

	ctx := context.Background()
	msgID, err := s.fcmClient.Send(ctx, msg)
	if err != nil {
		s.logger.Error("FCM send failed for token ", token[:min(len(token), 10)], "...: ", err)
		return fmt.Errorf("failed to send push notification: %w", err)
	}

	s.logger.Info("✅ Push notification sent successfully, messageID: ", msgID)
	return nil
}

// SendToTopic sends a push notification to all subscribers of a topic
func (s *Service) SendToTopic(topic, title, body string, data map[string]string) error {
	if !s.enabled {
		return fmt.Errorf("FCM push notifications are not configured")
	}

	msg := &messaging.Message{
		Topic: topic,
		Notification: &messaging.Notification{
			Title: title,
			Body:  body,
		},
		Android: &messaging.AndroidConfig{
			Notification: &messaging.AndroidNotification{
				Title:       title,
				Body:        body,
				Color:       "#E96B5B",
				ChannelID:   "mapmytour_default",
				DefaultSound: true,
			},
			Priority: "high",
		},
		APNS: &messaging.APNSConfig{
			Payload: &messaging.APNSPayload{
				Aps: &messaging.Aps{
					Alert: &messaging.ApsAlert{
						Title: title,
						Body:  body,
					},
					Sound: "default",
				},
			},
		},
		Data: data,
	}

	ctx := context.Background()
	msgID, err := s.fcmClient.Send(ctx, msg)
	if err != nil {
		s.logger.Error("FCM topic send failed for topic '", topic, "': ", err)
		return fmt.Errorf("failed to send topic push notification: %w", err)
	}

	s.logger.Infof("✅ Topic push sent to '%s', messageID: %s", topic, msgID)
	return nil
}

// SendMulticast sends a notification to multiple device tokens at once (max 500 per batch)
func (s *Service) SendMulticast(tokens []string, title, body string, data map[string]string) (*BatchResult, error) {
	if !s.enabled {
		return nil, fmt.Errorf("FCM push notifications are not configured")
	}

	if len(tokens) == 0 {
		return nil, fmt.Errorf("at least one device token is required")
	}

	// FCM multicast supports max 500 tokens per batch
	const batchSize = 500
	result := &BatchResult{}

	for i := 0; i < len(tokens); i += batchSize {
		end := i + batchSize
		if end > len(tokens) {
			end = len(tokens)
		}
		batch := tokens[i:end]

		msg := &messaging.MulticastMessage{
			Tokens: batch,
			Notification: &messaging.Notification{
				Title: title,
				Body:  body,
			},
			Android: &messaging.AndroidConfig{
				Notification: &messaging.AndroidNotification{
					Color:        "#E96B5B",
					ChannelID:    "mapmytour_default",
					DefaultSound: true,
				},
				Priority: "high",
			},
			APNS: &messaging.APNSConfig{
				Payload: &messaging.APNSPayload{
					Aps: &messaging.Aps{
						Alert: &messaging.ApsAlert{
							Title: title,
							Body:  body,
						},
						Sound: "default",
					},
				},
			},
			Data: data,
		}

		ctx := context.Background()
		batchResp, err := s.fcmClient.SendEachForMulticast(ctx, msg)
		if err != nil {
			s.logger.Error("FCM multicast batch failed: ", err)
			result.FailureCount += len(batch)
			result.FailedTokens = append(result.FailedTokens, batch...)
			continue
		}

		result.SuccessCount += batchResp.SuccessCount
		result.FailureCount += batchResp.FailureCount

		// Collect failed tokens for cleanup/retry
		for j, r := range batchResp.Responses {
			if !r.Success {
				result.FailedTokens = append(result.FailedTokens, batch[j])
				s.logger.Warnf("Token %s... failed: %v", batch[j][:min(len(batch[j]), 10)], r.Error)
			}
		}
	}

	s.logger.Infof("Multicast complete: %d success, %d failed (out of %d)", result.SuccessCount, result.FailureCount, len(tokens))
	return result, nil
}

// SendAdvanced sends a fully customized push notification using the PushRequest struct
func (s *Service) SendAdvanced(req *PushRequest) error {
	if !s.enabled {
		return fmt.Errorf("FCM push notifications are not configured")
	}

	msg := &messaging.Message{
		Notification: &messaging.Notification{
			Title:    req.Title,
			Body:     req.Body,
			ImageURL: req.ImageURL,
		},
	}

	// Set delivery target
	if req.Token != "" {
		msg.Token = req.Token
	} else if req.Topic != "" {
		msg.Topic = req.Topic
	} else if req.Condition != "" {
		msg.Condition = req.Condition
	} else {
		return fmt.Errorf("one of token, topic, or condition must be set")
	}

	// Android config
	if req.Android != nil {
		msg.Android = &messaging.AndroidConfig{
			Notification: &messaging.AndroidNotification{
				Title:        req.Title,
				Body:         req.Body,
				ChannelID:    req.Android.ChannelID,
				Color:        req.Android.Color,
				Sound:        req.Android.Sound,
				DefaultSound: req.Android.Sound == "",
			},
			Priority: req.Android.Priority,
		}
		if req.Android.Priority == "" {
			msg.Android.Priority = "high"
		}
	} else {
		msg.Android = &messaging.AndroidConfig{
			Notification: &messaging.AndroidNotification{
				Color:        "#E96B5B",
				ChannelID:    "mapmytour_default",
				DefaultSound: true,
			},
			Priority: "high",
		}
	}

	// APNS config
	if req.APNS != nil {
		msg.APNS = &messaging.APNSConfig{
			Payload: &messaging.APNSPayload{
				Aps: &messaging.Aps{
					Alert: &messaging.ApsAlert{
						Title: req.Title,
						Body:  req.Body,
					},
					Sound: req.APNS.Sound,
					Badge: intPtr(req.APNS.Badge),
				},
			},
		}
	} else {
		msg.APNS = &messaging.APNSConfig{
			Payload: &messaging.APNSPayload{
				Aps: &messaging.Aps{
					Alert: &messaging.ApsAlert{
						Title: req.Title,
						Body:  req.Body,
					},
					Sound: "default",
					Badge: intPtr(1),
				},
			},
		}
	}

	// Data payload
	if len(req.Data) > 0 {
		msg.Data = req.Data
	}

	ctx := context.Background()
	msgID, err := s.fcmClient.Send(ctx, msg)
	if err != nil {
		s.logger.Error("FCM advanced send failed: ", err)
		return fmt.Errorf("failed to send advanced push notification: %w", err)
	}

	s.logger.Info("✅ Advanced push sent, messageID: ", msgID)
	return nil
}

// SubscribeToTopic subscribes a list of device tokens to an FCM topic
func (s *Service) SubscribeToTopic(tokens []string, topic string) error {
	if !s.enabled {
		return fmt.Errorf("FCM push notifications are not configured")
	}

	ctx := context.Background()
	resp, err := s.fcmClient.SubscribeToTopic(ctx, tokens, topic)
	if err != nil {
		return fmt.Errorf("failed to subscribe to topic: %w", err)
	}

	s.logger.Infof("Topic subscription: %d success, %d failed for topic '%s'", resp.SuccessCount, resp.FailureCount, topic)
	return nil
}

// UnsubscribeFromTopic unsubscribes a list of device tokens from an FCM topic
func (s *Service) UnsubscribeFromTopic(tokens []string, topic string) error {
	if !s.enabled {
		return fmt.Errorf("FCM push notifications are not configured")
	}

	ctx := context.Background()
	resp, err := s.fcmClient.UnsubscribeFromTopic(ctx, tokens, topic)
	if err != nil {
		return fmt.Errorf("failed to unsubscribe from topic: %w", err)
	}

	s.logger.Infof("Topic unsubscription: %d success, %d failed for topic '%s'", resp.SuccessCount, resp.FailureCount, topic)
	return nil
}

// IsEnabled returns whether the FCM service is properly configured and ready
func (s *Service) IsEnabled() bool {
	return s.enabled
}

// SendPushFromRaw builds and sends a push from a JSON-encoded PushRequest string
// Used when the notification Body is a JSON-encoded push request
func (s *Service) SendPushFromRaw(recipient, title, rawBody string) error {
	// Try to parse rawBody as a PushRequest JSON
	var req PushRequest
	if err := json.Unmarshal([]byte(rawBody), &req); err == nil && req.Token != "" {
		// It's a full PushRequest — send it
		return s.SendAdvanced(&req)
	}

	// Fall back: treat recipient as token, title/body as direct values
	return s.SendToToken(recipient, title, rawBody, nil)
}

// Helper
func intPtr(i int) *int {
	return &i
}

func min(a, b int) int {
	if a < b {
		return a
	}
	return b
}
