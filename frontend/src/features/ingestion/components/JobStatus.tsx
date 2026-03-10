import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { useJobStatus } from '../hooks/useJobStatus';

interface Props {
  jobId: string;
  onReset: () => void;
}

export const JobStatus = ({ jobId, onReset }: Props) => {
  const { data, isLoading } = useJobStatus(jobId);

  if (isLoading || !data) {
    return (
      <Card>
        <CardContent className="py-8 text-center text-sm text-muted-foreground">
          Carregando...
        </CardContent>
      </Card>
    );
  }

  if (data.status === 'FAILED') {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="text-destructive">Falha na ingestão</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          <p className="text-sm text-muted-foreground">{data.errorMessage}</p>
          <Button variant="outline" onClick={onReset}>
            Nova ingestão
          </Button>
        </CardContent>
      </Card>
    );
  }

  if (data.status === 'COMPLETED') {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Ingestão concluída!</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          <div className="flex gap-8 text-sm">
            <div>
              <span className="text-muted-foreground">Questões salvas</span>
              <p
                className="text-2xl font-bold text-green-600"
                data-testid="questoes-salvas"
              >
                {data.questoesSalvas}
              </p>
            </div>
            <div>
              <span className="text-muted-foreground">Questões inválidas</span>
              <p
                className="text-2xl font-bold text-amber-600"
                data-testid="questoes-invalidas"
              >
                {data.questoesInvalidas}
              </p>
            </div>
          </div>
          <Button variant="outline" onClick={onReset}>
            Nova ingestão
          </Button>
        </CardContent>
      </Card>
    );
  }

  const statusLabel =
    data.status === 'PROCESSING' ? 'Processando questões...' : 'Aguardando processamento...';

  return (
    <Card>
      <CardContent className="py-8 text-center text-sm text-muted-foreground">
        {statusLabel}
      </CardContent>
    </Card>
  );
};
