import HowToPlayPage from './pages/HowToPlayPage';
import PrivacyPage from './pages/PrivacyPage';
import TermsPage from './pages/TermsPage';

export const PUBLIC_PAGES = [
  { path: '/how-to-play', element: <HowToPlayPage /> },
  { path: '/privacy', element: <PrivacyPage /> },
  { path: '/terms', element: <TermsPage /> },
];
