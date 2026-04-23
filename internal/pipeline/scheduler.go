package pipeline

import (
	"context"
	"log/slog"
	"time"
)

type Scheduler struct {
	pipelines  []*Pipeline
	intervalMs int64
}

func NewScheduler(pipelines []*Pipeline, intervalMs int64) *Scheduler {
	return &Scheduler{pipelines: pipelines, intervalMs: intervalMs}
}

func (s *Scheduler) Start(ctx context.Context) {
	slog.Info("Starting pipeline scheduler", "interval_ms", s.intervalMs, "pipelines", len(s.pipelines))

	s.runAll(ctx)

	ticker := time.NewTicker(time.Duration(s.intervalMs) * time.Millisecond)
	defer ticker.Stop()

	for {
		select {
		case <-ticker.C:
			s.runAll(ctx)
		case <-ctx.Done():
			slog.Info("Scheduler stopped")
			return
		}
	}
}

func (s *Scheduler) runAll(ctx context.Context) {
	for _, p := range s.pipelines {
		p.Run(ctx)
	}
}
