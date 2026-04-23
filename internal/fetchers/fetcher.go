package fetchers

type Fetcher interface {
	Fetch() (string, error)
}
