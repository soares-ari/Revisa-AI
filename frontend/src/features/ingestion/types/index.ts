export type IngestionStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

export interface IngestionJob {
  id: string;
  status: IngestionStatus;
  banca: string;
  ano: number | null;
  orgao: string;
  cargo: string;
  questoesSalvas: number;
  questoesInvalidas: number;
  errorMessage: string | null;
}
