import { getGig } from '@/lib/google-calendar';
import Link from 'next/link';

export const dynamic = 'force-dynamic';

export default async function GigDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const gig = await getGig(id);

  const formattedDate = gig.eventDate.toLocaleDateString('en-GB', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  });
  const formattedTime = gig.eventDate.toLocaleTimeString('en-GB', {
    hour: '2-digit',
    minute: '2-digit',
  });

  const now = new Date();
  const daysUntil = Math.ceil(
    (gig.eventDate.getTime() - now.getTime()) / (1000 * 60 * 60 * 24)
  );
  const isPast = daysUntil < 0;

  return (
    <div className="max-w-2xl">
      <Link href="/gigs" className="text-gray-400 hover:text-white text-sm mb-6 block">
        ← All gigs
      </Link>

      <div className="bg-gray-900 rounded-xl p-6 space-y-4">
        <div className="flex items-start justify-between gap-4">
          <h1 className="text-3xl font-bold">{gig.artist}</h1>
          {!isPast && (
            <span
              className={`text-xs font-medium px-2 py-1 rounded-full shrink-0 ${
                daysUntil <= 3
                  ? 'bg-red-900 text-red-300'
                  : daysUntil <= 14
                  ? 'bg-yellow-900 text-yellow-300'
                  : 'bg-gray-800 text-gray-400'
              }`}
            >
              {daysUntil === 0 ? 'Today' : daysUntil === 1 ? 'Tomorrow' : `${daysUntil} days`}
            </span>
          )}
        </div>

        <div className="text-gray-300 space-y-1">
          <p>{gig.location}</p>
          <p>
            {formattedDate} at {formattedTime}
          </p>
        </div>

        {gig.ticketUrl && (
          <a
            href={gig.ticketUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-block bg-indigo-600 hover:bg-indigo-500 px-4 py-2 rounded-lg text-sm font-medium"
          >
            Buy tickets ↗
          </a>
        )}

        <div className="border-t border-gray-800 pt-4 space-y-2">
          <h2 className="text-xs font-semibold text-gray-400 uppercase tracking-wide">Alerts</h2>
          <div className="flex flex-col gap-1 text-sm">
            <span
              className={`inline-flex items-center gap-1.5 ${
                gig.ticketSaleDate ? 'text-gray-300' : 'text-gray-500'
              }`}
            >
              {gig.ticketSaleDate ? (
                <>
                  🎫 Ticket sale:{' '}
                  {gig.ticketSaleDate.toLocaleDateString('en-GB', {
                    day: 'numeric',
                    month: 'long',
                    year: 'numeric',
                  })}
                  {gig.ticketSaleAlertSent && (
                    <span className="text-green-400 text-xs">✓ alert sent</span>
                  )}
                </>
              ) : (
                '🎫 No ticket sale date set'
              )}
            </span>
            <span className="text-gray-300">
              🔔 Reminder: {gig.reminderDaysBefore} day
              {gig.reminderDaysBefore === 1 ? '' : 's'} before
              {gig.preEventAlertSent && (
                <span className="text-green-400 text-xs ml-2">✓ sent</span>
              )}
            </span>
          </div>
        </div>

        {gig.notes && (
          <div className="border-t border-gray-800 pt-4">
            <h2 className="text-xs font-semibold text-gray-400 uppercase tracking-wide mb-2">
              Notes
            </h2>
            <p className="text-gray-300 text-sm whitespace-pre-wrap">{gig.notes}</p>
          </div>
        )}
      </div>

      <div className="mt-4">
        <Link
          href={`/gigs/${id}/edit`}
          className="bg-gray-800 hover:bg-gray-700 px-4 py-2 rounded-lg text-sm font-medium inline-block"
        >
          Edit / Set alerts
        </Link>
      </div>
    </div>
  );
}
