package extractors

import (
	"fmt"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestBanquetExtractor_Extract_ParsesEvent(t *testing.T) {
	year := time.Now().Year()
	html := `<html><body>
		<a class="card" href="/events/test-event">
			<span class="artist">Test Artist</span>
			<span class="title">15th April at O2 Academy Brixton, London 7:30pm</span>
		</a>
	</body></html>`

	extractor := NewBanquetExtractor()
	events, err := extractor.Extract(html)

	require.NoError(t, err)
	require.Len(t, events, 1)
	assert.Equal(t, "Test Artist", events[0].Artist)
	assert.Equal(t, "O2 Academy Brixton", events[0].Location)
	assert.Equal(t, time.Date(year, time.April, 15, 19, 30, 0, 0, time.UTC), events[0].DateTime)
	assert.Equal(t, "https://www.banquetrecords.com/events/test-event", events[0].URL)
}

func TestBanquetExtractor_Extract_DefaultsMidnight(t *testing.T) {
	year := time.Now().Year()
	html := `<html><body>
		<a class="card" href="/events/no-time">
			<span class="artist">Midnight Artist</span>
			<span class="title">20th June at Roundhouse</span>
		</a>
	</body></html>`

	extractor := NewBanquetExtractor()
	events, err := extractor.Extract(html)

	require.NoError(t, err)
	require.Len(t, events, 1)
	assert.Equal(t, time.Date(year, time.June, 20, 0, 0, 0, 0, time.UTC), events[0].DateTime)
}

func TestBanquetExtractor_Extract_SkipsMissingAtSeparator(t *testing.T) {
	html := `<html><body>
		<a class="card" href="/events/bad">
			<span class="artist">Artist</span>
			<span class="title">15th April no-venue-separator</span>
		</a>
	</body></html>`

	extractor := NewBanquetExtractor()
	events, err := extractor.Extract(html)

	require.NoError(t, err)
	assert.Empty(t, events)
}

func TestBanquetExtractor_Extract_SkipsMissingArtist(t *testing.T) {
	html := `<html><body>
		<a class="card" href="/events/missing-artist">
			<span class="title">15th April at Venue</span>
		</a>
	</body></html>`

	extractor := NewBanquetExtractor()
	events, err := extractor.Extract(html)

	require.NoError(t, err)
	assert.Empty(t, events)
}

func TestBanquetExtractor_Extract_MultipleEvents(t *testing.T) {
	html := `<html><body>
		<a class="card" href="/events/event1">
			<span class="artist">Artist One</span>
			<span class="title">1st January at Venue A</span>
		</a>
		<a class="card" href="/events/event2">
			<span class="artist">Artist Two</span>
			<span class="title">2nd February at Venue B</span>
		</a>
	</body></html>`

	extractor := NewBanquetExtractor()
	events, err := extractor.Extract(html)

	require.NoError(t, err)
	assert.Len(t, events, 2)
}

func TestBanquetExtractor_Extract_OrdinalVariants(t *testing.T) {
	year := time.Now().Year()
	cases := []struct {
		title    string
		day      int
		month    time.Month
	}{
		{fmt.Sprintf("%dst January at Venue", 1), 1, time.January},
		{fmt.Sprintf("%dnd February at Venue", 2), 2, time.February},
		{fmt.Sprintf("%drd March at Venue", 3), 3, time.March},
		{fmt.Sprintf("%dth April at Venue", 4), 4, time.April},
	}

	extractor := NewBanquetExtractor()
	for _, tc := range cases {
		html := fmt.Sprintf(`<html><body><a class="card" href="/e"><span class="artist">A</span><span class="title">%s</span></a></body></html>`, tc.title)
		events, err := extractor.Extract(html)
		require.NoError(t, err)
		require.Len(t, events, 1, "title: %s", tc.title)
		assert.Equal(t, time.Date(year, tc.month, tc.day, 0, 0, 0, 0, time.UTC), events[0].DateTime)
	}
}
