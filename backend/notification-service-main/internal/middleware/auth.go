package middleware

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"net/http"
	"notification-service/config"
	"notification-service/internal/utils"
	"sort"
	"strings"

	"github.com/gin-gonic/gin"
)

// AuthMiddleware validates authentication from API Gateway (JWT) or direct API key
func AuthMiddleware(cfg *config.Config) gin.HandlerFunc {
	return func(c *gin.Context) {
		// Skip auth if disabled
		if !cfg.EnableAuth {
			c.Next()
			return
		}

		// Check if request comes from API Gateway (trust gateway's authentication)
		authenticated := c.GetHeader("X-Authenticated")
		requestSource := c.GetHeader("X-Request-Source")
		timestamp := c.GetHeader("X-Gateway-Timestamp")
		signature := c.GetHeader("X-Gateway-Signature")

		isFromGateway := requestSource == "api-gateway"
		isInternalCall := requestSource == "internal-service"

		if isFromGateway || isInternalCall {
			// Verify HMAC signature
			headersToVerify := map[string]string{
				"X-User-Id":           c.GetHeader("X-User-Id"),
				"X-User-Email":        c.GetHeader("X-User-Email"),
				"X-User-Role":         c.GetHeader("X-User-Role"),
				"X-Authenticated":     c.GetHeader("X-Authenticated"),
				"X-Request-Source":    requestSource,
				"X-Gateway-Timestamp": timestamp,
			}

			if !verifyHMACSignature(headersToVerify, signature, cfg.JWTSecret) {
				utils.ErrorResponse(c, http.StatusForbidden, "Invalid request signature", []string{"The request signature does not match the expected value for " + requestSource})
				c.Abort()
				return
			}
		} else {
			utils.ErrorResponse(c, http.StatusForbidden, "Direct access forbidden", []string{"Direct access to this service is NOT allowed. Access must be via API Gateway or internal trust."})
			c.Abort()
			return
		}

		// Extraction of user context for gateway calls
		// Extract user context from gateway headers
		userId := c.GetHeader("X-User-Id")
		userEmail := c.GetHeader("X-User-Email")
		userRole := c.GetHeader("X-User-Role")
		
		// If authenticated is true, we must have a valid user context
		if authenticated == "true" {
			if userId == "" || userRole == "" {
				utils.ErrorResponse(c, http.StatusUnauthorized, "Missing user context", []string{"Request is marked as authenticated but missing user ID or role"})
				c.Abort()
				return
			}
		}

		// Store user context in gin context for use in handlers
		c.Set("userId", userId)
		c.Set("userEmail", userEmail)
		c.Set("userRole", userRole)
		c.Set("authenticated", authenticated == "true")
		c.Set("authSource", "api-gateway")
		
		c.Next()
	}
}

// verifyHMACSignature verifies the HMAC-SHA256 signature
func verifyHMACSignature(headers map[string]string, receivedSignature string, secret string) bool {
	if receivedSignature == "" {
		return false
	}

	// Sort keys for consistent data string
	keys := make([]string, 0, len(headers))
	for k := range headers {
		keys = append(keys, k)
	}
	sort.Strings(keys)

	var sb strings.Builder
	for _, k := range keys {
		if v := headers[k]; v != "" {
			sb.WriteString(k)
			sb.WriteString("=")
			sb.WriteString(v)
			sb.WriteString(";")
		}
	}

	data := sb.String()
	h := hmac.New(sha256.New, []byte(secret))
	h.Write([]byte(data))
	expectedSignature := base64.StdEncoding.EncodeToString(h.Sum(nil))

	return receivedSignature == expectedSignature
}
