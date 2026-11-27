import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { tierService } from "../../services/tierService";
import { userService } from "../../services/userService";
import { orderService } from "../../services/orderService";
import { extractErrorMessage } from "../../services/api";
import TierCard from "../../components/TierCard";

export default function MembershipPage() {
  const navigate = useNavigate();

  const [tiers, setTiers] = useState([]);
  const [users, setUsers] = useState([]);
  const [selectedTier, setSelectedTier] = useState(null);
  const [userId, setUserId] = useState("user-1");
  const [months, setMonths] = useState(1);
  const [provider, setProvider] = useState("MOMO");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  const usersMap = Object.fromEntries(users.map((u) => [u.id, u.channelName]));

  useEffect(() => {
    async function load() {
      try {
        setLoading(true);
        const [tierData, userData] = await Promise.all([
          tierService.getAll({ activeOnly: true }),
          userService.getAll(),
        ]);
        setTiers(tierData);
        setUsers(userData);
        if (tierData.length > 0) setSelectedTier(tierData[0]);
        if (userData.length > 0) {
          const defaultUser = userData.find((u) => u.role === "USER") || userData[0];
          setUserId(defaultUser.id);
        }
      } catch (err) {
        setError(extractErrorMessage(err, "Không thể tải danh sách gói hội viên"));
      } finally {
        setLoading(false);
      }
    }
    load();
  }, []);

  const totalAmount = selectedTier ? Number(selectedTier.price) * Number(months || 0) : 0;

  async function handleCheckout(e) {
    e.preventDefault();
    if (!selectedTier) {
      setError("Vui lòng chọn một gói hội viên!");
      return;
    }
    try {
      setSubmitting(true);
      setError("");
      const order = await orderService.create({
        userId,
        type: "MEMBERSHIP",
        tierId: selectedTier.id,
        months: Number(months),
        provider,
        message,
      });
      navigate("/order-success", { state: { order } });
    } catch (err) {
      setError(extractErrorMessage(err, "Không thể đăng ký gói hội viên"));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div>
      <div style={{ marginBottom: 20 }}>
        <h1>Đăng ký Gói Hội Viên Kênh (Membership)</h1>
        <p className="muted">
          Chọn gói hội viên, số tháng và thanh toán. Payment Service sẽ xác thực người dùng & kênh qua User Service (RestClient).
        </p>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      <div style={{ display: "grid", gridTemplateColumns: "1fr 370px", gap: 24 }}>
        <div>
          {loading ? (
            <p className="muted">Đang tải gói hội viên...</p>
          ) : (
            <div className="grid-3">
              {tiers.map((tier) => (
                <TierCard
                  key={tier.id}
                  tier={tier}
                  channelName={usersMap[tier.channelId]}
                  selected={selectedTier?.id === tier.id}
                  onSelect={setSelectedTier}
                />
              ))}
            </div>
          )}
        </div>

        <aside className="card" style={{ height: "fit-content", position: "sticky", top: 84 }}>
          <h2 style={{ marginBottom: 14 }}>Thông tin đăng ký</h2>
          <form onSubmit={handleCheckout}>
            <div className="form-group">
              <label>Gói đã chọn</label>
              <input
                type="text"
                className="form-control"
                readOnly
                value={
                  selectedTier
                    ? `${selectedTier.name} — ${usersMap[selectedTier.channelId] || selectedTier.channelId}`
                    : "Chưa chọn gói"
                }
              />
            </div>

            <div className="form-group">
              <label>Tài khoản đăng ký</label>
              <select className="form-control" value={userId} onChange={(e) => setUserId(e.target.value)}>
                {users.map((u) => (
                  <option key={u.id} value={u.id}>
                    {u.channelName} ({u.username})
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label>Số tháng đăng ký (1 - 24 tháng)</label>
              <input
                type="number"
                min="1"
                max="24"
                className="form-control"
                value={months}
                onChange={(e) => setMonths(e.target.value)}
                required
              />
            </div>

            <div className="form-group">
              <label>Phương thức thanh toán</label>
              <select className="form-control" value={provider} onChange={(e) => setProvider(e.target.value)}>
                <option value="MOMO">MoMo</option>
                <option value="VNPAY">VNPay</option>
                <option value="BANK">Chuyển khoản ngân hàng</option>
              </select>
            </div>

            <div className="form-group">
              <label>Lời nhắn (tùy chọn)</label>
              <textarea
                rows="2"
                className="form-control"
                placeholder="Chúc kênh ngày càng phát triển!"
                value={message}
                onChange={(e) => setMessage(e.target.value)}
              />
            </div>

            <div
              className="flex-between"
              style={{
                padding: "12px 0",
                margin: "12px 0",
                borderTop: "1px solid var(--border)",
                borderBottom: "1px solid var(--border)",
              }}
            >
              <span>Tổng thanh toán:</span>
              <strong style={{ fontSize: "1.35rem", color: "#6ee7b7" }}>
                {totalAmount.toLocaleString("vi-VN")} ₫
              </strong>
            </div>

            <button
              type="submit"
              className="btn btn-success"
              style={{ width: "100%" }}
              disabled={submitting || !selectedTier}
            >
              {submitting ? "Đang xử lý đơn hàng..." : "Xác nhận đăng ký"}
            </button>
          </form>
        </aside>
      </div>
    </div>
  );
}
