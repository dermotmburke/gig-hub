import { createServer } from 'http';
import { parse } from 'url';
import next from 'next';
import cron from 'node-cron';
import { checkAndSendAlerts } from './lib/alerts';

const dev = process.env.NODE_ENV !== 'production';
const app = next({ dev });
const handle = app.getRequestHandler();

app.prepare().then(() => {
  // Check for due alerts every hour
  cron.schedule('0 * * * *', async () => {
    console.log('[cron] Running alert check…');
    try {
      await checkAndSendAlerts();
    } catch (err) {
      console.error('[cron] Alert check failed:', err);
    }
  });

  createServer((req, res) => {
    const parsedUrl = parse(req.url!, true);
    handle(req, res, parsedUrl);
  }).listen(3000, () => {
    console.log('> Ready on http://localhost:3000');
    console.log('> Alert cron active (runs hourly)');
  });
});
