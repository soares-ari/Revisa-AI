import { render, screen } from '@testing-library/react';
import { AreaChart } from '../AreaChart';

vi.mock('recharts', () => ({
  ResponsiveContainer: ({ children }: { children: React.ReactNode }) => (
    <div>{children}</div>
  ),
  BarChart: ({
    data,
    children,
  }: {
    data: unknown[];
    children: React.ReactNode;
  }) => (
    <div data-testid="recharts-barchart" data-count={data.length}>
      {children}
    </div>
  ),
  Bar: () => null,
  XAxis: ({ dataKey }: { dataKey: string }) => (
    <div data-testid={`xaxis-${dataKey}`} />
  ),
  YAxis: () => null,
  Tooltip: () => null,
  Cell: () => null,
}));

const desempenhoPorArea = { Informática: 84, Português: 72, Matemática: 55 };

describe('AreaChart', () => {
  it('renderiza uma entrada por área no gráfico', () => {
    render(<AreaChart desempenhoPorArea={desempenhoPorArea} />);
    expect(screen.getByTestId('recharts-barchart')).toHaveAttribute(
      'data-count',
      '3'
    );
  });

  it('ordena as áreas do maior para o menor percentual', () => {
    const twoAreas = { Matemática: 55, Informática: 84 };
    render(<AreaChart desempenhoPorArea={twoAreas} />);
    expect(screen.getByTestId('recharts-barchart')).toHaveAttribute(
      'data-count',
      '2'
    );
  });
});
