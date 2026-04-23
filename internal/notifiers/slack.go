package notifiers

import (
	"bytes"
	"encoding/json"
	"fmt"
	"log/slog"
	"net/http"
	"strings"

	"github.com/dermotmburke/gig-hub/internal/models"
	"github.com/dermotmburke/gig-hub/internal/utilities"
)

type SlackNotifier struct {
	client             *http.Client
	webhookURL         string
	calendarURLBuilder *utilities.CalendarURLBuilder
}

func NewSlackNotifier(client *http.Client, webhookURL string, calendarURLBuilder *utilities.CalendarURLBuilder) *SlackNotifier {
	return &SlackNotifier{
		client:             client,
		webhookURL:         webhookURL,
		calendarURLBuilder: calendarURLBuilder,
	}
}

func (n *SlackNotifier) Notify(events []models.Event) {
	if len(events) == 0 {
		return
	}
	payload, err := n.buildPayload(events)
	if err != nil {
		slog.Error("Failed to build Slack payload", "error", err)
		return
	}

	resp, err := n.client.Post(n.webhookURL, "application/json", bytes.NewBufferString(payload))
	if err != nil {
		slog.Error("Failed to send Slack notification", "error", err)
		return
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		slog.Error("Slack notification failed", "status", resp.StatusCode)
		return
	}
	slog.Info("Notified Slack", "count", len(events))
}

func (n *SlackNotifier) BuildPayload(events []models.Event) (string, error) {
	return n.buildPayload(events)
}

func (n *SlackNotifier) buildPayload(events []models.Event) (string, error) {
	lines := make([]string, 0, len(events))
	for _, e := range events {
		lines = append(lines, n.formatEvent(e))
	}
	text := fmt.Sprintf("*%d upcoming events*\n%s", len(events), strings.Join(lines, "\n"))
	b, err := json.Marshal(map[string]string{"text": text})
	return string(b), err
}

func (n *SlackNotifier) formatEvent(e models.Event) string {
	date := e.DateTime.Format("Monday 2 January")
	var line string
	if strings.HasPrefix(e.URL, "http") {
		line = fmt.Sprintf("• *<%s|%s>* — %s @ %s", e.URL, e.Artist, date, e.Location)
	} else {
		line = fmt.Sprintf("• *%s* — %s @ %s", e.Artist, date, e.Location)
	}
	if n.calendarURLBuilder != nil {
		line += fmt.Sprintf(" | <%s|\U0001F5AB Save>", n.calendarURLBuilder.Build(e))
	}
	return line
}
