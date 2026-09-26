import { Alert, Button, Divider, Select, Skeleton, Tag, Typography } from "antd";
import { ArrowLeftRight, Footprints, Info, Route, X } from "lucide-react";
import { useMemo, useState } from "react";
import { crowdColor, formatDistance, formatDuration, modeColor } from "@/lib/format";
import { useTravelStore, type RouteOption, type RouteProfileKey, type SegmentInfo } from "@/stores/useTravelStore";

const { Text } = Typography;

const PROFILE_OPTIONS: Array<{ value: RouteProfileKey; label: string; hint: string }> = [
  { value: "comprehensive", label: "综合耗时", hint: "步行+爬坡+拥挤+换乘" },
  { value: "distance", label: "最短距离", hint: "只看道路距离" },
  { value: "less_transfer", label: "最少换乘", hint: "优先步行直达" },
];

export default function ControlPanel() {
  const nodes = useTravelStore((s) => s.nodes);
  const nodesLoading = useTravelStore((s) => s.nodesLoading);
  const startId = useTravelStore((s) => s.startId);
  const endId = useTravelStore((s) => s.endId);
  const route = useTravelStore((s) => s.route);
  const routeLoading = useTravelStore((s) => s.routeLoading);
  const profile = useTravelStore((s) => s.profile);
  const activeOption = useTravelStore((s) => s.activeOptionIndex);
  const setStartId = useTravelStore((s) => s.setStartId);
  const setEndId = useTravelStore((s) => s.setEndId);
  const setProfile = useTravelStore((s) => s.setProfile);
  const setActiveOption = useTravelStore((s) => s.setActiveOptionIndex);
  const swap = useTravelStore((s) => s.swap);
  const clear = useTravelStore((s) => s.clear);
  const fetchRoute = useTravelStore((s) => s.fetchRoute);

  const [keyword, setKeyword] = useState<string>("");

  const options = useMemo(() => {
    const k = keyword.trim().toLowerCase();
    const list = k ? nodes.filter((n) => (n.name || "").toLowerCase().includes(k)) : nodes;
    return list.map((n) => ({ label: n.name || n.id, value: n.id }));
  }, [keyword, nodes]);

  // 请求结果变化（新路线）时，store 会把序号重置为后端标记的 selected 方案
  const optionIndex = useMemo(() => {
    if (!route) return 0;
    return activeOption < route.options.length ? activeOption : 0;
  }, [route, activeOption]);

  const selected: RouteOption | undefined = route?.options[optionIndex];

  return (
    <div className="h-full flex flex-col p-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <div className="text-base font-semibold text-slate-900">重庆旅游线路规划</div>
          <div className="mt-1 flex flex-wrap items-center gap-2">
            <span className="inline-flex items-center rounded-full bg-slate-100 px-2 py-0.5 text-xs text-slate-700">
              多维真实权重
            </span>
            <span className="inline-flex items-center rounded-full bg-blue-50 px-2 py-0.5 text-xs text-blue-700">
              默认：综合耗时
            </span>
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

          <div className="mt-3 space-y-2">
            <Text type="secondary">寻路偏好</Text>
            <div className="grid grid-cols-3 gap-1.5">
              {PROFILE_OPTIONS.map((p) => {
                const active = profile === p.value;
                return (
                  <button
                    key={p.value}
                    type="button"
                    onClick={() => setProfile(p.value)}
                    className={`rounded-lg border px-2 py-1.5 text-left transition-colors ${
                      active
                        ? "border-blue-500 bg-blue-50 ring-1 ring-blue-500"
                        : "border-slate-200 bg-white hover:border-slate-300"
                    }`}
                  >
                    <div className={`text-xs font-semibold ${active ? "text-blue-700" : "text-slate-800"}`}>
                      {p.label}
                    </div>
                    <div className="mt-0.5 text-[10px] leading-tight text-slate-500">{p.hint}</div>
                  </button>
                );
              })}
            </div>
          </div>

          <div className="mt-4 grid grid-cols-2 gap-2">
            <Button onClick={() => swap()} icon={<ArrowLeftRight className="h-4 w-4" />}>
              交换
            </Button>
            <Button type="primary" loading={routeLoading} onClick={() => fetchRoute()} icon={<Route className="h-4 w-4" />}>
              开始规划
            </Button>
          </div>

          <Divider className="my-4" />

          <div className="flex-1 overflow-auto rounded-xl border border-slate-200 bg-white p-3">
            <div className="flex items-center justify-between">
              <div className="text-sm font-semibold text-slate-900">路径结果</div>
              {selected ? (
                <div className="text-xs text-slate-500">
                  {formatDistance(selected.totalDistanceMeters)} · {formatDuration(selected.totalSeconds)}
                </div>
              ) : null}
            </div>

            {route && selected ? (
              <div className="mt-3 space-y-3">
                {/* 方案对比卡片 */}
                {route.options.length > 1 ? (
                  <div className="space-y-1.5">
                    {route.options.map((o, idx) => (
                      <OptionCard
                        key={`${o.profile}-${idx}`}
                        option={o}
                        active={idx === optionIndex}
                        onClick={() => setActiveOption(idx)}
                      />
                    ))}
                  </div>
                ) : null}

                {/* 为什么推荐这条路 */}
                {route.summary ? (
                  <Alert
                    type="info"
                    showIcon
                    icon={<Info className="h-4 w-4" />}
                    className="text-xs"
                    message={<span className="text-xs leading-relaxed">{route.summary}</span>}
                  />
                ) : null}

                {/* 当前方案的耗时构成 */}
                <DurationBreakdown option={selected} />

                {/* 逐段明细 */}
                <div className="space-y-2">
                  {selected.pathNodes.map((n, idx) => {
                    const seg: SegmentInfo | undefined = selected.segments[idx - 1];
                    return (
                      <div key={`${n.id}-${idx}`} className="rounded-lg border border-slate-200 p-2 hover:bg-slate-50 transition-colors">
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
                        {seg ? <SegmentDetail seg={seg} /> : null}
                      </div>
                    );
                  })}
                </div>
              </div>
            ) : (
              <div className="mt-3 space-y-2 text-sm text-slate-600">
                <p>选择起点与终点后开始规划，默认按综合耗时找路。</p>
                <p className="text-xs text-slate-500">
                  综合耗时 = 步行/乘车基础时间 + 爬坡附加（每爬升 1 米约 4 秒）+ 拥挤延误 + 换乘等候。
                </p>
                <p className="text-xs text-slate-500">
                  结果会同时给出最短距离、最少换乘等其他走法，并解释这条路为什么更合适。
                </p>
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
}

function OptionCard({ option, active, onClick }: { option: RouteOption; active: boolean; onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`w-full rounded-lg border p-2 text-left transition-colors ${
        active ? "border-blue-500 bg-blue-50/60 ring-1 ring-blue-500" : "border-slate-200 bg-white hover:bg-slate-50"
      }`}
    >
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-1.5">
          <span className={`text-xs font-semibold ${active ? "text-blue-700" : "text-slate-800"}`}>
            {option.profileLabel}
          </span>
          {option.recommended ? (
            <Tag color="blue" className="!mr-0 !text-[10px] !leading-4 !px-1.5">
              推荐
            </Tag>
          ) : null}
        </div>
        <div className="text-[11px] text-slate-500">
          {formatDuration(option.totalSeconds)} · {formatDistance(option.totalDistanceMeters)}
        </div>
      </div>
      {option.highlight ? <div className="mt-1 text-[11px] text-slate-600">{option.highlight}</div> : null}
      {!option.recommended && option.reasons && option.reasons.length > 0 ? (
        <ul className="mt-1 list-disc pl-4 text-[11px] leading-relaxed text-slate-500">
          {option.reasons.map((r, i) => (
            <li key={i}>{r}</li>
          ))}
        </ul>
      ) : null}
    </button>
  );
}

function DurationBreakdown({ option }: { option: RouteOption }) {
  const rows = [
    { key: "base", label: "步行/乘车", value: option.baseSeconds, color: "bg-slate-400" },
    { key: "slope", label: "坡度附加", value: option.slopeSeconds, color: "bg-amber-500" },
    { key: "crowd", label: "拥挤延误", value: option.crowdSeconds, color: "bg-orange-500" },
    { key: "transfer", label: "换乘等候", value: option.transferSeconds, color: "bg-purple-500" },
  ].filter((r) => r.value > 0);
  const total = Math.max(1, rows.reduce((s, r) => s + r.value, 0));

  return (
    <div className="rounded-lg border border-slate-200 bg-slate-50/60 p-2">
      <div className="flex items-center justify-between text-xs">
        <span className="font-semibold text-slate-700">综合耗时构成</span>
        <span className="text-slate-500">
          {option.transferCount > 0 ? `${option.transferCount} 次换乘 · ` : "无需换乘 · "}
          爬升 {Math.round(option.totalClimbMeters)} m
          {option.totalDescentMeters ? ` · 下降 ${Math.round(option.totalDescentMeters)} m` : ""}
        </span>
      </div>
      <div className="mt-2 flex h-2.5 w-full overflow-hidden rounded-full bg-slate-200">
        {rows.map((r) => (
          <div
            key={r.key}
            className={`${r.color} h-full`}
            style={{ width: `${(r.value / total) * 100}%` }}
            title={`${r.label} ${formatDuration(r.value)}`}
          />
        ))}
      </div>
      <div className="mt-1.5 flex flex-wrap gap-x-3 gap-y-1">
        {rows.map((r) => (
          <span key={r.key} className="inline-flex items-center gap-1 text-[10px] text-slate-600">
            <span className={`${r.color} inline-block h-2 w-2 rounded-full`} />
            {r.label} {formatDuration(r.value)}
          </span>
        ))}
      </div>
    </div>
  );
}

function SegmentDetail({ seg }: { seg: SegmentInfo }) {
  const isWalk = seg.mode === "walk";
  return (
    <div className="mt-2 rounded-md bg-slate-50 p-2 text-xs">
      <div className="flex flex-wrap items-center gap-1.5">
        <span
          className="inline-flex items-center gap-1 rounded-full px-2 py-0.5 font-medium text-white"
          style={{ backgroundColor: modeColor(seg.mode) }}
        >
          {isWalk ? <Footprints className="h-3 w-3" /> : null}
          {seg.modeLabel}
        </span>
        <span className="rounded-full border border-slate-200 bg-white px-2 py-0.5 text-slate-600">
          {formatDistance(seg.distanceMeters)}
        </span>
        <span className="rounded-full border border-slate-200 bg-white px-2 py-0.5 text-slate-600">
          {isWalk ? "步行" : "在途"}约 {formatDuration(seg.baseSeconds)}
        </span>
        <span className={`rounded-full border px-2 py-0.5 ${crowdColor(seg.crowdLevel)}`}>{seg.crowdLabel}</span>
        {seg.transfer ? (
          <span className="rounded-full border border-purple-200 bg-purple-100 px-2 py-0.5 text-purple-700">
            需换乘 · 等候 {formatDuration(seg.transferSeconds)}
          </span>
        ) : null}
        {Math.abs(seg.slopePercent) >= 5 ? (
          <span className="rounded-full border border-amber-200 bg-amber-100 px-2 py-0.5 text-amber-700">
            {seg.slopePercent >= 0 ? "上坡" : "下坡"} {Math.abs(seg.slopePercent)}%
            （约 {Math.round(seg.slopePercent >= 0 ? seg.climbMeters ?? 0 : seg.descentMeters ?? 0)} m，
            +{formatDuration(seg.slopeSeconds)}）
          </span>
        ) : null}
      </div>
      <div className="mt-1.5 flex flex-wrap gap-x-3 text-[11px] text-slate-500">
        <span>
          耗时合计 <span className="font-semibold text-slate-700">{formatDuration(seg.totalSeconds)}</span>
        </span>
        {seg.crowdSeconds > 0 ? <span>拥挤延误 +{formatDuration(seg.crowdSeconds)}</span> : null}
      </div>
      {seg.note ? <div className="mt-1 text-[11px] leading-relaxed text-slate-500">{seg.note}</div> : null}
    </div>
  );
}
