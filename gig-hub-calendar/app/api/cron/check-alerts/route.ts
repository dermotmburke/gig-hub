import { NextResponse } from 'next/server';
import { checkAndSendAlerts } from '@/lib/alerts';

export const dynamic = 'force-dynamic';

export async function GET() {
  await checkAndSendAlerts();
  return NextResponse.json({ ok: true });
}
