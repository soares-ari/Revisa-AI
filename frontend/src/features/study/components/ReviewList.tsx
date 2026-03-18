import { Link } from 'react-router-dom';
import type { Answer } from '../types';

interface ReviewListProps {
  erradas: Answer[];
  questionIds: string[];
}

export const ReviewList = ({ erradas, questionIds }: ReviewListProps) => {
  if (erradas.length === 0) return null;

  return (
    <div className="flex flex-col gap-2">
      {erradas.map((answer) => {
        const n = questionIds.indexOf(answer.questionId) + 1;
        return (
          <div
            key={answer.questionId}
            className="flex items-center justify-between rounded-md border px-4 py-2 text-sm"
          >
            <span>
              Q{n} · <span className="text-muted-foreground">{answer.area}</span>
            </span>
            <Link
              to={`/questions/${answer.questionId}`}
              className="text-primary hover:underline"
            >
              Ver questão →
            </Link>
          </div>
        );
      })}
    </div>
  );
};
