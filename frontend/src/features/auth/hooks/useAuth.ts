import { useAuthStore } from '../store/authStore';

export const useAuth = () => {
  const { isAuthenticated, userId, accessToken, clearAuth } = useAuthStore();
  return { isAuthenticated, userId, accessToken, clearAuth };
};
