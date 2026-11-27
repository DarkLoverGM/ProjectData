import { useEffect, useState } from "react";
import { userService } from "../../services/userService";
import { videoService } from "../../services/videoService";
import { tierService } from "../../services/tierService";
import { orderService } from "../../services/orderService";
import { extractErrorMessage } from "../../services/api";

export default function DashboardPage() {
  const [stats, setStats] = useState({ users: 0, videos: 0, tiers: 0, orders: 0, revenue: 0 });
  const [recentOrders, setRecentOrders] = useState([]);
  const [error, setError] = useState("");

  // RestClient test panel (Câu B)
  const [testVideoId, setTestVideoId] = useState("vid-001");
  const [testResult, setTestResult] = useState(null);
  const [testError, setTestError] = useState("");

  useEffect(() => {
    async function load() {
      try {
        const [users, videos, tiers, orders] = await Promise.all([
          userService.getAll(),
          videoService.getAll(),
          tierService.getAll(),
          orderService.getAll(),
        ]);
        const revenue = orders
          .filter((o) => o.status === "PAID")
          .reduce((sum, o) => sum + Number(o.totalAmount || 0), 0);

        setStats({
          users: users.length,
          videos: videos.length,
          tiers: tiers.length,
          orders: orders.length,
          revenue,
        });
        setRecentOrders(orders.slice(0, 5));
      } catch (err) {
        setError(extractErrorMessage(err, "Không thể tải dữ liệu tổng quan"));
      }
    }
    load();
  }, []);

  async function handleTestRestClient() {
    try {
      setTestError("");
      const data = await orderService.getVideoViaPayment(testVideoId.trim());
      setTestResult(data);
    } catch (err) {
      setTestResult(null);
      setTestError(extractErrorMessage(err, "Lỗi gọi RestClient"));
    }
  }

  return (
    <div>
      <h1 style={{ marginBottom: 6 }}>Dashboard Quản trị</h1>
      <p className="muted" style={{ marginBottom: 20 }}>
        Tổng quan dữ liệu từ 3 Microservices: User Service (3003), Video Service (3004), Payment Service (3005).
      </p>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="grid-4" style={{ marginBottom: 24 }}>
        <div className="card">
          <div className="muted">Người dùng / Kênh</div>
          <div className="stat-value">{stats.users}</div>
        </div>
        <div className="card">
          <div className="muted">Tổng số Video</div>
          <div className="stat-value">{stats.videos}</div>
        </div>
        <div className="card">
          <div className="muted">Gói hội viên</div>
          <div className="stat-value">{stats.tiers}</div>
        </div>
        <div className="card">
          <div className="muted">Doanh thu (PAID)</div>
          <div className="stat-value" style={{ color: "#6ee7b7" }}>
            {stats.revenue.toLocaleString("vi-VN")} ₫
          </div>
        </div>
      </div>

      <div className="card" style={{ marginBottom: 24 }}>
        <h3 style={{ marginBottom: 8 }}>Kiểm tra giao tiếp RestClient (Payment → Video Service)</h3>
        <p className="muted" style={{ marginBottom: 12 }}>
          Gọi <code>GET http://localhost:3005/api/orders/video/{"{videoId}"}</code> để kiểm tra giao tiếp đồng bộ liên Microservices.
        </p>
        <div className="flex-gap" style={{ marginBottom: 12 }}>
          <input
            type="text"
            className="form-control"
            style={{ maxWidth: 260 }}
            value={testVideoId}
            onChange={(e) => setTestVideoId(e.target.value)}
            placeholder="Nhập mã video (vd: vid-001)"
          />
          <button type="button" className="btn btn-accent" onClick={handleTestRestClient}>
            Gọi thử RestClient
          </button>
        </div>
        {testError && <div className="alert alert-error">{testError}</div>}
        {testResult && (
          <pre
            style={{
              background: "#0b1220",
              padding: 14,
              borderRadius: 8,
              overflowX: "auto",
              fontSize: "0.85rem",
            }}
          >
            {JSON.stringify(testResult, null, 2)}
          </pre>
        )}
      </div>

      <div className="card">
        <h3 style={{ marginBottom: 14 }}>Đơn thanh toán gần đây</h3>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Mã đơn</th>
                <th>Loại</th>
                <th>Người mua</th>
                <th>Kênh nhận</th>
                <th>Tổng tiền</th>
                <th>Trạng thái</th>
              </tr>
            </thead>
            <tbody>
              {recentOrders.map((o) => (
                <tr key={o.id}>
                  <td><code>{o.id}</code></td>
                  <td><span className="badge badge-blue">{o.type}</span></td>
                  <td>{o.userName}</td>
                  <td>{o.channelName}</td>
                  <td>{Number(o.totalAmount).toLocaleString("vi-VN")} ₫</td>
                  <td>
                    <span className={`badge ${o.status === "PAID" ? "badge-green" : o.status === "PENDING" ? "badge-yellow" : "badge-red"}`}>
                      {o.status}
                    </span>
                  </td>
                </tr>
              ))}
              {recentOrders.length === 0 && (
                <tr>
                  <td colSpan="6" className="muted">Chưa có đơn hàng nào.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
