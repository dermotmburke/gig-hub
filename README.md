# gig-hub

A Spring Boot CLI that scrapes upcoming music events and delivers them to Slack. Designed to run as a Kubernetes Job or cron job — it runs all pipelines once and exits. Events are deduplicated using Redis so each event is only notified once.

## How it works

On each run, gig-hub executes every configured event pipeline in sequence:

1. **Fetch** — pull raw content from the venue source (HTML scrape or Ticketmaster API)
2. **Extract** — parse raw content into a list of `Event` records
3. **Deduplicate** — filter out events already seen in Redis (skipped if Redis is not configured)
4. **Notify** — fan out to all active notifiers (Slack, stdout)
5. **Mark sent** — record new events in Redis with a TTL

After every pipeline has run, `EventPipelineRunner` reports the run's outcome **once** to the heartbeat targets. If any pipeline failed, the run is reported as failed (see [Heartbeat monitoring](#heartbeat-monitoring)).

```mermaid
flowchart TD
    Runner["EventPipelineRunner\n(CommandLineRunner)"]

    Runner --> BR["BanquetEventPipeline"]
    Runner --> TM["TicketmasterVenueEventPipeline ×N"]

    BR --> BF["BanquetEventFetcher\n(HTML via FlareSolverr, or plain HTTP)"]
    TM --> RF["TicketmasterEventFetcher\n(Ticketmaster API)"]

    BF --> |raw HTML| BE["BanquetEventExtractor"]
    RF --> |raw JSON| TE["TicketmasterEventExtractor"]

    BE --> |List&lt;Event&gt;| Dedup
    TE --> |List&lt;Event&gt;| Dedup

    Dedup{"EventDeduplicator\n(optional — requires Redis)"}
    Dedup --> |new events only| Notify

    Notify["Notifiers"]
    Notify --> Log["LoggingEventNotifier\n(always active)"]
    Notify --> Slack["SlackEventNotifier\n(requires slack.webhook-url)"]

    Dedup --> |markSent| Redis[("Redis")]
    Slack --> |POST| SlackAPI["Slack webhook"]
    Runner --> |once per run: success / failed pipelines| Heartbeat["HeartbeatReporter\n(requires heartbeat.targets[0].url)"]
    Heartbeat --> |GET or POST| HeartbeatURLs["Heartbeat target(s)"]
```

## Supported venues

| Venue | Source | Enabled |
|---|---|---|
| [Banquet Records](https://www.banquetrecords.com) | HTML scrape (via FlareSolverr when `fetchers.flaresolverr.url` is set) | Always |
| Royal Albert Hall | Ticketmaster API | When `fetchers.ticketmaster.api-key` and `fetchers.ticketmaster.venues.royal-albert-hall.id` are set |
| Brixton Academy | Ticketmaster API | When `fetchers.ticketmaster.api-key` and `fetchers.ticketmaster.venues.brixton-academy.id` are set |
| Eventim Apollo | Ticketmaster API | When `fetchers.ticketmaster.api-key` and `fetchers.ticketmaster.venues.eventim-apollo.id` are set |
| Royal Festival Hall | Ticketmaster API | When `fetchers.ticketmaster.api-key` and `fetchers.ticketmaster.venues.royal-festival-hall.id` are set |

Any number of additional Ticketmaster venues can be added with no code changes — see [Adding a Ticketmaster venue](#adding-a-ticketmaster-venue).

## Configuration

All configuration is via environment variables or `application.properties`. Everything is optional — the app runs with no configuration, logging events to stdout only.

### Notifications

| Property | Env var | Optional? | Description |
|---|---|---|---|
| `slack.webhook-url` | `SLACK_WEBHOOK_URL` | Optional | Slack incoming webhook URL. When set, new events are posted to Slack. |
| `heartbeat.targets[0].url`..`[N].url` | `HEARTBEAT_TARGETS_0_URL`..`_N_URL` | Optional | URL to call once per run. Add as many targets as needed. See [Heartbeat monitoring](#heartbeat-monitoring) for how failures are reported. |
| `heartbeat.targets[N].method` | `HEARTBEAT_TARGETS_N_METHOD` | Optional | HTTP method for target N. `GET` (default) or `POST`. Use `POST` for Gatus external-endpoint push monitors. |
| `heartbeat.targets[N].token` | `HEARTBEAT_TARGETS_N_TOKEN` | Optional | Bearer token for target N. When set, an `Authorization: Bearer <token>` header is included. Required for Gatus push monitors. |
| `gig-hub-calendar.base-url` | `GIGHUBCALENDAR_BASEURL` | Optional | Base URL of a [gig-hub-calendar](https://github.com/dermotmburke/gig-hub-calendar) instance. When set, each Slack notification includes a save link. |

### Heartbeat monitoring

The heartbeat is sent once per run, after all pipelines have finished:

- **All pipelines succeeded** — every target is called with its configured URL.
- **Any pipeline failed** — targets whose URL has a `success=` query parameter (Gatus external endpoints) are called with `success=false&error=Pipelines failed: <ids>`. Targets without one (healthchecks.io, Dead Man's Snitch) are **not** called, so the missed check-in raises the alert.

### Fetching

| Property | Env var | Optional? | Description |
|---|---|---|---|
| `fetchers.banquet.url` | `FETCHERS_BANQUET_URL` | Optional | Banquet events page. Defaults to `https://www.banquetrecords.com/events?w=1000`. |
| `fetchers.flaresolverr.url` | `FETCHERS_FLARESOLVERR_URL` | Optional | [FlareSolverr](https://github.com/FlareSolverr/FlareSolverr) endpoint, e.g. `http://flaresolverr:8191/v1`. Banquet sits behind a Cloudflare browser check that rejects plain HTTP clients with a 403; when this is set, the Banquet page is loaded through FlareSolverr's headless browser instead. |

### Deduplication

| Property | Env var | Optional? | Description |
|---|---|---|---|
| `redis.url` | `REDIS_URL` | Optional | Redis connection URL. When set, each event is only notified once. Without it, every event is notified on every run. |

[Upstash](https://upstash.com) offers a free tier with a `rediss://` URL:

```properties
redis.url=rediss://default:<password>@<host>:6379
```

### Fetchers

| Property | Env var | Optional? | Description |
|---|---|---|---|
| `fetchers.banquet.url` | `FETCHERS_BANQUET_URL` | Optional | Override the Banquet Records scrape URL. Default: `https://www.banquetrecords.com/events?w=1000` |
| `fetchers.ticketmaster.api-key` | `FETCHERS_TICKETMASTER_API_KEY` | Optional | Ticketmaster Discovery API key. Required to enable any Ticketmaster venue. |
| `fetchers.ticketmaster.venues.<name>.id` | `FETCHERS_TICKETMASTER_VENUES_<NAME>_ID` | Optional | Ticketmaster venue ID. One entry per venue; name must be kebab-case. Requires `api-key` to be set. |

### OpenTelemetry

Traces are exported via OTLP/HTTP. All properties are optional with sensible defaults.

| Property | Env var | Default | Description |
|---|---|---|---|
| `otel.service.name` | `OTEL_SERVICE_NAME` | `gig-hub` | Service name in traces |
| `otel.exporter.otlp.endpoint` | `OTEL_EXPORTER_OTLP_ENDPOINT` | `http://localhost:4318` | OTLP/HTTP collector (Jaeger, Grafana Tempo, etc.) |
| `otel.traces.exporter` | `OTEL_TRACES_EXPORTER` | `logging` | Set to `otlp` to export to the collector above |

### Full example

```properties
# Slack (optional)
slack.webhook-url=https://hooks.slack.com/services/your/webhook/url

# Redis deduplication (optional)
redis.url=rediss://default:<password>@<host>:6379

# Heartbeat monitoring (optional — add as many targets as needed)
# Plain GET — Dead Man's Snitch, healthchecks.io, etc.
heartbeat.targets[0].url=https://nosnch.in/abc123

# POST with Bearer token — Gatus external-endpoint push monitor
heartbeat.targets[1].url=https://gatus.example.com/api/v1/endpoints/jobs_gig-hub/external?success=true
heartbeat.targets[1].method=POST
heartbeat.targets[1].token=${HEARTBEAT_TOKEN}

# gig-hub-calendar save links in Slack (optional)
gig-hub-calendar.base-url=https://calendar.example.com

# Ticketmaster venues (optional — add as many as needed)
fetchers.ticketmaster.api-key=your_consumer_key
fetchers.ticketmaster.venues.royal-albert-hall.id=KovZ9177Arf
fetchers.ticketmaster.venues.brixton-academy.id=KovZ91777af
fetchers.ticketmaster.venues.eventim-apollo.id=KovZpZAtadaA
fetchers.ticketmaster.venues.royal-festival-hall.id=KovZpZAnFvlA
```

## Adding a Ticketmaster venue

No code changes needed. Add one property to `application.properties` (or the equivalent env var):

```properties
fetchers.ticketmaster.venues.my-venue.id=KovZ...
```

The venue name (e.g. `my-venue`) must be kebab-case. To find the Ticketmaster venue ID:

```bash
curl "https://app.ticketmaster.com/discovery/v2/venues.json?keyword=My+Venue&countryCode=GB&apikey=YOUR_API_KEY"
```

## Running locally

**Prerequisites:** Java 21, Maven

```bash
# Run with defaults (logs only, no Slack, no dedup)
mvn spring-boot:run

# Run with Slack and Redis
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/... \
REDIS_URL=rediss://default:...@...upstash.io:6379 \
mvn spring-boot:run

# Run with Ticketmaster venues
FETCHERS_TICKETMASTER_API_KEY=your_consumer_key \
FETCHERS_TICKETMASTER_VENUES_ROYAL_ALBERT_HALL_ID=KovZ9177Arf \
FETCHERS_TICKETMASTER_VENUES_BRIXTON_ACADEMY_ID=KovZ91777af \
mvn spring-boot:run
```

## Running with Docker

```bash
docker run \
  -e SLACK_WEBHOOK_URL=https://hooks.slack.com/services/... \
  -e REDIS_URL=rediss://default:...@...upstash.io:6379 \
  -e FETCHERS_TICKETMASTER_API_KEY=your_consumer_key \
  -e FETCHERS_TICKETMASTER_VENUES_ROYAL_ALBERT_HALL_ID=KovZ9177Arf \
  -e FETCHERS_TICKETMASTER_VENUES_BRIXTON_ACADEMY_ID=KovZ91777af \
  ghcr.io/dermotmburke/gig-hub:latest
```

## Running on Kubernetes (Helm chart)

gig-hub runs all pipelines once and exits, so it is deployed as a `CronJob`. The chart in [`deploy/helm/gig-hub`](deploy/helm/gig-hub) contains:

- **CronJob** `<release>` — runs the image every 15 minutes (`schedule`), `concurrencyPolicy: Forbid`
- **Secret** `<release>-env` — everything under `env:` in values, loaded with `envFrom`
- **FlareSolverr** Deployment + Service `<release>-flaresolverr` (`flaresolverr.enabled`, default `true`). The chart sets `FETCHERS_FLARESOLVERR_URL` to point at it.

```bash
helm upgrade --install gig-hub oci://ghcr.io/dermotmburke/helm/gig-hub -n gig-hub --create-namespace \
  --set 'imagePullSecrets[0].name=ghcr-pull-secret' \
  --set env.REDIS_URL=rediss://... --set env.SLACK_WEBHOOKURL=https://hooks.slack.com/...
```

The chart's `appVersion` is the image tag, so installing chart `X.Y.0` runs image `X.Y`. The homelab cluster tracks the newest chart with Flux (an `OCIRepository` + `HelmRelease`), so every release here rolls out automatically.

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
| CI | Pull request to `main` | Runs tests, lints the Helm chart |
| Docker Build and Publish | Push to `main` | Runs tests → builds and pushes Docker image to GHCR → packages and pushes the Helm chart → creates GitHub release → bumps minor version |

For pom version `X.Y-SNAPSHOT`, a release publishes:

- image `ghcr.io/dermotmburke/gig-hub:X.Y` (and `latest`)
- chart `oci://ghcr.io/dermotmburke/helm/gig-hub:X.Y.0` (and `latest`), with `appVersion: X.Y`

## Project structure

```
src/main/java/com/d3bot/events/
├── Main.java                               # Entry point — System.exit(SpringApplication.exit(...))
├── models/
│   └── Event.java                          # Immutable record (artist, location, dateTime, url)
├── pipelines/
│   ├── EventPipeline.java                  # Abstract base: fetch→extract→dedup→notify→markSent
│   ├── BanquetEventPipeline.java           # Always active
│   └── TicketmasterVenueEventPipeline.java # One instance per configured Ticketmaster venue
├── runners/
│   └── EventPipelineRunner.java            # CommandLineRunner — runs all pipelines on startup
├── fetchers/
│   ├── EventFetcher.java                   # Interface: fetch() → String
│   ├── BanquetEventFetcher.java            # Delegates to UrlFetcher
│   └── TicketmasterEventFetcher.java       # Ticketmaster Discovery API — instantiated per venue
├── extractors/
│   ├── EventExtractor.java                 # Interface: extract(String) → List<Event>
│   ├── BanquetEventExtractor.java          # HTML parsing via Jsoup
│   └── TicketmasterEventExtractor.java     # JSON parsing via Jackson
├── notifiers/
│   ├── EventNotifier.java                  # Interface
│   ├── LoggingEventNotifier.java           # Always active
│   ├── SlackEventNotifier.java             # Active when slack.webhook-url is set
│   └── HeartbeatEventNotifier.java         # Active when heartbeat.targets[0].url is set
├── deduplicators/
│   └── EventDeduplicator.java              # Active when redis.url is set
├── utilities/
│   ├── UrlFetcher.java                     # Shared HTTP GET via Java HttpClient
│   └── RouteIdBuilder.java                 # Derives kebab-case pipeline IDs from class names
└── config/
    ├── HttpClientConfig.java               # Java HttpClient bean
    ├── RedisConfig.java                    # Active when redis.url is set
    ├── OpenTelemetryConfig.java            # OTLP/HTTP exporter — sends traces to Jaeger/Tempo
    ├── TicketmasterPipelineFactory.java    # Creates TicketmasterVenueEventPipeline instances
    └── TicketmasterVenueBeanRegistrar.java # Registers one pipeline bean per configured venue
```
