package email

import (
	"bytes"
	"embed"
	"html/template"
	"log"
	"strings"
	"time"
)

//go:embed templates/*.html
var templateFS embed.FS

var templates *template.Template

func init() {
	var err error
	templates, err = template.New("").Funcs(template.FuncMap{
		"safeHTML": func(s string) template.HTML {
			return template.HTML(s)
		},
	}).ParseFS(templateFS, "templates/*.html")
	if err != nil {
		log.Fatalf("Failed to parse email templates: %v", err)
	}
}

type TemplateData struct {
	Subject       string
	Title         string
	Subtitle      string
	Body          template.HTML
	Year          int
	BookingRef    string
	ServiceName   string
	BookingDate   string
	Amount        string
	Currency      string
	ActionUrl     string
	StatusBadge   string
	Name          string
	Email         string
	Phone         string
	Message       string
	Date          string
	PaymentId     string
	PaymentMethod string
	Travellers    string
	AlertType     string
	Description   string
	Timestamp     string
}

func RenderTemplate(templateName string, data TemplateData) (string, error) {
	if data.Year == 0 {
		data.Year = time.Now().Year()
	}

	// To correctly use base.html as a layout, we need to execute "base.html"
	// but ensure the specific template's blocks are available.

	var t *template.Template
	var err error

	if templateName == "base.html" {
		t, err = template.New("render").Funcs(template.FuncMap{
			"safeHTML": func(s string) template.HTML {
				return template.HTML(s)
			},
		}).ParseFS(templateFS, "templates/base.html")
	} else {
		t, err = template.New("render").Funcs(template.FuncMap{
			"safeHTML": func(s string) template.HTML {
				return template.HTML(s)
			},
		}).ParseFS(templateFS, "templates/base.html", "templates/"+templateName)

		// Resilient fallback: if the original parse fails and templateName has hyphens,
		// try replacing them with underscores (e.g. booking-confirmation -> booking_confirmation).
		if err != nil && strings.Contains(templateName, "-") {
			fallbackName := strings.ReplaceAll(templateName, "-", "_")
			t2, err2 := template.New("render").Funcs(template.FuncMap{
				"safeHTML": func(s string) template.HTML {
					return template.HTML(s)
				},
			}).ParseFS(templateFS, "templates/base.html", "templates/"+fallbackName)
			if err2 == nil {
				t = t2
				err = nil
			}
		}
	}

	if err != nil {
		return "", err
	}

	var buf bytes.Buffer
	err = t.ExecuteTemplate(&buf, "base.html", data)
	if err != nil {
		return "", err
	}
	return buf.String(), nil
}

func GetHTMLTemplate(body string) string {
	return GetHTMLTemplateWithTitle("Notification", body)
}

func GetHTMLTemplateWithTitle(title, body string) string {
	data := TemplateData{
		Subject: title,
		Title:   title,
		Body:    template.HTML(strings.ReplaceAll(body, "\n", "<br>")),
		Year:    time.Now().Year(),
	}

	html, err := RenderTemplate("notification_alert.html", data)
	if err != nil {
		log.Printf("Failed to render generic template: %v", err)
		return body // Fallback to plain body
	}
	return html
}
