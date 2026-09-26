import type { PathResult, RouteSummary } from "@/stores/useTravelStore";

export function formatMinutes(min: number): string {
  if (!Number.isFinite(min)) return "-";
  const rounded = Math.round(min);
  if (rounded < 60) return `${rounded} 分钟`;
  const h = Math.floor(rounded / 60);
  const m = rounded % 60;
  return m === 0 ? `${h} 小时` : `${h} 小时 ${m} 分钟`;
}

export function formatDistance(meters: number): string {
  if (!Number.isFinite(meters)) return "-";
  if (meters < 1000) return `${Math.round(meters)} m`;
  return `${(meters / 1000).toFixed(1)} km`;
}

export function crowdingLabel(level: number): string {
  if (level <= 1.5) return "清净";
  if (level <= 2.5) return "较清净";
  if (level <= 3.5) return "一般";
  if (level <= 4.5) return "较拥挤";
  return "非常拥挤";
}

/** 拥挤程度对应的颜色（地图线段与标签共用） */
export function crowdingColor(level: number): string {
  if (level <= 2) return "#16a34a";
  if (level <= 3) return "#d97706";
  return "#dc2626";
}

export type RouteMetrics = Pick<
  RouteSummary,
  | "totalDistanceMeters"
  | "totalWalkMinutes"
  | "totalCostMinutes"
  | "totalTransfers"
  | "maxSlopePercent"
  | "avgCrowding"
>;

export function metricsOf(route: PathResult): RouteMetrics {
  return {
    totalDistanceMeters: route.totalDistanceMeters,
    totalWalkMinutes: route.totalWalkMinutes,
    totalCostMinutes: route.totalCostMinutes,
    totalTransfers: route.totalTransfers,
    maxSlopePercent: route.maxSlopePercent,
    avgCrowding: route.avgCrowding,
  };
}

export type RouteComparison = {
  /** 推荐路线胜出的原因 */
  reasons: string[];
  /** 备选路线仍有的优势（取舍说明） */
  altAdvantages: string[];
};

/** 对比推荐路线与备选路线，生成人类可读的取舍说明 */
export function compareRoutes(rec: RouteMetrics, alt: RouteMetrics): RouteComparison {
  const reasons: string[] = [];
  const altAdvantages: string[] = [];

  const costDiff = alt.totalCostMinutes - rec.totalCostMinutes;
  if (costDiff >= 1) {
    reasons.push(`综合耗时多 ${formatMinutes(costDiff)}`);
  }

  const transferDiff = alt.totalTransfers - rec.totalTransfers;
  if (transferDiff > 0) {
    reasons.push(`多换乘 ${transferDiff} 次`);
  } else if (transferDiff < 0) {
    altAdvantages.push(`少换乘 ${-transferDiff} 次`);
  }

  const slopeDiff = alt.maxSlopePercent - rec.maxSlopePercent;
  if (slopeDiff >= 1) {
    reasons.push(`最大坡度更陡（${alt.maxSlopePercent.toFixed(1)}% 对 ${rec.maxSlopePercent.toFixed(1)}%）`);
  } else if (slopeDiff <= -1) {
    altAdvantages.push(`坡更缓（最大 ${alt.maxSlopePercent.toFixed(1)}%）`);
  }

  const crowdDiff = alt.avgCrowding - rec.avgCrowding;
  if (crowdDiff >= 0.5) {
    reasons.push(`路段更拥挤（平均 ${alt.avgCrowding.toFixed(1)} 级 对 ${rec.avgCrowding.toFixed(1)} 级）`);
  } else if (crowdDiff <= -0.5) {
    altAdvantages.push(`更清净（平均拥挤 ${alt.avgCrowding.toFixed(1)} 级）`);
  }

  const distDiff = alt.totalDistanceMeters - rec.totalDistanceMeters;
  if (distDiff >= 100) {
    reasons.push(`距离多 ${formatDistance(distDiff)}`);
  } else if (distDiff <= -100) {
    altAdvantages.push(`距离短 ${formatDistance(-distDiff)}`);
  }

  const walkDiff = alt.totalWalkMinutes - rec.totalWalkMinutes;
  if (walkDiff >= 2) {
    reasons.push(`纯步行多 ${formatMinutes(walkDiff)}`);
  } else if (walkDiff <= -2) {
    altAdvantages.push(`纯步行少 ${formatMinutes(-walkDiff)}`);
  }

  if (reasons.length === 0 && altAdvantages.length === 0) {
    reasons.push("各项指标与推荐路线接近，但综合耗时仍略高");
  }
  return { reasons, altAdvantages };
}
