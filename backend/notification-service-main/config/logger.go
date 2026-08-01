package config

import (
	"fmt"
	"os"
	"time"

	"github.com/sirupsen/logrus"
)

func InitLogger() *logrus.Logger {
	logger := logrus.New()

	// Create logs directory
	if err := os.MkdirAll("logs", 0755); err != nil {
		panic(fmt.Sprintf("Failed to create logs directory: %v", err))
	}

	// Set up log file with date
	logFileName := fmt.Sprintf("logs/notification-service-%s.log", time.Now().Format("2006-01-02"))
	logFile, err := os.OpenFile(logFileName, os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0666)
	if err != nil {
		panic(fmt.Sprintf("Failed to open log file: %v", err))
	}

	// Set up error log file
	errorLogFile, err := os.OpenFile("logs/error.log", os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0666)
	if err != nil {
		panic(fmt.Sprintf("Failed to open error log file: %v", err))
	}

	// Configure logger
	logger.SetOutput(logFile)
	logger.SetLevel(logrus.InfoLevel)
	logger.SetFormatter(&logrus.JSONFormatter{
		TimestampFormat: time.RFC3339,
	})

	// Add hook for error logging
	logger.AddHook(&ErrorFileHook{errorLogFile})

	return logger
}

type ErrorFileHook struct {
	file *os.File
}

func (hook *ErrorFileHook) Fire(entry *logrus.Entry) error {
	if entry.Level <= logrus.ErrorLevel {
		line, err := entry.String()
		if err != nil {
			return err
		}
		_, err = hook.file.WriteString(line)
		return err
	}
	return nil
}

func (hook *ErrorFileHook) Levels() []logrus.Level {
	return []logrus.Level{
		logrus.PanicLevel,
		logrus.FatalLevel,
		logrus.ErrorLevel,
	}
}