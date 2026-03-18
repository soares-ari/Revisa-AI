import { useMutation } from '@tanstack/react-query';
import { studyService } from '../services/studyService';
import type { CreateSessionRequest } from '../types';

export const useCreateSession = () =>
  useMutation({
    mutationFn: (req: CreateSessionRequest) => studyService.createSession(req),
  });
