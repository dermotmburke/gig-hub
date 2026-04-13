import { NextResponse } from 'next/server';
import { getGig, updateGig, deleteGig } from '@/lib/google-calendar';

export const dynamic = 'force-dynamic';

export async function GET(
  _req: Request,
  { params }: { params: Promise<{ id: string }> }
) {
  const { id } = await params;
  const gig = await getGig(id);
  return NextResponse.json(gig);
}

export async function PUT(
  req: Request,
  { params }: { params: Promise<{ id: string }> }
) {
  const { id } = await params;
  const body = await req.json();

  const updated = await updateGig(id, {
    notes: body.notes ?? null,
    ticketUrl: body.ticketUrl ?? null,
    ticketSaleDate: body.ticketSaleDate ? new Date(body.ticketSaleDate) : null,
    reminderDaysBefore: typeof body.reminderDaysBefore === 'number' ? body.reminderDaysBefore : undefined,
  });

  return NextResponse.json(updated);
}

export async function DELETE(
  _req: Request,
  { params }: { params: Promise<{ id: string }> }
) {
  const { id } = await params;
  await deleteGig(id);
  return new NextResponse(null, { status: 204 });
}
