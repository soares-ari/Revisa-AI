import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createElement } from 'react';
import { http, HttpResponse } from 'msw';
import { server } from '@/mocks/server';
import { useJobStatus } from '../useJobStatus';

const BASE = 'http://localhost:8080';

const wrapper = ({ children }: { children: React.ReactNode }) =>
  createElement(
    QueryClientProvider,
    {
      client: new QueryClient({
        defaultOptions: { queries: { retry: false } },
      }),
    },
    children
  );

describe('useJobStatus', () => {
  it('retorna dados do job quando jobId fornecido', async () => {
    const { result } = renderHook(() => useJobStatus('job-1'), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data?.status).toBe('COMPLETED');
    expect(result.current.data?.questoesSalvas).toBe(10);
  });

  it('não faz fetch quando jobId é null', () => {
    const { result } = renderHook(() => useJobStatus(null), { wrapper });
    expect(result.current.fetchStatus).toBe('idle');
  });

  it('para polling quando status é FAILED', async () => {
    server.use(
      http.get(`${BASE}/ingestion/jobs/:id`, () =>
        HttpResponse.json({
          id: 'job-1',
          status: 'FAILED',
          banca: 'CEBRASPE',
          ano: 2024,
          orgao: 'PF',
          cargo: 'Analista',
          questoesSalvas: 0,
          questoesInvalidas: 0,
          errorMessage: 'Falha na extração',
        })
      )
    );

    const { result } = renderHook(() => useJobStatus('job-1'), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));

    expect(result.current.data?.status).toBe('FAILED');
    expect(result.current.data?.errorMessage).toBe('Falha na extração');
  });
});
