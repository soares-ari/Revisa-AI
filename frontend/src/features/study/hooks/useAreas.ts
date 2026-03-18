import { useQuery } from '@tanstack/react-query';
import { studyService } from '../services/studyService';

export const useAreas = () =>
  useQuery({ queryKey: ['areas'], queryFn: studyService.getAreas });
