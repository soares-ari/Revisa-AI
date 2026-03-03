import { render, screen } from '@testing-library/react';
import { ActivityHeatmap } from '../ActivityHeatmap';
import type { SessionSummary } from '../../types';

const makeSession = (id: string, createdAt: string): SessionSummary => ({
  id,
  banca: 'CEBRASPE',
  areas: ['Informática'],
  quantidade: 20,
  modo: 'ESTUDO',
  resultado: { total: 20, acertos: 17, percentual: 85, desempenhoPorArea: {} },
  createdAt,
});

describe('ActivityHeatmap', () => {
  it('renderiza pelo menos 364 células (52–53 semanas × 7 dias)', () => {
    render(<ActivityHeatmap sessions={[]} />);
    const cells = screen.getAllByTestId('heatmap-cell');
    expect(cells.length).toBeGreaterThanOrEqual(364);
  });

  it('células dos dias com sessões têm data-count > 0', () => {
    const sessions = [
      makeSession('1', '2025-03-01T10:00:00Z'),
      makeSession('2', '2025-03-01T15:00:00Z'), // mesmo dia
      makeSession('3', '2025-03-02T10:00:00Z'),
    ];
    render(<ActivityHeatmap sessions={sessions} />);
    const cells = screen.getAllByTestId('heatmap-cell');
    const activeCells = cells.filter(
      (c) => parseInt(c.getAttribute('data-count') ?? '0') > 0
    );
    expect(activeCells).toHaveLength(2);
  });
});
