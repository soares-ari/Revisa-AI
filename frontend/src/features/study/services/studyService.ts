import { api } from '@/lib/axios';
import type {
  StudySession,
  Question,
  Explanation,
  CreateSessionRequest,
  AnswerRequest,
  AnswerResponse,
} from '../types';

export const studyService = {
  getAreas: () => api.get<string[]>('/questions/areas').then((r) => r.data),

  createSession: (req: CreateSessionRequest) =>
    api.post<StudySession>('/study/sessions', req).then((r) => r.data),

  getSession: (id: string) =>
    api.get<StudySession>(`/study/sessions/${id}`).then((r) => r.data),

  getQuestion: (id: string) =>
    api.get<Question>(`/questions/${id}`).then((r) => r.data),

  getExplanation: (questionId: string) =>
    api.get<Explanation>(`/explanations/${questionId}`).then((r) => r.data),

  answerQuestion: (sessionId: string, req: AnswerRequest) =>
    api
      .post<AnswerResponse>(`/study/sessions/${sessionId}/answer`, req)
      .then((r) => r.data),

  finishSession: (sessionId: string) =>
    api
      .post<StudySession>(`/study/sessions/${sessionId}/finish`)
      .then((r) => r.data),
};
