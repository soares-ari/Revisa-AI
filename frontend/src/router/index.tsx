import { createBrowserRouter } from 'react-router-dom';
import { LandingPage } from '@/features/auth/pages/LandingPage';
import { LoginPage } from '@/features/auth/pages/LoginPage';
import { RegisterPage } from '@/features/auth/pages/RegisterPage';
import { DashboardPage } from '@/features/dashboard/pages/DashboardPage';
import { IngestionPage } from '@/features/ingestion/pages/IngestionPage';
import { StudyConfigPage } from '@/features/study/pages/StudyConfigPage';
import { StudyPage } from '@/features/study/pages/StudyPage';
import { StudyResultPage } from '@/features/study/pages/StudyResultPage';
import { ProtectedRoute } from './ProtectedRoute';

export const router = createBrowserRouter([
  { path: '/', element: <LandingPage /> },
  { path: '/login', element: <LoginPage /> },
  { path: '/register', element: <RegisterPage /> },
  {
    element: <ProtectedRoute />,
    children: [
      { path: '/dashboard', element: <DashboardPage /> },
      { path: '/ingestion/new', element: <IngestionPage /> },
      { path: '/study/new', element: <StudyConfigPage /> },
      { path: '/study/:id', element: <StudyPage /> },
      { path: '/study/:id/result', element: <StudyResultPage /> },
    ],
  },
]);
