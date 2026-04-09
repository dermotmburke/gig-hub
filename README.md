# gig-hub

A Spring Boot application that scrapes upcoming music events and delivers them to Slack. Events are deduplicated using Redis so each event is only notified once.

## How it works

On a configurable schedule (default: hourly), gig-hub runs each configured event pipeline using [Apache Camel](https://camel.apache.org/):

1. A Camel timer triggers the scheduler, which fires each venue's pipeline route
2. Each pipeline route: fetches → extracts → deduplicates → notifies → marks sent
3. Deduplication uses Redis keyed on a checksum of venue + date/time (skipped if Redis is not configured)
4. New events are posted to a Slack channel via an incoming webhook
5. Sent events are marked in Redis with a TTL set to expire the day after the event

Distributed traces are exported via OpenTelemetry (OTLP/HTTP) — each pipeline run produces a span tree visible in Jaeger or Grafana Tempo.

```mermaid
flowchart TD
    Scheduler["EventSchedulerRoute\n(Camel timer)"]

    Scheduler --> BR["BanquetEventRouteBuilder\n(direct:banquet-pipeline)"]
    Scheduler --> RR["RoyalAlbertHallEventRouteBuilder\n(direct:royal-albert-hall-pipeline)"]

    BR --> BF["BanquetEventFetcher\n(HTTP / HTML)"]
    RR --> RF["RoyalAlbertHallEventFetcher\n(Ticketmaster API)"]

    BF --> |raw HTML| BE["BanquetEventExtractor"]
    RF --> |raw JSON| TE["TicketmasterEventExtractor"]

    BE --> |List&lt;Event&gt;| Dedup
    TE --> |List&lt;Event&gt;| Dedup

    Dedup{"EventDeduplicatorProcessor\n(optional — requires Redis)"}
    Dedup --> |new events only| Notify

    Notify["EventNotificationProcessor"]
    Notify --> Log["LoggingEventNotifier\n(always active)"]
    Notify --> Slack["SlackEventNotifier\n(requires slack.webhook-url)"]

    Dedup --> |markSent| Redis[("Redis")]
    Slack --> |POST| SlackAPI["Slack webhook"]
```

## Supported venues

| Venue | Source | Enabled |
|---|---|---|
| [Banquet Records](https://www.banquetrecords.com) | HTML scrape | Always |
| Royal Albert Hall | Ticketmaster API | When `fetchers.ticketmaster.*` properties are set |

## Configuration

All configuration is via environment variables or `application.properties`.

### Core

| Property | Env var | Default | Description |
|---|---|---|---|
| `slack.webhook-url` | `SLACK_WEBHOOK_URL` | *(unset)* | Slack incoming webhook URL — notifications only sent if set |
| `redis.url` | `REDIS_URL` | *(unset)* | Redis connection URL — deduplication only enabled if set |

### Fetchers

| Property | Env var | Default | Description |
|---|---|---|---|
| `fetchers.banquet.url` | `FETCHERS_BANQUET_URL` | Banquet events URL | Override the Banquet Records scrape URL |
| `fetchers.ticketmaster.api-key` | `FETCHERS_TICKETMASTER_API_KEY` | *(unset)* | Ticketmaster Discovery API consumer key — required to enable Ticketmaster venues |
| `fetchers.ticketmaster.venues.royalalberthall.id` | `FETCHERS_TICKETMASTER_VENUES_ROYALALBERTHALL_ID` | *(unset)* | Ticketmaster venue ID for the Royal Albert Hall — enables the RAH pipeline when set alongside `fetchers.ticketmaster.api-key` |

To find the Ticketmaster venue ID for a venue:

```bash
curl "https://app.ticketmaster.com/discovery/v2/venues.json?keyword=Royal+Albert+Hall&countryCode=GB&apikey=YOUR_API_KEY"
```

### Scheduling

| Property | Env var | Default | Description |
|---|---|---|---|
| `scraper.interval-ms` | `SCRAPER_INTERVAL_MS` | `3600000` (1 hour) | How often to run all pipelines, in milliseconds |

### OpenTelemetry

| Property | Env var | Default | Description |
|---|---|---|---|
| `otel.service.name` | `OTEL_SERVICE_NAME` | `gig-hub` | Service name reported in traces |
| `otel.exporter.otlp.endpoint` | `OTEL_EXPORTER_OTLP_ENDPOINT` | `http://localhost:4318` | OTLP/HTTP collector endpoint (Jaeger, Grafana Tempo, etc.) |

Traces are exported via OTLP HTTP/protobuf to `<endpoint>/v1/traces`. To disable tracing, set `camel.opentelemetry.enabled=false`.

### Slack

Create an [incoming webhook](https://api.slack.com/messaging/webhooks) in your Slack workspace and set `SLACK_WEBHOOK_URL`. If not set, events are logged to stdout only.

### Redis (deduplication)

[Upstash](https://upstash.com) works well — it offers a free tier and provides a `rediss://` URL directly:

```
REDIS_URL=rediss://default:<password>@<host>:6379
```

If `REDIS_URL` is not set the app runs without deduplication — every event is notified on every run.

## Running locally

**Prerequisites:** Java 21, Maven

```bash
# Run with defaults (logs only, no Slack, no dedup)
mvn spring-boot:run

# Run with Slack and Redis
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/... \
REDIS_URL=rediss://default:...@...upstash.io:6379 \
mvn spring-boot:run

# Run with Ticketmaster (Royal Albert Hall)
FETCHERS_TICKETMASTER_API_KEY=your_consumer_key \
FETCHERS_TICKETMASTER_VENUES_ROYALALBERTHALL_ID=KovZpZAEdntA \
mvn spring-boot:run
```

## Running with Docker

```bash
docker run \
  -e SLACK_WEBHOOK_URL=https://hooks.slack.com/services/... \
  -e REDIS_URL=rediss://default:...@...upstash.io:6379 \
  -e FETCHERS_TICKETMASTER_API_KEY=your_consumer_key \
  -e FETCHERS_TICKETMASTER_VENUES_ROYALALBERTHALL_ID=KovZpZAEdntA \
  ghcr.io/dermotmburke/gig-hub:latest
```

## Tests

```bash
mvn test
```

Coverage is enforced at 90% line coverage via JaCoCo. To generate a full coverage report:

```bash
mvn verify
open target/site/jacoco/index.html
```

## CI / CD

| Workflow | Trigger | Action |
|---|---|---|
| CI | Pull request to `main` | Runs tests |
| Docker Build and Publish | Push to `main` | Runs tests → builds and pushes Docker image to GHCR → creates GitHub release → bumps minor version |

The Docker image is published to `ghcr.io/dermotmburke/gig-hub` tagged with `latest` and the version from `pom.xml`.

## Project structure

```
src/main/java/com/d3bot/events/
├── Main.java                               # @SpringBootApplication + @CamelOpenTelemetry
├── models/
│   └── Event.java                          # Immutable record (artist, location, dateTime, url)
├── routes/
│   ├── EventRouteBuilder.java              # Abstract Camel route: fetch→extract→dedup→notify→markSent
│   ├── EventSchedulerRoute.java            # Camel timer — fires each pipeline route on schedule
│   ├── BanquetEventRouteBuilder.java       # Always active
│   ├── RoyalAlbertHallEventRouteBuilder.java  # Active when Ticketmaster properties are set
│   └── processors/
│       ├── EventFetchProcessor.java        # Calls EventFetcher, handles InterruptedException
│       ├── EventExtractorProcessor.java    # Calls EventExtractor, sets List<Event> body
│       ├── EventDeduplicatorProcessor.java # Filters already-seen events via Redis
│       ├── EventNotificationProcessor.java # Fans out to all EventNotifiers
│       └── EventMarkSentProcessor.java     # Marks new events as sent in Redis
├── fetchers/
│   ├── EventFetcher.java                   # Interface: fetch() → String
│   ├── BanquetEventFetcher.java            # Delegates to UrlFetcher
│   ├── TicketmasterEventFetcher.java       # Abstract base for Ticketmaster API fetches
│   └── RoyalAlbertHallEventFetcher.java    # Supplies venue ID from config
├── extractors/
│   ├── EventExtractor.java                 # Interface: extract(String) → List<Event>
│   ├── BanquetEventExtractor.java          # HTML parsing via Jsoup
│   └── TicketmasterEventExtractor.java     # JSON parsing via Jackson
├── notifiers/
│   ├── EventNotifier.java                  # Interface
│   ├── LoggingEventNotifier.java           # Always active
│   └── SlackEventNotifier.java             # Active when slack.webhook-url is set
├── deduplicators/
│   └── EventDeduplicator.java              # Active when redis.url is set
├── utilities/
│   ├── UrlFetcher.java                     # Shared HTTP GET via Java HttpClient
│   └── RouteIdBuilder.java                 # Derives kebab-case route IDs from class names
└── config/
    ├── HttpClientConfig.java               # Java HttpClient bean
    ├── RedisConfig.java                    # Active when redis.url is set
    └── OpenTelemetryConfig.java            # OTLP/HTTP exporter — sends traces to Jaeger/Tempo
```
