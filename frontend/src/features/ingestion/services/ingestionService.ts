import { api } from '@/lib/axios';
import type { IngestionJob } from '../types';

export const ingestionService = {
  createJob: (formData: FormData) =>
    api.post<IngestionJob>('/ingestion/jobs', formData).then((r) => r.data),
  getJob: (id: string) =>
    api.get<IngestionJob>(`/ingestion/jobs/${id}`).then((r) => r.data),
};
