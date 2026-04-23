package notifiers

import (
	"testing"
	"time"

	"github.com/dermotmburke/gig-hub/internal/models"
)

func TestLoggingNotifier_Notify_DoesNotPanic(t *testing.T) {
	n := NewLoggingNotifier()
	events := []models.Event{
		{Artist: "Artist", Location: "Venue", DateTime: time.Now(), URL: "https://example.com"},
	}
	n.Notify(events)
}

func TestLoggingNotifier_Notify_EmptyList(t *testing.T) {
	n := NewLoggingNotifier()
	n.Notify([]models.Event{})
}
