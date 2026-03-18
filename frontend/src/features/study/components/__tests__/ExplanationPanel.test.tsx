import { render, screen } from '@testing-library/react';
import { ExplanationPanel } from '../ExplanationPanel';

vi.mock('framer-motion', () => ({
  motion: {
    div: ({
      children,
      animate,
      ...props
    }: React.PropsWithChildren<{ animate?: unknown; [key: string]: unknown }>) => (
      <div
        data-testid="motion-div"
        data-animate={JSON.stringify(animate)}
        {...props}
      >
        {children}
      </div>
    ),
  },
  AnimatePresence: ({ children }: React.PropsWithChildren) => <>{children}</>,
}));

describe('ExplanationPanel', () => {
  it('está presente no DOM quando visible=true', () => {
    render(<ExplanationPanel texto="Explicação da questão gerada pelo Claude." visible />);
    expect(screen.getByTestId('motion-div')).toBeInTheDocument();
    expect(
      screen.getByText(/Explicação da questão gerada pelo Claude/i)
    ).toBeInTheDocument();
  });

  it('motion.div recebe prop animate definida', () => {
    render(<ExplanationPanel texto="Texto de exemplo." visible />);
    const el = screen.getByTestId('motion-div');
    expect(el.getAttribute('data-animate')).not.toBeNull();
  });

  it('não está no DOM quando visible=false', () => {
    render(<ExplanationPanel texto="Texto de exemplo." visible={false} />);
    expect(screen.queryByTestId('motion-div')).not.toBeInTheDocument();
  });
});
