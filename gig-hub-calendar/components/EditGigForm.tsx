'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import type { Gig } from '@/lib/google-calendar';

export default function EditGigForm({ gig }: { gig: Gig }) {
  const router = useRouter();
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [ticketUrl, setTicketUrl] = useState(gig.ticketUrl ?? '');
  const [ticketSaleDate, setTicketSaleDate] = useState(
    gig.ticketSaleDate ? gig.ticketSaleDate.toISOString().slice(0, 16) : ''
  );
  const [reminderDaysBefore, setReminderDaysBefore] = useState(gig.reminderDaysBefore);
  const [notes, setNotes] = useState(gig.notes ?? '');

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError(null);

    try {
      const res = await fetch(`/api/gigs/${gig.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          ticketUrl: ticketUrl || null,
          ticketSaleDate: ticketSaleDate || null,
          reminderDaysBefore,
          notes: notes || null,
        }),
      });

      if (!res.ok) throw new Error('Save failed');

      router.push(`/gigs/${gig.id}`);
      router.refresh();
    } catch {
      setError('Failed to save. Please try again.');
      setSaving(false);
    }
  }

  async function handleDelete() {
    if (!confirm(`Remove "${gig.artist}" from your saved gigs?`)) return;

    const res = await fetch(`/api/gigs/${gig.id}`, { method: 'DELETE' });
    if (res.ok) {
      router.push('/gigs');
      router.refresh();
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div>
        <label className="block text-sm font-medium text-gray-300 mb-1.5">Ticket URL</label>
        <input
          type="url"
          value={ticketUrl}
          onChange={(e) => setTicketUrl(e.target.value)}
          placeholder="https://ticketmaster.com/..."
          className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-sm text-white placeholder-gray-600 focus:outline-none focus:ring-2 focus:ring-indigo-500"
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-300 mb-1.5">
          Ticket sale date{' '}
          <span className="text-gray-500 font-normal">(leave blank if unknown)</span>
        </label>
        <input
          type="datetime-local"
          value={ticketSaleDate}
          onChange={(e) => setTicketSaleDate(e.target.value)}
          className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
        />
        <p className="text-xs text-gray-500 mt-1">
          You&apos;ll get a Slack DM when this date arrives.
        </p>
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-300 mb-1.5">
          Pre-event reminder
        </label>
        <div className="flex items-center gap-3">
          <input
            type="number"
            min={1}
            max={30}
            value={reminderDaysBefore}
            onChange={(e) => setReminderDaysBefore(parseInt(e.target.value, 10))}
            className="w-20 bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-sm text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
          <span className="text-gray-400 text-sm">days before the gig</span>
        </div>
        <p className="text-xs text-gray-500 mt-1">
          You&apos;ll get a Slack DM this many days before.
        </p>
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-300 mb-1.5">Notes</label>
        <textarea
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          placeholder="Anything to remember..."
          rows={3}
          className="w-full bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-sm text-white placeholder-gray-600 focus:outline-none focus:ring-2 focus:ring-indigo-500 resize-none"
        />
      </div>

      {error && <p className="text-red-400 text-sm">{error}</p>}

      <div className="flex gap-3">
        <button
          type="submit"
          disabled={saving}
          className="flex-1 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 px-5 py-2.5 rounded-lg text-sm font-medium transition-colors"
        >
          {saving ? 'Saving…' : 'Save changes'}
        </button>
        <button
          type="button"
          onClick={handleDelete}
          className="px-4 py-2.5 rounded-lg text-sm font-medium text-red-400 hover:bg-red-900/30 transition-colors"
        >
          Remove
        </button>
      </div>
    </form>
  );
}
