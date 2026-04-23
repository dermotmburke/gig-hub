package main

import (
	"context"
	"log/slog"
	"net/http"
	"os"
	"os/signal"
	"syscall"

	"github.com/gin-gonic/gin"
	"github.com/redis/go-redis/v9"

	"github.com/dermotmburke/gig-hub/internal/config"
	"github.com/dermotmburke/gig-hub/internal/deduplicator"
	"github.com/dermotmburke/gig-hub/internal/extractors"
	"github.com/dermotmburke/gig-hub/internal/fetchers"
	"github.com/dermotmburke/gig-hub/internal/health"
	"github.com/dermotmburke/gig-hub/internal/notifiers"
	"github.com/dermotmburke/gig-hub/internal/pipeline"
	"github.com/dermotmburke/gig-hub/internal/telemetry"
	"github.com/dermotmburke/gig-hub/internal/utilities"
)

var version = "dev"

func main() {
	cfg := config.Load()

	ctx, cancel := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer cancel()

	shutdown, err := telemetry.Setup(ctx, cfg.OtelServiceName, cfg.OtelExporterOtlpEndpoint)
	if err != nil {
		slog.Warn("OpenTelemetry setup failed, tracing disabled", "error", err)
	} else {
		defer shutdown(context.Background()) //nolint:errcheck
	}

	httpClient := &http.Client{}
	urlFetcher := utilities.NewURLFetcher(httpClient)

	var redisClient *redis.Client
	var dedup *deduplicator.EventDeduplicator
	if cfg.RedisURL != "" {
		opts, err := redis.ParseURL(cfg.RedisURL)
		if err != nil {
			slog.Error("Invalid Redis URL", "error", err)
			os.Exit(1)
		}
		redisClient = redis.NewClient(opts)
		dedup = deduplicator.New(redisClient)
		slog.Info("Redis deduplication enabled")
	}

	var calURLBuilder *utilities.CalendarURLBuilder
	if cfg.GigHubCalendarBaseURL != "" {
		calURLBuilder = utilities.NewCalendarURLBuilder(cfg.GigHubCalendarBaseURL)
	}

	notifierList := []notifiers.Notifier{notifiers.NewLoggingNotifier()}
	if cfg.SlackWebhookURL != "" {
		notifierList = append(notifierList, notifiers.NewSlackNotifier(httpClient, cfg.SlackWebhookURL, calURLBuilder))
		slog.Info("Slack notifications enabled")
	}

	var pipelines []*pipeline.Pipeline

	banquetFetcher := fetchers.NewBanquetFetcher(urlFetcher, cfg.BanquetURL)
	pipelines = append(pipelines, pipeline.New("banquet-pipeline", banquetFetcher, extractors.NewBanquetExtractor(), notifierList, dedup))

	if cfg.TicketmasterAPIKey != "" {
		tmExtractor := extractors.NewTicketmasterExtractor()
		for name, venue := range cfg.TicketmasterVenues {
			tmFetcher := fetchers.NewTicketmasterFetcher(urlFetcher, venue.ID, cfg.TicketmasterAPIKey)
			pipelines = append(pipelines, pipeline.New(name+"-pipeline", tmFetcher, tmExtractor, notifierList, dedup))
			slog.Info("Ticketmaster pipeline registered", "venue", name)
		}
	}

	gin.SetMode(gin.ReleaseMode)
	router := gin.New()
	router.Use(gin.Recovery())
	health.NewHandler(redisClient, version).RegisterRoutes(router)

	go func() {
		slog.Info("Health server listening", "addr", ":8080")
		if err := router.Run(":8080"); err != nil {
			slog.Error("Health server error", "error", err)
		}
	}()

	pipeline.NewScheduler(pipelines, cfg.RunnerIntervalMs).Start(ctx)
}
