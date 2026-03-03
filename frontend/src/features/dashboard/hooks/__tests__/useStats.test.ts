import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createElement } from 'react';
import { useStats } from '../useStats';

const wrapper = ({ children }: { children: React.ReactNode }) =>
  createElement(
    QueryClientProvider,
    { client: new QueryClient({ defaultOptions: { queries: { retry: false } } }) },
    children
  );

describe('useStats', () => {
  it('retorna dados do endpoint /users/me/stats', async () => {
    const { result } = renderHook(() => useStats(), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data?.totalQuestoes).toBe(150);
    expect(result.current.data?.percentualAcertos).toBe(72.5);
    expect(result.current.data?.totalSessoes).toBe(8);
  });

  it('começa com data undefined enquanto carrega', () => {
    const { result } = renderHook(() => useStats(), { wrapper });
    expect(result.current.data).toBeUndefined();
  });
});
