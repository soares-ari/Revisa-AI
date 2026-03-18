import { renderHook, waitFor, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createElement } from 'react';
import { http, HttpResponse } from 'msw';
import { server } from '@/mocks/server';
import { useAnswerQuestion } from '../useAnswerQuestion';

const BASE = 'http://localhost:8080';

const wrapper = ({ children }: { children: React.ReactNode }) =>
  createElement(
    QueryClientProvider,
    { client: new QueryClient({ defaultOptions: { mutations: { retry: false } } }) },
    children
  );

describe('useAnswerQuestion', () => {
  it('modo ESTUDO — retorna gabarito não-null', async () => {
    const { result } = renderHook(() => useAnswerQuestion('session-1'), { wrapper });

    act(() => {
      result.current.mutate({ questionId: 'q-1', resposta: 'CERTO' });
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data?.gabarito).toBe('CERTO');
    expect(result.current.data?.correta).toBe(true);
  });

  it('modo SIMULADO — gabarito retornado pelo backend é null', async () => {
    server.use(
      http.post(`${BASE}/study/sessions/:id/answer`, () =>
        HttpResponse.json({
          questionId: 'q-1',
          respostaUsuario: 'CERTO',
          correta: true,
          gabarito: null,
          area: 'Informática',
        })
      )
    );

    const { result } = renderHook(() => useAnswerQuestion('session-1'), { wrapper });

    act(() => {
      result.current.mutate({ questionId: 'q-1', resposta: 'CERTO' });
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data?.gabarito).toBeNull();
  });
});
