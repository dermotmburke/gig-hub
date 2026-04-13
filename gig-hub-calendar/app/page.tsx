import { listGigs } from '@/lib/google-calendar';
import GigList from '@/components/GigList';

export const dynamic = 'force-dynamic';

export default async function HomePage() {
  const gigs = await listGigs(true);

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">Upcoming Gigs</h1>
      <GigList gigs={gigs} />
    </div>
  );
}
