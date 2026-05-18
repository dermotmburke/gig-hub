# Configure Gatus heartbeat monitoring for gig-hub

## Background

`gig-hub` is a Spring Boot CLI that runs as a Kubernetes `CronJob`. After each pipeline run it
pings one or more heartbeat URLs via HTTP GET. The property is:

```properties
heartbeat.urls[0]=https://<gatus-host>/api/v1/endpoints/<key>/heartbeat
```

This is already implemented — you only need to configure Gatus and then set the property on gig-hub.

## How Gatus heartbeat works

Normal Gatus monitoring has Gatus polling an endpoint. Heartbeat is the inverse — the job calls
Gatus, and Gatus alerts if no ping arrives within the expected window. This is the right pattern
for a batch job with no persistent HTTP endpoint.

## Gatus configuration to add

Add a heartbeat endpoint to your Gatus config file (typically `config.yaml`):

```yaml
endpoints:
  - name: gig-hub
    group: jobs
    url: "heartbeat"
    interval: 1h          # match this to your CronJob schedule
    alerts:
      - type: slack        # or whichever alerting provider you use
        failure-threshold: 1
        success-threshold: 1
        description: "gig-hub has not run in the last hour"
        send-on-resolved: true
```

## Heartbeat URL format

Once the above is in place, the URL gig-hub must ping is:

```
https://<gatus-host>/api/v1/endpoints/jobs_gig-hub/heartbeat
```

The key is `<group>_<name>` — if you change the `name` or `group` above, adjust accordingly.
If there is no group, the key is just the name.

## Property to set on gig-hub

```properties
heartbeat.urls[0]=https://<gatus-host>/api/v1/endpoints/jobs_gig-hub/heartbeat
```

Or via env var in the Kubernetes `CronJob` manifest:

```yaml
env:
  - name: HEARTBEAT_URLS_0
    value: "https://<gatus-host>/api/v1/endpoints/jobs_gig-hub/heartbeat"
```

## What to do

1. Add the heartbeat endpoint block to the Gatus config file
2. Confirm the key (`jobs_gig-hub`, or adjusted if `name`/`group` differs)
3. Output the exact `heartbeat.urls[0]` value to set on gig-hub
