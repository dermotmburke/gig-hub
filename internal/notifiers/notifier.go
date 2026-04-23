package notifiers

import "github.com/dermotmburke/gig-hub/internal/models"

type Notifier interface {
	Notify(events []models.Event)
}
