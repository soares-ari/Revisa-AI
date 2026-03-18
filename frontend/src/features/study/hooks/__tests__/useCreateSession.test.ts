import { renderHook, waitFor, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createElement } from 'react';
import { useCreateSession } from '../useCreateSession';

const wrapper = ({ children }: { children: React.ReactNode }) =>
  createElement(
    QueryClientProvider,
    { client: new QueryClient({ defaultOptions: { mutations: { retry: false } } }) },
    children
  );

describe('useCreateSession', () => {
  it('chama POST /study/sessions e retorna a sessão criada', async () => {
    const { result } = renderHook(() => useCreateSession(), { wrapper });

    act(() => {
      result.current.mutate({ quantidade: 20, modo: 'ESTUDO' });
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data?.id).toBe('session-1');
    expect(result.current.data?.status).toBe('EM_ANDAMENTO');
    expect(result.current.data?.modo).toBe('ESTUDO');
  });
});
