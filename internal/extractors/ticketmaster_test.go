package extractors

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

const sampleTicketmasterJSON = `{
	"_embedded": {
		"events": [
			{
				"name": "Test Artist",
				"url": "https://www.ticketmaster.com/event/123",
				"dates": {
					"start": {
						"localDate": "2024-04-15",
						"localTime": "19:30:00"
					}
				},
				"_embedded": {
					"venues": [{"name": "O2 Academy Brixton"}]
				}
			}
		]
	}
}`

func TestTicketmasterExtractor_Extract_ParsesEvent(t *testing.T) {
	extractor := NewTicketmasterExtractor()
	events, err := extractor.Extract(sampleTicketmasterJSON)

	require.NoError(t, err)
	require.Len(t, events, 1)
	assert.Equal(t, "Test Artist", events[0].Artist)
	assert.Equal(t, "O2 Academy Brixton", events[0].Location)
	assert.Equal(t, time.Date(2024, 4, 15, 19, 30, 0, 0, time.UTC), events[0].DateTime)
	assert.Equal(t, "https://www.ticketmaster.com/event/123", events[0].URL)
}

func TestTicketmasterExtractor_Extract_DefaultsMidnightWhenNoTime(t *testing.T) {
	json := `{
		"_embedded": {
			"events": [{
				"name": "Artist",
				"url": "",
				"dates": {"start": {"localDate": "2024-06-20"}},
				"_embedded": {"venues": [{"name": "Venue"}]}
			}]
		}
	}`

	extractor := NewTicketmasterExtractor()
	events, err := extractor.Extract(json)

	require.NoError(t, err)
	require.Len(t, events, 1)
	assert.Equal(t, 0, events[0].DateTime.Hour())
	assert.Equal(t, 0, events[0].DateTime.Minute())
}

func TestTicketmasterExtractor_Extract_EmptyWhenNoEmbedded(t *testing.T) {
	extractor := NewTicketmasterExtractor()
	events, err := extractor.Extract(`{}`)

	require.NoError(t, err)
	assert.Empty(t, events)
}

func TestTicketmasterExtractor_Extract_UnknownVenueWhenNoVenues(t *testing.T) {
	json := `{
		"_embedded": {
			"events": [{
				"name": "Artist",
				"url": "",
				"dates": {"start": {"localDate": "2024-06-20"}},
				"_embedded": {"venues": []}
			}]
		}
	}`

	extractor := NewTicketmasterExtractor()
	events, err := extractor.Extract(json)

	require.NoError(t, err)
	require.Len(t, events, 1)
	assert.Equal(t, "Unknown Venue", events[0].Location)
}

func TestTicketmasterExtractor_Extract_SkipsEventsWithMissingName(t *testing.T) {
	json := `{
		"_embedded": {
			"events": [{"url": "", "dates": {"start": {"localDate": "2024-06-20"}}}]
		}
	}`

	extractor := NewTicketmasterExtractor()
	events, err := extractor.Extract(json)

	require.NoError(t, err)
	assert.Empty(t, events)
}

func TestTicketmasterExtractor_Extract_InvalidJSON(t *testing.T) {
	extractor := NewTicketmasterExtractor()
	_, err := extractor.Extract("not json")
	assert.Error(t, err)
}
