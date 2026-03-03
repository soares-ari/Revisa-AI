import { useQuery } from '@tanstack/react-query';
import { dashboardService } from '../services/dashboardService';

export const useStats = () =>
  useQuery({ queryKey: ['userStats'], queryFn: dashboardService.getStats });
