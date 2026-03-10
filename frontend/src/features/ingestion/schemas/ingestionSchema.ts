import { z } from 'zod';

const currentYear = new Date().getFullYear();

const pdfFile = z
  .custom<File>(
    (val) => val != null && typeof (val as File).name === 'string',
    'Arquivo obrigatório'
  )
  .refine((f) => (f as File).type === 'application/pdf', 'Apenas arquivos PDF são aceitos');

export const ingestionSchema = z.object({
  banca: z
    .string()
    .refine(
      (v) => v === 'CEBRASPE' || v === 'FGV' || v === 'CESGRANRIO',
      'Selecione uma banca'
    ),
  ano: z
    .number({ message: 'Ano inválido' })
    .int()
    .min(2000, 'Ano mínimo: 2000')
    .max(currentYear, `Ano máximo: ${currentYear}`),
  orgao: z.string().min(2, 'Mínimo 2 caracteres'),
  cargo: z.string().min(2, 'Mínimo 2 caracteres'),
  provaArquivo: pdfFile,
  gabaritoArquivo: pdfFile,
});

export type IngestionFormData = z.infer<typeof ingestionSchema>;
