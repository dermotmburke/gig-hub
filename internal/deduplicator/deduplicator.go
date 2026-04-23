package deduplicator

import (
	"context"
	"encoding/json"
	"log/slog"
	"time"

	"github.com/dermotmburke/gig-hub/internal/models"
	"github.com/redis/go-redis/v9"
)

const ttlDaysAfterEvent = 2

type EventDeduplicator struct {
	client *redis.Client
}

func New(client *redis.Client) *EventDeduplicator {
	return &EventDeduplicator{client: client}
}

func (d *EventDeduplicator) Filter(events []models.Event) []models.Event {
	if len(events) == 0 {
		return events
	}
	keys := make([]string, len(events))
	for i, e := range events {
		keys[i] = e.Key()
	}

	values, err := d.client.MGet(context.Background(), keys...).Result()
	if err != nil {
		slog.Error("Redis filter failed", "error", err)
		return events
	}

	var newEvents []models.Event
	for i, v := range values {
		if v == nil {
			newEvents = append(newEvents, events[i])
		}
	}
	return newEvents
}

func (d *EventDeduplicator) MarkSent(events []models.Event) {
	if len(events) == 0 {
		return
	}
	ctx := context.Background()
	pipe := d.client.Pipeline()
	for _, e := range events {
		data, err := json.Marshal(e)
		if err != nil {
			slog.Error("Failed to marshal event", "error", err)
			continue
		}
		pipe.Set(ctx, e.Key(), string(data), ttlFor(e))
	}
	if _, err := pipe.Exec(ctx); err != nil {
		slog.Error("Failed to store events in Redis", "error", err)
	}
}

func ttlFor(e models.Event) time.Duration {
	eventDate := time.Date(e.DateTime.Year(), e.DateTime.Month(), e.DateTime.Day(), 0, 0, 0, 0, time.UTC)
	expiry := eventDate.AddDate(0, 0, ttlDaysAfterEvent)
	ttl := time.Until(expiry)
	if ttl < 60*time.Second {
		return 60 * time.Second
	}
	return ttl
}
