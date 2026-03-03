import { render, screen } from '@testing-library/react';
import { StatsCards } from '../StatsCards';
import type { UserStatsResponse } from '../../types';

const stats: UserStatsResponse = {
  totalQuestoes: 150,
  percentualAcertos: 72.5,
  totalSessoes: 8,
  desempenhoPorArea: { Informática: 84 },
};

describe('StatsCards', () => {
  it('renderiza o total de questões respondidas', () => {
    render(<StatsCards stats={stats} />);
    expect(screen.getByText('150')).toBeInTheDocument();
  });

  it('renderiza o percentual global de acertos', () => {
    render(<StatsCards stats={stats} />);
    expect(screen.getByText(/72[,.]5\s*%/)).toBeInTheDocument();
  });

  it('renderiza o total de sessões finalizadas', () => {
    render(<StatsCards stats={stats} />);
    expect(screen.getByText('8')).toBeInTheDocument();
  });
});
