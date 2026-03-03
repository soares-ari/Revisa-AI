import { api } from '@/lib/axios';
import type { UserStatsResponse, SessionSummary } from '../types';

export const dashboardService = {
  getStats: () =>
    api.get<UserStatsResponse>('/users/me/stats').then((r) => r.data),
  getHistory: () =>
    api.get<SessionSummary[]>('/users/me/history').then((r) => r.data),
};
