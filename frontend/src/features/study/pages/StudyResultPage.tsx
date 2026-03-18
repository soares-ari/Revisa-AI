import { useParams, useNavigate } from 'react-router-dom';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
} from 'recharts';
import { useSession } from '../hooks/useSession';
import { ResultSummary } from '../components/ResultSummary';
import { ReviewList } from '../components/ReviewList';

export const StudyResultPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { data: session } = useSession(id!);

  if (!session || !session.resultado) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <p className="text-muted-foreground">Carregando resultado...</p>
      </div>
    );
  }

  const { resultado, answers, questionIds, createdAt } = session;
  const durationMs = Date.now() - new Date(createdAt).getTime();
  const erradas = answers.filter((a) => !a.correta);

  const areaData = Object.entries(resultado.desempenhoPorArea)
    .map(([area, percentual]) => ({ area, percentual }))
    .sort((a, b) => b.percentual - a.percentual);

  return (
    <div className="mx-auto flex max-w-2xl flex-col gap-8 p-6">
      <h1 className="text-center text-2xl font-bold">Sessão concluída!</h1>

      <ResultSummary resultado={resultado} durationMs={durationMs} />

      {areaData.length > 0 && (
        <div>
          <h2 className="mb-3 text-sm font-medium">Desempenho por área</h2>
          <ResponsiveContainer width="100%" height={areaData.length * 40 + 20}>
            <BarChart data={areaData} layout="vertical">
              <XAxis type="number" domain={[0, 100]} hide />
              <YAxis type="category" dataKey="area" width={180} tick={{ fontSize: 12 }} />
              <Tooltip formatter={(v: number) => `${v.toFixed(1)}%`} />
              <Bar dataKey="percentual" fill="hsl(var(--primary))" radius={[0, 4, 4, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}

      {erradas.length > 0 && (
        <div>
          <h2 className="mb-3 text-sm font-medium">Questões para revisão</h2>
          <ReviewList erradas={erradas} questionIds={questionIds} />
        </div>
      )}

      <div className="flex gap-3 justify-center">
        <button
          onClick={() => void navigate('/study/new')}
          className="rounded-md border px-6 py-2 text-sm font-medium hover:bg-muted"
        >
          Nova sessão
        </button>
        <button
          onClick={() => void navigate('/dashboard')}
          className="rounded-md bg-primary px-6 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        >
          Ver dashboard
        </button>
      </div>
    </div>
  );
};
