import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { http, HttpResponse } from 'msw';
import { server } from '@/mocks/server';
import { JobStatus } from '../JobStatus';

const BASE = 'http://localhost:8080';

const wrapper = ({ children }: { children: React.ReactNode }) => (
  <QueryClientProvider
    client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}
  >
    {children}
  </QueryClientProvider>
);

describe('JobStatus', () => {
  it('renderiza estado de loading enquanto carrega', () => {
    render(<JobStatus jobId="job-1" onReset={vi.fn()} />, { wrapper });
    expect(screen.getByText(/carregando/i)).toBeInTheDocument();
  });

  it('renderiza COMPLETED com questoesSalvas e questoesInvalidas', async () => {
    render(<JobStatus jobId="job-1" onReset={vi.fn()} />, { wrapper });

    await waitFor(() => {
      expect(screen.getByTestId('questoes-salvas')).toHaveTextContent('10');
      expect(screen.getByTestId('questoes-invalidas')).toHaveTextContent('2');
    });
  });

  it('renderiza FAILED com errorMessage', async () => {
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
          errorMessage: 'PDF corrompido',
        })
      )
    );

    render(<JobStatus jobId="job-1" onReset={vi.fn()} />, { wrapper });

    await waitFor(() => {
      expect(screen.getByText(/PDF corrompido/i)).toBeInTheDocument();
    });
  });

  it('exibe botão Nova ingestão quando COMPLETED', async () => {
    const onReset = vi.fn();
    const user = userEvent.setup();
    render(<JobStatus jobId="job-1" onReset={onReset} />, { wrapper });

    await waitFor(() => {
      expect(
        screen.getByRole('button', { name: /nova ingestão/i })
      ).toBeInTheDocument();
    });

    await user.click(screen.getByRole('button', { name: /nova ingestão/i }));
    expect(onReset).toHaveBeenCalledOnce();
  });
});
