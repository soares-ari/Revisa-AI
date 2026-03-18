export type Banca = 'CEBRASPE' | 'FGV' | 'CESGRANRIO';
export type Modo = 'ESTUDO' | 'SIMULADO';
export type SessionStatus = 'EM_ANDAMENTO' | 'FINALIZADA';
export type Dificuldade = 'FACIL' | 'MEDIO' | 'DIFICIL';

export interface Answer {
  questionId: string;
  respostaUsuario: string;
  correta: boolean;
  area: string;
}

export interface Resultado {
  total: number;
  acertos: number;
  percentual: number;
  desempenhoPorArea: Record<string, number>;
}

export interface StudySession {
  id: string;
  userId: string;
  banca: Banca | null;
  areas: string[] | null;
  quantidade: number;
  modo: Modo;
  status: SessionStatus;
  questionIds: string[];
  currentIndex: number;
  answers: Answer[];
  resultado: Resultado | null;
  createdAt: string;
  updatedAt: string;
}

export interface Question {
  id: string;
  enunciado: string;
  alternativas: string[];
  gabarito: string;
  area: string;
  dificuldade: Dificuldade;
  banca: Banca;
  ano: number;
  cargo: string;
  provaId: string;
  valid: boolean;
}

export interface Explanation {
  id: string;
  questionId: string;
  texto: string;
  createdAt: string;
}

export interface CreateSessionRequest {
  banca?: Banca;
  areas?: string[];
  quantidade: number;
  modo: Modo;
}

export interface AnswerRequest {
  questionId: string;
  resposta: string;
}

export interface AnswerResponse {
  questionId: string;
  respostaUsuario: string;
  correta: boolean;
  gabarito: string | null;
  area: string;
}
