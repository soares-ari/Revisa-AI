import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';

export const AreaChart = ({
  desempenhoPorArea,
}: {
  desempenhoPorArea: Record<string, number>;
}) => {
  const data = Object.entries(desempenhoPorArea)
    .map(([area, percentual]) => ({ area, percentual }))
    .sort((a, b) => b.percentual - a.percentual);

  return (
    <div className="h-48 w-full">
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={data} layout="vertical">
          <XAxis type="number" domain={[0, 100]} tick={{ fontSize: 11 }} />
          <YAxis
            dataKey="area"
            type="category"
            width={140}
            tick={{ fontSize: 11 }}
          />
          <Tooltip
            formatter={(v) => [`${Number(v).toFixed(1)}%`, 'Acertos']}
          />
          <Bar dataKey="percentual" radius={[0, 4, 4, 0]} fill="#0ea5e9" />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
};
