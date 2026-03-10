import { useEffect } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { ingestionSchema, type IngestionFormData } from '../schemas/ingestionSchema';
import { useCreateJob } from '../hooks/useCreateJob';

interface Props {
  onJobCreated: (jobId: string) => void;
}

export const IngestionForm = ({ onJobCreated }: Props) => {
  const {
    register,
    handleSubmit,
    control,
    formState: { errors },
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  } = useForm<IngestionFormData, any, IngestionFormData>({
    resolver: zodResolver(ingestionSchema) as any,
  });

  const { mutate, isPending, isSuccess, data } = useCreateJob();

  useEffect(() => {
    if (isSuccess && data) {
      onJobCreated(data.id);
    }
  }, [isSuccess, data, onJobCreated]);

  const onSubmit = (formData: IngestionFormData) => {
    mutate(formData);
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Nova ingestão de prova</CardTitle>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
          <div className="flex flex-col gap-1">
            <Label htmlFor="banca">Banca</Label>
            <select
              id="banca"
              {...register('banca')}
              className="rounded-md border px-3 py-2 text-sm"
            >
              <option value="">Selecione...</option>
              <option value="CEBRASPE">CEBRASPE</option>
              <option value="FGV">FGV</option>
              <option value="CESGRANRIO">CESGRANRIO</option>
            </select>
            {errors.banca && (
              <span className="text-sm text-destructive">{errors.banca.message}</span>
            )}
          </div>

          <div className="flex flex-col gap-1">
            <Label htmlFor="ano">Ano</Label>
            <Input id="ano" type="number" {...register('ano', { valueAsNumber: true })} />
            {errors.ano && (
              <span className="text-sm text-destructive">{errors.ano.message}</span>
            )}
          </div>

          <div className="flex flex-col gap-1">
            <Label htmlFor="orgao">Órgão</Label>
            <Input id="orgao" type="text" {...register('orgao')} />
            {errors.orgao && (
              <span className="text-sm text-destructive">{errors.orgao.message}</span>
            )}
          </div>

          <div className="flex flex-col gap-1">
            <Label htmlFor="cargo">Cargo</Label>
            <Input id="cargo" type="text" {...register('cargo')} />
            {errors.cargo && (
              <span className="text-sm text-destructive">{errors.cargo.message}</span>
            )}
          </div>

          <div className="flex flex-col gap-1">
            <Label htmlFor="provaArquivo">Arquivo da prova (PDF)</Label>
            <Controller
              name="provaArquivo"
              control={control}
              render={({ field: { onChange, onBlur, name, ref } }) => (
                <input
                  id="provaArquivo"
                  type="file"
                  accept=".pdf"
                  className="text-sm"
                  name={name}
                  ref={ref}
                  onBlur={onBlur}
                  onChange={(e) => onChange(e.target.files?.[0])}
                />
              )}
            />
            {errors.provaArquivo && (
              <span className="text-sm text-destructive">
                {errors.provaArquivo.message}
              </span>
            )}
          </div>

          <div className="flex flex-col gap-1">
            <Label htmlFor="gabaritoArquivo">Arquivo do gabarito (PDF)</Label>
            <Controller
              name="gabaritoArquivo"
              control={control}
              render={({ field: { onChange, onBlur, name, ref } }) => (
                <input
                  id="gabaritoArquivo"
                  type="file"
                  accept=".pdf"
                  className="text-sm"
                  name={name}
                  ref={ref}
                  onBlur={onBlur}
                  onChange={(e) => onChange(e.target.files?.[0])}
                />
              )}
            />
            {errors.gabaritoArquivo && (
              <span className="text-sm text-destructive">
                {errors.gabaritoArquivo.message}
              </span>
            )}
          </div>

          <Button type="submit" disabled={isPending}>
            {isPending ? 'Enviando...' : 'Iniciar ingestão'}
          </Button>
        </form>
      </CardContent>
    </Card>
  );
};
