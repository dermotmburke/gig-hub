import Link from 'next/link';

export default function Nav() {
  return (
    <nav className="border-b border-gray-800 bg-gray-950 sticky top-0 z-10">
      <div className="max-w-4xl mx-auto px-4 py-4 flex items-center gap-6">
        <Link href="/" className="text-lg font-bold text-white">
          🎸 gig-hub-calendar
        </Link>
        <Link href="/" className="text-gray-400 hover:text-white text-sm transition-colors">
          Upcoming
        </Link>
        <Link href="/gigs" className="text-gray-400 hover:text-white text-sm transition-colors">
          All Gigs
        </Link>
      </div>
    </nav>
  );
}
