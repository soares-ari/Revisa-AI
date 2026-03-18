import { useMutation } from '@tanstack/react-query';
import { studyService } from '../services/studyService';

export const useFinishSession = (sessionId: string) =>
  useMutation({
    mutationFn: () => studyService.finishSession(sessionId),
  });
