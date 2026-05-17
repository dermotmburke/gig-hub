# gig-hub

A Spring Boot CLI that scrapes upcoming music events and delivers them to Slack. Designed to run as a Kubernetes Job or cron job — it runs all pipelines once and exits. Events are deduplicated using Redis so each event is only notified once.

## How it works

On each run, gig-hub executes every configured event pipeline in sequence:

1. **Fetch** — pull raw content from the venue source (HTML scrape or Ticketmaster API)
2. **Extract** — parse raw content into a list of `Event` records
3. **Deduplicate** — filter out events already seen in Redis (skipped if Redis is not configured)
4. **Notify** — fan out to all active notifiers (Slack, heartbeat URLs, stdout)
5. **Mark sent** — record new events in Redis with a TTL

```mermaid
flowchart TD
    Runner["EventPipelineRunner\n(CommandLineRunner)"]

    Runner --> BR["BanquetEventPipeline"]
    Runner --> TM["TicketmasterVenueEventPipeline ×N"]

    BR --> BF["BanquetEventFetcher\n(HTTP / HTML)"]
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
    Notify --> Heartbeat["HeartbeatEventNotifier\n(requires heartbeat.urls[0])"]

    Dedup --> |markSent| Redis[("Redis")]
    Slack --> |POST| SlackAPI["Slack webhook"]
    Heartbeat --> |GET| HeartbeatURLs["Heartbeat URL(s)"]
```

## Supported venues

| Venue | Source | Enabled |
|---|---|---|
| [Banquet Records](https://www.banquetrecords.com) | HTML scrape | Always |
| Royal Albert Hall | Ticketmaster API | When `fetchers.ticketmaster.api-key` and `fetchers.ticketmaster.venues.royal-albert-hall.id` are set |
| Brixton Academy | Ticketmaster API | When `fetchers.ticketmaster.api-key` and `fetchers.ticketmaster.venues.brixton-academy.id` are set |
| Eventim Apollo | Ticketmaster API | When `fetchers.ticketmaster.api-key` and `fetchers.ticketmaster.venues.eventim-apollo.id` are set |
| Royal Festival Hall | Ticketmaster API | When `fetchers.ticketmaster.api-key` and `fetchers.ticketmaster.venues.royal-festival-hall.id` are set |

Any number of additional Ticketmaster venues can be added with no code changes — see [Adding a Ticketmaster venue](#adding-a-ticketmaster-venue).

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
| `fetchers.ticketmaster.api-key` | `FETCHERS_TICKETMASTER_API_KEY` | *(unset)* | Ticketmaster Discovery API consumer key — required to enable any Ticketmaster venue |
| `fetchers.ticketmaster.venues.<name>.id` | `FETCHERS_TICKETMASTER_VENUES_<NAME>_ID` | *(unset)* | Ticketmaster venue ID for a named venue — one entry per venue, kebab-case name |

### Heartbeat monitoring

gig-hub can ping one or more URLs on each successful run, compatible with [Gatus](https://github.com/TwiN/gatus), Dead Man's Snitch, or any HTTP heartbeat endpoint:

| Property | Description |
|---|---|
| `heartbeat.urls[0]` | First heartbeat URL to ping |
| `heartbeat.urls[1]` | Second heartbeat URL (add as many as needed) |

```properties
heartbeat.urls[0]=https://status.example.com/api/v1/endpoints/gig-hub/heartbeat
heartbeat.urls[1]=https://nosnch.in/abc123
```

### OpenTelemetry

| Property | Env var | Default | Description |
|---|---|---|---|
| `otel.service.name` | `OTEL_SERVICE_NAME` | `gig-hub` | Service name reported in traces |
| `otel.exporter.otlp.endpoint` | `OTEL_EXPORTER_OTLP_ENDPOINT` | `http://localhost:4318` | OTLP/HTTP collector endpoint (Jaeger, Grafana Tempo, etc.) |

### Slack

Create an [incoming webhook](https://api.slack.com/messaging/webhooks) in your Slack workspace and set `SLACK_WEBHOOK_URL`. If not set, events are logged to stdout only.

### Redis (deduplication)

[Upstash](https://upstash.com) works well — it offers a free tier and provides a `rediss://` URL directly:

```
REDIS_URL=rediss://default:<password>@<host>:6379
```

If `REDIS_URL` is not set the app runs without deduplication — every event is notified on every run.

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

## Running as a Kubernetes Job

gig-hub runs all pipelines once and exits with code 0 on success, making it suited for a `CronJob`:

```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: gig-hub
spec:
  schedule: "0 * * * *"   # hourly
  jobTemplate:
    spec:
      template:
        spec:
          containers:
            - name: gig-hub
              image: ghcr.io/dermotmburke/gig-hub:latest
              env:
                - name: SLACK_WEBHOOK_URL
                  valueFrom:
                    secretKeyRef:
                      name: gig-hub
                      key: slack-webhook-url
                - name: REDIS_URL
                  valueFrom:
                    secretKeyRef:
                      name: gig-hub
                      key: redis-url
          restartPolicy: OnFailure
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
│   └── HeartbeatEventNotifier.java         # Active when heartbeat.urls[0] is set
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
