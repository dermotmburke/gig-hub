export async function sendSlackDM(message: string): Promise<void> {
  const token = process.env.SLACK_BOT_TOKEN;
  const userId = process.env.SLACK_USER_ID;

  if (!token || !userId) {
    console.warn('[slack] SLACK_BOT_TOKEN or SLACK_USER_ID not set — skipping DM');
    return;
  }

  const response = await fetch('https://slack.com/api/chat.postMessage', {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ channel: userId, text: message }),
  });

  const data = (await response.json()) as { ok: boolean; error?: string };
  if (!data.ok) {
    throw new Error(`Slack API error: ${data.error}`);
  }
}
