package notification

import (
	"strings"
)

// NotificationBuilder defines standard actions to construct the custom redesigned payload block.
type NotificationBuilder interface {
	GetType() string          // TOUR, FLIGHT, HOTEL, BUS, ACTIVITY, TRANSFER, VISA, INSURANCE, PAYMENT, WALLET, SYSTEM
	GetCategory() string      // BOOKING, PAYMENT, REFUND, CANCELLATION, VOUCHER, REMINDER, PROMOTION, SOCIAL, WALLET, SYSTEM
	GetEventType() string     // BOOKING_CREATED, BOOKING_CONFIRMED, PAYMENT_SUCCESS, REFUND_COMPLETED, etc.
	GetPriority() string      // LOW, MEDIUM, HIGH, CRITICAL
	GetDefaultChannel() string // EMAIL, SMS, PUSH, WHATSAPP, IN_APP
	BuildTitle(params map[string]string) string
	BuildMessage(params map[string]string) string
	BuildAction(params map[string]string) *NotificationActionDto
	BuildUi(params map[string]string) *NotificationUiDto
}

// 1. Tour Booking Confirmed Strategy
type TourBookingConfirmedBuilder struct {
	EvType string
}

func (b *TourBookingConfirmedBuilder) GetType() string           { return "TOUR" }
func (b *TourBookingConfirmedBuilder) GetCategory() string       { return "BOOKING" }
func (b *TourBookingConfirmedBuilder) GetEventType() string      { return b.EvType }
func (b *TourBookingConfirmedBuilder) GetPriority() string       { return "HIGH" }
func (b *TourBookingConfirmedBuilder) GetDefaultChannel() string { return "EMAIL" }
func (b *TourBookingConfirmedBuilder) BuildTitle(params map[string]string) string {
	return "Tour Booking Confirmed"
}
func (b *TourBookingConfirmedBuilder) BuildMessage(params map[string]string) string {
	pkgName := params["packageName"]
	if pkgName == "" {
		pkgName = params["packageName"]
	}
	if pkgName == "" {
		pkgName = params["serviceName"]
	}
	if pkgName == "" {
		pkgName = params["service_name"]
	}
	if pkgName == "" {
		pkgName = "Holiday Package"
	}
	return Localize("notification.tour.confirmed", "en", map[string]string{"packageName": pkgName})
}
func (b *TourBookingConfirmedBuilder) BuildAction(params map[string]string) *NotificationActionDto {
	return &NotificationActionDto{
		Label: "View Booking",
		Type:  "INTERNAL",
		URL:   "/bookings/" + getBookingId(params),
	}
}
func (b *TourBookingConfirmedBuilder) BuildUi(params map[string]string) *NotificationUiDto {
	return &NotificationUiDto{
		Icon:     "booking-confirmed",
		Color:    "green",
		Badge:    "Confirmed",
		Deeplink: "/bookings/" + getBookingId(params),
	}
}

// 2. Flight Booking Confirmed Strategy
type FlightBookingConfirmedBuilder struct {
	EvType string
}

func (b *FlightBookingConfirmedBuilder) GetType() string           { return "FLIGHT" }
func (b *FlightBookingConfirmedBuilder) GetCategory() string       { return "BOOKING" }
func (b *FlightBookingConfirmedBuilder) GetEventType() string      { return b.EvType }
func (b *FlightBookingConfirmedBuilder) GetPriority() string       { return "HIGH" }
func (b *FlightBookingConfirmedBuilder) GetDefaultChannel() string { return "PUSH" }
func (b *FlightBookingConfirmedBuilder) BuildTitle(params map[string]string) string {
	return "Flight Booking Confirmed"
}
func (b *FlightBookingConfirmedBuilder) BuildMessage(params map[string]string) string {
	from := params["from"]
	if from == "" {
		from = params["source"]
	}
	if from == "" {
		from = params["departure"]
	}
	if from == "" {
		from = "departure city"
	}
	
	to := params["to"]
	if to == "" {
		to = params["destination"]
	}
	if to == "" {
		to = params["arrival"]
	}
	if to == "" {
		to = "arrival city"
	}
	return Localize("notification.flight.confirmed", "en", map[string]string{"from": from, "to": to})
}
func (b *FlightBookingConfirmedBuilder) BuildAction(params map[string]string) *NotificationActionDto {
	return &NotificationActionDto{
		Label: "Download Ticket",
		Type:  "INTERNAL",
		URL:   "/bookings/" + getBookingId(params),
	}
}
func (b *FlightBookingConfirmedBuilder) BuildUi(params map[string]string) *NotificationUiDto {
	return &NotificationUiDto{
		Icon:     "flight-confirmed",
		Color:    "blue",
		Badge:    "Confirmed",
		Deeplink: "/bookings/" + getBookingId(params),
	}
}

// 3. Hotel Booking Confirmed Strategy
type HotelBookingConfirmedBuilder struct {
	EvType string
}

func (b *HotelBookingConfirmedBuilder) GetType() string           { return "HOTEL" }
func (b *HotelBookingConfirmedBuilder) GetCategory() string       { return "BOOKING" }
func (b *HotelBookingConfirmedBuilder) GetEventType() string      { return b.EvType }
func (b *HotelBookingConfirmedBuilder) GetPriority() string       { return "HIGH" }
func (b *HotelBookingConfirmedBuilder) GetDefaultChannel() string { return "EMAIL" }
func (b *HotelBookingConfirmedBuilder) BuildTitle(params map[string]string) string {
	return "Hotel Booking Confirmed"
}
func (b *HotelBookingConfirmedBuilder) BuildMessage(params map[string]string) string {
	hotelName := params["hotelName"]
	if hotelName == "" {
		hotelName = params["serviceName"]
	}
	if hotelName == "" {
		hotelName = "Hotel"
	}
	checkInDate := params["checkInDate"]
	if checkInDate == "" {
		checkInDate = params["travelDate"]
	}
	if checkInDate == "" {
		checkInDate = params["booking_date"]
	}
	if checkInDate == "" {
		checkInDate = "scheduled date"
	}
	return Localize("notification.hotel.confirmed", "en", map[string]string{"hotelName": hotelName, "checkInDate": checkInDate})
}
func (b *HotelBookingConfirmedBuilder) BuildAction(params map[string]string) *NotificationActionDto {
	return &NotificationActionDto{
		Label: "View Voucher",
		Type:  "INTERNAL",
		URL:   "/bookings/" + getBookingId(params),
	}
}
func (b *HotelBookingConfirmedBuilder) BuildUi(params map[string]string) *NotificationUiDto {
	return &NotificationUiDto{
		Icon:     "hotel-confirmed",
		Color:    "green",
		Badge:    "Confirmed",
		Deeplink: "/bookings/" + getBookingId(params),
	}
}

// 4. Bus Booking Confirmed Strategy
type BusBookingConfirmedBuilder struct {
	EvType string
}

func (b *BusBookingConfirmedBuilder) GetType() string           { return "BUS" }
func (b *BusBookingConfirmedBuilder) GetCategory() string       { return "BOOKING" }
func (b *BusBookingConfirmedBuilder) GetEventType() string      { return b.EvType }
func (b *BusBookingConfirmedBuilder) GetPriority() string       { return "HIGH" }
func (b *BusBookingConfirmedBuilder) GetDefaultChannel() string { return "SMS" }
func (b *BusBookingConfirmedBuilder) BuildTitle(params map[string]string) string {
	return "Bus Booking Confirmed"
}
func (b *BusBookingConfirmedBuilder) BuildMessage(params map[string]string) string {
	opName := params["operatorName"]
	if opName == "" {
		opName = params["serviceName"]
	}
	if opName == "" {
		opName = "Bus Operator"
	}
	depCity := params["departureCity"]
	if depCity == "" {
		depCity = params["source"]
	}
	if depCity == "" {
		depCity = "departure city"
	}
	arrCity := params["arrivalCity"]
	if arrCity == "" {
		arrCity = params["destination"]
	}
	if arrCity == "" {
		arrCity = "arrival city"
	}
	return Localize("notification.bus.confirmed", "en", map[string]string{
		"operatorName":  opName,
		"departureCity": depCity,
		"arrivalCity":   arrCity,
	})
}
func (b *BusBookingConfirmedBuilder) BuildAction(params map[string]string) *NotificationActionDto {
	return &NotificationActionDto{
		Label: "View Details",
		Type:  "INTERNAL",
		URL:   "/bookings/" + getBookingId(params),
	}
}
func (b *BusBookingConfirmedBuilder) BuildUi(params map[string]string) *NotificationUiDto {
	return &NotificationUiDto{
		Icon:     "bus-confirmed",
		Color:    "purple",
		Badge:    "Confirmed",
		Deeplink: "/bookings/" + getBookingId(params),
	}
}

// 5. Activity Booking Confirmed Strategy
type ActivityBookingConfirmedBuilder struct {
	EvType string
}

func (b *ActivityBookingConfirmedBuilder) GetType() string           { return "ACTIVITY" }
func (b *ActivityBookingConfirmedBuilder) GetCategory() string       { return "BOOKING" }
func (b *ActivityBookingConfirmedBuilder) GetEventType() string      { return b.EvType }
func (b *ActivityBookingConfirmedBuilder) GetPriority() string       { return "HIGH" }
func (b *ActivityBookingConfirmedBuilder) GetDefaultChannel() string { return "PUSH" }
func (b *ActivityBookingConfirmedBuilder) BuildTitle(params map[string]string) string {
	return "Activity Booking Confirmed"
}
func (b *ActivityBookingConfirmedBuilder) BuildMessage(params map[string]string) string {
	actName := params["activityName"]
	if actName == "" {
		actName = params["serviceName"]
	}
	if actName == "" {
		actName = "Activity"
	}
	actDate := params["activityDate"]
	if actDate == "" {
		actDate = params["travelDate"]
	}
	if actDate == "" {
		actDate = "scheduled date"
	}
	return Localize("notification.activity.confirmed", "en", map[string]string{"activityName": actName, "activityDate": actDate})
}
func (b *ActivityBookingConfirmedBuilder) BuildAction(params map[string]string) *NotificationActionDto {
	return &NotificationActionDto{
		Label: "View Ticket",
		Type:  "INTERNAL",
		URL:   "/bookings/" + getBookingId(params),
	}
}
func (b *ActivityBookingConfirmedBuilder) BuildUi(params map[string]string) *NotificationUiDto {
	return &NotificationUiDto{
		Icon:     "activity-confirmed",
		Color:    "teal",
		Badge:    "Confirmed",
		Deeplink: "/bookings/" + getBookingId(params),
	}
}

// 6. Payment Successful Strategy
type PaymentSuccessBuilder struct{}

func (b *PaymentSuccessBuilder) GetType() string           { return "PAYMENT" }
func (b *PaymentSuccessBuilder) GetCategory() string       { return "PAYMENT" }
func (b *PaymentSuccessBuilder) GetEventType() string      { return "PAYMENT_SUCCESS" }
func (b *PaymentSuccessBuilder) GetPriority() string       { return "HIGH" }
func (b *PaymentSuccessBuilder) GetDefaultChannel() string { return "PUSH" }
func (b *PaymentSuccessBuilder) BuildTitle(params map[string]string) string {
	return "Payment Successful"
}
func (b *PaymentSuccessBuilder) BuildMessage(params map[string]string) string {
	currency := params["currency"]
	if currency == "" {
		currency = "₹"
	}
	amount := params["amount"]
	if amount == "" {
		amount = params["totalAmount"]
	}
	return Localize("notification.payment.success", "en", map[string]string{"currency": currency, "amount": amount})
}
func (b *PaymentSuccessBuilder) BuildAction(params map[string]string) *NotificationActionDto {
	return &NotificationActionDto{
		Label: "View Details",
		Type:  "INTERNAL",
		URL:   "/bookings/" + getBookingId(params),
	}
}
func (b *PaymentSuccessBuilder) BuildUi(params map[string]string) *NotificationUiDto {
	return &NotificationUiDto{
		Icon:     "payment-success",
		Color:    "green",
		Badge:    "Success",
		Deeplink: "/bookings/" + getBookingId(params),
	}
}

// 7. Payment Failed Strategy
type PaymentFailedBuilder struct{}

func (b *PaymentFailedBuilder) GetType() string           { return "PAYMENT" }
func (b *PaymentFailedBuilder) GetCategory() string       { return "PAYMENT" }
func (b *PaymentFailedBuilder) GetEventType() string      { return "PAYMENT_FAILED" }
func (b *PaymentFailedBuilder) GetPriority() string       { return "CRITICAL" }
func (b *PaymentFailedBuilder) GetDefaultChannel() string { return "PUSH" }
func (b *PaymentFailedBuilder) BuildTitle(params map[string]string) string {
	return "Payment Failed"
}
func (b *PaymentFailedBuilder) BuildMessage(params map[string]string) string {
	currency := params["currency"]
	if currency == "" {
		currency = "₹"
	}
	amount := params["amount"]
	if amount == "" {
		amount = params["totalAmount"]
	}
	return Localize("notification.payment.failed", "en", map[string]string{"currency": currency, "amount": amount})
}
func (b *PaymentFailedBuilder) BuildAction(params map[string]string) *NotificationActionDto {
	return &NotificationActionDto{
		Label: "Complete Payment",
		Type:  "INTERNAL",
		URL:   "/bookings/" + getBookingId(params) + "/payment",
	}
}
func (b *PaymentFailedBuilder) BuildUi(params map[string]string) *NotificationUiDto {
	return &NotificationUiDto{
		Icon:     "payment-failed",
		Color:    "red",
		Badge:    "Failed",
		Deeplink: "/bookings/" + getBookingId(params) + "/payment",
	}
}

// 8. Refund Completed Strategy
type RefundCompletedBuilder struct{}

func (b *RefundCompletedBuilder) GetType() string           { return "PAYMENT" }
func (b *RefundCompletedBuilder) GetCategory() string       { return "REFUND" }
func (b *RefundCompletedBuilder) GetEventType() string      { return "REFUND_COMPLETED" }
func (b *RefundCompletedBuilder) GetPriority() string       { return "HIGH" }
func (b *RefundCompletedBuilder) GetDefaultChannel() string { return "EMAIL" }
func (b *RefundCompletedBuilder) BuildTitle(params map[string]string) string {
	return "Refund Processed"
}
func (b *RefundCompletedBuilder) BuildMessage(params map[string]string) string {
	currency := params["currency"]
	if currency == "" {
		currency = "₹"
	}
	amount := params["amount"]
	if amount == "" {
		amount = params["totalAmount"]
	}
	return Localize("notification.refund.processed", "en", map[string]string{"currency": currency, "amount": amount})
}
func (b *RefundCompletedBuilder) BuildAction(params map[string]string) *NotificationActionDto {
	paymentId := params["paymentId"]
	if paymentId == "" {
		paymentId = params["payment_id"]
	}
	return &NotificationActionDto{
		Label: "Track Refund",
		Type:  "INTERNAL",
		URL:   "/payments/" + paymentId,
	}
}
func (b *RefundCompletedBuilder) BuildUi(params map[string]string) *NotificationUiDto {
	paymentId := params["paymentId"]
	if paymentId == "" {
		paymentId = params["payment_id"]
	}
	return &NotificationUiDto{
		Icon:     "refund-success",
		Color:    "green",
		Badge:    "Refunded",
		Deeplink: "/payments/" + paymentId,
	}
}

// 9. Booking Cancelled Strategy
type BookingCancelledBuilder struct{}

func (b *BookingCancelledBuilder) GetType() string           { return "SYSTEM" }
func (b *BookingCancelledBuilder) GetCategory() string       { return "CANCELLATION" }
func (b *BookingCancelledBuilder) GetEventType() string      { return "BOOKING_CANCELLED" }
func (b *BookingCancelledBuilder) GetPriority() string       { return "HIGH" }
func (b *BookingCancelledBuilder) GetDefaultChannel() string { return "EMAIL" }
func (b *BookingCancelledBuilder) BuildTitle(params map[string]string) string {
	return "Booking Cancelled"
}
func (b *BookingCancelledBuilder) BuildMessage(params map[string]string) string {
	bookingId := getBookingId(params)
	return Localize("notification.booking.cancelled", "en", map[string]string{"bookingId": bookingId})
}
func (b *BookingCancelledBuilder) BuildAction(params map[string]string) *NotificationActionDto {
	return &NotificationActionDto{
		Label: "View Details",
		Type:  "INTERNAL",
		URL:   "/bookings/" + getBookingId(params),
	}
}
func (b *BookingCancelledBuilder) BuildUi(params map[string]string) *NotificationUiDto {
	return &NotificationUiDto{
		Icon:     "booking-cancelled",
		Color:    "orange",
		Badge:    "Cancelled",
		Deeplink: "/bookings/" + getBookingId(params),
	}
}

// 10. Fallback System Strategy
type FallbackSystemBuilder struct {
	EvType string
}

func (b *FallbackSystemBuilder) GetType() string           { return getFallbackType(b.EvType) }
func (b *FallbackSystemBuilder) GetCategory() string       { return getCategoryFromEvent(b.EvType) }
func (b *FallbackSystemBuilder) GetEventType() string      { return b.EvType }
func (b *FallbackSystemBuilder) GetPriority() string       { return getPriorityFromEvent(b.EvType) }
func (b *FallbackSystemBuilder) GetDefaultChannel() string { return "EMAIL" }
func (b *FallbackSystemBuilder) BuildTitle(params map[string]string) string {
	if s, ok := params["subject"]; ok && s != "" {
		return s
	}
	t := strings.ReplaceAll(b.EvType, "_", " ")
	return strings.Title(strings.ToLower(t))
}
func (b *FallbackSystemBuilder) BuildMessage(params map[string]string) string {
	if msg, ok := params["body"]; ok && msg != "" {
		return msg
	}
	if msg, ok := params["message"]; ok && msg != "" {
		return msg
	}
	return "Notification Update"
}
func (b *FallbackSystemBuilder) BuildAction(params map[string]string) *NotificationActionDto {
	return &NotificationActionDto{
		Label: "View Details",
		Type:  "INTERNAL",
		URL:   "/bookings/" + getBookingId(params),
	}
}
func (b *FallbackSystemBuilder) BuildUi(params map[string]string) *NotificationUiDto {
	return &NotificationUiDto{
		Icon:     "system-alert",
		Color:    "gray",
		Badge:    "Update",
		Deeplink: "/bookings/" + getBookingId(params),
	}
}

// Registry maps unique event types to their dedicated Strategy.
var buildersRegistry = map[string]NotificationBuilder{
	"PAYMENT_SUCCESS":   &PaymentSuccessBuilder{},
	"PAYMENT_FAILED":    &PaymentFailedBuilder{},
	"REFUND_COMPLETED":  &RefundCompletedBuilder{},
	"REFUND_INITIATED":  &RefundCompletedBuilder{},
	"BOOKING_CANCELLED": &BookingCancelledBuilder{},
}

// Helper to retrieve metadata values case-insensitively
func getMetadataVal(m map[string]string, keys ...string) string {
	for _, key := range keys {
		target := strings.ToLower(key)
		// Check case-insensitive match
		for k, v := range m {
			if strings.ToLower(k) == target && v != "" {
				return v
			}
		}
	}
	return ""
}

// GetBuilder resolves the proper builder interface dynamically for a given event type.
func GetBuilder(eventType string, metadata map[string]string) NotificationBuilder {
	eventType = strings.ToUpper(eventType)
	
	// Handle booking events dynamically based on booking service type
	if eventType == "BOOKING_CONFIRMED" || eventType == "BOOKING_CREATED" {
		bookingType := strings.ToUpper(getMetadataVal(metadata, "bookingType", "booking_type", "bookingtype"))
		
		switch bookingType {
		case "FLIGHT":
			return &FlightBookingConfirmedBuilder{EvType: eventType}
		case "HOTEL":
			return &HotelBookingConfirmedBuilder{EvType: eventType}
		case "BUS":
			return &BusBookingConfirmedBuilder{EvType: eventType}
		case "ACTIVITY":
			return &ActivityBookingConfirmedBuilder{EvType: eventType}
		default:
			return &TourBookingConfirmedBuilder{EvType: eventType}
		}
	}
	
	if b, ok := buildersRegistry[eventType]; ok {
		return b
	}
	
	return &FallbackSystemBuilder{EvType: eventType}
}

// Helper: Extract bookingId safely
func getBookingId(params map[string]string) string {
	bId := getMetadataVal(params, "bookingId", "booking_id", "bookingReference", "booking_ref", "bookingreference")
	if bId == "" {
		return "0"
	}
	return bId
}

// Helper: Dynamically maps arbitrary event string to service type enum.
func getFallbackType(e string) string {
	e = strings.ToUpper(e)
	if strings.Contains(e, "TOUR") {
		return "TOUR"
	}
	if strings.Contains(e, "FLIGHT") {
		return "FLIGHT"
	}
	if strings.Contains(e, "HOTEL") {
		return "HOTEL"
	}
	if strings.Contains(e, "BUS") {
		return "BUS"
	}
	if strings.Contains(e, "ACTIVITY") {
		return "ACTIVITY"
	}
	if strings.Contains(e, "TRANSFER") {
		return "TRANSFER"
	}
	if strings.Contains(e, "VISA") {
		return "VISA"
	}
	if strings.Contains(e, "INSURANCE") {
		return "INSURANCE"
	}
	if strings.Contains(e, "PAYMENT") || strings.Contains(e, "REFUND") {
		return "PAYMENT"
	}
	if strings.Contains(e, "WALLET") {
		return "WALLET"
	}
	return "SYSTEM"
}

// Helper: Dynamically maps event keys to categories.
func getCategoryFromEvent(e string) string {
	e = strings.ToUpper(e)
	if strings.Contains(e, "BOOKING") {
		return "BOOKING"
	}
	if strings.Contains(e, "PAYMENT") {
		return "PAYMENT"
	}
	if strings.Contains(e, "REFUND") {
		return "REFUND"
	}
	if strings.Contains(e, "CANCEL") {
		return "CANCELLATION"
	}
	if strings.Contains(e, "VOUCHER") {
		return "VOUCHER"
	}
	if strings.Contains(e, "REMIND") {
		return "REMINDER"
	}
	if strings.Contains(e, "PROMO") || strings.Contains(e, "OFFER") {
		return "PROMOTION"
	}
	if strings.Contains(e, "WALLET") {
		return "WALLET"
	}
	return "SYSTEM"
}

// Helper: Maps dynamic event prioritisation
func getPriorityFromEvent(e string) string {
	e = strings.ToUpper(e)
	if strings.Contains(e, "FAILED") || strings.Contains(e, "CRITICAL") || strings.Contains(e, "ALERT") {
		return "CRITICAL"
	}
	if strings.Contains(e, "SUCCESS") || strings.Contains(e, "CONFIRMED") || strings.Contains(e, "HIGH") {
		return "HIGH"
	}
	return "MEDIUM"
}
