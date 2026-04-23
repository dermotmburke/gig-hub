package fetchers

import (
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/dermotmburke/gig-hub/internal/utilities"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestBanquetFetcher_Fetch(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("<html>events</html>"))
	}))
	defer server.Close()

	fetcher := NewBanquetFetcher(utilities.NewURLFetcher(server.Client()), server.URL)
	body, err := fetcher.Fetch()

	require.NoError(t, err)
	assert.Equal(t, "<html>events</html>", body)
}

func TestBanquetFetcher_Fetch_Error(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusInternalServerError)
	}))
	defer server.Close()

	fetcher := NewBanquetFetcher(utilities.NewURLFetcher(server.Client()), server.URL)
	_, err := fetcher.Fetch()

	assert.Error(t, err)
}
