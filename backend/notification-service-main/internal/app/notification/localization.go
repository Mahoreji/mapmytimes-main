package notification

import (
	"strings"
)

// Translations dictionary ready for multi-language support (defaulting to English)
var translations = map[string]map[string]string{
	"en": {
		"notification.tour.confirmed":     "Your {packageName} booking has been confirmed.",
		"notification.flight.confirmed":   "Your {from} to {to} flight has been confirmed.",
		"notification.hotel.confirmed":    "Your booking at {hotelName} check-in on {checkInDate} is confirmed.",
		"notification.bus.confirmed":      "Your seat with {operatorName} from {departureCity} to {arrivalCity} is confirmed.",
		"notification.activity.confirmed": "Your booking for {activityName} on {activityDate} has been confirmed.",
		"notification.payment.success":    "Payment of {currency}{amount} received successfully.",
		"notification.payment.failed":     "Payment of {currency}{amount} failed. Please retry.",
		"notification.refund.processed":   "Your refund of {currency}{amount} has been processed.",
		"notification.booking.cancelled":  "Your booking {bookingId} has been successfully cancelled.",
		"notification.system.alert":       "{body}",
	},
}

// Localize looks up a translation key for the given language (defaults to "en") 
// and replaces variables formatted as {variable_name} with values from the params map.
func Localize(key string, lang string, params map[string]string) string {
	if lang == "" {
		lang = "en"
	}
	
	langDict, ok := translations[lang]
	if !ok {
		// Fallback to English
		langDict = translations["en"]
	}

	templateStr, ok := langDict[key]
	if !ok {
		// If the translation key is not found, return the key itself as a fallback
		return key
	}

	// Replace all parameterized placeholders
	result := templateStr
	for k, v := range params {
		result = strings.ReplaceAll(result, "{"+k+"}", v)
	}

	return result
}
