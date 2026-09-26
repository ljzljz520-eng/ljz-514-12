import { Button, Divider, Select, Skeleton, Typography } from "antd";
import {
  ArrowDown,
  ArrowLeftRight,
  CheckCircle2,
  Clock,
  Footprints,
  Mountain,
  Repeat,
  Route as RouteIcon,
  Users,
  X,
} from "lucide-react";
import { useMemo, useState } from "react";
import { useTravelStore } from "@/stores/useTravelStore";
import type { RouteSegment, RouteSummary } from "@/stores/useTravelStore";
import {
  compareRoutes,
  crowdingColor,
  crowdingLabel,
  formatDistance,
  formatMinutes,
  metricsOf,
} from "@/lib/routeCompare";

const { Text } = Typography;

function MetricChip({ icon, label, value }: { icon: React.ReactNode; label: string; value: string }) {
  return (
    <div className="rounded-lg border border-slate-200 bg-slate-50 px-2 py-1.5">
      <div className="flex items-center gap-1 text-[11px] text-slate-500">
        {icon}
        {label}
      </div>
      <div className="mt-0.5 text-sm font-semibold text-slate-900">{value}</div>
    </div>
  );
}

function SegmentRow({ segment }: { segment: RouteSegment }) {
  const crowdColor = crowdingColor(segment.crowding);
  return (
    <div className="ml-3 border-l-2 border-slate-200 py-1.5 pl-3">
      <div className="flex items-center gap-1.5 text-xs text-slate-600">
        <ArrowDown className="h-3 w-3 text-slate-400" />
        <span>
          步行 {formatMinutes(segment.walkMinutes)} · {formatDistance(segment.distanceMeters)}
        </span>
      </div>
      <div className="mt-1 flex flex-wrap gap-1">
        <span className="inline-flex items-center gap-0.5 rounded-full bg-slate-100 px-1.5 py-0.5 text-[11px] text-slate-600">
          <Mountain className="h-3 w-3" />
          坡度 {segment.slopePercent.toFixed(1)}%
        </span>
        <span
          className="inline-flex items-center gap-0.5 rounded-full px-1.5 py-0.5 text-[11px] text-white"
          style={{ backgroundColor: crowdColor }}
        >
          <Users className="h-3 w-3" />
          {crowdingLabel(segment.crowding)}
        </span>
        {segment.transfers > 0 ? (
          <span className="inline-flex items-center gap-0.5 rounded-full bg-violet-100 px-1.5 py-0.5 text-[11px] text-violet-700">
            <Repeat className="h-3 w-3" />
            需换乘 {segment.transfers} 次
          </span>
        ) : null}
      </div>
    </div>
  );
}

function AlternativeCard({
  index,
  alt,
  nodeName,
}: {
  index: number;
  alt: RouteSummary;
  nodeName: (id: string) => string;
}) {
  const route = useTravelStore((s) => s.route);
  const comparison = useMemo(() => (route ? compareRoutes(metricsOf(route), alt) : null), [route, alt]);
  if (!route || !comparison) return null;

  const chain = alt.pathNodeIds.map(nodeName).join(" → ");
  return (
    <div className="rounded-lg border border-slate-200 p-2">
      <div className="flex items-center justify-between gap-2">
        <div className="text-xs font-semibold text-slate-700">备选 {index + 1}</div>
        <div className="text-xs text-slate-500">综合耗时 {formatMinutes(alt.totalCostMinutes)}</div>
      </div>
      <div className="mt-1 text-xs text-slate-600 break-all">{chain}</div>
      <div className="mt-1 text-[11px] text-slate-500">
        {formatDistance(alt.totalDistanceMeters)} · 步行 {formatMinutes(alt.totalWalkMinutes)} · 换乘{" "}
        {alt.totalTransfers} 次 · 最大坡度 {alt.maxSlopePercent.toFixed(1)}% · {crowdingLabel(alt.avgCrowding)}
      </div>
      <div className="mt-1.5 space-y-0.5">
        {comparison.reasons.map((r) => (
          <div key={r} className="flex items-start gap-1 text-[11px] text-slate-600">
            <X className="mt-0.5 h-3 w-3 shrink-0 text-rose-500" />
            <span>备选{r}</span>
          </div>
        ))}
        {comparison.altAdvantages.map((a) => (
          <div key={a} className="flex items-start gap-1 text-[11px] text-slate-500">
            <CheckCircle2 className="mt-0.5 h-3 w-3 shrink-0 text-emerald-500" />
            <span>备选虽{a}，但综合耗时仍更高</span>
          </div>
        ))}
      </div>
    </div>
  );
}

export default function ControlPanel() {
  const nodes = useTravelStore((s) => s.nodes);
  const nodesLoading = useTravelStore((s) => s.nodesLoading);
  const startId = useTravelStore((s) => s.startId);
  const endId = useTravelStore((s) => s.endId);
  const route = useTravelStore((s) => s.route);
  const routeLoading = useTravelStore((s) => s.routeLoading);
  const setStartId = useTravelStore((s) => s.setStartId);
  const setEndId = useTravelStore((s) => s.setEndId);
  const swap = useTravelStore((s) => s.swap);
  const clear = useTravelStore((s) => s.clear);
  const fetchRoute = useTravelStore((s) => s.fetchRoute);

  const [keyword, setKeyword] = useState<string>("");

  const options = useMemo(() => {
    const k = keyword.trim().toLowerCase();
    const list = k ? nodes.filter((n) => (n.name || "").toLowerCase().includes(k)) : nodes;
    return list.map((n) => ({ label: n.name || n.id, value: n.id }));
  }, [keyword, nodes]);

  const nodeName = useMemo(() => {
    const map = new Map(nodes.map((n) => [n.id, n.name || n.id]));
    return (id: string) => map.get(id) ?? id;
  }, [nodes]);

  return (
    <div className="h-full flex flex-col p-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <div className="text-base font-semibold text-slate-900">重庆旅游线路规划</div>
          <div className="mt-1 flex items-center gap-2">
            <span className="inline-flex items-center rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-700">数据源：nodes.csv</span>
            <span className="inline-flex items-center rounded-full bg-blue-50 px-2 py-0.5 text-xs text-blue-700">权重：综合耗时</span>
          </div>
        </div>
        <Button type="text" onClick={() => clear()} icon={<X className="h-4 w-4" />} />
      </div>

      <Divider className="my-3" />

      {nodesLoading ? (
        <Skeleton active paragraph={{ rows: 6 }} />
      ) : (
        <>
          <div className="space-y-2">
            <Text type="secondary">起点</Text>
            <Select
              showSearch
              value={startId}
              placeholder="选择起点"
              options={options}
              className="w-full"
              filterOption={false}
              onSearch={setKeyword}
              onChange={(v) => setStartId(v)}
              allowClear
            />
          </div>

          <div className="mt-3 space-y-2">
            <Text type="secondary">终点</Text>
            <Select
              showSearch
              value={endId}
              placeholder="选择终点"
              options={options}
              className="w-full"
              filterOption={false}
              onSearch={setKeyword}
              onChange={(v) => setEndId(v)}
              allowClear
            />
          </div>

          <div className="mt-4 grid grid-cols-2 gap-2">
            <Button onClick={() => swap()} icon={<ArrowLeftRight className="h-4 w-4" />}>
              交换
            </Button>
            <Button type="primary" loading={routeLoading} onClick={() => fetchRoute()} icon={<RouteIcon className="h-4 w-4" />}>
              开始规划
            </Button>
          </div>

          <Divider className="my-4" />

          <div className="flex-1 overflow-auto rounded-xl border border-slate-200 bg-white p-3">
            <div className="flex items-center justify-between">
              <div className="text-sm font-semibold text-slate-900">路径结果</div>
              {route ? (
                <div className="text-xs font-medium text-blue-700">综合耗时 {formatMinutes(route.totalCostMinutes)}</div>
              ) : null}
            </div>

            {route ? (
              <div className="mt-3">
                <div className="grid grid-cols-3 gap-1.5">
                  <MetricChip icon={<Clock className="h-3 w-3" />} label="综合耗时" value={formatMinutes(route.totalCostMinutes)} />
                  <MetricChip icon={<Footprints className="h-3 w-3" />} label="纯步行" value={formatMinutes(route.totalWalkMinutes)} />
                  <MetricChip icon={<RouteIcon className="h-3 w-3" />} label="总距离" value={formatDistance(route.totalDistanceMeters)} />
                  <MetricChip icon={<Repeat className="h-3 w-3" />} label="换乘" value={`${route.totalTransfers} 次`} />
                  <MetricChip icon={<Mountain className="h-3 w-3" />} label="最大坡度" value={`${route.maxSlopePercent.toFixed(1)}%`} />
                  <MetricChip icon={<Users className="h-3 w-3" />} label="平均拥挤" value={crowdingLabel(route.avgCrowding)} />
                </div>

                <div className="mt-3 space-y-2">
                  {route.pathNodes.map((n, idx) => (
                    <div key={n.id}>
                      {idx > 0 && route.segments?.[idx - 1] ? <SegmentRow segment={route.segments[idx - 1]} /> : null}
                      <div className="rounded-lg border border-slate-200 p-2 hover:bg-slate-50 transition-colors">
                        <div className="flex items-center justify-between">
                          <div className="text-sm text-slate-900">
                            <span className="mr-2 inline-flex h-6 w-6 items-center justify-center rounded-full bg-slate-900 text-white text-xs">
                              {idx + 1}
                            </span>
                            {n.name}
                          </div>
                          <div className="text-xs text-slate-500">{n.type || ""}</div>
                        </div>
                        {n.desc ? <div className="mt-1 text-xs text-slate-600">{n.desc}</div> : null}
                      </div>
                    </div>
                  ))}
                </div>

                {route.alternatives && route.alternatives.length > 0 ? (
                  <div className="mt-4">
                    <div className="text-xs font-semibold text-slate-700">为什么推荐这条路？</div>
                    <div className="mt-1 text-[11px] text-slate-500">
                      推荐路线综合耗时最低（步行耗时经坡度、拥挤修正并计入换乘惩罚），与备选路线对比如下：
                    </div>
                    <div className="mt-2 space-y-2">
                      {route.alternatives.map((alt, i) => (
                        <AlternativeCard key={alt.pathNodeIds.join(">")} index={i} alt={alt} nodeName={nodeName} />
                      ))}
                    </div>
                  </div>
                ) : null}
              </div>
            ) : (
              <div className="mt-3 text-sm text-slate-600">选择起点与终点后开始规划。</div>
            )}
          </div>
        </>
      )}
    </div>
  );
}
