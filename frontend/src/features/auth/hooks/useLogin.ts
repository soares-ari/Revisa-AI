import { useMutation } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { authService } from '../services/authService';
import { useAuthStore } from '../store/authStore';

export const useLogin = () => {
  const setAuth = useAuthStore((s) => s.setAuth);
  const navigate = useNavigate();
  return useMutation({
    mutationFn: authService.login,
    onSuccess: ({ accessToken, userId }) => {
      setAuth(accessToken, userId);
      void navigate('/dashboard');
    },
  });
};
