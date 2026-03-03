import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { http, HttpResponse } from 'msw';
import { server } from '@/mocks/server';
import { LoginForm } from '../LoginForm';

const BASE = 'http://localhost:8080';

const wrapper = ({ children }: { children: React.ReactNode }) => (
  <QueryClientProvider client={new QueryClient({ defaultOptions: { mutations: { retry: false } } })}>
    <MemoryRouter>
      {children}
    </MemoryRouter>
  </QueryClientProvider>
);

describe('LoginForm', () => {
  it('submissão válida chama mutation de login e redireciona', async () => {
    const user = userEvent.setup();
    render(<LoginForm />, { wrapper });

    await user.type(screen.getByLabelText(/e-mail/i), 'joao@example.com');
    await user.type(screen.getByLabelText(/senha/i), 'senha123');
    await user.click(screen.getByRole('button', { name: /entrar/i }));

    await waitFor(() => {
      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });
  });

  it('exibe erros de validação Zod com campos inválidos', async () => {
    const user = userEvent.setup();
    render(<LoginForm />, { wrapper });

    await user.type(screen.getByLabelText(/e-mail/i), 'email-invalido');
    await user.type(screen.getByLabelText(/senha/i), '123');
    await user.click(screen.getByRole('button', { name: /entrar/i }));

    await waitFor(() => {
      expect(screen.getByText(/e-mail inválido/i)).toBeInTheDocument();
      expect(screen.getByText(/ao menos 6/i)).toBeInTheDocument();
    });
  });

  it('erro 401 do backend exibe mensagem de credenciais inválidas', async () => {
    server.use(
      http.post(`${BASE}/auth/login`, () =>
        new HttpResponse(null, { status: 401 })
      )
    );

    const user = userEvent.setup();
    render(<LoginForm />, { wrapper });

    await user.type(screen.getByLabelText(/e-mail/i), 'joao@example.com');
    await user.type(screen.getByLabelText(/senha/i), 'senhaerrada');
    await user.click(screen.getByRole('button', { name: /entrar/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
    });
  });
});
