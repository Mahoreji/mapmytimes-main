package main

import (
	"fmt"
	"log"
	"notification-service/config"
	"notification-service/internal/db"

	"github.com/joho/godotenv"
)

func main() {
	// Load environment variables
	if err := godotenv.Load(); err != nil {
		log.Println("No .env file found")
	}

	// Initialize configuration
	cfg := config.Load()

	// Initialize database
	database, err := db.InitPostgres(cfg)
	if err != nil {
		log.Fatal("Failed to connect to database: ", err)
	}

	// Run migrations
	if err := db.RunMigrations(database); err != nil {
		log.Fatal("Failed to run migrations: ", err)
	}

	fmt.Println("✅ Database seeded successfully!")
}
