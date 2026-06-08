interface Props {
  label: string;
  value: string | number;
  sub?: string;
  accent?: string;
  big?: boolean;
}

export default function MetricCard({ label, value, sub, accent = "#58a6ff", big }: Props) {
  return (
    <div className="bg-panel border border-border rounded-lg px-4 py-3 flex flex-col gap-1">
      <div className="text-muted text-xs uppercase tracking-wide">{label}</div>
      <div
        className="font-mono font-semibold leading-none"
        style={{ color: accent, fontSize: big ? "2.6rem" : "1.6rem" }}
      >
        {value}
      </div>
      {sub && <div className="text-muted text-xs">{sub}</div>}
    </div>
  );
}
