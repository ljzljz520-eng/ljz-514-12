import "leaflet/dist/leaflet.css";
import L from "leaflet";
import { useEffect, useMemo } from "react";
import {
  MapContainer as LeafletMap,
  Marker,
  Polyline,
  Popup,
  TileLayer,
  Tooltip,
  useMap,
} from "react-leaflet";
import marker2x from "leaflet/dist/images/marker-icon-2x.png";
import marker from "leaflet/dist/images/marker-icon.png";
import shadow from "leaflet/dist/images/marker-shadow.png";
import { Button } from "antd";
import { formatDistance, formatDuration, modeColor } from "@/lib/format";
import { useTravelStore, type RouteOption } from "@/stores/useTravelStore";

L.Icon.Default.mergeOptions({
  iconRetinaUrl: marker2x,
  iconUrl: marker,
  shadowUrl: shadow,
});

function FitBounds({ points }: { points: Array<[number, number]> }) {
  const map = useMap();
  useEffect(() => {
    if (points.length < 2) return;
    const bounds = L.latLngBounds(points.map((p) => L.latLng(p[0], p[1])));
    map.fitBounds(bounds.pad(0.15));
  }, [map, points]);
  return null;
}

const LEGEND: Array<{ label: string; color: string; dashed?: boolean }> = [
  { label: "步行", color: modeColor("walk") },
  { label: "公交/大巴", color: modeColor("bus") },
  { label: "轨道交通", color: modeColor("rail") },
  { label: "其他走法", color: "#94a3b8", dashed: true },
];

export default function MapContainer() {
  const nodes = useTravelStore((s) => s.nodes);
  const startId = useTravelStore((s) => s.startId);
  const endId = useTravelStore((s) => s.endId);
  const setStartId = useTravelStore((s) => s.setStartId);
  const setEndId = useTravelStore((s) => s.setEndId);
  const route = useTravelStore((s) => s.route);
  const activeIndex = useTravelStore((s) => s.activeOptionIndex);

  const center: [number, number] = [29.56301, 106.57577];

  const nodeById = useMemo(() => new Map(nodes.map((n) => [n.id, n])), [nodes]);

  const selected: RouteOption | undefined =
    route && route.options[Math.min(activeIndex, route.options.length - 1)];

  const selectedPoints = useMemo(() => {
    if (!selected) return [] as Array<[number, number]>;
    return selected.pathNodes.map((n) => [n.lat, n.lng] as [number, number]);
  }, [selected]);

  const alternatives = useMemo(() => {
    if (!route || !selected) return [] as RouteOption[];
    return route.options.filter((o) => o !== selected);
  }, [route, selected]);

  return (
    <LeafletMap center={center} zoom={12} className="h-full w-full">
      <TileLayer
        attribution="&copy; OpenStreetMap contributors"
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />

      {nodes.map((n) => {
        const isStart = n.id === startId;
        const isEnd = n.id === endId;
        let icon: L.Icon | L.DivIcon | undefined;
        if (isStart) {
          icon = L.divIcon({
            className: "",
            html:
              "<div style='width:18px;height:18px;border-radius:999px;background:#16a34a;border:2px solid white;box-shadow:0 6px 18px rgba(0,0,0,.18)'></div>",
            iconSize: [18, 18],
            iconAnchor: [9, 9],
          });
        } else if (isEnd) {
          icon = L.divIcon({
            className: "",
            html:
              "<div style='width:18px;height:18px;border-radius:999px;background:#dc2626;border:2px solid white;box-shadow:0 6px 18px rgba(0,0,0,.18)'></div>",
            iconSize: [18, 18],
            iconAnchor: [9, 9],
          });
        }
        return (
          <Marker key={n.id} position={[n.lat, n.lng]} {...(icon ? { icon } : {})}>
            <Popup>
              <div className="min-w-[220px]">
                <div className="text-sm font-semibold text-slate-900">{n.name}</div>
                {n.desc ? <div className="mt-1 text-xs text-slate-600">{n.desc}</div> : null}
                <div className="mt-3 grid grid-cols-2 gap-2">
                  <Button size="small" onClick={() => setStartId(n.id)}>
                    设为起点
                  </Button>
                  <Button size="small" type="primary" onClick={() => setEndId(n.id)}>
                    设为终点
                  </Button>
                </div>
              </div>
            </Popup>
          </Marker>
        );
      })}

      {/* 备选走法：灰色虚线，便于看出“另一条路”的走向 */}
      {alternatives.map((o, oi) => (
        <Polyline
          key={`alt-${oi}`}
          positions={o.pathNodes.map((n) => [n.lat, n.lng] as [number, number])}
          pathOptions={{ color: "#94a3b8", weight: 3, opacity: 0.65, dashArray: "6 8" }}
        >
          <Tooltip sticky>
            <span className="text-xs">
              {o.profileLabel}：{formatDuration(o.totalSeconds)} · {formatDistance(o.totalDistanceMeters)}
            </span>
          </Tooltip>
        </Polyline>
      ))}

      {/* 选中方案：每段按通行方式着色 */}
      {selected?.segments.map((seg, i) => {
        const a = nodeById.get(seg.fromId);
        const b = nodeById.get(seg.toId);
        if (!a || !b) return null;
        const color = modeColor(seg.mode);
        return (
          <Polyline
            key={`seg-${i}`}
            positions={[
              [a.lat, a.lng],
              [b.lat, b.lng],
            ]}
            pathOptions={{ color, weight: 6, opacity: 0.95 }}
          >
            <Tooltip sticky>
              <span className="text-xs leading-relaxed">
                <b>{seg.fromName} → {seg.toName}</b>
                <br />
                {seg.modeLabel} · {formatDistance(seg.distanceMeters)} · 合计{" "}
                {formatDuration(seg.totalSeconds)}
                <br />
                {seg.crowdLabel}
                {seg.transfer ? ` · 换乘等候 ${formatDuration(seg.transferSeconds)}` : ""}
                {Math.abs(seg.slopePercent) >= 5
                  ? ` · ${seg.slopePercent >= 0 ? "上坡" : "下坡"} ${Math.abs(seg.slopePercent)}%`
                  : ""}
              </span>
            </Tooltip>
          </Polyline>
        );
      })}

      {selectedPoints.length >= 2 ? <FitBounds points={selectedPoints} /> : null}

      {route ? (
        <div className="pointer-events-auto absolute bottom-4 left-4 z-[1000] rounded-lg border border-slate-200 bg-white/95 px-3 py-2 shadow-md backdrop-blur">
          <div className="text-[11px] font-semibold text-slate-700">路线图例</div>
          <div className="mt-1 space-y-1">
            {LEGEND.map((item) => (
              <div key={item.label} className="flex items-center gap-2 text-[11px] text-slate-600">
                <svg width="26" height="8">
                  <line
                    x1="0"
                    y1="4"
                    x2="26"
                    y2="4"
                    stroke={item.color}
                    strokeWidth="4"
                    strokeDasharray={item.dashed ? "4 4" : undefined}
                    strokeLinecap="round"
                  />
                </svg>
                {item.label}
              </div>
            ))}
          </div>
        </div>
      ) : null}
    </LeafletMap>
  );
}
