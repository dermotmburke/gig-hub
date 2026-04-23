package fetchers

import (
	"fmt"

	"github.com/dermotmburke/gig-hub/internal/utilities"
)

const ticketmasterBaseURL = "https://app.ticketmaster.com/discovery/v2/events.json"

type TicketmasterFetcher struct {
	urlFetcher *utilities.URLFetcher
	venueID    string
	apiKey     string
}

func NewTicketmasterFetcher(urlFetcher *utilities.URLFetcher, venueID, apiKey string) *TicketmasterFetcher {
	return &TicketmasterFetcher{urlFetcher: urlFetcher, venueID: venueID, apiKey: apiKey}
}

func (f *TicketmasterFetcher) Fetch() (string, error) {
	url := fmt.Sprintf("%s?venueId=%s&apikey=%s&size=200", ticketmasterBaseURL, f.venueID, f.apiKey)
	return f.urlFetcher.Fetch(url)
}
