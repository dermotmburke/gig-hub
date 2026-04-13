/**
 * One-time Google OAuth2 setup.
 *
 * Usage:
 *   npm run auth
 *
 * Prerequisites:
 *   1. Go to https://console.cloud.google.com
 *   2. Create a project and enable the Google Calendar API
 *   3. Create OAuth 2.0 credentials (Application type: Desktop App)
 *   4. Copy Client ID and Client Secret into .env.local as:
 *        GOOGLE_CLIENT_ID=...
 *        GOOGLE_CLIENT_SECRET=...
 *   5. Run this script, authorise in the browser, then copy the
 *      GOOGLE_REFRESH_TOKEN value into .env.local
 */

import { google } from 'googleapis';
import * as readline from 'readline';

const SCOPES = ['https://www.googleapis.com/auth/calendar'];

async function main() {
  const clientId = process.env.GOOGLE_CLIENT_ID;
  const clientSecret = process.env.GOOGLE_CLIENT_SECRET;

  if (!clientId || !clientSecret) {
    console.error(
      'Error: GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET must be set in .env.local\n' +
        'See .env.local.example for instructions.'
    );
    process.exit(1);
  }

  const oauth2Client = new google.auth.OAuth2(
    clientId,
    clientSecret,
    'urn:ietf:wg:oauth:2.0:oob'
  );

  const authUrl = oauth2Client.generateAuthUrl({
    access_type: 'offline',
    scope: SCOPES,
    prompt: 'consent',
  });

  console.log('\n━━━ Google Calendar Auth ━━━\n');
  console.log('1. Open this URL in your browser:\n');
  console.log('  ', authUrl);
  console.log('\n2. Sign in and approve access.');
  console.log('3. Copy the code shown and paste it below.\n');

  const rl = readline.createInterface({ input: process.stdin, output: process.stdout });

  rl.question('Paste the code here: ', async (code) => {
    rl.close();
    try {
      const { tokens } = await oauth2Client.getToken(code.trim());

      if (!tokens.refresh_token) {
        console.error(
          '\nNo refresh token received. This usually means the app was already authorised.\n' +
            'Go to https://myaccount.google.com/permissions, revoke access, then run this script again.'
        );
        process.exit(1);
      }

      console.log('\n✓ Authorisation successful!\n');
      console.log('Add this to your .env.local:\n');
      console.log(`GOOGLE_REFRESH_TOKEN=${tokens.refresh_token}\n`);
    } catch (err) {
      console.error('\nFailed to exchange code for token:', err);
      process.exit(1);
    }
  });
}

main().catch(console.error);
