package middleware

import (
	"notification-service/config"
	"strings"

	"github.com/gin-gonic/gin"
)

// CORSMiddleware handles CORS with configurable allowed origins
func CORSMiddleware(cfg *config.Config) gin.HandlerFunc {
	return func(c *gin.Context) {
		origin := c.Request.Header.Get("Origin")
		
		// If no CORS origins configured, deny all
		if len(cfg.CORSAllowedOrigins) == 0 {
			c.Header("Access-Control-Allow-Origin", "null")
			c.Next()
			return
		}

		// Check if origin is allowed
		allowed := false
		for _, allowedOrigin := range cfg.CORSAllowedOrigins {
			if allowedOrigin == "*" {
				allowed = true
				break
			}
			if origin != "" && (allowedOrigin == origin || strings.HasSuffix(origin, allowedOrigin)) {
				allowed = true
				break
			}
		}

		if allowed {
			c.Header("Access-Control-Allow-Origin", origin)
		} else {
			c.Header("Access-Control-Allow-Origin", "null")
		}

		// Set CORS headers
		c.Header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD")
		c.Header("Access-Control-Allow-Headers", "Accept, Authorization, Content-Type, Content-Length, X-CSRF-Token, X-API-Key, X-Requested-With")
		c.Header("Access-Control-Allow-Credentials", "true")
		c.Header("Access-Control-Expose-Headers", "Content-Length, Content-Type")
		c.Header("Access-Control-Max-Age", "86400")

		// Handle preflight requests
		if c.Request.Method == "OPTIONS" {
			c.AbortWithStatus(204)
			return
		}

		c.Next()
	}
}
