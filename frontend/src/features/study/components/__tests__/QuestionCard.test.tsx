import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QuestionCard } from '../QuestionCard';
import type { Question } from '../../types';

const mockQuestion: Question = {
  id: 'q-1',
  enunciado: 'O firewall é um dispositivo de segurança que controla o tráfego de rede.',
  alternativas: ['CERTO', 'ERRADO'],
  gabarito: 'CERTO',
  area: 'Informática',
  dificuldade: 'MEDIO',
  banca: 'CEBRASPE',
  ano: 2023,
  cargo: 'Analista',
  provaId: 'prova-1',
  valid: true,
};

describe('QuestionCard', () => {
  it('renderiza o enunciado da questão', () => {
    render(<QuestionCard question={mockQuestion} onAnswer={vi.fn()} />);
    expect(
      screen.getByText(/O firewall é um dispositivo de segurança/i)
    ).toBeInTheDocument();
  });

  it('renderiza todas as alternativas dinamicamente', () => {
    render(<QuestionCard question={mockQuestion} onAnswer={vi.fn()} />);
    expect(screen.getByRole('button', { name: /^CERTO$/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^ERRADO$/i })).toBeInTheDocument();
  });

  it('chama onAnswer com o valor da alternativa ao clicar', async () => {
    const user = userEvent.setup();
    const onAnswer = vi.fn();
    render(<QuestionCard question={mockQuestion} onAnswer={onAnswer} />);
    await user.click(screen.getByRole('button', { name: /^CERTO$/i }));
    expect(onAnswer).toHaveBeenCalledWith('CERTO');
  });

  it('desabilita os botões de alternativas quando disabled=true', () => {
    render(<QuestionCard question={mockQuestion} onAnswer={vi.fn()} disabled />);
    screen.getAllByRole('button').forEach((btn) => expect(btn).toBeDisabled());
  });
});
