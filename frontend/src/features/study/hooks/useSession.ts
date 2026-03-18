import { useQuery } from '@tanstack/react-query';
import { studyService } from '../services/studyService';

export const useSession = (id: string) =>
  useQuery({ queryKey: ['session', id], queryFn: () => studyService.getSession(id) });
