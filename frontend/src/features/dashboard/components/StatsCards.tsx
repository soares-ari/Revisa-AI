import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import type { UserStatsResponse } from '../types';

export const StatsCards = ({ stats }: { stats: UserStatsResponse }) => (
  <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
    <Card>
      <CardHeader>
        <CardTitle className="text-sm font-medium text-muted-foreground">
          Questões respondidas
        </CardTitle>
      </CardHeader>
      <CardContent>
        <p className="text-3xl font-bold">{stats.totalQuestoes}</p>
      </CardContent>
    </Card>

    <Card>
      <CardHeader>
        <CardTitle className="text-sm font-medium text-muted-foreground">
          Percentual de acertos
        </CardTitle>
      </CardHeader>
      <CardContent>
        <p className="text-3xl font-bold">{stats.percentualAcertos}%</p>
      </CardContent>
    </Card>

    <Card>
      <CardHeader>
        <CardTitle className="text-sm font-medium text-muted-foreground">
          Sessões finalizadas
        </CardTitle>
      </CardHeader>
      <CardContent>
        <p className="text-3xl font-bold">{stats.totalSessoes}</p>
      </CardContent>
    </Card>
  </div>
);
