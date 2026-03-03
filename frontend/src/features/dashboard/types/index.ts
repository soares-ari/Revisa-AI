export interface UserStatsResponse {
  totalQuestoes: number;
  percentualAcertos: number;
  totalSessoes: number;
  desempenhoPorArea: Record<string, number>;
}

export interface Resultado {
  total: number;
  acertos: number;
  percentual: number;
  desempenhoPorArea: Record<string, number>;
}

export interface SessionSummary {
  id: string;
  banca: string | null;
  areas: string[] | null;
  quantidade: number;
  modo: 'ESTUDO' | 'SIMULADO';
  resultado: Resultado;
  createdAt: string;
}
