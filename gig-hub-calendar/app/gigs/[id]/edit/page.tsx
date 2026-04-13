import { getGig } from '@/lib/google-calendar';
import EditGigForm from '@/components/EditGigForm';
import Link from 'next/link';

export const dynamic = 'force-dynamic';

export default async function EditGigPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const gig = await getGig(id);

  return (
    <div className="max-w-lg">
      <Link href={`/gigs/${id}`} className="text-gray-400 hover:text-white text-sm mb-6 block">
        ← Back
      </Link>
      <h1 className="text-2xl font-bold mb-1">{gig.artist}</h1>
      <p className="text-gray-400 mb-8">{gig.location}</p>
      <EditGigForm gig={gig} />
    </div>
  );
}
