export function formatDistance(m: number): string {
  if (!Number.isFinite(m)) return "-";
  if (m < 1000) return `${Math.round(m)} m`;
  return `${(m / 1000).toFixed(2)} km`;
}

export function formatDuration(seconds: number): string {
  if (!Number.isFinite(seconds) || seconds < 0) return "-";
  if (seconds < 60) return `${Math.round(seconds)} 秒`;
  const min = Math.round(seconds / 60);
  if (min < 60) return `${min} 分钟`;
  const h = Math.floor(min / 60);
  const m = min % 60;
  return m === 0 ? `${h} 小时` : `${h} 小时 ${m} 分`;
}

/** 各通行方式在地图上的配色 */
export function modeColor(mode?: string): string {
  switch ((mode || "walk").toLowerCase()) {
    case "bus":
      return "#7c3aed";
    case "rail":
      return "#0891b2";
    case "cableway":
      return "#db2777";
    case "escalator":
      return "#d97706";
    case "walk":
    default:
      return "#2563eb";
  }
}

export function crowdColor(level: number): string {
  switch (level) {
    case 4:
      return "bg-red-100 text-red-700 border-red-200";
    case 3:
      return "bg-orange-100 text-orange-700 border-orange-200";
    case 2:
      return "bg-amber-100 text-amber-700 border-amber-200";
    case 1:
      return "bg-lime-100 text-lime-700 border-lime-200";
    default:
      return "bg-emerald-100 text-emerald-700 border-emerald-200";
  }
}
