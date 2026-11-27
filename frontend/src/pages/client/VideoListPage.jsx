import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { videoService } from "../../services/videoService";
import { userService } from "../../services/userService";
import { orderService } from "../../services/orderService";
import { extractErrorMessage } from "../../services/api";
import VideoCard from "../../components/VideoCard";

export default function VideoListPage() {
  const navigate = useNavigate();
  const [videos, setVideos] = useState([]);
  const [users, setUsers] = useState([]);
  const [keyword, setKeyword] = useState("");
  const [category, setCategory] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // Quick Super Thanks state
  const [selectedVideo, setSelectedVideo] = useState(null);
  const [userId, setUserId] = useState("user-1");
  const [amount, setAmount] = useState(20000);
  const [provider, setProvider] = useState("MOMO");
  const [message, setMessage] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const usersMap = Object.fromEntries(users.map((u) => [u.id, u.channelName]));

  async function loadData(searchKw = keyword, cat = category) {
    try {
      setLoading(true);
      setError("");
      const filters = {};
      if (searchKw.trim()) filters.keyword = searchKw.trim();
      else if (cat) filters.category = cat;

      const [videoData, userData] = await Promise.all([
        videoService.getAll(filters),
        userService.getAll(),
      ]);
      setVideos(videoData);
      setUsers(userData);
      if (userData.length > 0 && !userData.some((u) => u.id === userId)) {
        setUserId(userData[0].id);
      }
    } catch (err) {
      setError(extractErrorMessage(err, "Không thể tải danh sách video"));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadData("", "");
  }, []);

  async function handleSuperThanks(e) {
    e.preventDefault();
    if (!selectedVideo) return;
    try {
      setSubmitting(true);
      setError("");
      const result = await orderService.create({
        userId,
        type: "SUPER_THANKS",
        videoId: selectedVideo.id,
        amount: Number(amount),
        provider,
        message,
      });
      navigate("/order-success", { state: { order: result } });
    } catch (err) {
      setError(extractErrorMessage(err, "Không thể gửi Super Thanks"));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div>
      <div className="flex-between" style={{ marginBottom: 20 }}>
        <div>
          <h1>Danh sách Video & Gửi Super Thanks</h1>
          <p className="muted">Chọn video bất kỳ để xem chi tiết hoặc gửi Super Thanks ủng hộ nhà sáng tạo.</p>
        </div>
      </div>

      <div className="card flex-gap" style={{ marginBottom: 24 }}>
        <input
          type="text"
          className="form-control"
          style={{ maxWidth: 320 }}
          placeholder="Tìm theo tiêu đề video..."
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
        />
        <select
          className="form-control"
          style={{ maxWidth: 200 }}
          value={category}
          onChange={(e) => {
            setCategory(e.target.value);
            setKeyword("");
            loadData("", e.target.value);
          }}
        >
          <option value="">Tất cả thể loại</option>
          <option value="Education">Education</option>
          <option value="Gaming">Gaming</option>
          <option value="Music">Music</option>
          <option value="Technology">Technology</option>
        </select>
        <button type="button" className="btn btn-accent" onClick={() => loadData(keyword, category)}>
          Tìm kiếm
        </button>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      <div style={{ display: "grid", gridTemplateColumns: selectedVideo ? "1fr 360px" : "1fr", gap: 24 }}>
        <div>
          {loading ? (
            <p className="muted">Đang tải video...</p>
          ) : videos.length === 0 ? (
            <div className="card muted">Không tìm thấy video phù hợp.</div>
          ) : (
            <div className="grid-3">
              {videos.map((v) => (
                <VideoCard
                  key={v.id}
                  video={v}
                  channelName={usersMap[v.channelId]}
                  onSelectThanks={setSelectedVideo}
                />
              ))}
            </div>
          )}
        </div>

        {selectedVideo && (
          <aside className="card" style={{ height: "fit-content", position: "sticky", top: 84 }}>
            <div className="flex-between" style={{ marginBottom: 12 }}>
              <h3>♥ Gửi Super Thanks</h3>
              <button
                type="button"
                className="btn btn-outline btn-sm"
                onClick={() => setSelectedVideo(null)}
              >
                Đóng
              </button>
            </div>
            <p className="muted" style={{ marginBottom: 14 }}>
              Video: <strong>{selectedVideo.title}</strong>
            </p>

            <form onSubmit={handleSuperThanks}>
              <div className="form-group">
                <label>Người gửi (User Service)</label>
                <select className="form-control" value={userId} onChange={(e) => setUserId(e.target.value)}>
                  {users.map((u) => (
                    <option key={u.id} value={u.id}>
                      {u.channelName} ({u.username})
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label>Số tiền ủng hộ (VNĐ)</label>
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
                <label>Phương thức thanh toán</label>
                <select className="form-control" value={provider} onChange={(e) => setProvider(e.target.value)}>
                  <option value="MOMO">MoMo</option>
                  <option value="VNPAY">VNPay</option>
                  <option value="BANK">Chuyển khoản ngân hàng</option>
                </select>
              </div>

              <div className="form-group">
                <label>Lời nhắn cho nhà sáng tạo</label>
                <textarea
                  rows="3"
                  className="form-control"
                  placeholder="Cảm ơn kênh đã chia sẻ video hữu ích!"
                  value={message}
                  onChange={(e) => setMessage(e.target.value)}
                />
              </div>

              <button type="submit" className="btn btn-primary" style={{ width: "100%" }} disabled={submitting}>
                {submitting ? "Đang xử lý..." : `Thanh toán ${Number(amount || 0).toLocaleString("vi-VN")} ₫`}
              </button>
            </form>
          </aside>
        )}
      </div>
    </div>
  );
}
