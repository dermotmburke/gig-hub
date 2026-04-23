package pipeline

import (
	"context"
	"errors"
	"testing"
	"time"

	"github.com/dermotmburke/gig-hub/internal/models"
	"github.com/dermotmburke/gig-hub/internal/notifiers"
	"github.com/stretchr/testify/assert"
)

type mockFetcher struct {
	result string
	err    error
}

func (m *mockFetcher) Fetch() (string, error) { return m.result, m.err }

type mockExtractor struct {
	events []models.Event
	err    error
}

func (m *mockExtractor) Extract(_ string) ([]models.Event, error) { return m.events, m.err }

type mockNotifier struct {
	notified [][]models.Event
}

func (m *mockNotifier) Notify(events []models.Event) { m.notified = append(m.notified, events) }

func TestPipeline_Run_NotifiesEvents(t *testing.T) {
	events := []models.Event{{Artist: "Artist", Location: "Venue", DateTime: time.Now()}}
	notifier := &mockNotifier{}

	p := New("test-pipeline",
		&mockFetcher{result: "raw"},
		&mockExtractor{events: events},
		[]notifiers.Notifier{notifier},
		nil,
	)
	p.Run(context.Background())

	assert.Len(t, notifier.notified, 1)
	assert.Equal(t, events, notifier.notified[0])
}

func TestPipeline_Run_StopsOnFetchError(t *testing.T) {
	notifier := &mockNotifier{}

	p := New("test-pipeline",
		&mockFetcher{err: errors.New("network error")},
		&mockExtractor{},
		[]notifiers.Notifier{notifier},
		nil,
	)
	p.Run(context.Background())

	assert.Empty(t, notifier.notified)
}

func TestPipeline_Run_StopsOnExtractError(t *testing.T) {
	notifier := &mockNotifier{}

	p := New("test-pipeline",
		&mockFetcher{result: "raw"},
		&mockExtractor{err: errors.New("parse error")},
		[]notifiers.Notifier{notifier},
		nil,
	)
	p.Run(context.Background())

	assert.Empty(t, notifier.notified)
}

func TestPipeline_Name(t *testing.T) {
	p := New("my-pipeline", &mockFetcher{}, &mockExtractor{}, nil, nil)
	assert.Equal(t, "my-pipeline", p.Name())
}
