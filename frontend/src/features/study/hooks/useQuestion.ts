import { useQuery } from '@tanstack/react-query';
import { studyService } from '../services/studyService';

export const useQuestion = (id: string | undefined) =>
  useQuery({
    queryKey: ['question', id],
    queryFn: () => studyService.getQuestion(id!),
    enabled: !!id,
  });
