package notifiers

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/dermotmburke/gig-hub/internal/models"
	"github.com/dermotmburke/gig-hub/internal/utilities"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestSlackNotifier_BuildPayload_FormatsEvents(t *testing.T) {
	n := NewSlackNotifier(&http.Client{}, "https://hooks.example.com", nil)
	events := []models.Event{
		{
			Artist:   "Test Artist",
			Location: "O2 Academy",
			DateTime: time.Date(2024, 4, 15, 19, 30, 0, 0, time.UTC),
			URL:      "https://example.com/event",
		},
	}

	payload, err := n.BuildPayload(events)
	require.NoError(t, err)

	var m map[string]string
	require.NoError(t, json.Unmarshal([]byte(payload), &m))
	assert.Contains(t, m["text"], "*1 upcoming events*")
	assert.Contains(t, m["text"], "Test Artist")
	assert.Contains(t, m["text"], "Monday 15 April")
	assert.Contains(t, m["text"], "O2 Academy")
	assert.Contains(t, m["text"], "https://example.com/event")
}

func TestSlackNotifier_BuildPayload_NoURLEvent(t *testing.T) {
	n := NewSlackNotifier(&http.Client{}, "https://hooks.example.com", nil)
	events := []models.Event{
		{Artist: "Artist", Location: "Venue", DateTime: time.Now(), URL: "not-a-url"},
	}

	payload, err := n.BuildPayload(events)
	require.NoError(t, err)

	var m map[string]string
	require.NoError(t, json.Unmarshal([]byte(payload), &m))
	assert.Contains(t, m["text"], "Artist")
	assert.NotContains(t, m["text"], "<not-a-url|")
}

func TestSlackNotifier_BuildPayload_WithCalendarURL(t *testing.T) {
	calBuilder := utilities.NewCalendarURLBuilder("https://calendar.example.com")
	n := NewSlackNotifier(&http.Client{}, "https://hooks.example.com", calBuilder)
	events := []models.Event{
		{Artist: "Artist", Location: "Venue", DateTime: time.Now(), URL: "https://example.com"},
	}

	payload, err := n.BuildPayload(events)
	require.NoError(t, err)
	assert.Contains(t, payload, "Save")
	assert.Contains(t, payload, "calendar.example.com")
}

func TestSlackNotifier_Notify_PostsToWebhook(t *testing.T) {
	var receivedBody string
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		buf := make([]byte, 4096)
		n, _ := r.Body.Read(buf)
		receivedBody = string(buf[:n])
		w.WriteHeader(http.StatusOK)
	}))
	defer server.Close()

	n := NewSlackNotifier(server.Client(), server.URL, nil)
	events := []models.Event{
		{Artist: "Artist", Location: "Venue", DateTime: time.Now(), URL: "https://example.com"},
	}
	n.Notify(events)

	assert.Contains(t, receivedBody, "Artist")
}

func TestSlackNotifier_Notify_SkipsEmptyEvents(t *testing.T) {
	called := false
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		called = true
		w.WriteHeader(http.StatusOK)
	}))
	defer server.Close()

	n := NewSlackNotifier(server.Client(), server.URL, nil)
	n.Notify([]models.Event{})

	assert.False(t, called)
}
