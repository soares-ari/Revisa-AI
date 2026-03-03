import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Home, BookOpen, BarChart2, Settings, Menu, X } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { StatsCards } from '../components/StatsCards';
import { AreaChart } from '../components/AreaChart';
import { ActivityHeatmap } from '../components/ActivityHeatmap';
import { SessionHistory } from '../components/SessionHistory';
import { useStats } from '../hooks/useStats';
import { useHistory } from '../hooks/useHistory';

const navLinks = [
  { to: '/', label: 'Home', icon: Home },
  { to: '/study/new', label: 'Estudar', icon: BookOpen },
  { to: '/dashboard', label: 'Stats', icon: BarChart2 },
  { to: '/settings', label: 'Config', icon: Settings },
];

const Sidebar = ({
  open,
  onClose,
}: {
  open: boolean;
  onClose: () => void;
}) => (
  <>
    {open && (
      <div
        className="fixed inset-0 z-30 bg-black/40 md:hidden"
        onClick={onClose}
      />
    )}
    <aside
      className={[
        'fixed inset-y-0 left-0 z-40 flex w-56 flex-col border-r bg-background p-4 transition-transform md:static md:translate-x-0',
        open ? 'translate-x-0' : '-translate-x-full',
      ].join(' ')}
    >
      <div className="mb-6 flex items-center justify-between">
        <span className="font-bold">REVISA AI</span>
        <button className="md:hidden" onClick={onClose} aria-label="Fechar menu">
          <X className="h-4 w-4" />
        </button>
      </div>
      <nav className="flex flex-col gap-1">
        {navLinks.map(({ to, label, icon: Icon }) => (
          <Link
            key={to}
            to={to}
            onClick={onClose}
            className="flex items-center gap-2 rounded-md px-3 py-2 text-sm hover:bg-muted"
          >
            <Icon className="h-4 w-4" />
            {label}
          </Link>
        ))}
      </nav>
    </aside>
  </>
);

export const DashboardPage = () => {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const navigate = useNavigate();
  const { data: stats } = useStats();
  const { data: history } = useHistory();

  return (
    <div className="flex min-h-screen">
      <Sidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />

      <div className="flex flex-1 flex-col">
        <header className="flex items-center gap-4 border-b px-6 py-4">
          <button
            className="md:hidden"
            onClick={() => setSidebarOpen(true)}
            aria-label="Abrir menu"
          >
            <Menu className="h-5 w-5" />
          </button>
          <h1 className="text-lg font-semibold">Seu progresso</h1>
        </header>

        <main className="flex flex-1 flex-col gap-6 p-6">
          {stats && <StatsCards stats={stats} />}

          {history && (
            <Card>
              <CardHeader>
                <CardTitle className="text-sm font-medium">
                  Atividade — últimos 12 meses
                </CardTitle>
              </CardHeader>
              <CardContent>
                <ActivityHeatmap sessions={history} />
              </CardContent>
            </Card>
          )}

          {stats?.desempenhoPorArea &&
            Object.keys(stats.desempenhoPorArea).length > 0 && (
              <Card>
                <CardHeader>
                  <CardTitle className="text-sm font-medium">
                    Desempenho por área
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <AreaChart desempenhoPorArea={stats.desempenhoPorArea} />
                </CardContent>
              </Card>
            )}

          {history && history.length > 0 && (
            <Card>
              <CardHeader>
                <CardTitle className="text-sm font-medium">
                  Histórico de sessões
                </CardTitle>
              </CardHeader>
              <CardContent>
                <SessionHistory sessions={history} />
              </CardContent>
            </Card>
          )}

          <Button onClick={() => void navigate('/study/new')} className="self-start">
            Nova sessão de estudos →
          </Button>
        </main>
      </div>
    </div>
  );
};
