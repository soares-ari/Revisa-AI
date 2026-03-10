import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { http, HttpResponse } from 'msw';
import { server } from '@/mocks/server';
import { IngestionForm } from '../IngestionForm';

const BASE = 'http://localhost:8080';

const makePdf = (name: string) =>
  new File(['%PDF-1.4'], name, { type: 'application/pdf' });

const wrapper = ({ children }: { children: React.ReactNode }) => (
  <QueryClientProvider
    client={new QueryClient({ defaultOptions: { mutations: { retry: false } } })}
  >
    <MemoryRouter>{children}</MemoryRouter>
  </QueryClientProvider>
);

describe('IngestionForm', () => {
  it('submissão válida chama POST /ingestion/jobs', async () => {
    let called = false;
    server.use(
      http.post(`${BASE}/ingestion/jobs`, () => {
        called = true;
        return HttpResponse.json(
          {
            id: 'job-1',
            status: 'COMPLETED',
            banca: 'CEBRASPE',
            ano: 2024,
            orgao: 'PF',
            cargo: 'Analista',
            questoesSalvas: 10,
            questoesInvalidas: 2,
            errorMessage: null,
          },
          { status: 201 }
        );
      })
    );

    const onJobCreated = vi.fn();
    const user = userEvent.setup({ applyAccept: false });
    render(<IngestionForm onJobCreated={onJobCreated} />, { wrapper });

    await user.selectOptions(screen.getByLabelText(/banca/i), 'CEBRASPE');
    await user.type(screen.getByLabelText(/ano/i), '2024');
    await user.type(screen.getByLabelText(/órgão/i), 'PF');
    await user.type(screen.getByLabelText(/cargo/i), 'Analista');

    const provaInput = screen.getByLabelText(/arquivo da prova/i);
    const gabaritoInput = screen.getByLabelText(/arquivo do gabarito/i);
    await user.upload(provaInput, makePdf('prova.pdf'));
    await user.upload(gabaritoInput, makePdf('gabarito.pdf'));

    await user.click(screen.getByRole('button', { name: /iniciar ingestão/i }));

    await waitFor(() => {
      expect(called).toBe(true);
      expect(onJobCreated).toHaveBeenCalledWith('job-1');
    });
  });

  it('exibe erros quando campos obrigatórios estão vazios', async () => {
    const user = userEvent.setup();
    render(<IngestionForm onJobCreated={vi.fn()} />, { wrapper });

    await user.click(screen.getByRole('button', { name: /iniciar ingestão/i }));

    await waitFor(() => {
      expect(screen.getByText(/selecione uma banca/i)).toBeInTheDocument();
      expect(screen.getByText(/ano inválido/i)).toBeInTheDocument();
      expect(screen.getAllByText(/mínimo 2 caracteres/i)).toHaveLength(2);
    });
  });

  it('arquivo não-PDF exibe erro de validação', async () => {
    const user = userEvent.setup({ applyAccept: false });
    render(<IngestionForm onJobCreated={vi.fn()} />, { wrapper });

    const txtFile = new File(['conteúdo'], 'documento.txt', {
      type: 'text/plain',
    });
    const provaInput = screen.getByLabelText(/arquivo da prova/i);
    await user.upload(provaInput, txtFile);

    await user.click(screen.getByRole('button', { name: /iniciar ingestão/i }));

    await waitFor(() => {
      expect(
        screen.getAllByText(/apenas arquivos pdf são aceitos/i).length
      ).toBeGreaterThan(0);
    });
  });
});
