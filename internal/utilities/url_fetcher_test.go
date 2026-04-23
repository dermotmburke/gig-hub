package utilities

import (
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestURLFetcher_Fetch_Success(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("hello world"))
	}))
	defer server.Close()

	fetcher := NewURLFetcher(server.Client())
	body, err := fetcher.Fetch(server.URL)

	require.NoError(t, err)
	assert.Equal(t, "hello world", body)
}

func TestURLFetcher_Fetch_NonOKStatus(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusNotFound)
		w.Write([]byte("not found"))
	}))
	defer server.Close()

	fetcher := NewURLFetcher(server.Client())
	_, err := fetcher.Fetch(server.URL)

	require.Error(t, err)
	assert.Contains(t, err.Error(), "404")
}

func TestURLFetcher_Fetch_ConnectionError(t *testing.T) {
	fetcher := NewURLFetcher(&http.Client{})
	_, err := fetcher.Fetch("http://localhost:1")
	assert.Error(t, err)
}
