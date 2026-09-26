import { create } from "zustand";
import { notification } from "antd";

export type TravelNode = {
  id: string;
  name: string;
  lat: number;
  lng: number;
  type?: string;
  desc?: string;
};

export type RouteProfileKey = "comprehensive" | "distance" | "less_transfer";

export type SegmentInfo = {
  fromId: string;
  toId: string;
  fromName: string;
  toName: string;
  mode: string;
  modeLabel: string;
  distanceMeters: number;
  baseSeconds: number;
  slopeSeconds: number;
  crowdSeconds: number;
  transferSeconds: number;
  totalSeconds: number;
  slopePercent: number;
  climbMeters?: number;
  descentMeters?: number;
  transfer: boolean;
  crowdLevel: number;
  crowdLabel: string;
  note?: string;
};

export type RouteOption = {
  profile: string;
  profileLabel: string;
  selected: boolean;
  recommended: boolean;
  highlight?: string;
  reasons?: string[];
  totalDistanceMeters: number;
  totalSeconds: number;
  baseSeconds: number;
  slopeSeconds: number;
  crowdSeconds: number;
  transferSeconds: number;
  transferCount: number;
  crowdedSegmentCount: number;
  totalClimbMeters: number;
  totalDescentMeters?: number;
  pathNodeIds: string[];
  pathNodes: TravelNode[];
  segments: SegmentInfo[];
};

export type PathResult = {
  startId: string;
  endId: string;
  profile: string;
  profileLabel: string;
  options: RouteOption[];
  summary?: string;
  // 兼容字段（后端继续返回）
  totalDistanceMeters: number;
  totalSeconds: number;
  pathNodeIds: string[];
  pathNodes: TravelNode[];
  segmentDistanceMeters: number[];
};

type State = {
  nodes: TravelNode[];
  nodesLoading: boolean;
  startId?: string;
  endId?: string;
  route?: PathResult;
  routeLoading: boolean;
  selectedNodeId?: string;
  profile: RouteProfileKey;
  activeOptionIndex: number;
};

type Actions = {
  loadNodes: () => Promise<void>;
  setStartId: (id?: string) => void;
  setEndId: (id?: string) => void;
  setProfile: (p: RouteProfileKey) => void;
  setActiveOptionIndex: (i: number) => void;
  swap: () => void;
  clear: () => void;
  setSelectedNodeId: (id?: string) => void;
  fetchRoute: () => Promise<void>;
};

const apiBase = import.meta.env.VITE_API_BASE || "/api";

export const useTravelStore = create<State & Actions>((set, get) => ({
  nodes: [],
  nodesLoading: false,
  routeLoading: false,
  profile: "comprehensive",
  activeOptionIndex: 0,

  loadNodes: async () => {
    if (get().nodesLoading) return;
    set({ nodesLoading: true });
    try {
      const res = await fetch(`${apiBase}/nodes`);
      if (!res.ok) throw new Error("nodes_fetch_failed");
      const data = await res.json();
      if (!Array.isArray(data)) throw new Error("nodes_payload_invalid");
      const nodes = (data as unknown[])
        .map((raw) => {
          const r = (raw ?? {}) as Record<string, unknown>;
          const id = String(r.id ?? "").trim();
          const name = String(r.name ?? "").trim();
          const lat = Number(r.lat);
          const lng = Number(r.lng);
          const type = typeof r.type === "string" ? r.type : undefined;
          const desc = typeof r.desc === "string" ? r.desc : undefined;
          return { id, name, lat, lng, type, desc } satisfies TravelNode;
        })
        .filter((n) => n.id && Number.isFinite(n.lat) && Number.isFinite(n.lng));
      set({ nodes });
    } catch {
      notification.error({ message: "加载节点失败", description: "请检查后端服务是否已启动" });
    } finally {
      set({ nodesLoading: false });
    }
  },

  setStartId: (id) => set({ startId: id, route: undefined }),
  setEndId: (id) => set({ endId: id, route: undefined }),
  setSelectedNodeId: (id) => set({ selectedNodeId: id }),

  setProfile: (p) => {
    // 已经有结果时，切换偏好立即按新偏好重新请求
    const { startId, endId } = get();
    set({ profile: p, activeOptionIndex: 0 });
    if (startId && endId) {
      void get().fetchRoute();
    }
  },

  setActiveOptionIndex: (i) => set({ activeOptionIndex: i }),

  swap: () => {
    const { startId, endId } = get();
    set({ startId: endId, endId: startId, route: undefined });
  },

  clear: () => set({ startId: undefined, endId: undefined, route: undefined, selectedNodeId: undefined }),

  fetchRoute: async () => {
    const { startId, endId, profile } = get();
    if (!startId || !endId) {
      notification.warning({ message: "请选择起点与终点" });
      return;
    }
    set({ routeLoading: true });
    try {
      const qs = new URLSearchParams({ from: startId, to: endId, profile });
      const res = await fetch(`${apiBase}/path?${qs.toString()}`);
      const data = await res.json();
      if (!res.ok) {
        notification.error({ message: "规划失败", description: data?.error || "后端错误" });
        return;
      }
      if (!data?.options || !Array.isArray(data.options) || data.options.length === 0) {
        notification.error({ message: "规划失败", description: "返回结果缺少路线方案" });
        return;
      }
      const selectedIdx = (data.options as unknown[]).findIndex((o) => {
        const obj = (o ?? {}) as Record<string, unknown>;
        return obj.selected === true;
      });
      set({ route: data as PathResult, activeOptionIndex: selectedIdx >= 0 ? selectedIdx : 0 });
    } catch {
      notification.error({ message: "规划失败", description: "网络异常或后端不可用" });
    } finally {
      set({ routeLoading: false });
    }
  },
}));
