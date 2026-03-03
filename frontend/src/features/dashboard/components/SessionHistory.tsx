import type { SessionSummary } from '../types';

const fmt = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short' });

export const SessionHistory = ({ sessions }: { sessions: SessionSummary[] }) => {
  const recent = sessions.slice(0, 10);

  return (
    <table className="w-full text-sm">
      <thead>
        <tr className="border-b text-left text-muted-foreground">
          <th className="pb-2 font-medium">Banca</th>
          <th className="pb-2 font-medium">Modo</th>
          <th className="pb-2 font-medium">Acertos</th>
          <th className="pb-2 font-medium">Data</th>
        </tr>
      </thead>
      <tbody>
        {recent.map((s) => (
          <tr key={s.id} className="border-b last:border-0">
            <td className="py-2">{s.banca ?? 'Todas'}</td>
            <td className="py-2">{s.modo}</td>
            <td className="py-2">{s.resultado.percentual.toFixed(1)}%</td>
            <td className="py-2">{fmt.format(new Date(s.createdAt))}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
};
