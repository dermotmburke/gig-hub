package models

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

func TestEvent_Key_HasPrefix(t *testing.T) {
	e := Event{Artist: "A", Location: "B", DateTime: time.Now(), URL: ""}
	assert.True(t, len(e.Key()) > len("banquet:event:"))
	assert.Contains(t, e.Key(), "banquet:event:")
}

func TestEvent_Checksum_IsDeterministic(t *testing.T) {
	dt := time.Date(2024, 4, 15, 19, 30, 0, 0, time.UTC)
	e := Event{Artist: "Artist", Location: "Venue", DateTime: dt, URL: "https://example.com"}
	assert.Equal(t, e.Checksum(), e.Checksum())
}

func TestEvent_Checksum_NormalisesCase(t *testing.T) {
	dt := time.Date(2024, 4, 15, 19, 30, 0, 0, time.UTC)
	e1 := Event{Location: "  O2 Academy Brixton  ", DateTime: dt}
	e2 := Event{Location: "o2 academy brixton", DateTime: dt}
	assert.Equal(t, e1.Checksum(), e2.Checksum())
}

func TestEvent_Checksum_DiffersByLocation(t *testing.T) {
	dt := time.Date(2024, 4, 15, 19, 30, 0, 0, time.UTC)
	e1 := Event{Location: "Venue A", DateTime: dt}
	e2 := Event{Location: "Venue B", DateTime: dt}
	assert.NotEqual(t, e1.Checksum(), e2.Checksum())
}

func TestEvent_Checksum_DiffersByDate(t *testing.T) {
	e1 := Event{Location: "Venue", DateTime: time.Date(2024, 4, 15, 19, 30, 0, 0, time.UTC)}
	e2 := Event{Location: "Venue", DateTime: time.Date(2024, 4, 16, 19, 30, 0, 0, time.UTC)}
	assert.NotEqual(t, e1.Checksum(), e2.Checksum())
}
