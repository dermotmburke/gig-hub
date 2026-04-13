import { listGigs } from '@/lib/google-calendar';
import GigList from '@/components/GigList';

export const dynamic = 'force-dynamic';

export default async function GigsPage({
  searchParams,
}: {
  searchParams: Promise<{ past?: string }>;
}) {
  const params = await searchParams;
  const showPast = params.past === 'true';
  const gigs = await listGigs(!showPast);

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold">All Gigs</h1>
        <a
          href={showPast ? '/gigs' : '/gigs?past=true'}
          className="text-sm text-indigo-400 hover:underline"
        >
          {showPast ? 'Hide past gigs' : 'Show past gigs'}
        </a>
      </div>
      <GigList gigs={gigs} />
    </div>
  );
}
