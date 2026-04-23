package utilities

import (
	"testing"
	"time"

	"github.com/dermotmburke/gig-hub/internal/models"
	"github.com/stretchr/testify/assert"
)

func TestCalendarURLBuilder_Build(t *testing.T) {
	builder := NewCalendarURLBuilder("https://calendar.example.com")
	event := models.Event{
		Artist:   "Test Artist",
		Location: "Test Venue",
		DateTime: time.Date(2024, 4, 15, 19, 30, 0, 0, time.UTC),
		URL:      "https://example.com/event",
	}

	result := builder.Build(event)

	assert.Contains(t, result, "https://calendar.example.com/save")
	assert.Contains(t, result, "artist=Test+Artist")
	assert.Contains(t, result, "location=Test+Venue")
	assert.Contains(t, result, "date=2024-04-15T19:30:00")
	assert.Contains(t, result, "url=https%3A%2F%2Fexample.com%2Fevent")
}

func TestCalendarURLBuilder_Build_EncodesSpecialChars(t *testing.T) {
	builder := NewCalendarURLBuilder("https://calendar.example.com")
	event := models.Event{
		Artist:   "Artist & Band",
		Location: "Venue (London)",
		DateTime: time.Date(2024, 4, 15, 0, 0, 0, 0, time.UTC),
		URL:      "",
	}

	result := builder.Build(event)

	assert.Contains(t, result, "artist=Artist+%26+Band")
	assert.Contains(t, result, "location=Venue+%28London%29")
}
