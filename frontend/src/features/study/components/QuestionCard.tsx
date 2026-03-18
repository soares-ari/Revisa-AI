import type { Question } from '../types';

interface QuestionCardProps {
  question: Question;
  onAnswer: (resposta: string) => void;
  disabled?: boolean;
}

export const QuestionCard = ({ question, onAnswer, disabled = false }: QuestionCardProps) => {
  return (
    <div className="flex flex-col gap-4">
      <p className="text-base leading-relaxed">{question.enunciado}</p>
      <div className="flex flex-wrap gap-3">
        {question.alternativas.map((alt) => (
          <button
            key={alt}
            onClick={() => onAnswer(alt)}
            disabled={disabled}
            className="rounded-md border px-6 py-3 text-sm font-medium transition-colors hover:bg-muted disabled:cursor-not-allowed disabled:opacity-50"
          >
            {alt}
          </button>
        ))}
      </div>
    </div>
  );
};
