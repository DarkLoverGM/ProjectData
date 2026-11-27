import { useEffect, useState } from "react";
import { orderService } from "../../services/orderService";
import { extractErrorMessage } from "../../services/api";

export default function OrdersAdminPage() {
  const [orders, setOrders] = useState([]);
  const [filterType, setFilterType] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  async function loadOrders(type = filterType) {
    try {
      setError("");
      const params = type ? { type } : {};
      const data = await orderService.getAll(params);
      setOrders(data);
    } catch (err) {
      setError(extractErrorMessage(err, "Không thể tải danh sách đơn hàng"));
    }
  }

  useEffect(() => {
    loadOrders("");
  }, []);

  async function handleStatusChange(id, status) {
    try {
      setError("");
      await orderService.updateStatus(id, status);
      setSuccess(`Đã cập nhật trạng thái đơn ${id} thành ${status}`);
      loadOrders();
    } catch (err) {
      setError(extractErrorMessage(err, "Không thể cập nhật trạng thái"));
    }
  }

  async function handleDelete(id) {
    if (!window.confirm("Bạn có chắc muốn xóa đơn hàng này?")) return;
    try {
      await orderService.delete(id);
      setSuccess("Đã xóa đơn hàng.");
      loadOrders();
    } catch (err) {
      setError(extractErrorMessage(err, "Không thể xóa đơn hàng"));
    }
  }

  return (
    <div>
      <div className="flex-between" style={{ marginBottom: 16 }}>
        <h1>Quản lý Đơn thanh toán (Payment Service)</h1>
        <select
          className="form-control"
          style={{ maxWidth: 220 }}
          value={filterType}
          onChange={(e) => {
            setFilterType(e.target.value);
            loadOrders(e.target.value);
          }}
        >
          <option value="">Tất cả loại đơn</option>
          <option value="MEMBERSHIP">MEMBERSHIP</option>
          <option value="SUPER_THANKS">SUPER_THANKS</option>
        </select>
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Mã đơn</th>
              <th>Loại</th>
              <th>Người mua</th>
              <th>Nội dung</th>
              <th>Cổng TT</th>
              <th>Tổng tiền</th>
              <th>Trạng thái</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {orders.map((o) => (
              <tr key={o.id}>
                <td><code>{o.id}</code></td>
                <td><span className="badge badge-blue">{o.type}</span></td>
                <td>{o.userName}</td>
                <td>
                  <div><strong>{o.channelName}</strong></div>
                  <div className="muted">
                    {o.type === "MEMBERSHIP"
                      ? `${o.tierName} × ${o.months} tháng`
                      : `Video: ${o.videoTitle}`}
                  </div>
                </td>
                <td>{o.provider}</td>
                <td><strong>{Number(o.totalAmount).toLocaleString("vi-VN")} ₫</strong></td>
                <td>
                  <select
                    className="form-control"
                    style={{ padding: "5px 8px", width: 120 }}
                    value={o.status}
                    onChange={(e) => handleStatusChange(o.id, e.target.value)}
                  >
                    <option value="PENDING">PENDING</option>
                    <option value="PAID">PAID</option>
                    <option value="CANCELLED">CANCELLED</option>
                  </select>
                </td>
                <td>
                  <button type="button" className="btn btn-danger btn-sm" onClick={() => handleDelete(o.id)}>
                    Xóa
                  </button>
                </td>
              </tr>
            ))}
            {orders.length === 0 && (
              <tr>
                <td colSpan="8" className="muted">Chưa có đơn thanh toán nào.</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
