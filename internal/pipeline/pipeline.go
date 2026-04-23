package pipeline

import (
	"context"
	"log/slog"

	"go.opentelemetry.io/otel"
	"go.opentelemetry.io/otel/trace"

	"github.com/dermotmburke/gig-hub/internal/deduplicator"
	"github.com/dermotmburke/gig-hub/internal/extractors"
	"github.com/dermotmburke/gig-hub/internal/fetchers"
	"github.com/dermotmburke/gig-hub/internal/notifiers"
)

type Pipeline struct {
	name         string
	fetcher      fetchers.Fetcher
	extractor    extractors.Extractor
	notifiers    []notifiers.Notifier
	deduplicator *deduplicator.EventDeduplicator
	tracer       trace.Tracer
}

func New(
	name string,
	fetcher fetchers.Fetcher,
	extractor extractors.Extractor,
	notifierList []notifiers.Notifier,
	dedup *deduplicator.EventDeduplicator,
) *Pipeline {
	return &Pipeline{
		name:         name,
		fetcher:      fetcher,
		extractor:    extractor,
		notifiers:    notifierList,
		deduplicator: dedup,
		tracer:       otel.Tracer("gig-hub"),
	}
}

func (p *Pipeline) Name() string {
	return p.name
}

func (p *Pipeline) Run(ctx context.Context) {
	ctx, span := p.tracer.Start(ctx, p.name)
	defer span.End()

	raw, err := p.fetcher.Fetch()
	if err != nil {
		slog.Error("Pipeline fetch failed", "pipeline", p.name, "error", err)
		return
	}

	events, err := p.extractor.Extract(raw)
	if err != nil {
		slog.Error("Pipeline extract failed", "pipeline", p.name, "error", err)
		return
	}

	slog.Info("Pipeline found events", "pipeline", p.name, "count", len(events))

	if p.deduplicator != nil {
		before := len(events)
		events = p.deduplicator.Filter(events)
		if len(events) < before {
			slog.Info("Deduplicated events", "pipeline", p.name, "new", len(events), "already_sent", before-len(events))
		}
	}

	for _, n := range p.notifiers {
		n.Notify(events)
	}

	if p.deduplicator != nil {
		p.deduplicator.MarkSent(events)
	}
}
