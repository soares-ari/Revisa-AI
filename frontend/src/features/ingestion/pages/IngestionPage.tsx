import { useState } from 'react';
import { IngestionForm } from '../components/IngestionForm';
import { JobStatus } from '../components/JobStatus';

export const IngestionPage = () => {
  const [jobId, setJobId] = useState<string | null>(null);

  return (
    <div className="flex min-h-screen items-center justify-center p-6">
      <div className="w-full max-w-lg">
        {jobId ? (
          <JobStatus jobId={jobId} onReset={() => setJobId(null)} />
        ) : (
          <IngestionForm onJobCreated={setJobId} />
        )}
      </div>
    </div>
  );
};
