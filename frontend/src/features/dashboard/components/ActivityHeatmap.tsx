import { cn } from '@/lib/utils';
import type { SessionSummary } from '../types';

function buildGrid(sessions: SessionSummary[]) {
  const sessionsByDay: Record<string, number> = {};
  sessions.forEach((s) => {
    if (!s.createdAt) return;
    const day = s.createdAt.slice(0, 10);
    sessionsByDay[day] = (sessionsByDay[day] ?? 0) + 1;
  });

  const today = new Date();
  today.setHours(0, 0, 0, 0);

  // Start from the Sunday of the week 52 full weeks ago
  const start = new Date(today);
  start.setDate(start.getDate() - 364);
  start.setDate(start.getDate() - start.getDay()); // back to Sunday

  const cells: { date: string; count: number }[] = [];
  const current = new Date(start);
  while (current <= today) {
    const dateStr = current.toISOString().slice(0, 10);
    cells.push({ date: dateStr, count: sessionsByDay[dateStr] ?? 0 });
    current.setDate(current.getDate() + 1);
  }
  return cells;
}

function intensityClass(count: number) {
  if (count === 0) return 'bg-muted';
  if (count === 1) return 'bg-primary/30';
  if (count <= 3) return 'bg-primary/60';
  return 'bg-primary';
}

export const ActivityHeatmap = ({ sessions }: { sessions: SessionSummary[] }) => {
  const cells = buildGrid(sessions);

  return (
    <div
      className="grid gap-0.75"
      style={{
        gridTemplateColumns: `repeat(${Math.ceil(cells.length / 7)}, minmax(0, 1fr))`,
        gridTemplateRows: 'repeat(7, minmax(0, 1fr))',
        gridAutoFlow: 'column',
      }}
    >
      {cells.map(({ date, count }) => (
        <div
          key={date}
          data-testid="heatmap-cell"
          data-count={count}
          title={`${date}: ${count} sessão${count !== 1 ? 'ões' : ''}`}
          className={cn('aspect-square rounded-xs', intensityClass(count))}
        />
      ))}
    </div>
  );
};
