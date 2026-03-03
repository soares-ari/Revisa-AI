import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { http, HttpResponse } from 'msw';
import { server } from '@/mocks/server';
import { RegisterForm } from '../RegisterForm';

const BASE = 'http://localhost:8080';

const wrapper = ({ children }: { children: React.ReactNode }) => (
  <QueryClientProvider client={new QueryClient({ defaultOptions: { mutations: { retry: false } } })}>
    <MemoryRouter>
      {children}
    </MemoryRouter>
  </QueryClientProvider>
);

describe('RegisterForm', () => {
  it('submissão válida chama mutation de register', async () => {
    const user = userEvent.setup();
    render(<RegisterForm />, { wrapper });

    await user.type(screen.getByLabelText(/nome/i), 'João Silva');
    await user.type(screen.getByLabelText(/e-mail/i), 'joao@example.com');
    await user.type(screen.getByLabelText(/senha/i), 'Senha123');
    await user.click(screen.getByRole('button', { name: /cadastrar/i }));

    await waitFor(() => {
      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });
  });

  it('senha fraca exibe erros de validação Zod', async () => {
    const user = userEvent.setup();
    render(<RegisterForm />, { wrapper });

    await user.type(screen.getByLabelText(/nome/i), 'Jo');
    await user.type(screen.getByLabelText(/e-mail/i), 'joao@example.com');
    await user.type(screen.getByLabelText(/senha/i), 'semmaius1');
    await user.click(screen.getByRole('button', { name: /cadastrar/i }));

    await waitFor(() => {
      expect(screen.getByText(/ao menos uma letra maiúscula/i)).toBeInTheDocument();
    });
  });

  it('erro 409 (e-mail duplicado) exibe mensagem de erro', async () => {
    server.use(
      http.post(`${BASE}/auth/register`, () =>
        new HttpResponse(null, { status: 409 })
      )
    );

    const user = userEvent.setup();
    render(<RegisterForm />, { wrapper });

    await user.type(screen.getByLabelText(/nome/i), 'João Silva');
    await user.type(screen.getByLabelText(/e-mail/i), 'duplicado@example.com');
    await user.type(screen.getByLabelText(/senha/i), 'Senha123');
    await user.click(screen.getByRole('button', { name: /cadastrar/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
    });
  });
});
