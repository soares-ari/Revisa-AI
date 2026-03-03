import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createElement } from 'react';
import { http, HttpResponse } from 'msw';
import { server } from '@/mocks/server';
import { useHistory } from '../useHistory';

const BASE = 'http://localhost:8080';

const wrapper = ({ children }: { children: React.ReactNode }) =>
  createElement(
    QueryClientProvider,
    { client: new QueryClient({ defaultOptions: { queries: { retry: false } } }) },
    children
  );

describe('useHistory', () => {
  it('retorna lista de sessões do endpoint /users/me/history', async () => {
    const { result } = renderHook(() => useHistory(), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data).toHaveLength(1);
    expect(result.current.data?.[0].id).toBe('session-1');
    expect(result.current.data?.[0].banca).toBe('CEBRASPE');
  });

  it('retorna lista vazia quando não há sessões', async () => {
    server.use(
      http.get(`${BASE}/users/me/history`, () => HttpResponse.json([]))
    );

    const { result } = renderHook(() => useHistory(), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data).toEqual([]);
  });
});
