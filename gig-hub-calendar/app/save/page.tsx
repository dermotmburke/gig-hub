import { createGig, listGigs } from '@/lib/google-calendar';
import Link from 'next/link';

export const dynamic = 'force-dynamic';

export default async function SavePage({
  searchParams,
}: {
  searchParams: Promise<{ artist?: string; location?: string; date?: string; url?: string }>;
}) {
  const params = await searchParams;
  const { artist, location, date, url } = params;

  if (!artist || !location || !date) {
    return (
      <div className="text-center py-16">
        <p className="text-red-400 text-lg">Missing required event data.</p>
        <Link href="/" className="text-indigo-400 hover:underline mt-4 block">
          ← Back to gigs
        </Link>
      </div>
    );
  }

  const eventDate = new Date(date);
  if (isNaN(eventDate.getTime())) {
    return (
      <div className="text-center py-16">
        <p className="text-red-400 text-lg">Invalid event date.</p>
        <Link href="/" className="text-indigo-400 hover:underline mt-4 block">
          ← Back to gigs
        </Link>
      </div>
    );
  }

  // Deduplicate: check if already saved (same artist + date within 1 min)
  const existing = await listGigs();
  const duplicate = existing.find(
    (g) =>
      g.artist === artist && Math.abs(g.eventDate.getTime() - eventDate.getTime()) < 60_000
  );

  if (duplicate) {
    return (
      <div className="max-w-md mx-auto text-center py-16">
        <p className="text-yellow-400 text-xl font-medium mb-2">Already saved!</p>
        <p className="text-gray-300">{artist}</p>
        <p className="text-gray-400">{location}</p>
        <Link
          href={`/gigs/${duplicate.id}`}
          className="text-indigo-400 hover:underline mt-6 block"
        >
          View gig →
        </Link>
      </div>
    );
  }

  const gig = await createGig({ artist, location, eventDate, ticketUrl: url });

  const formattedDate = gig.eventDate.toLocaleDateString('en-GB', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  });

  return (
    <div className="max-w-md mx-auto text-center py-16">
      <div className="text-5xl mb-4">✓</div>
      <h1 className="text-2xl font-bold mb-2">Gig saved!</h1>
      <p className="text-gray-200 text-lg font-medium">{gig.artist}</p>
      <p className="text-gray-400">{gig.location}</p>
      <p className="text-gray-400">{formattedDate}</p>

      <div className="mt-8 flex gap-3 justify-center">
        <Link
          href={`/gigs/${gig.id}/edit`}
          className="bg-indigo-600 hover:bg-indigo-500 px-5 py-2 rounded-lg text-sm font-medium"
        >
          Set alerts →
        </Link>
        <Link
          href="/"
          className="bg-gray-800 hover:bg-gray-700 px-5 py-2 rounded-lg text-sm font-medium text-gray-300"
        >
          View all gigs
        </Link>
      </div>
    </div>
  );
}
