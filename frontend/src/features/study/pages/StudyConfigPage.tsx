import { useNavigate } from 'react-router-dom';
import { useForm, Controller } from 'react-hook-form';
import { Button } from '@/components/ui/button';
import { AreasTags } from '../components/AreasTags';
import { useCreateSession } from '../hooks/useCreateSession';
import type { Banca, Modo, CreateSessionRequest } from '../types';

interface FormValues {
  banca: Banca | '';
  areas: string[];
  quantidade: number;
  modo: Modo;
}

const bancas: Array<{ value: Banca | ''; label: string }> = [
  { value: '', label: 'Todas' },
  { value: 'CEBRASPE', label: 'CEBRASPE/CESPE' },
  { value: 'FGV', label: 'FGV' },
  { value: 'CESGRANRIO', label: 'CESGRANRIO' },
];

const quantidades = [10, 20, 30, 50];

export const StudyConfigPage = () => {
  const navigate = useNavigate();
  const { mutate, isPending } = useCreateSession();

  const { register, handleSubmit, control, watch } = useForm<FormValues>({
    defaultValues: { banca: '', areas: [], quantidade: 20, modo: 'ESTUDO' },
  });

  const onSubmit = (values: FormValues) => {
    const req: CreateSessionRequest = {
      quantidade: values.quantidade,
      modo: values.modo,
      ...(values.banca ? { banca: values.banca } : {}),
      ...(values.areas.length > 0 ? { areas: values.areas } : {}),
    };

    mutate(req, {
      onSuccess: (session) => void navigate(`/study/${session.id}`),
    });
  };

  const modo = watch('modo');

  return (
    <div className="mx-auto max-w-lg p-6">
      <h1 className="mb-6 text-xl font-semibold">Nova sessão de estudos</h1>

      <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-6">
        {/* Banca */}
        <fieldset>
          <legend className="mb-2 text-sm font-medium">Banca</legend>
          <div className="flex flex-wrap gap-3">
            {bancas.map(({ value, label }) => (
              <label key={label} className="flex cursor-pointer items-center gap-2 text-sm">
                <input type="radio" value={value} {...register('banca')} />
                {label}
              </label>
            ))}
          </div>
        </fieldset>

        {/* Áreas */}
        <div>
          <p className="mb-2 text-sm font-medium">Área de conhecimento</p>
          <Controller
            name="areas"
            control={control}
            render={({ field }) => (
              <AreasTags value={field.value} onChange={field.onChange} />
            )}
          />
        </div>

        {/* Quantidade */}
        <fieldset>
          <legend className="mb-2 text-sm font-medium">Quantidade</legend>
          <div className="flex gap-4">
            {quantidades.map((q) => (
              <label key={q} className="flex cursor-pointer items-center gap-2 text-sm">
                <input
                  type="radio"
                  value={q}
                  {...register('quantidade', { valueAsNumber: true })}
                />
                {q}
              </label>
            ))}
          </div>
        </fieldset>

        {/* Modo */}
        <fieldset>
          <legend className="mb-2 text-sm font-medium">Modo</legend>
          <div className="flex flex-col gap-2">
            <label className="flex cursor-pointer items-center gap-2 text-sm">
              <input type="radio" value="ESTUDO" {...register('modo')} />
              <span>
                Estudo{' '}
                <span className="text-muted-foreground">
                  — gabarito e explicação após cada resposta
                </span>
              </span>
            </label>
            <label className="flex cursor-pointer items-center gap-2 text-sm">
              <input type="radio" value="SIMULADO" {...register('modo')} />
              <span>
                Simulado{' '}
                <span className="text-muted-foreground">
                  — gabarito revelado apenas ao finalizar
                </span>
              </span>
            </label>
          </div>
          <p className="mt-1 text-xs text-muted-foreground">
            {modo === 'ESTUDO'
              ? 'Você verá a resposta correta e uma explicação gerada pelo Claude após cada questão.'
              : 'O resultado completo só será revelado ao finalizar a sessão.'}
          </p>
        </fieldset>

        <Button type="submit" disabled={isPending} className="self-end">
          {isPending ? 'Iniciando...' : 'Iniciar sessão →'}
        </Button>
      </form>
    </div>
  );
};
