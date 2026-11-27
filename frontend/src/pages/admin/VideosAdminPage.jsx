import { useEffect, useState } from "react";
import { videoService } from "../../services/videoService";
import { userService } from "../../services/userService";
import { extractErrorMessage } from "../../services/api";

const EMPTY_FORM = {
  title: "",
  description: "",
  thumbnail: "",
  videoUrl: "",
  channelId: "creator-1",
  category: "Education",
  membersOnly: false,
  status: "PUBLIC",
};

export default function VideosAdminPage() {
  const [videos, setVideos] = useState([]);
  const [users, setUsers] = useState([]);
  const [form, setForm] = useState(EMPTY_FORM);
  const [editingId, setEditingId] = useState(null);
  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState({});
  const [success, setSuccess] = useState("");

  async function loadData() {
    try {
      const [vids, usrs] = await Promise.all([videoService.getAll(), userService.getAll()]);
      setVideos(vids);
      setUsers(usrs);
    } catch (err) {
      setError(extractErrorMessage(err, "Không thể tải danh sách video"));
    }
  }

  useEffect(() => {
    loadData();
  }, []);

  function startEdit(v) {
    setEditingId(v.id);
    setForm({
      title: v.title || "",
      description: v.description || "",
      thumbnail: v.thumbnail || "",
      videoUrl: v.videoUrl || "",
      channelId: v.channelId || "creator-1",
      category: v.category || "Education",
      membersOnly: Boolean(v.membersOnly),
      status: v.status || "PUBLIC",
    });
    setError("");
    setFieldErrors({});
    setSuccess("");
  }

  function resetForm() {
    setEditingId(null);
    setForm(EMPTY_FORM);
    setFieldErrors({});
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");
    setFieldErrors({});
    setSuccess("");
    try {
      if (editingId) {
        await videoService.update(editingId, form);
        setSuccess("Cập nhật video thành công!");
      } else {
        await videoService.create(form);
        setSuccess("Tạo video mới thành công!");
      }
      resetForm();
      loadData();
    } catch (err) {
      if (err?.response?.data?.errors) {
        setFieldErrors(err.response.data.errors);
      }
      setError(extractErrorMessage(err, "Không thể lưu video"));
    }
  }

  async function handleDelete(id) {
    if (!window.confirm("Bạn có chắc muốn xóa video này?")) return;
    try {
      await videoService.delete(id);
      setSuccess("Đã xóa video.");
      loadData();
    } catch (err) {
      setError(extractErrorMessage(err, "Không thể xóa video"));
    }
  }

  return (
    <div>
      <h1 style={{ marginBottom: 16 }}>Quản lý Video (Video Service)</h1>

      {error && <div className="alert alert-error">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      <div className="card" style={{ marginBottom: 24 }}>
        <h3 style={{ marginBottom: 14 }}>{editingId ? `Chỉnh sửa video: ${editingId}` : "Thêm video mới"}</h3>
        <form onSubmit={handleSubmit}>
          <div className="grid-3">
            <div className="form-group">
              <label>Tiêu đề video *</label>
              <input
                className="form-control"
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
              />
              {fieldErrors.title && <span className="field-error">{fieldErrors.title}</span>}
            </div>
            <div className="form-group">
              <label>Kênh đăng tải *</label>
              <select
                className="form-control"
                value={form.channelId}
                onChange={(e) => setForm({ ...form, channelId: e.target.value })}
              >
                {users.map((u) => (
                  <option key={u.id} value={u.id}>
                    {u.channelName} ({u.id})
                  </option>
                ))}
              </select>
              {fieldErrors.channelId && <span className="field-error">{fieldErrors.channelId}</span>}
            </div>
            <div className="form-group">
              <label>Thể loại *</label>
              <select
                className="form-control"
                value={form.category}
                onChange={(e) => setForm({ ...form, category: e.target.value })}
              >
                <option value="Education">Education</option>
                <option value="Gaming">Gaming</option>
                <option value="Music">Music</option>
                <option value="Technology">Technology</option>
              </select>
              {fieldErrors.category && <span className="field-error">{fieldErrors.category}</span>}
            </div>
            <div className="form-group">
              <label>Trạng thái</label>
              <select
                className="form-control"
                value={form.status}
                onChange={(e) => setForm({ ...form, status: e.target.value })}
              >
                <option value="PUBLIC">PUBLIC</option>
                <option value="UNLISTED">UNLISTED</option>
                <option value="PRIVATE">PRIVATE</option>
              </select>
            </div>
            <div className="form-group">
              <label>Thumbnail URL</label>
              <input
                className="form-control"
                value={form.thumbnail}
                onChange={(e) => setForm({ ...form, thumbnail: e.target.value })}
              />
            </div>
            <div className="form-group" style={{ justifyContent: "center" }}>
              <label>
                <input
                  type="checkbox"
                  checked={form.membersOnly}
                  onChange={(e) => setForm({ ...form, membersOnly: e.target.checked })}
                  style={{ marginRight: 8 }}
                />
                Chỉ dành cho hội viên (Members Only)
              </label>
            </div>
          </div>

          <div className="form-group">
            <label>Mô tả</label>
            <textarea
              rows="2"
              className="form-control"
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
            />
          </div>

          <div className="flex-gap">
            <button type="submit" className="btn btn-primary">
              {editingId ? "Lưu thay đổi" : "Thêm video"}
            </button>
            {editingId && (
              <button type="button" className="btn btn-outline" onClick={resetForm}>
                Hủy
              </button>
            )}
          </div>
        </form>
      </div>

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>Tiêu đề</th>
              <th>Kênh</th>
              <th>Thể loại</th>
              <th>Lượt xem</th>
              <th>Trạng thái</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {videos.map((v) => (
              <tr key={v.id}>
                <td><code>{v.id}</code></td>
                <td>
                  <strong>{v.title}</strong>
                  {v.membersOnly && (
                    <span className="badge badge-purple" style={{ marginLeft: 8 }}>
                      Members
                    </span>
                  )}
                </td>
                <td>{v.channelId}</td>
                <td><span className="badge badge-blue">{v.category}</span></td>
                <td>{Number(v.views || 0).toLocaleString("vi-VN")}</td>
                <td><span className="badge badge-green">{v.status}</span></td>
                <td>
                  <div className="flex-gap">
                    <button type="button" className="btn btn-outline btn-sm" onClick={() => startEdit(v)}>
                      Sửa
                    </button>
                    <button type="button" className="btn btn-danger btn-sm" onClick={() => handleDelete(v.id)}>
                      Xóa
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
