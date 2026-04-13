import type { Gig } from '@/lib/google-calendar';
import GigCard from './GigCard';

export default function GigList({ gigs }: { gigs: Gig[] }) {
  if (gigs.length === 0) {
    return (
      <div className="text-center py-20 text-gray-500">
        <p className="text-5xl mb-4">🎸</p>
        <p className="text-lg">No gigs saved yet.</p>
        <p className="text-sm mt-2">Click a 💾 Save link in Slack to add one.</p>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {gigs.map((gig) => (
        <GigCard key={gig.id} gig={gig} />
      ))}
    </div>
  );
}
