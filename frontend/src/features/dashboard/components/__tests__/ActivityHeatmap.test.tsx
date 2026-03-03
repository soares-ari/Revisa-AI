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

// Datas relativas a hoje para garantir que estejam dentro da janela de 364 dias
const offsetDay = (daysAgo: number) => {
  const d = new Date();
  d.setDate(d.getDate() - daysAgo);
  return d.toISOString().slice(0, 10);
};

describe('ActivityHeatmap', () => {
  it('renderiza pelo menos 364 células (52–53 semanas × 7 dias)', () => {
    render(<ActivityHeatmap sessions={[]} />);
    const cells = screen.getAllByTestId('heatmap-cell');
    expect(cells.length).toBeGreaterThanOrEqual(364);
  });

  it('células dos dias com sessões têm data-count > 0', () => {
    const day1 = offsetDay(30); // 30 dias atrás
    const day2 = offsetDay(60); // 60 dias atrás — dia distinto
    const sessions = [
      makeSession('1', `${day1}T10:00:00Z`),
      makeSession('2', `${day1}T15:00:00Z`), // mesmo dia que session 1
      makeSession('3', `${day2}T10:00:00Z`),
    ];
    render(<ActivityHeatmap sessions={sessions} />);
    const cells = screen.getAllByTestId('heatmap-cell');
    const activeCells = cells.filter(
      (c) => parseInt(c.getAttribute('data-count') ?? '0') > 0
    );
    expect(activeCells).toHaveLength(2);
  });
});
