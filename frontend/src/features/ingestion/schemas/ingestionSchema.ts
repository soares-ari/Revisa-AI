import { z } from 'zod';

const currentYear = new Date().getFullYear();

const pdfFile = z
  .instanceof(File)
  .refine((f) => f.type === 'application/pdf', 'Apenas arquivos PDF são aceitos');

export const ingestionSchema = z.object({
  banca: z.enum(['CEBRASPE', 'FGV', 'CESGRANRIO'], {
    errorMap: () => ({ message: 'Selecione uma banca' }),
  }),
  ano: z.coerce
    .number({ invalid_type_error: 'Ano inválido' })
    .int()
    .min(2000, 'Ano mínimo: 2000')
    .max(currentYear, `Ano máximo: ${currentYear}`),
  orgao: z.string().min(2, 'Mínimo 2 caracteres'),
  cargo: z.string().min(2, 'Mínimo 2 caracteres'),
  provaArquivo: pdfFile,
  gabaritoArquivo: pdfFile,
});

export type IngestionFormData = z.infer<typeof ingestionSchema>;
