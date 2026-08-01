package notification

// BookingSummaryDto represents the enriched structured booking details returned to the frontend.
type BookingSummaryDto struct {
	BookingID        string  `json:"bookingId,omitempty"`
	BookingType      string  `json:"bookingType,omitempty"` // FLIGHT, HOTEL, BUS, TRAIN, TOUR, ACTIVITY, VISA, CAB
	BookingStatus    string  `json:"bookingStatus,omitempty"`
	ServiceName      string  `json:"serviceName,omitempty"`
	PassengerCount   int     `json:"passengerCount,omitempty"`
	TravelDate       string  `json:"travelDate,omitempty"`
	ReturnDate       string  `json:"returnDate,omitempty"`
	Destination      string  `json:"destination,omitempty"`
	TotalAmount      float64 `json:"totalAmount,omitempty"`
	Currency         string  `json:"currency,omitempty"`
	VoucherAvailable bool    `json:"voucherAvailable,omitempty"`
	
	// Variant attributes:
	PackageName      string  `json:"packageName,omitempty"`
	TotalDays        int     `json:"totalDays,omitempty"`
	Airline          string  `json:"airline,omitempty"`
	FlightNumber     string  `json:"flightNumber,omitempty"`
	From             string  `json:"from,omitempty"`
	To               string  `json:"to,omitempty"`
	HotelName        string  `json:"hotelName,omitempty"`
	CheckInDate      string  `json:"checkInDate,omitempty"`
	CheckOutDate     string  `json:"checkOutDate,omitempty"`
	RoomCount        int     `json:"roomCount,omitempty"`
	OperatorName     string  `json:"operatorName,omitempty"`
	DepartureCity    string  `json:"departureCity,omitempty"`
	ArrivalCity      string  `json:"arrivalCity,omitempty"`
	ActivityName     string  `json:"activityName,omitempty"`
	ActivityDate     string  `json:"activityDate,omitempty"`
	ActivityTime     string  `json:"activityTime,omitempty"`
}

// PaymentSummaryDto represents structured details for payment notifications.
type PaymentSummaryDto struct {
	PaymentID     string  `json:"paymentId,omitempty"`
	TransactionID string  `json:"transactionId,omitempty"`
	Amount        float64 `json:"amount,omitempty"`
	Currency      string  `json:"currency,omitempty"`
	PaymentStatus string  `json:"paymentStatus,omitempty"`
	PaymentMethod string  `json:"paymentMethod,omitempty"`
}

// RefundSummaryDto represents structured details for refund notifications.
type RefundSummaryDto struct {
	RefundID    string  `json:"refundId,omitempty"`
	Amount      float64 `json:"amount,omitempty"`
	Currency    string  `json:"currency,omitempty"`
	Status      string  `json:"status,omitempty"`
	ProcessedAt string  `json:"processedAt,omitempty"`
}

// NotificationActionDto represents a single frontend action object.
type NotificationActionDto struct {
	Label string `json:"label,omitempty"`
	Type  string `json:"type,omitempty"` // INTERNAL, EXTERNAL, DOWNLOAD, RETRY
	URL   string `json:"url,omitempty"`
}

// NotificationUserDto represents lightweight user information.
type NotificationUserDto struct {
	UserID    string `json:"userId,omitempty"`
	FirstName string `json:"firstName,omitempty"`
	LastName  string `json:"lastName,omitempty"`
}

// NotificationUiDto represents dynamic frontend rendering properties.
type NotificationUiDto struct {
	Icon     string `json:"icon,omitempty"`
	Color    string `json:"color,omitempty"`
	Badge    string `json:"badge,omitempty"`
	Deeplink string `json:"deeplink,omitempty"`
}

// NotificationDeliveryDto encapsulates channel transmission tracking.
type NotificationDeliveryDto struct {
	Channel      string  `json:"channel,omitempty"`      // EMAIL, SMS, PUSH, WHATSAPP, IN_APP
	Status       string  `json:"status,omitempty"`       // PENDING, DELIVERED, FAILED, RETRYING
	SentAt       string  `json:"sentAt,omitempty"`
	DeliveredAt  string  `json:"deliveredAt,omitempty"`
	FailedReason *string `json:"failedReason"`
}

// NotificationResponseDto is the unified production DTO matching the exact required schema.
type NotificationResponseDto struct {
	NotificationID string                   `json:"notificationId"`
	Type           string                   `json:"type"`      // TOUR, FLIGHT, HOTEL, BUS, ACTIVITY, etc.
	Category       string                   `json:"category"`  // BOOKING, PAYMENT, REFUND, etc.
	EventType      string                   `json:"eventType"` // BOOKING_CONFIRMED, PAYMENT_SUCCESS, etc.
	Priority       string                   `json:"priority"`  // LOW, MEDIUM, HIGH, CRITICAL
	Title          string                   `json:"title"`
	Message        string                   `json:"message"`
	IsRead         bool                     `json:"isRead"`
	IsArchived     bool                     `json:"isArchived"`
	ReadAt         string                   `json:"readAt,omitempty"`
	CreatedAt      string                   `json:"createdAt"`
	UpdatedAt      string                   `json:"updatedAt,omitempty"`
	ExpiresAt      string                   `json:"expiresAt,omitempty"`
	Delivery       *NotificationDeliveryDto `json:"delivery,omitempty"`
	Booking        *BookingSummaryDto       `json:"booking,omitempty"`
	Payment        *PaymentSummaryDto       `json:"payment,omitempty"`
	Refund         *RefundSummaryDto        `json:"refund,omitempty"`
	Action         *NotificationActionDto   `json:"action,omitempty"`
	User           *NotificationUserDto     `json:"user,omitempty"`
	Ui             *NotificationUiDto       `json:"ui,omitempty"`
}
