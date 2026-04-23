package extractors

import "github.com/dermotmburke/gig-hub/internal/models"

type Extractor interface {
	Extract(raw string) ([]models.Event, error)
}
