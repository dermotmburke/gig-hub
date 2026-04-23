package utilities

import (
	"net/url"

	"github.com/dermotmburke/gig-hub/internal/models"
)

type CalendarURLBuilder struct {
	baseURL string
}

func NewCalendarURLBuilder(baseURL string) *CalendarURLBuilder {
	return &CalendarURLBuilder{baseURL: baseURL}
}

func (b *CalendarURLBuilder) Build(event models.Event) string {
	return b.baseURL + "/save" +
		"?artist=" + url.QueryEscape(event.Artist) +
		"&location=" + url.QueryEscape(event.Location) +
		"&date=" + event.DateTime.Format("2006-01-02T15:04:05") +
		"&url=" + url.QueryEscape(event.URL)
}
