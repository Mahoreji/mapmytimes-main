package middleware

import (
	"net/http"
	"notification-service/config"
	"notification-service/internal/utils"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/go-redis/redis/v8"
	"golang.org/x/time/rate"
)
	
// RateLimitMiddleware implements rate limiting using token bucket algorithm
func RateLimitMiddleware(cfg *config.Config, redisClient *redis.Client) gin.HandlerFunc {
	if !cfg.RateLimitEnabled {
		return func(c *gin.Context) {
			c.Next()
		}
	}

	// Create rate limiter
	limiter := rate.NewLimiter(rate.Limit(cfg.RateLimitRPS), cfg.RateLimitBurst)

	return func(c *gin.Context) {
		// Use client IP for rate limiting
		clientIP := c.ClientIP()

		// Try to get from Redis first (for distributed rate limiting)
		if redisClient != nil {
			ctx := c.Request.Context()
			key := "rate_limit:" + clientIP
			
			// Check current count
			count, err := redisClient.Get(ctx, key).Int()
			if err != nil && err != redis.Nil {
				// Redis error, fall back to in-memory limiter
				if !limiter.Allow() {
					utils.ErrorResponse(c, http.StatusTooManyRequests, "Rate limit exceeded", []string{"Too many requests. Please try again later."})
					c.Abort()
					return
				}
				c.Next()
				return
			}

			// If count exists and exceeds limit
			if count >= cfg.RateLimitRPS {
				utils.ErrorResponse(c, http.StatusTooManyRequests, "Rate limit exceeded", []string{"Too many requests. Please try again later."})
			c.Abort()
			return
			}

			// Increment count
			pipe := redisClient.Pipeline()
			pipe.Incr(ctx, key)
			pipe.Expire(ctx, key, time.Second)
			_, _ = pipe.Exec(ctx)
		} else {
			// Fallback to in-memory limiter
			if !limiter.Allow() {
				utils.ErrorResponse(c, http.StatusTooManyRequests, "Rate limit exceeded", []string{"Too many requests. Please try again later."})
				c.Abort()
				return
			}
		}

		c.Next()
	}
}
