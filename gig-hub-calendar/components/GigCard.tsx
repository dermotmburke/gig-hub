import Link from 'next/link';
import type { Gig } from '@/lib/google-calendar';

function getDaysUntil(date: Date): number {
  const now = new Date();
  return Math.ceil((date.getTime() - now.getTime()) / (1000 * 60 * 60 * 24));
}

export default function GigCard({ gig }: { gig: Gig }) {
  const daysUntil = getDaysUntil(gig.eventDate);
  const isPast = daysUntil < 0;

  const formattedDate = gig.eventDate.toLocaleDateString('en-GB', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
  });
  const formattedTime = gig.eventDate.toLocaleTimeString('en-GB', {
    hour: '2-digit',
    minute: '2-digit',
  });

  return (
    <div
      className={`bg-gray-900 rounded-xl p-5 flex flex-col gap-3 ${
        isPast ? 'opacity-50' : ''
      }`}
    >
      <div className="flex items-start justify-between gap-4">
        <div className="min-w-0">
          <h2 className="text-lg font-semibold text-white truncate">{gig.artist}</h2>
          <p className="text-gray-400 text-sm">{gig.location}</p>
          <p className="text-gray-500 text-sm">
            {formattedDate} · {formattedTime}
          </p>
        </div>
        {!isPast && (
          <span
            className={`text-xs font-medium px-2.5 py-1 rounded-full shrink-0 ${
              daysUntil === 0
                ? 'bg-red-900 text-red-300'
                : daysUntil <= 3
                ? 'bg-orange-900 text-orange-300'
                : daysUntil <= 14
                ? 'bg-yellow-900 text-yellow-300'
                : 'bg-gray-800 text-gray-400'
            }`}
          >
            {daysUntil === 0 ? 'Today' : daysUntil === 1 ? 'Tomorrow' : `${daysUntil}d`}
          </span>
        )}
      </div>

      <div className="flex items-center gap-4 text-xs">
        {gig.ticketUrl && (
          <a
            href={gig.ticketUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="text-indigo-400 hover:underline"
          >
            Tickets ↗
          </a>
        )}
        <Link href={`/gigs/${gig.id}`} className="text-gray-500 hover:text-white">
          Details
        </Link>
        <Link href={`/gigs/${gig.id}/edit`} className="text-gray-500 hover:text-white">
          Edit / Alerts
        </Link>
      </div>

      {!isPast && (
        <div className="flex flex-wrap gap-2">
          {gig.ticketSaleDate && (
            <span
              className={`text-xs px-2 py-0.5 rounded-full ${
                gig.ticketSaleAlertSent
                  ? 'bg-green-900/40 text-green-400'
                  : 'bg-gray-800 text-gray-500'
              }`}
            >
              {gig.ticketSaleAlertSent ? '✓ Ticket alert sent' : '⏳ Ticket sale alert set'}
            </span>
          )}
          <span
            className={`text-xs px-2 py-0.5 rounded-full ${
              gig.preEventAlertSent
                ? 'bg-green-900/40 text-green-400'
                : 'bg-gray-800 text-gray-500'
            }`}
          >
            {gig.preEventAlertSent
              ? '✓ Event reminder sent'
              : `⏳ Reminder ${gig.reminderDaysBefore}d before`}
          </span>
        </div>
      )}
    </div>
  );
}
