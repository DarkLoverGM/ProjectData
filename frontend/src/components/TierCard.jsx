export default function TierCard({ tier, channelName, selected, onSelect }) {
  return (
    <div
      className="card"
      style={{
        borderColor: selected ? "#ef4444" : undefined,
        display: "flex",
        flexDirection: "column",
        gap: 10,
      }}
    >
      <div className="flex-between">
        <span className="badge badge-purple">{channelName || tier.channelId}</span>
        <span className={`badge ${tier.active ? "badge-green" : "badge-red"}`}>
          {tier.active ? "Đang mở" : "Tạm ngưng"}
        </span>
      </div>
      <h3>{tier.name}</h3>
      <div style={{ fontSize: "1.4rem", fontWeight: 800, color: "#fca5a5" }}>
        {Number(tier.price).toLocaleString("vi-VN")} ₫
        <span className="muted" style={{ fontSize: "0.85rem", fontWeight: 400 }}> / tháng</span>
      </div>
      <p className="muted" style={{ flex: 1 }}>
        {tier.perks || "Quyền lợi hội viên đặc biệt của kênh."}
      </p>
      <button
        type="button"
        disabled={!tier.active}
        className={`btn ${selected ? "btn-success" : "btn-primary"}`}
        onClick={() => onSelect(tier)}
      >
        {selected ? "✓ Đang chọn gói này" : "Chọn gói hội viên"}
      </button>
    </div>
  );
}
