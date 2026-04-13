# gig-hub-calendar

A companion app for [gig-hub](https://github.com/dermotmburke/gig-hub). Click a link in a Slack message to save a gig, then view your saved events and get Slack DM alerts when tickets go on sale or a gig is approaching.

**Stack:** Next.js 15 · TypeScript · Google Calendar API · Tailwind CSS

---

## How it works

1. `gig-hub` posts events to Slack — each event includes a 💾 Save link
2. Clicking the link opens `http://localhost:3000/save?artist=...` which saves the event to your Google Calendar (tagged so it's visible in this app)
3. Open `http://localhost:3000` to see your saved gigs, set ticket sale dates, and configure reminders
4. Run the app with `npm run start:cron` to activate hourly Slack DM alerts

---

## Setup

### 1. Prerequisites

- Node.js 20+
- A Google account
- A Slack workspace where you can install apps

### 2. Google Calendar API

1. Go to [Google Cloud Console](https://console.cloud.google.com) and create a new project
2. Enable the **Google Calendar API** (APIs & Services → Library)
3. Create **OAuth 2.0 credentials** (APIs & Services → Credentials → Create Credentials → OAuth client ID)
   - Application type: **Desktop App**
4. Copy the **Client ID** and **Client Secret** — you'll add these to `.env.local` in step 4

### 3. Slack Bot (for DM alerts)

1. Go to [api.slack.com/apps](https://api.slack.com/apps) → Create New App → From scratch
2. Under **OAuth & Permissions**, add the `chat:write` scope
3. Install the app to your workspace and copy the **Bot User OAuth Token** (`xoxb-…`)
4. Find your **Slack User ID**: open your profile in Slack → ⋮ → Copy member ID

### 4. Configure environment

```bash
cp .env.local.example .env.local
```

Edit `.env.local` and fill in `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET`.

### 5. Get your Google refresh token

```bash
npm install
npm run auth
```

Follow the prompts — it will print your `GOOGLE_REFRESH_TOKEN`. Add it to `.env.local`.

### 6. Connect gig-hub

Set the `GIG_SAVER_URL` environment variable when running gig-hub:

```bash
GIG_SAVER_URL=http://localhost:3000 java -jar gig-hub.jar
```

Each Slack notification will now include a 💾 Save link per event.

---

## Running

```bash
# Development (no cron — alerts won't fire automatically)
npm run dev

# Production (includes hourly alert cron)
npm run build
npm run start:cron
```

To test alerts manually without waiting for the cron:
```
GET http://localhost:3000/api/cron/check-alerts
```

---

## Environment variables

| Variable | Required | Description |
|---|---|---|
| `GOOGLE_CLIENT_ID` | Yes | OAuth2 client ID from Google Cloud Console |
| `GOOGLE_CLIENT_SECRET` | Yes | OAuth2 client secret |
| `GOOGLE_REFRESH_TOKEN` | Yes | From `npm run auth` |
| `GOOGLE_CALENDAR_ID` | No | Calendar to use (default: `primary`) |
| `SLACK_BOT_TOKEN` | For alerts | Bot token (`xoxb-…`) |
| `SLACK_USER_ID` | For alerts | Your Slack user ID (`U01XXXXXXX`) |
