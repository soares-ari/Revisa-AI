import type { UseQueryResult } from '@tanstack/react-query';
import type { SessionSummary } from '../types';

export const useHistory = (): UseQueryResult<SessionSummary[]> => {
  throw new Error('não implementado');
};
