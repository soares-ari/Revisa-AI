import type { Resultado } from '../types';

interface ResultSummaryProps {
  resultado: Resultado;
  durationMs: number;
}

const formatDuration = (ms: number) => {
  const minutes = Math.floor(ms / 60000);
  const seconds = Math.floor((ms % 60000) / 1000);
  return `${minutes}min ${seconds}s`;
};

export const ResultSummary = ({ resultado, durationMs }: ResultSummaryProps) => {
  const erros = resultado.total - resultado.acertos;
  return (
    <div className="flex flex-col items-center gap-4">
      <p className="text-4xl font-bold">
        {resultado.acertos}{' '}
        <span className="text-muted-foreground text-2xl">/ {resultado.total}</span>
      </p>
      <div className="h-3 w-full max-w-xs overflow-hidden rounded-full bg-muted">
        <div
          className="h-full bg-primary"
          style={{ width: `${resultado.percentual}%` }}
        />
      </div>
      <p className="text-lg font-medium">{resultado.percentual.toFixed(1)}%</p>
      <div className="grid grid-cols-3 gap-4 text-center">
        <div className="rounded-md border p-3">
          <p className="text-2xl font-bold text-green-600">{resultado.acertos}</p>
          <p className="text-xs text-muted-foreground">acertos</p>
        </div>
        <div className="rounded-md border p-3">
          <p className="text-2xl font-bold text-red-500">{erros}</p>
          <p className="text-xs text-muted-foreground">erros</p>
        </div>
        <div className="rounded-md border p-3">
          <p className="text-2xl font-bold">{formatDuration(durationMs)}</p>
          <p className="text-xs text-muted-foreground">duração</p>
        </div>
      </div>
    </div>
  );
};
