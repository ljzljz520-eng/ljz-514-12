package com.cqu.service;

/**
 * 寻路偏好（策略）。默认使用 COMPREHENSIVE（综合耗时）。
 */
public enum RouteProfile {
    /** 综合耗时：步行基础耗时 + 爬坡代价 + 拥挤延误 + 换乘等候 */
    COMPREHENSIVE,
    /** 最短距离：仅按真实道路距离 */
    DISTANCE,
    /** 最少换乘：尽量选择纯步行/直达连接 */
    LESS_TRANSFER;

    public static RouteProfile fromRaw(String raw) {
        if (raw == null) {
            return COMPREHENSIVE;
        }
        switch (raw.trim().toLowerCase()) {
            case "distance":
            case "shortest":
            case "距离":
                return DISTANCE;
            case "less_transfer":
            case "transfer":
            case "换乘":
                return LESS_TRANSFER;
            case "comprehensive":
            case "time":
            case "":
                return COMPREHENSIVE;
            default:
                return COMPREHENSIVE;
        }
    }

    public String getLabel() {
        switch (this) {
            case DISTANCE:
                return "最短距离";
            case LESS_TRANSFER:
                return "最少换乘";
            case COMPREHENSIVE:
            default:
                return "综合耗时";
        }
    }
}
