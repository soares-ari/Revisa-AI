import { render, screen } from '@testing-library/react';
import { SessionHistory } from '../SessionHistory';
import type { SessionSummary } from '../../types';

const sessions: SessionSummary[] = [
  {
    id: 'session-1',
    banca: 'CEBRASPE',
    areas: ['Informática'],
    quantidade: 20,
    modo: 'ESTUDO',
    resultado: { total: 20, acertos: 17, percentual: 85.0, desempenhoPorArea: {} },
    createdAt: '2025-03-01T10:00:00Z',
  },
  {
    id: 'session-2',
    banca: 'FGV',
    areas: ['Português'],
    quantidade: 10,
    modo: 'SIMULADO',
    resultado: { total: 10, acertos: 6, percentual: 60.0, desempenhoPorArea: {} },
    createdAt: '2025-02-20T14:00:00Z',
  },
];

describe('SessionHistory', () => {
  it('renderiza uma linha por sessão da lista', () => {
    render(<SessionHistory sessions={sessions} />);
    const rows = screen.getAllByRole('row');
    // Exclui o cabeçalho
    expect(rows.length - 1).toBe(2);
  });

  it('exibe banca, modo e percentual de acerto corretamente', () => {
    render(<SessionHistory sessions={sessions} />);
    expect(screen.getByText('CEBRASPE')).toBeInTheDocument();
    expect(screen.getByText('ESTUDO')).toBeInTheDocument();
    expect(screen.getByText(/85/)).toBeInTheDocument();
  });
});
