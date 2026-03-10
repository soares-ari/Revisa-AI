import { useMutation } from '@tanstack/react-query';
import { ingestionService } from '../services/ingestionService';
import type { IngestionFormData } from '../schemas/ingestionSchema';

export const useCreateJob = () =>
  useMutation({
    mutationFn: (data: IngestionFormData) => {
      const fd = new FormData();
      fd.append('banca', data.banca);
      fd.append('ano', String(data.ano));
      fd.append('orgao', data.orgao);
      fd.append('cargo', data.cargo);
      fd.append('provaArquivo', data.provaArquivo);
      fd.append('gabaritoArquivo', data.gabaritoArquivo);
      return ingestionService.createJob(fd);
    },
  });
