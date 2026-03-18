import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AreasTags } from '../AreasTags';

const Wrapper = ({ children }: { children: React.ReactNode }) => (
  <QueryClientProvider
    client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}
  >
    {children}
  </QueryClientProvider>
);

describe('AreasTags', () => {
  it('carrega e exibe áreas disponíveis via MSW', async () => {
    render(<AreasTags value={[]} onChange={vi.fn()} />, { wrapper: Wrapper });
    await waitFor(() => {
      expect(screen.getByText(/Informática/)).toBeInTheDocument();
    });
  });

  it('filtra áreas pelo texto digitado no input', async () => {
    const user = userEvent.setup();
    render(<AreasTags value={[]} onChange={vi.fn()} />, { wrapper: Wrapper });
    await waitFor(() => screen.getByText(/Informática/));

    await user.type(screen.getByRole('textbox'), 'Info');

    expect(screen.getByText(/Informática/)).toBeInTheDocument();
    expect(screen.queryByText(/Português/)).not.toBeInTheDocument();
  });

  it('chama onChange com a área adicionada ao clicar numa sugestão', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<AreasTags value={[]} onChange={onChange} />, { wrapper: Wrapper });
    await waitFor(() => screen.getByText(/Informática/));

    await user.click(screen.getByText(/Informática/));

    expect(onChange).toHaveBeenCalledWith(['Informática']);
  });

  it('chama onChange com a área removida ao clicar no X da tag', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(
      <AreasTags value={['Informática']} onChange={onChange} />,
      { wrapper: Wrapper }
    );

    await user.click(screen.getByRole('button', { name: /remover Informática/i }));

    expect(onChange).toHaveBeenCalledWith([]);
  });

  it('não chama onChange quando o limite de 10 áreas já foi atingido', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    const dez = Array.from({ length: 10 }, (_, i) => `Area ${i + 1}`);
    render(<AreasTags value={dez} onChange={onChange} />, { wrapper: Wrapper });
    await waitFor(() => screen.getByText(/Informática/));

    await user.click(screen.getByText(/Informática/));

    expect(onChange).not.toHaveBeenCalled();
  });
});
