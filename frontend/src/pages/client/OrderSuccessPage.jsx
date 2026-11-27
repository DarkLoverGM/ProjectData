import { Link, useLocation } from "react-router-dom";

export default function OrderSuccessPage() {
  const { state } = useLocation();
  const order = state?.order;

  return (
    <div style={{ maxWidth: 640, margin: "0 auto" }}>
      <div className="card">
        <span className="badge badge-green" style={{ marginBottom: 12 }}>
          ✓ Giao dịch thành công (201 Created)
        </span>
        <h1 style={{ marginBottom: 8 }}>Cảm ơn bạn đã ủng hộ nhà sáng tạo!</h1>
        <p className="muted" style={{ marginBottom: 20 }}>
          Đơn hàng đã được Payment Service xác thực liên Microservices và lưu vào hệ thống.
        </p>

        {order ? (
          <div className="table-wrap" style={{ marginBottom: 20 }}>
            <table>
              <tbody>
                <tr>
                  <th>Mã đơn hàng</th>
                  <td><code>{order.id}</code></td>
                </tr>
                <tr>
                  <th>Loại giao dịch</th>
                  <td>
                    <span className="badge badge-blue">{order.type}</span>
                  </td>
                </tr>
                <tr>
                  <th>Người thanh toán</th>
                  <td>{order.userName} ({order.userId})</td>
                </tr>
                <tr>
                  <th>Kênh nhận</th>
                  <td>{order.channelName} ({order.channelId})</td>
                </tr>
                {order.tierName && (
                  <tr>
                    <th>Gói hội viên</th>
                    <td>{order.tierName} × {order.months} tháng</td>
                  </tr>
                )}
                {order.videoTitle && (
                  <tr>
                    <th>Video Super Thanks</th>
                    <td>{order.videoTitle}</td>
                  </tr>
                )}
                <tr>
                  <th>Phương thức</th>
                  <td>{order.provider}</td>
                </tr>
                <tr>
                  <th>Tổng tiền</th>
                  <td>
                    <strong style={{ color: "#6ee7b7", fontSize: "1.1rem" }}>
                      {Number(order.totalAmount).toLocaleString("vi-VN")} ₫
                    </strong>
                  </td>
                </tr>
                {order.message && (
                  <tr>
                    <th>Lời nhắn</th>
                    <td>{order.message}</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        ) : (
          <p className="muted" style={{ marginBottom: 20 }}>
            Không có thông tin đơn hàng trong phiên hiện tại.
          </p>
        )}

        <div className="flex-gap">
          <Link to="/videos" className="btn btn-primary">
            Tiếp tục xem Video
          </Link>
          <Link to="/memberships" className="btn btn-outline">
            Xem gói hội viên khác
          </Link>
          <Link to="/admin/orders" className="btn btn-accent">
            Xem trong Admin Orders
          </Link>
        </div>
      </div>
    </div>
  );
}
