import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useSession } from '../hooks/useSession';
import { useQuestion } from '../hooks/useQuestion';
import { useExplanation } from '../hooks/useExplanation';
import { useAnswerQuestion } from '../hooks/useAnswerQuestion';
import { useFinishSession } from '../hooks/useFinishSession';
import { QuestionCard } from '../components/QuestionCard';
import { ProgressBar } from '../components/ProgressBar';
import { ExplanationPanel } from '../components/ExplanationPanel';
import type { AnswerResponse } from '../types';

export const StudyPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const { data: session } = useSession(id!);

  const [localCurrentIndex, setLocalCurrentIndex] = useState<number | null>(null);
  const [answeredResult, setAnsweredResult] = useState<AnswerResponse | null>(null);

  const currentIndex = localCurrentIndex ?? session?.currentIndex ?? 0;
  const questionId = session?.questionIds[currentIndex];
  const isLastQuestion = session ? currentIndex === session.questionIds.length - 1 : false;
  const modoEstudo = session?.modo === 'ESTUDO';

  const { data: question } = useQuestion(questionId);
  const { data: explanation } = useExplanation(
    questionId,
    !!answeredResult && modoEstudo
  );

  const { mutate: answer, isPending: isAnswering } = useAnswerQuestion(id!);
  const { mutate: finish, isPending: isFinishing } = useFinishSession(id!);

  const handleAnswer = (resposta: string) => {
    if (!questionId || answeredResult) return;
    answer(
      { questionId, resposta },
      { onSuccess: (result) => setAnsweredResult(result) }
    );
  };

  const handleNext = () => {
    setAnsweredResult(null);
    setLocalCurrentIndex(currentIndex + 1);
  };

  const handleFinish = () => {
    finish(undefined, {
      onSuccess: () => void navigate(`/study/${id}/result`),
    });
  };

  if (!session || !question) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <p className="text-muted-foreground">Carregando...</p>
      </div>
    );
  }

  const showGabarito = !!answeredResult && modoEstudo;
  const acertou = answeredResult?.correta;

  return (
    <div className="mx-auto flex max-w-2xl flex-col gap-6 p-6">
      <div className="flex flex-col gap-2">
        <div className="flex items-center justify-between text-sm text-muted-foreground">
          <span>
            Questão {currentIndex + 1} de {session.questionIds.length}
          </span>
          <span>
            {session.banca} · {question.area}
          </span>
        </div>
        <ProgressBar current={currentIndex} total={session.questionIds.length} />
      </div>

      <div className="text-xs text-muted-foreground">
        Ano: {question.ano} · Cargo: {question.cargo}
      </div>

      <QuestionCard
        question={question}
        onAnswer={handleAnswer}
        disabled={!!answeredResult || isAnswering}
      />

      {showGabarito && (
        <div
          className={`rounded-md border p-3 text-sm font-medium ${
            acertou
              ? 'border-green-500 bg-green-50 text-green-700'
              : 'border-red-400 bg-red-50 text-red-700'
          }`}
        >
          {acertou ? '✅ Você acertou!' : '❌ Você errou!'}{' '}
          {answeredResult?.gabarito && (
            <span className="font-normal">
              Resposta correta: <strong>{answeredResult.gabarito}</strong>
            </span>
          )}
        </div>
      )}

      {modoEstudo && explanation && (
        <div>
          <p className="mb-1 text-xs font-medium text-muted-foreground">
            💡 Explicação
          </p>
          <ExplanationPanel
            texto={explanation.texto}
            visible={!!answeredResult}
          />
        </div>
      )}

      {answeredResult && (
        <div className="flex justify-end">
          {isLastQuestion ? (
            <button
              onClick={handleFinish}
              disabled={isFinishing}
              className="rounded-md bg-primary px-6 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
            >
              {isFinishing ? 'Finalizando...' : 'Finalizar'}
            </button>
          ) : (
            <button
              onClick={handleNext}
              className="rounded-md bg-primary px-6 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
            >
              Próxima questão →
            </button>
          )}
        </div>
      )}
    </div>
  );
};
