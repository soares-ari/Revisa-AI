import { http, HttpResponse } from 'msw';
import { server } from '@/mocks/server';
import { api } from '@/lib/axios';
import { useAuthStore } from '@/features/auth/store/authStore';
import { authService } from '@/features/auth/services/authService';

const BASE = 'http://localhost:8080';

beforeEach(() => {
  useAuthStore.getState().clearAuth();
});

describe('useAuth — token em memória, não em localStorage', () => {
  it('login bem-sucedido armazena token no store Zustand, não no localStorage', async () => {
    await authService.login({ email: 'u@example.com', password: 'senha123' });

    useAuthStore.getState().setAuth('mock-token', 'user-1');

    expect(useAuthStore.getState().accessToken).toBe('mock-token');
    expect(useAuthStore.getState().isAuthenticated).toBe(true);
    expect(localStorage.getItem('accessToken')).toBeNull();
    expect(localStorage.getItem('userId')).toBeNull();
  });

  it('clearAuth limpa o store sem afetar localStorage', () => {
    useAuthStore.getState().setAuth('some-token', 'user-x');
    useAuthStore.getState().clearAuth();

    expect(useAuthStore.getState().accessToken).toBeNull();
    expect(useAuthStore.getState().isAuthenticated).toBe(false);
    expect(localStorage.getItem('accessToken')).toBeNull();
  });

  it('interceptor Axios re-tenta requisição após 401 + refresh bem-sucedido', async () => {
    let callCount = 0;

    server.use(
      http.get(`${BASE}/test-protected`, () => {
        callCount++;
        if (callCount === 1) return new HttpResponse(null, { status: 401 });
        return HttpResponse.json({ ok: true });
      }),
      http.post(`${BASE}/auth/refresh`, () =>
        HttpResponse.json({ accessToken: 'refreshed-token', userId: 'user-1' })
      )
    );

    const response = await api.get('/test-protected');

    expect(response.data).toEqual({ ok: true });
    expect(useAuthStore.getState().accessToken).toBe('refreshed-token');
    expect(callCount).toBe(2);
  });
});
