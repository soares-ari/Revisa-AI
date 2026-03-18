import { useState } from 'react';
import { X } from 'lucide-react';
import { useAreas } from '../hooks/useAreas';

interface AreasTagsProps {
  value: string[];
  onChange: (areas: string[]) => void;
}

export const AreasTags = ({ value, onChange }: AreasTagsProps) => {
  const [search, setSearch] = useState('');
  const { data: areas = [] } = useAreas();

  const filtered = areas.filter(
    (a) => a.toLowerCase().includes(search.toLowerCase()) && !value.includes(a)
  );

  const add = (area: string) => {
    if (value.length >= 10) return;
    onChange([...value, area]);
  };

  const remove = (area: string) => {
    onChange(value.filter((a) => a !== area));
  };

  return (
    <div className="flex flex-col gap-2">
      <input
        type="text"
        role="textbox"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        placeholder="Buscar área..."
        className="rounded-md border px-3 py-2 text-sm"
      />

      {filtered.length > 0 && (
        <ul className="rounded-md border bg-background shadow-sm">
          {filtered.map((area) => (
            <li
              key={area}
              onClick={() => add(area)}
              className="cursor-pointer px-3 py-2 text-sm hover:bg-muted"
            >
              {area}
            </li>
          ))}
        </ul>
      )}

      {value.length > 0 && (
        <div className="flex flex-wrap gap-2">
          {value.map((area) => (
            <span
              key={area}
              className="flex items-center gap-1 rounded-full bg-primary/10 px-3 py-1 text-xs"
            >
              {area}
              <button
                type="button"
                aria-label={`remover ${area}`}
                onClick={() => remove(area)}
                className="ml-1"
              >
                <X className="h-3 w-3" />
              </button>
            </span>
          ))}
        </div>
      )}
    </div>
  );
};
