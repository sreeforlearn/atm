import { Link } from 'react-router-dom';
import { Home, Compass } from 'lucide-react';
import Button from '@/components/ui/Button';

/**
 * 404 page — friendly CTA back to the dashboard.
 */
export default function NotFound() {
  return (
    <div className="flex min-h-screen items-center justify-center p-6 text-white">
      <div className="text-center">
        <p className="text-[120px] font-extrabold leading-none text-accent-400">404</p>
        <h1 className="mt-2 text-2xl font-bold">Lost in the network</h1>
        <p className="mt-2 text-sm text-white/70 max-w-sm">
          The page you're looking for has been moved, deleted, or never existed.
          Let's get you back to safe territory.
        </p>
        <div className="mt-6 flex items-center justify-center gap-3">
          <Link to="/dashboard">
            <Button leftIcon={<Home className="h-4 w-4" />}>Go to dashboard</Button>
          </Link>
          <Link to="/login">
            <Button variant="ghost" leftIcon={<Compass className="h-4 w-4" />}>
              Sign in again
            </Button>
          </Link>
        </div>
      </div>
    </div>
  );
}
