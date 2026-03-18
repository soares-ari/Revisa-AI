import { useQuery } from '@tanstack/react-query';
import { studyService } from '../services/studyService';

export const useExplanation = (questionId: string | undefined, enabled: boolean) =>
  useQuery({
    queryKey: ['explanation', questionId],
    queryFn: () => studyService.getExplanation(questionId!),
    enabled: !!questionId && enabled,
  });
