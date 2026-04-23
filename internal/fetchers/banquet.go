package fetchers

import "github.com/dermotmburke/gig-hub/internal/utilities"

type BanquetFetcher struct {
	urlFetcher *utilities.URLFetcher
	url        string
}

func NewBanquetFetcher(urlFetcher *utilities.URLFetcher, url string) *BanquetFetcher {
	return &BanquetFetcher{urlFetcher: urlFetcher, url: url}
}

func (f *BanquetFetcher) Fetch() (string, error) {
	return f.urlFetcher.Fetch(f.url)
}
