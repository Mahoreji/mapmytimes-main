package utils

import (
	"net/http"

	"github.com/gin-gonic/gin"
)

type APIResponse struct {
	Success    bool        `json:"success"`
	StatusCode int         `json:"statusCode"`
	Message    string      `json:"message"`
	Data       interface{} `json:"data,omitempty"`
	Errors     []string    `json:"errors,omitempty"`
}

func SuccessResponse(c *gin.Context, statusCode int, message string, data interface{}) {
	c.JSON(statusCode, APIResponse{
		Success:    true,
		StatusCode: statusCode,
		Message:    message,
		Data:       data,
	})
}

func ErrorResponse(c *gin.Context, statusCode int, message string, errors []string) {
	c.JSON(statusCode, APIResponse{
		Success:    false,
		StatusCode: statusCode,
		Message:    message,
		Errors:     errors,
	})
}

func InternalErrorResponse(c *gin.Context, message string) {
	ErrorResponse(c, http.StatusInternalServerError, message, nil)
}

func BadRequestResponse(c *gin.Context, message string, errors []string) {
	ErrorResponse(c, http.StatusBadRequest, message, errors)
}

func NotFoundResponse(c *gin.Context, message string) {
	ErrorResponse(c, http.StatusNotFound, message, nil)
}
