import { NextResponse } from 'next/server';
import { listGigs } from '@/lib/google-calendar';

export const dynamic = 'force-dynamic';

export async function GET(request: Request) {
  const { searchParams } = new URL(request.url);
  const upcoming = searchParams.get('upcoming') === 'true';
  const gigs = await listGigs(upcoming);
  return NextResponse.json(gigs);
}
