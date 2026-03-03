import { useQuery } from '@tanstack/react-query';
import { dashboardService } from '../services/dashboardService';

export const useHistory = () =>
  useQuery({ queryKey: ['userHistory'], queryFn: dashboardService.getHistory });
