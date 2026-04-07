# gig-hub

A Spring Boot application that scrapes upcoming music events and delivers them to Slack. Events are deduplicated using Redis so each event is only notified once.

## How it works

On a configurable schedule (default: hourly), gig-hub:

1. Scrapes event listings from supported venues
2. Filters out events already seen (using Redis as a cache keyed on a checksum of venue + date/time)
3. Posts new events to a Slack channel via an incoming webhook
4. Marks the new events as sent in Redis with a TTL set to expire the day after the event

## Supported scrapers

| Venue | Status |
|---|---|
| [Banquet Records](https://www.banquetrecords.com) | Implemented |
| Royal Albert Hall | Stub (not yet implemented) |

## Configuration

All configuration is via environment variables or `application.properties`.

| Property | Env var | Default | Description |
|---|---|---|---|
| `scraper.interval-ms` | `SCRAPER_INTERVAL_MS` | `3600000` | How often to scrape, in milliseconds |
| `scrapers.banquet.url` | `SCRAPERS_BANQUET_URL` | Banquet events URL | Override the Banquet scrape URL |
| `slack.webhook-url` | `SLACK_WEBHOOK_URL` | *(unset)* | Slack incoming webhook URL — Slack notifications only sent if set |
| `redis.url` | `REDIS_URL` | *(unset)* | Redis connection URL — deduplication only enabled if set |

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
```

## Running with Docker

```bash
docker run \
  -e SLACK_WEBHOOK_URL=https://hooks.slack.com/services/... \
  -e REDIS_URL=rediss://default:...@...upstash.io:6379 \
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
├── Main.java                            # Spring Boot entry point
├── models/
│   └── Event.java                       # Immutable record (artist, location, dateTime, url)
├── jobs/
│   └── EventScrapeJob.java              # Scheduled orchestrator
├── scrapers/
│   ├── EventScraper.java                # Interface
│   ├── BanquetEventScraper.java
│   └── RoyalAlbertHallEventScraper.java # Stub
├── extractors/
│   └── BanquetEventExtractor.java       # HTML parsing via Jsoup
├── fetchers/
│   └── EventFetcher.java                # HTTP fetcher
├── notifiers/
│   ├── EventNotifier.java               # Interface
│   ├── LoggingEventNotifier.java        # Always active
│   └── SlackEventNotifier.java          # Active when slack.webhook-url is set
├── deduplication/
│   └── EventDeduplicationService.java   # Active when redis.url is set
└── config/
    ├── HttpClientConfig.java
    └── RedisConfig.java                 # Active when redis.url is set
```
