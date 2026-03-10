import { useQuery } from '@tanstack/react-query';
import { ingestionService } from '../services/ingestionService';

export const useJobStatus = (jobId: string | null) =>
  useQuery({
    queryKey: ['ingestion-job', jobId],
    queryFn: () => ingestionService.getJob(jobId!),
    enabled: jobId !== null,
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      if (status === 'COMPLETED' || status === 'FAILED') return false;
      return 3000;
    },
  });
