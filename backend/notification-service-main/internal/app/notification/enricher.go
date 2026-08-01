package notification

import (
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"notification-service/config"
	"notification-service/internal/db"
	"strconv"
	"strings"
	"time"
)

// Enricher manages REST API requests to other microservices to pull context.
type Enricher struct {
	cfg        *config.Config
	httpClient *http.Client
}

// NewEnricher constructs an Enricher instance with timeout limits.
func NewEnricher(cfg *config.Config) *Enricher {
	return &Enricher{
		cfg: cfg,
		httpClient: &http.Client{
			Timeout: 2 * time.Second,
		},
	}
}

// MapToDto maps the DB Notification entity to a structured base Response DTO.
func (e *Enricher) MapToDto(n *db.Notification) (*NotificationResponseDto, map[string]string) {
	// Parse metadata
	var metadataMap map[string]string
	if n.Metadata != "" {
		_ = json.Unmarshal([]byte(n.Metadata), &metadataMap)
	}
	if metadataMap == nil {
		metadataMap = make(map[string]string)
	}

	// Resolve builder using granular EventType (fall back to Type for migration support)
	notifType := n.EventType
	if notifType == "" {
		notifType = n.Type
	}
	builder := GetBuilder(notifType, metadataMap)

	// Determine category, priority, title, and message
	category := n.Category
	if category == "" {
		category = builder.GetCategory()
	}
	priority := n.Priority
	if priority == "" {
		priority = builder.GetPriority()
	}
	
	title := n.Title
	if title == "" {
		title = builder.BuildTitle(metadataMap)
	}
	
	message := n.Message
	if message == "" {
		message = builder.BuildMessage(metadataMap)
	}

	// Build nested delivery block
	deliveryChannel := n.DeliveryChannel
	if deliveryChannel == "" {
		deliveryChannel = builder.GetDefaultChannel()
	}
	deliveryStatus := n.DeliveryStatus
	if deliveryStatus == "" {
		deliveryStatus = "DELIVERED"
	}
	
	sentAtStr := time.Unix(n.CreatedAt, 0).Format(time.RFC3339)
	if n.SentAt != nil && *n.SentAt > 0 {
		sentAtStr = time.Unix(*n.SentAt, 0).Format(time.RFC3339)
	}
	
	deliveredAtStr := sentAtStr
	if n.DeliveredAt != nil && *n.DeliveredAt > 0 {
		deliveredAtStr = time.Unix(*n.DeliveredAt, 0).Format(time.RFC3339)
	}

	deliveryDto := &NotificationDeliveryDto{
		Channel:     deliveryChannel,
		Status:      deliveryStatus,
		SentAt:      sentAtStr,
		DeliveredAt: deliveredAtStr,
	}
	if n.FailedReason != "" {
		deliveryDto.FailedReason = &n.FailedReason
	}

	createdAtStr := time.Unix(n.CreatedAt, 0).Format(time.RFC3339)
	updatedAtStr := time.Unix(n.UpdatedAt, 0).Format(time.RFC3339)
	
	var expiresAtStr string
	if n.ExpiresAt != nil && *n.ExpiresAt > 0 {
		expiresAtStr = time.Unix(*n.ExpiresAt, 0).Format(time.RFC3339)
	} else {
		// Default expires in 30 days
		expiresAtStr = time.Unix(n.CreatedAt+2592000, 0).Format(time.RFC3339)
	}

	var readAtStr string
	if n.ReadAt != nil && *n.ReadAt > 0 {
		readAtStr = time.Unix(*n.ReadAt, 0).Format(time.RFC3339)
	}

	dto := &NotificationResponseDto{
		NotificationID: n.ID,
		Type:           builder.GetType(),
		Category:       category,
		EventType:      builder.GetEventType(),
		Priority:       priority,
		Title:          title,
		Message:        message,
		IsRead:         n.Read,
		IsArchived:     n.IsArchived,
		ReadAt:         readAtStr,
		CreatedAt:      createdAtStr,
		UpdatedAt:      updatedAtStr,
		ExpiresAt:      expiresAtStr,
		Delivery:       deliveryDto,
		Action:         builder.BuildAction(metadataMap),
		Ui:             builder.BuildUi(metadataMap),
	}

	return dto, metadataMap
}

// Enrich resolves booking, payment, and user contextual details (HTTP with robust metadata fallback).
func (e *Enricher) Enrich(dto *NotificationResponseDto, metadataMap map[string]string) {
	// 1. Resolve Booking Context
	bookingId := metadataMap["bookingId"]
	if bookingId == "" {
		bookingId = metadataMap["booking_id"]
	}
	bookingRef := metadataMap["booking_ref"]
	if bookingRef == "" {
		bookingRef = metadataMap["bookingReference"]
	}

	if bookingId != "" || bookingRef != "" {
		dto.Booking = e.fetchBookingDetails(bookingId, bookingRef, metadataMap)
	}

	// 2. Resolve Payment Context
	paymentId := metadataMap["paymentId"]
	if paymentId == "" {
		paymentId = metadataMap["payment_id"]
	}
	if paymentId != "" {
		dto.Payment = e.fetchPaymentDetails(paymentId, metadataMap)
	}

	// 3. Resolve Refund Context
	refundId := metadataMap["refundId"]
	if refundId == "" {
		refundId = metadataMap["refund_id"]
	}
	if refundId != "" {
		dto.Refund = e.fetchRefundDetails(refundId, metadataMap)
	}

	// 4. Resolve User Context
	userId := metadataMap["userId"]
	if userId == "" {
		userId = metadataMap["user_id"]
	}
	email := metadataMap["email"]
	if email == "" {
		email = metadataMap["recipient"]
	}
	if userId != "" || email != "" {
		dto.User = e.resolveUserContext(userId, email, metadataMap)
	}
}

// fetchBookingDetails queries booking-service or falls back to metadata attributes.
func (e *Enricher) fetchBookingDetails(bookingId, bookingRef string, metadataMap map[string]string) *BookingSummaryDto {
	var bookingDto *BookingSummaryDto

	// Attempt real-time HTTP fetch
	if bookingId != "" && e.cfg.BookingServiceURL != "" {
		url := fmt.Sprintf("%s/api/v1/bookings/%s", e.cfg.BookingServiceURL, bookingId)
		req, err := http.NewRequest("GET", url, nil)
		if err == nil {
			req.Header.Set("X-User-Id", "system_enricher")
			req.Header.Set("X-User-Role", "ROLE_ADMIN")
			
			resp, err := e.httpClient.Do(req)
			if err == nil && resp.StatusCode == http.StatusOK {
				defer resp.Body.Close()
				bodyBytes, _ := io.ReadAll(resp.Body)
				
				var wrapper struct {
					Success bool               `json:"success"`
					Data    *BookingSummaryDto `json:"data"`
				}
				if err := json.Unmarshal(bodyBytes, &wrapper); err == nil && wrapper.Data != nil {
					bookingDto = wrapper.Data
				} else {
					_ = json.Unmarshal(bodyBytes, &bookingDto)
				}
			}
		}
	}

	// Fallback to metadata mapping
	if bookingDto == nil {
		bookingDto = &BookingSummaryDto{}
	}

	if bookingDto.BookingID == "" {
		bookingDto.BookingID = bookingId
	}
	if bookingDto.BookingID == "" {
		bookingDto.BookingID = bookingRef
	}
	if bookingDto.BookingStatus == "" {
		bookingDto.BookingStatus = getFallback(metadataMap, "bookingStatus", "booking_status", "CONFIRMED")
	}

	// Dynamic Booking Type resolving
	bookingDto.BookingType = getFallback(metadataMap, "bookingType", "booking_type", "")
	if bookingDto.BookingType == "" {
		srv := strings.ToUpper(getFallback(metadataMap, "serviceName", "service_name", ""))
		if strings.Contains(srv, "FLIGHT") {
			bookingDto.BookingType = "FLIGHT"
		} else if strings.Contains(srv, "HOTEL") {
			bookingDto.BookingType = "HOTEL"
		} else if strings.Contains(srv, "BUS") {
			bookingDto.BookingType = "BUS"
		} else if strings.Contains(srv, "TRAIN") {
			bookingDto.BookingType = "TRAIN"
		} else if strings.Contains(srv, "TOUR") || strings.Contains(srv, "PACKAGE") {
			bookingDto.BookingType = "TOUR"
		} else if strings.Contains(srv, "ACTIVITY") {
			bookingDto.BookingType = "ACTIVITY"
		} else if strings.Contains(srv, "VISA") {
			bookingDto.BookingType = "VISA"
		} else if strings.Contains(srv, "CAB") || strings.Contains(srv, "TAXI") {
			bookingDto.BookingType = "CAB"
		} else {
			bookingDto.BookingType = "TOUR"
		}
	}

	bookingDto.ServiceName = getFallback(metadataMap, "serviceName", "service_name", "Travel Booking")
	
	pCount, _ := strconv.Atoi(getFallback(metadataMap, "passengerCount", "passenger_count", "1"))
	bookingDto.PassengerCount = pCount
	
	bookingDto.TravelDate = getFallback(metadataMap, "travelDate", "booking_date", "")
	bookingDto.ReturnDate = getFallback(metadataMap, "returnDate", "return_date", "")
	bookingDto.Destination = getFallback(metadataMap, "destination", "arrival", "")
	
	amtStr := getFallback(metadataMap, "totalAmount", "amount", "0")
	amt, _ := strconv.ParseFloat(amtStr, 64)
	bookingDto.TotalAmount = amt
	
	bookingDto.Currency = getFallback(metadataMap, "currency", "currency_code", "INR")
	
	vAvailStr := getFallback(metadataMap, "voucherAvailable", "voucher_available", "false")
	bookingDto.VoucherAvailable = vAvailStr == "true"

	// Resolve variant service attributes dynamically
	switch bookingDto.BookingType {
	case "TOUR":
		bookingDto.PackageName = getFallback(metadataMap, "packageName", "package_name", bookingDto.ServiceName)
		days, _ := strconv.Atoi(getFallback(metadataMap, "totalDays", "total_days", "3"))
		bookingDto.TotalDays = days
	case "FLIGHT":
		bookingDto.Airline = getFallback(metadataMap, "airline", "carrier", "")
		bookingDto.FlightNumber = getFallback(metadataMap, "flightNumber", "flight_number", "")
		bookingDto.From = getFallback(metadataMap, "from", "source", "")
		bookingDto.To = getFallback(metadataMap, "to", "destination", "")
	case "HOTEL":
		bookingDto.HotelName = getFallback(metadataMap, "hotelName", "hotel_name", bookingDto.ServiceName)
		bookingDto.CheckInDate = getFallback(metadataMap, "checkInDate", "check_in", bookingDto.TravelDate)
		bookingDto.CheckOutDate = getFallback(metadataMap, "checkOutDate", "check_out", bookingDto.ReturnDate)
		rCount, _ := strconv.Atoi(getFallback(metadataMap, "roomCount", "room_count", "1"))
		bookingDto.RoomCount = rCount
	case "BUS":
		bookingDto.OperatorName = getFallback(metadataMap, "operatorName", "operator_name", bookingDto.ServiceName)
		bookingDto.DepartureCity = getFallback(metadataMap, "departureCity", "departure_city", "")
		bookingDto.ArrivalCity = getFallback(metadataMap, "arrivalCity", "arrival_city", "")
	case "ACTIVITY":
		bookingDto.ActivityName = getFallback(metadataMap, "activityName", "activity_name", bookingDto.ServiceName)
		bookingDto.ActivityDate = getFallback(metadataMap, "activityDate", "activity_date", bookingDto.TravelDate)
		bookingDto.ActivityTime = getFallback(metadataMap, "activityTime", "activity_time", "12:00")
	}

	return bookingDto
}

// fetchPaymentDetails queries payment-service or falls back to metadata attributes.
func (e *Enricher) fetchPaymentDetails(paymentId string, metadataMap map[string]string) *PaymentSummaryDto {
	var paymentDto *PaymentSummaryDto

	if paymentId != "" && e.cfg.PaymentServiceURL != "" {
		url := fmt.Sprintf("%s/api/v1/payment/payments/%s/", e.cfg.PaymentServiceURL, paymentId)
		resp, err := e.httpClient.Get(url)
		if err == nil && resp.StatusCode == http.StatusOK {
			defer resp.Body.Close()
			bodyBytes, _ := io.ReadAll(resp.Body)
			
			var wrapper struct {
				Success bool               `json:"success"`
				Data    *PaymentSummaryDto `json:"data"`
			}
			if err := json.Unmarshal(bodyBytes, &wrapper); err == nil && wrapper.Data != nil {
				paymentDto = wrapper.Data
			} else {
				_ = json.Unmarshal(bodyBytes, &paymentDto)
			}
		}
	}

	if paymentDto == nil {
		paymentDto = &PaymentSummaryDto{}
	}

	if paymentDto.PaymentID == "" {
		paymentDto.PaymentID = paymentId
	}
	if paymentDto.PaymentStatus == "" {
		paymentDto.PaymentStatus = getFallback(metadataMap, "paymentStatus", "payment_status", "SUCCESS")
	}
	if paymentDto.PaymentMethod == "" {
		paymentDto.PaymentMethod = getFallback(metadataMap, "paymentMethod", "payment_method", "UPI")
	}
	if paymentDto.TransactionID == "" {
		paymentDto.TransactionID = getFallback(metadataMap, "transactionId", "transaction_id", "")
	}
	if paymentDto.Currency == "" {
		paymentDto.Currency = getFallback(metadataMap, "currency", "currency_code", "INR")
	}
	if paymentDto.Amount == 0 {
		amtStr := getFallback(metadataMap, "amount", "totalAmount", "0")
		amt, _ := strconv.ParseFloat(amtStr, 64)
		paymentDto.Amount = amt
	}

	return paymentDto
}

// fetchRefundDetails queries payment-service or falls back to metadata attributes.
func (e *Enricher) fetchRefundDetails(refundId string, metadataMap map[string]string) *RefundSummaryDto {
	var refundDto *RefundSummaryDto

	if refundDto == nil {
		refundDto = &RefundSummaryDto{}
	}

	refundDto.RefundID = refundId
	refundDto.Status = getFallback(metadataMap, "refundStatus", "status", "COMPLETED")
	refundDto.Currency = getFallback(metadataMap, "currency", "currency_code", "INR")
	
	amtStr := getFallback(metadataMap, "amount", "totalAmount", "0")
	amt, _ := strconv.ParseFloat(amtStr, 64)
	refundDto.Amount = amt

	refundDto.ProcessedAt = getFallback(metadataMap, "processedAt", "processed_at", time.Now().Format(time.RFC3339))

	return refundDto
}

// resolveUserContext pulls user name, phone, etc., out of the metadata variables.
func (e *Enricher) resolveUserContext(userId, email string, metadataMap map[string]string) *NotificationUserDto {
	firstName := getFallback(metadataMap, "firstName", "name", "")
	if firstName == "" {
		firstName = getFallback(metadataMap, "first_name", "userName", "")
	}
	lastName := getFallback(metadataMap, "lastName", "last_name", "")

	return &NotificationUserDto{
		UserID:    userId,
		FirstName: firstName,
		LastName:  lastName,
	}
}

// Custom fallback helper to find keys and return default value if not found
func getFallback(m map[string]string, key1, key2, fallback string) string {
	if v, ok := m[key1]; ok && v != "" {
		return v
	}
	if v, ok := m[key2]; ok && v != "" {
		return v
	}
	return fallback
}
