import { Link } from "react-router-dom";

export default function VideoCard({ video, channelName, onSelectThanks }) {
  return (
    <div className="video-card">
      <Link to={`/videos/${video.id}`}>
        <img
          src={video.thumbnail || `https://picsum.photos/seed/${video.id}/640/360`}
          alt={video.title}
          className="video-thumb"
        />
      </Link>
      <div className="video-body">
        <div className="flex-between">
          <span className="badge badge-blue">{video.category}</span>
          {video.membersOnly && <span className="badge badge-purple">Members Only</span>}
        </div>
        <Link to={`/videos/${video.id}`} className="video-title">
          {video.title}
        </Link>
        <p className="muted">
          Kênh: <strong>{channelName || video.channelId}</strong> · {Number(video.views || 0).toLocaleString("vi-VN")} lượt xem
        </p>
        <div className="flex-gap" style={{ marginTop: "auto", paddingTop: 8 }}>
          <Link to={`/videos/${video.id}`} className="btn btn-outline btn-sm">
            Xem chi tiết
          </Link>
          {onSelectThanks && (
            <button
              type="button"
              className="btn btn-primary btn-sm"
              onClick={() => onSelectThanks(video)}
            >
              ♥ Super Thanks
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
