package extractors

import (
	"net/url"
	"regexp"
	"strconv"
	"strings"
	"time"

	"github.com/PuerkitoBio/goquery"
	"github.com/dermotmburke/gig-hub/internal/models"
)

const banquetBaseURL = "https://www.banquetrecords.com"

var (
	datePattern = regexp.MustCompile(`(\d+)(?:st|nd|rd|th)\s+(\w+)`)
	timePattern = regexp.MustCompile(`(?i)(\d+:\d+[ap]m)`)
)

type BanquetExtractor struct{}

func NewBanquetExtractor() *BanquetExtractor {
	return &BanquetExtractor{}
}

func (e *BanquetExtractor) Extract(page string) ([]models.Event, error) {
	doc, err := goquery.NewDocumentFromReader(strings.NewReader(page))
	if err != nil {
		return nil, err
	}

	base, _ := url.Parse(banquetBaseURL)
	var events []models.Event

	doc.Find("a.card").Each(func(_ int, s *goquery.Selection) {
		event := parseCard(s, base)
		if event != nil {
			events = append(events, *event)
		}
	})
	return events, nil
}

func parseCard(s *goquery.Selection, base *url.URL) *models.Event {
	artistEl := s.Find("span.artist")
	titleEl := s.Find("span.title")
	href, exists := s.Attr("href")
	if !exists || href == "" || artistEl.Length() == 0 || titleEl.Length() == 0 {
		return nil
	}

	eventURL := resolveURL(base, href)
	if eventURL == "" {
		return nil
	}

	titleParts := strings.SplitN(titleEl.Text(), " at ", 2)
	if len(titleParts) < 2 {
		return nil
	}

	dt := parseDateTime(titleParts[0], titleParts[1])
	if dt == nil {
		return nil
	}

	location := strings.SplitN(titleParts[1], ",", 2)[0]
	return &models.Event{
		Artist:   artistEl.Text(),
		Location: location,
		DateTime: *dt,
		URL:      eventURL,
	}
}

func parseDateTime(datePart, locationPart string) *time.Time {
	m := datePattern.FindStringSubmatch(datePart)
	if m == nil {
		return nil
	}

	day, err := strconv.Atoi(m[1])
	if err != nil {
		return nil
	}

	month, ok := parseMonth(strings.ToUpper(m[2]))
	if !ok {
		return nil
	}

	t := parseTime(locationPart)
	dt := time.Date(time.Now().Year(), month, day, t.Hour(), t.Minute(), 0, 0, time.UTC)
	return &dt
}

func parseTime(locationPart string) time.Time {
	m := timePattern.FindStringSubmatch(locationPart)
	if m == nil {
		return time.Time{}
	}
	t, err := time.Parse("3:04PM", strings.ToUpper(m[1]))
	if err != nil {
		return time.Time{}
	}
	return t
}

func parseMonth(s string) (time.Month, bool) {
	months := map[string]time.Month{
		"JANUARY": time.January, "FEBRUARY": time.February, "MARCH": time.March,
		"APRIL": time.April, "MAY": time.May, "JUNE": time.June,
		"JULY": time.July, "AUGUST": time.August, "SEPTEMBER": time.September,
		"OCTOBER": time.October, "NOVEMBER": time.November, "DECEMBER": time.December,
	}
	m, ok := months[s]
	return m, ok
}

func resolveURL(base *url.URL, href string) string {
	ref, err := url.Parse(href)
	if err != nil {
		return ""
	}
	return base.ResolveReference(ref).String()
}
