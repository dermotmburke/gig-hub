package utilities

import (
	"fmt"
	"io"
	"net/http"
)

type URLFetcher struct {
	client *http.Client
}

func NewURLFetcher(client *http.Client) *URLFetcher {
	return &URLFetcher{client: client}
}

func (f *URLFetcher) Fetch(url string) (string, error) {
	resp, err := f.client.Get(url)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return "", err
	}
	if resp.StatusCode != http.StatusOK {
		return "", fmt.Errorf("request failed with status %d: %s", resp.StatusCode, string(body))
	}
	return string(body), nil
}
