package config

import (
	"strings"

	"github.com/spf13/viper"
)

type Venue struct {
	ID string `mapstructure:"id"`
}

type Config struct {
	RunnerIntervalMs         int64
	SlackWebhookURL          string
	RedisURL                 string
	BanquetURL               string
	TicketmasterAPIKey       string
	TicketmasterVenues       map[string]Venue
	OtelServiceName          string
	OtelExporterOtlpEndpoint string
	GigHubCalendarBaseURL    string
}

func Load() *Config {
	v := viper.New()
	v.SetEnvKeyReplacer(strings.NewReplacer(".", "_", "-", "_"))
	v.AutomaticEnv()

	v.SetDefault("runner.interval-ms", 3600000)
	v.SetDefault("fetchers.banquet.url", "https://www.banquetrecords.com/events?w=1000")
	v.SetDefault("otel.service.name", "gig-hub")
	v.SetDefault("otel.exporter.otlp.endpoint", "http://localhost:4318")

	v.SetConfigName("config")
	v.SetConfigType("yaml")
	v.AddConfigPath(".")
	_ = v.ReadInConfig()

	venues := make(map[string]Venue)
	for name, val := range v.GetStringMap("fetchers.ticketmaster.venues") {
		if m, ok := val.(map[string]interface{}); ok {
			if id, ok := m["id"].(string); ok && id != "" {
				venues[name] = Venue{ID: id}
			}
		}
	}

	return &Config{
		RunnerIntervalMs:         v.GetInt64("runner.interval-ms"),
		SlackWebhookURL:          v.GetString("slack.webhook-url"),
		RedisURL:                 v.GetString("redis.url"),
		BanquetURL:               v.GetString("fetchers.banquet.url"),
		TicketmasterAPIKey:       v.GetString("fetchers.ticketmaster.api-key"),
		OtelServiceName:          v.GetString("otel.service.name"),
		OtelExporterOtlpEndpoint: v.GetString("otel.exporter.otlp.endpoint"),
		GigHubCalendarBaseURL:    v.GetString("gig-hub-calendar.base-url"),
		TicketmasterVenues:       venues,
	}
}
