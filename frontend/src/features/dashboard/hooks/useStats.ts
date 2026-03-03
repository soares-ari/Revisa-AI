import type { UseQueryResult } from '@tanstack/react-query';
import type { UserStatsResponse } from '../types';

export const useStats = (): UseQueryResult<UserStatsResponse> => {
  throw new Error('não implementado');
};
