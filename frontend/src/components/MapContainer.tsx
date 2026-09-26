import "leaflet/dist/leaflet.css";
import L from "leaflet";
import { useEffect, useMemo } from "react";
import { MapContainer as LeafletMap, Marker, Polyline, Popup, TileLayer, useMap } from "react-leaflet";
import marker2x from "leaflet/dist/images/marker-icon-2x.png";
import marker from "leaflet/dist/images/marker-icon.png";
import shadow from "leaflet/dist/images/marker-shadow.png";
import { Button } from "antd";
import { useTravelStore } from "@/stores/useTravelStore";
import { crowdingColor } from "@/lib/routeCompare";

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

function RouteLegend() {
  return (
    <div className="absolute bottom-4 right-4 z-[1000] rounded-lg border border-slate-200 bg-white/95 px-3 py-2 shadow-sm">
      <div className="text-[11px] font-semibold text-slate-700">路段拥挤程度</div>
      <div className="mt-1 space-y-1 text-[11px] text-slate-600">
        <div className="flex items-center gap-2">
          <span className="inline-block h-1 w-6 rounded" style={{ backgroundColor: crowdingColor(1) }} />
          清净
        </div>
        <div className="flex items-center gap-2">
          <span className="inline-block h-1 w-6 rounded" style={{ backgroundColor: crowdingColor(3) }} />
          一般
        </div>
        <div className="flex items-center gap-2">
          <span className="inline-block h-1 w-6 rounded" style={{ backgroundColor: crowdingColor(5) }} />
          拥挤
        </div>
        <div className="flex items-center gap-2">
          <span className="inline-block h-0 w-6 border-t-2 border-dashed border-slate-500" />
          需换乘
        </div>
      </div>
    </div>
  );
}

export default function MapContainer() {
  const nodes = useTravelStore((s) => s.nodes);
  const startId = useTravelStore((s) => s.startId);
  const endId = useTravelStore((s) => s.endId);
  const setStartId = useTravelStore((s) => s.setStartId);
  const setEndId = useTravelStore((s) => s.setEndId);
  const route = useTravelStore((s) => s.route);

  const center: [number, number] = [29.56301, 106.57577];

  const nodeById = useMemo(() => new Map(nodes.map((n) => [n.id, n])), [nodes]);

  const routePoints = useMemo(() => {
    if (!route) return [] as Array<[number, number]>;
    return route.pathNodes.map((n) => [n.lat, n.lng] as [number, number]);
  }, [route]);

  const segmentLines = useMemo(() => {
    if (!route || !route.segments) return [];
    return route.segments
      .map((seg) => {
        const a = nodeById.get(seg.fromId);
        const b = nodeById.get(seg.toId);
        if (!a || !b) return null;
        return {
          key: `${seg.fromId}->${seg.toId}`,
          positions: [
            [a.lat, a.lng],
            [b.lat, b.lng],
          ] as Array<[number, number]>,
          color: crowdingColor(seg.crowding),
          dashed: seg.transfers > 0,
        };
      })
      .filter((s): s is NonNullable<typeof s> => s !== null);
  }, [route, nodeById]);

  return (
    <div className="relative h-full w-full">
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

        {segmentLines.length > 0
          ? segmentLines.map((s) => (
              <Polyline
                key={s.key}
                positions={s.positions}
                pathOptions={{
                  color: s.color,
                  weight: 5,
                  opacity: 0.9,
                  dashArray: s.dashed ? "8 8" : undefined,
                }}
              />
            ))
          : null}
        {routePoints.length >= 2 ? <FitBounds points={routePoints} /> : null}
      </LeafletMap>

      {segmentLines.length > 0 ? <RouteLegend /> : null}
    </div>
  );
}
