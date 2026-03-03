import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '@/features/auth/store/authStore';

export const ProtectedRoute = () => {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  return isAuthenticated ? <Outlet /> : <Navigate to="/login" replace />;
};
