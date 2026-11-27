import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { videoService } from "../../services/videoService";
import { userService } from "../../services/userService";
import { orderService } from "../../services/orderService";
import { extractErrorMessage } from "../../services/api";

export default function VideoDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [video, setVideo] = useState(null);
  const [channel, setChannel] = useState(null);
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [userId, setUserId] = useState("user-1");
  const [amount, setAmount] = useState(25000);
  const [provider, setProvider] = useState("MOMO");
  const [message, setMessage] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    async function load() {
      try {
        setLoading(true);
        const [vid, userList] = await Promise.all([
          videoService.getById(id),
          userService.getAll(),
        ]);
        setVideo(vid);
        setUsers(userList);
        const foundChannel = userList.find((u) => u.id === vid.channelId);
        setChannel(foundChannel || null);
        if (userList.length > 0) setUserId(userList[0].id);
        // Tăng lượt xem
        videoService.incrementViews(id).then(setVideo).catch(() => {});
      } catch (err) {
        setError(extractErrorMessage(err, "Không thể tải chi tiết video"));
      } finally {
        setLoading(false);
      }
    }
    load();
  }, [id]);

  async function handleDonate(e) {
    e.preventDefault();
    try {
      setSubmitting(true);
      setError("");
      const order = await orderService.create({
        userId,
        type: "SUPER_THANKS",
        videoId: video.id,
        amount: Number(amount),
        provider,
        message,
      });
      navigate("/order-success", { state: { order } });
    } catch (err) {
      setError(extractErrorMessage(err, "Không thể gửi Super Thanks"));
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) return <p className="muted">Đang tải thông tin video...</p>;
  if (!video) return <div className="alert alert-error">{error || "Không tìm thấy video"}</div>;

  return (
    <div>
      <Link to="/videos" className="muted" style={{ display: "inline-block", marginBottom: 16 }}>
        ← Quay lại danh sách video
      </Link>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="grid-2">
        <div className="card">
          <img
            src={video.thumbnail || `https://picsum.photos/seed/${video.id}/800/450`}
            alt={video.title}
            className="video-thumb"
            style={{ borderRadius: 10, marginBottom: 16 }}
          />
          <div className="flex-gap" style={{ marginBottom: 10 }}>
            <span className="badge badge-blue">{video.category}</span>
            <span className="badge badge-green">{video.status}</span>
            {video.membersOnly && <span className="badge badge-purple">Members Only</span>}
          </div>
          <h1 style={{ fontSize: "1.5rem", marginBottom: 8 }}>{video.title}</h1>
          <p className="muted" style={{ marginBottom: 16 }}>
            Kênh: <strong>{channel?.channelName || video.channelId}</strong> ·{" "}
            {Number(video.views || 0).toLocaleString("vi-VN")} lượt xem
          </p>
          <p>{video.description || "Không có mô tả."}</p>
        </div>

        <div className="card" style={{ height: "fit-content" }}>
          <h2 style={{ marginBottom: 6 }}>♥ Gửi Super Thanks</h2>
          <p className="muted" style={{ marginBottom: 16 }}>
            Payment Service sẽ gọi Video Service & User Service qua RestClient để xác thực trước khi tạo đơn.
          </p>

          <form onSubmit={handleDonate}>
            <div className="form-group">
              <label>Người gửi</label>
              <select className="form-control" value={userId} onChange={(e) => setUserId(e.target.value)}>
                {users.map((u) => (
                  <option key={u.id} value={u.id}>
                    {u.channelName} ({u.username})
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label>Chọn nhanh số tiền</label>
              <div className="flex-gap">
                {[20000, 50000, 100000, 200000].map((v) => (
                  <button
                    key={v}
                    type="button"
                    className={`btn btn-sm ${Number(amount) === v ? "btn-primary" : "btn-outline"}`}
                    onClick={() => setAmount(v)}
                  >
                    {v.toLocaleString("vi-VN")} ₫
                  </button>
                ))}
              </div>
            </div>

            <div className="form-group">
              <label>Hoặc nhập số tiền (VNĐ)</label>
              <input
                type="number"
                min="1000"
                step="1000"
                className="form-control"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                required
              />
            </div>

            <div className="form-group">
              <label>Cổng thanh toán</label>
              <select className="form-control" value={provider} onChange={(e) => setProvider(e.target.value)}>
                <option value="MOMO">MoMo</option>
                <option value="VNPAY">VNPay</option>
                <option value="BANK">Chuyển khoản ngân hàng</option>
              </select>
            </div>

            <div className="form-group">
              <label>Lời nhắn</label>
              <textarea
                rows="3"
                className="form-control"
                placeholder="Gửi lời động viên tới nhà sáng tạo..."
                value={message}
                onChange={(e) => setMessage(e.target.value)}
              />
            </div>

            <button type="submit" className="btn btn-primary" style={{ width: "100%" }} disabled={submitting}>
              {submitting ? "Đang tạo đơn..." : `Ủng hộ ${Number(amount || 0).toLocaleString("vi-VN")} ₫`}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
