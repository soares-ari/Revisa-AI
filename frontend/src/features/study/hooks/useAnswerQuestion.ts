import { useMutation } from '@tanstack/react-query';
import { studyService } from '../services/studyService';
import type { AnswerRequest } from '../types';

export const useAnswerQuestion = (sessionId: string) =>
  useMutation({
    mutationFn: (req: AnswerRequest) => studyService.answerQuestion(sessionId, req),
  });
