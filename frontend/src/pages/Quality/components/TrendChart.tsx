import { Card, Typography } from "antd";
import type { TrendDay } from "../../../api/types";

const { Text } = Typography;

/**
 * 趋势图组件（简化版）
 * 等待 ECharts 依赖安装后替换为 ReactECharts
 */
export default function TrendChart({ data }: { data: TrendDay[] }) {
  if (!data || data.length === 0) {
    return <div style={{ height: 300, display: "flex", alignItems: "center", justifyContent: "center" }}>暂无数据</div>;
  }

  // 计算 SVG 坐标
  const width = 800;
  const height = 250;
  const padding = { top: 20, right: 20, bottom: 30, left: 50 };
  const chartWidth = width - padding.left - padding.right;
  const chartHeight = height - padding.top - padding.bottom;

  // 归一化数据
  const maxValue = 100;
  const xStep = chartWidth / (data.length - 1);

  const getPath = (key: keyof TrendDay, color: string) => {
    const points = data.map((d, i) => {
      const x = padding.left + i * xStep;
      const value = Number(d[key]);
      const y = padding.top + chartHeight - (value / maxValue) * chartHeight;
      return `${x},${y}`;
    });
    return (
      <polyline
        points={points.join(" ")}
        fill="none"
        stroke={color}
        strokeWidth="2"
        strokeLinejoin="round"
      />
    );
  };

  // X 轴标签（每隔 5 天显示一个）
  const xLabels = data
    .filter((_, i) => i % 5 === 0)
    .map((d, i) => {
      const x = padding.left + i * 5 * xStep;
      return (
        <text
          key={d.date}
          x={x}
          y={height - 5}
          textAnchor="middle"
          fontSize="10"
          fill="#666"
        >
          {d.date.substring(5)}
        </text>
      );
    });

  // Y 轴标签
  const yLabels = [0, 25, 50, 75, 100].map((v) => {
    const y = padding.top + chartHeight - (v / maxValue) * chartHeight;
    return (
      <text
        key={v}
        x={padding.left - 5}
        y={y + 3}
        textAnchor="end"
        fontSize="10"
        fill="#666"
      >
        {v}%
      </text>
    );
  });

  return (
    <div>
      <svg width="100%" viewBox={`0 0 ${width} ${height}`}>
        {/* 网格线 */}
        {[0, 25, 50, 75, 100].map((v) => {
          const y = padding.top + chartHeight - (v / maxValue) * chartHeight;
          return (
            <line
              key={v}
              x1={padding.left}
              y1={y}
              x2={width - padding.right}
              y2={y}
              stroke="#f0f0f0"
              strokeDasharray="4"
            />
          );
        })}

        {/* 数据线 */}
        {getPath("pathwayCompletionRate", "#52c41a")}
        {getPath("ruleHitRate", "#faad14")}
        {getPath("qcRectificationRate", "#1890ff")}

        {/* 坐标轴 */}
        <line
          x1={padding.left}
          y1={padding.top}
          x2={padding.left}
          y2={height - padding.bottom}
          stroke="#d9d9d9"
        />
        <line
          x1={padding.left}
          y1={height - padding.bottom}
          x2={width - padding.right}
          y2={height - padding.bottom}
          stroke="#d9d9d9"
        />

        {/* 标签 */}
        {xLabels}
        {yLabels}
      </svg>

      {/* 图例 */}
      <div style={{ display: "flex", justifyContent: "center", gap: 24, marginTop: 8 }}>
        <span>
          <span style={{ display: "inline-block", width: 16, height: 2, background: "#52c41a", marginRight: 4, verticalAlign: "middle" }} />
          <Text style={{ fontSize: 12 }}>路径完成率</Text>
        </span>
        <span>
          <span style={{ display: "inline-block", width: 16, height: 2, background: "#faad14", marginRight: 4, verticalAlign: "middle" }} />
          <Text style={{ fontSize: 12 }}>规则命中率</Text>
        </span>
        <span>
          <span style={{ display: "inline-block", width: 16, height: 2, background: "#1890ff", marginRight: 4, verticalAlign: "middle" }} />
          <Text style={{ fontSize: 12 }}>质控整改率</Text>
        </span>
      </div>
    </div>
  );
}
