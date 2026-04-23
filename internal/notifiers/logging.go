package notifiers

import (
	"log/slog"

	"github.com/dermotmburke/gig-hub/internal/models"
)

type LoggingNotifier struct{}

func NewLoggingNotifier() *LoggingNotifier {
	return &LoggingNotifier{}
}

func (n *LoggingNotifier) Notify(events []models.Event) {
	for _, e := range events {
		slog.Info("Event", "artist", e.Artist, "location", e.Location, "dateTime", e.DateTime, "url", e.URL)
	}
}
