import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { videoService } from "../../services/videoService";
import { userService } from "../../services/userService";
import { extractErrorMessage } from "../../services/api";
import VideoCard from "../../components/VideoCard";

export default function HomePage() {
  const [videos, setVideos] = useState([]);
  const [usersMap, setUsersMap] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    async function load() {
      try {
        setLoading(true);
        const [videoData, userData] = await Promise.all([
          videoService.getAll(),
          userService.getAll(),
        ]);
        setVideos(videoData.slice(0, 6));
        const map = {};
        userData.forEach((u) => {
          map[u.id] = u.channelName;
        });
        setUsersMap(map);
      } catch (err) {
        setError(extractErrorMessage(err, "Không thể tải dữ liệu từ Microservices."));
      } finally {
        setLoading(false);
      }
    }
    load();
  }, []);

  return (
    <div>
      <section className="hero">
        <h1>Nền tảng Video & Hội viên Nhà sáng tạo</h1>
        <p className="muted" style={{ maxWidth: 680, marginBottom: 20 }}>
          Kiến trúc Microservices kết hợp User Service (3003), Video Service (3004) và Payment Service (3005)
          giao tiếp đồng bộ qua Spring RestClient. Khám phá video, gửi Super Thanks hoặc đăng ký gói hội viên kênh!
        </p>
        <div className="flex-gap">
          <Link to="/videos" className="btn btn-primary">
            Khám phá Video & Super Thanks
          </Link>
          <Link to="/memberships" className="btn btn-accent">
            Đăng ký Gói hội viên
          </Link>
          <Link to="/admin" className="btn btn-outline">
            Mở trang Quản trị
          </Link>
        </div>
      </section>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="flex-between" style={{ marginBottom: 16 }}>
        <h2>Video nổi bật</h2>
        <Link to="/videos" className="muted">
          Xem tất cả →
        </Link>
      </div>

      {loading ? (
        <p className="muted">Đang tải danh sách video...</p>
      ) : (
        <div className="grid-3">
          {videos.map((video) => (
            <VideoCard key={video.id} video={video} channelName={usersMap[video.channelId]} />
          ))}
        </div>
      )}
    </div>
  );
}
