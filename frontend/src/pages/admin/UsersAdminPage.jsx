import { useEffect, useState } from "react";
import { userService } from "../../services/userService";
import { extractErrorMessage } from "../../services/api";

const EMPTY_FORM = {
  username: "",
  email: "",
  channelName: "",
  avatar: "",
  bio: "",
  role: "USER",
};

export default function UsersAdminPage() {
  const [users, setUsers] = useState([]);
  const [form, setForm] = useState(EMPTY_FORM);
  const [editingId, setEditingId] = useState(null);
  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState({});
  const [success, setSuccess] = useState("");

  async function loadUsers() {
    try {
      const data = await userService.getAll();
      setUsers(data);
    } catch (err) {
      setError(extractErrorMessage(err, "Không thể tải danh sách người dùng"));
    }
  }

  useEffect(() => {
    loadUsers();
  }, []);

  function startEdit(u) {
    setEditingId(u.id);
    setForm({
      username: u.username || "",
      email: u.email || "",
      channelName: u.channelName || "",
      avatar: u.avatar || "",
      bio: u.bio || "",
      role: u.role || "USER",
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
        await userService.update(editingId, form);
        setSuccess("Cập nhật người dùng thành công!");
      } else {
        await userService.create(form);
        setSuccess("Thêm người dùng mới thành công!");
      }
      resetForm();
      loadUsers();
    } catch (err) {
      if (err?.response?.data?.errors) {
        setFieldErrors(err.response.data.errors);
      }
      setError(extractErrorMessage(err, "Không thể lưu người dùng"));
    }
  }

  async function handleDelete(id) {
    if (!window.confirm("Bạn có chắc muốn xóa người dùng này?")) return;
    try {
      await userService.delete(id);
      setSuccess("Đã xóa người dùng.");
      loadUsers();
    } catch (err) {
      setError(extractErrorMessage(err, "Không thể xóa người dùng"));
    }
  }

  return (
    <div>
      <h1 style={{ marginBottom: 16 }}>Quản lý Người dùng & Kênh (User Service)</h1>

      {error && <div className="alert alert-error">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      <div className="card" style={{ marginBottom: 24 }}>
        <h3 style={{ marginBottom: 14 }}>{editingId ? `Chỉnh sửa: ${editingId}` : "Thêm người dùng mới"}</h3>
        <form onSubmit={handleSubmit}>
          <div className="grid-3">
            <div className="form-group">
              <label>Tên đăng nhập *</label>
              <input
                className="form-control"
                value={form.username}
                onChange={(e) => setForm({ ...form, username: e.target.value })}
              />
              {fieldErrors.username && <span className="field-error">{fieldErrors.username}</span>}
            </div>
            <div className="form-group">
              <label>Email *</label>
              <input
                className="form-control"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
              />
              {fieldErrors.email && <span className="field-error">{fieldErrors.email}</span>}
            </div>
            <div className="form-group">
              <label>Tên kênh *</label>
              <input
                className="form-control"
                value={form.channelName}
                onChange={(e) => setForm({ ...form, channelName: e.target.value })}
              />
              {fieldErrors.channelName && <span className="field-error">{fieldErrors.channelName}</span>}
            </div>
            <div className="form-group">
              <label>Vai trò *</label>
              <select
                className="form-control"
                value={form.role}
                onChange={(e) => setForm({ ...form, role: e.target.value })}
              >
                <option value="USER">USER</option>
                <option value="CREATOR">CREATOR</option>
                <option value="ADMIN">ADMIN</option>
              </select>
              {fieldErrors.role && <span className="field-error">{fieldErrors.role}</span>}
            </div>
            <div className="form-group">
              <label>Avatar URL</label>
              <input
                className="form-control"
                value={form.avatar}
                onChange={(e) => setForm({ ...form, avatar: e.target.value })}
              />
            </div>
            <div className="form-group">
              <label>Giới thiệu</label>
              <input
                className="form-control"
                value={form.bio}
                onChange={(e) => setForm({ ...form, bio: e.target.value })}
              />
            </div>
          </div>
          <div className="flex-gap">
            <button type="submit" className="btn btn-primary">
              {editingId ? "Lưu thay đổi" : "Thêm mới"}
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
              <th>Username</th>
              <th>Tên kênh</th>
              <th>Email</th>
              <th>Vai trò</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {users.map((u) => (
              <tr key={u.id}>
                <td><code>{u.id}</code></td>
                <td>{u.username}</td>
                <td><strong>{u.channelName}</strong></td>
                <td>{u.email}</td>
                <td>
                  <span className={`badge ${u.role === "CREATOR" ? "badge-purple" : u.role === "ADMIN" ? "badge-red" : "badge-blue"}`}>
                    {u.role}
                  </span>
                </td>
                <td>
                  <div className="flex-gap">
                    <button type="button" className="btn btn-outline btn-sm" onClick={() => startEdit(u)}>
                      Sửa
                    </button>
                    <button type="button" className="btn btn-danger btn-sm" onClick={() => handleDelete(u.id)}>
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
