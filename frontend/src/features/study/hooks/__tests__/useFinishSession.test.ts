import { renderHook, waitFor, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createElement } from 'react';
import { useFinishSession } from '../useFinishSession';

const wrapper = ({ children }: { children: React.ReactNode }) =>
  createElement(
    QueryClientProvider,
    { client: new QueryClient({ defaultOptions: { mutations: { retry: false } } }) },
    children
  );

describe('useFinishSession', () => {
  it('retorna sessão FINALIZADA com resultado preenchido', async () => {
    const { result } = renderHook(() => useFinishSession('session-1'), { wrapper });

    act(() => {
      result.current.mutate();
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data?.status).toBe('FINALIZADA');
    expect(result.current.data?.resultado?.total).toBe(2);
    expect(result.current.data?.resultado?.acertos).toBe(1);
    expect(result.current.data?.resultado?.percentual).toBe(50.0);
  });
});
