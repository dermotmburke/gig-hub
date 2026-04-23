package extractors

import (
	"encoding/json"
	"time"

	"github.com/dermotmburke/gig-hub/internal/models"
)

type ticketmasterResponse struct {
	Embedded struct {
		Events []ticketmasterEvent `json:"events"`
	} `json:"_embedded"`
}

type ticketmasterEvent struct {
	Name string `json:"name"`
	URL  string `json:"url"`
	Dates struct {
		Start struct {
			LocalDate string `json:"localDate"`
			LocalTime string `json:"localTime"`
		} `json:"start"`
	} `json:"dates"`
	Embedded struct {
		Venues []struct {
			Name string `json:"name"`
		} `json:"venues"`
	} `json:"_embedded"`
}

type TicketmasterExtractor struct{}

func NewTicketmasterExtractor() *TicketmasterExtractor {
	return &TicketmasterExtractor{}
}

func (e *TicketmasterExtractor) Extract(raw string) ([]models.Event, error) {
	var resp ticketmasterResponse
	if err := json.Unmarshal([]byte(raw), &resp); err != nil {
		return nil, err
	}

	var events []models.Event
	for _, ev := range resp.Embedded.Events {
		event := e.parseEvent(ev)
		if event != nil {
			events = append(events, *event)
		}
	}
	return events, nil
}

func (e *TicketmasterExtractor) parseEvent(ev ticketmasterEvent) *models.Event {
	if ev.Name == "" || ev.Dates.Start.LocalDate == "" {
		return nil
	}

	date, err := time.Parse("2006-01-02", ev.Dates.Start.LocalDate)
	if err != nil {
		return nil
	}

	t := e.extractTime(ev)
	dt := time.Date(date.Year(), date.Month(), date.Day(), t.Hour(), t.Minute(), t.Second(), 0, time.UTC)

	return &models.Event{
		Artist:   ev.Name,
		Location: e.extractVenueName(ev),
		DateTime: dt,
		URL:      ev.URL,
	}
}

func (e *TicketmasterExtractor) extractVenueName(ev ticketmasterEvent) string {
	if len(ev.Embedded.Venues) > 0 && ev.Embedded.Venues[0].Name != "" {
		return ev.Embedded.Venues[0].Name
	}
	return "Unknown Venue"
}

func (e *TicketmasterExtractor) extractTime(ev ticketmasterEvent) time.Time {
	if ev.Dates.Start.LocalTime != "" {
		t, err := time.Parse("15:04:05", ev.Dates.Start.LocalTime)
		if err == nil {
			return t
		}
	}
	return time.Time{}
}
