import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { http, HttpResponse } from 'msw';
import { server } from '@/mocks/server';
import { StudyConfigPage } from '../StudyConfigPage';

const BASE = 'http://localhost:8080';

const Wrapper = ({ children }: { children: React.ReactNode }) => (
  <QueryClientProvider
    client={
      new QueryClient({
        defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
      })
    }
  >
    <MemoryRouter initialEntries={['/study/new']}>
      <Routes>
        <Route path="/study/new" element={<>{children}</>} />
        <Route
          path="/study/:id"
          element={<div data-testid="study-page">StudyPage</div>}
        />
      </Routes>
    </MemoryRouter>
  </QueryClientProvider>
);

describe('StudyConfigPage', () => {
  it('submissão válida chama POST /study/sessions', async () => {
    let called = false;
    server.use(
      http.post(`${BASE}/study/sessions`, () => {
        called = true;
        return HttpResponse.json(
          {
            id: 'session-1',
            userId: 'user-1',
            banca: null,
            areas: [],
            quantidade: 20,
            modo: 'ESTUDO',
            status: 'EM_ANDAMENTO',
            questionIds: ['q-1', 'q-2'],
            currentIndex: 0,
            answers: [],
            resultado: null,
            createdAt: '2026-01-01T10:00:00Z',
            updatedAt: '2026-01-01T10:00:00Z',
          },
          { status: 201 }
        );
      })
    );

    const user = userEvent.setup();
    render(<StudyConfigPage />, { wrapper: Wrapper });

    await user.click(screen.getByRole('button', { name: /iniciar sessão/i }));

    await waitFor(() => expect(called).toBe(true));
  });

  it('navega para /study/:id após sucesso', async () => {
    const user = userEvent.setup();
    render(<StudyConfigPage />, { wrapper: Wrapper });

    await user.click(screen.getByRole('button', { name: /iniciar sessão/i }));

    await waitFor(() => {
      expect(screen.getByTestId('study-page')).toBeInTheDocument();
    });
  });
});
