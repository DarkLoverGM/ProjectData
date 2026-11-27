import { useEffect, useState } from "react";
import { tierService } from "../../services/tierService";
import { userService } from "../../services/userService";
import { extractErrorMessage } from "../../services/api";

const EMPTY_FORM = {
  channelId: "creator-1",
  name: "",
  price: "",
  perks: "",
  active: true,
};

export default function TiersAdminPage() {
  const [tiers, setTiers] = useState([]);
  const [users, setUsers] = useState([]);
  const [form, setForm] = useState(EMPTY_FORM);
  const [editingId, setEditingId] = useState(null);
  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState({});
  const [success, setSuccess] = useState("");

  async function loadData() {
    try {
      const [tierData, userData] = await Promise.all([tierService.getAll(), userService.getAll()]);
      setTiers(tierData);
      setUsers(userData);
    } catch (err) {
      setError(extractErrorMessage(err, "Không thể tải danh sách gói hội viên"));
    }
  }

  useEffect(() => {
    loadData();
  }, []);

  function startEdit(t) {
    setEditingId(t.id);
    setForm({
      channelId: t.channelId || "creator-1",
      name: t.name || "",
      price: String(t.price ?? ""),
      perks: t.perks || "",
      active: Boolean(t.active),
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
      const payload = { ...form, price: form.price === "" ? null : Number(form.price) };
      if (editingId) {
        await tierService.update(editingId, payload);
        setSuccess("Cập nhật gói hội viên thành công!");
      } else {
        await tierService.create(payload);
        setSuccess("Tạo gói hội viên mới thành công!");
      }
      resetForm();
      loadData();
    } catch (err) {
      if (err?.response?.data?.errors) {
        setFieldErrors(err.response.data.errors);
      }
      setError(extractErrorMessage(err, "Không thể lưu gói hội viên"));
    }
  }

  async function handleDelete(id) {
    if (!window.confirm("Bạn có chắc muốn xóa gói hội viên này?")) return;
    try {
      await tierService.delete(id);
      setSuccess("Đã xóa gói hội viên.");
      loadData();
    } catch (err) {
      setError(extractErrorMessage(err, "Không thể xóa gói hội viên"));
    }
  }

  return (
    <div>
      <h1 style={{ marginBottom: 16 }}>Quản lý Gói hội viên (Payment Service)</h1>

      {error && <div className="alert alert-error">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      <div className="card" style={{ marginBottom: 24 }}>
        <h3 style={{ marginBottom: 14 }}>{editingId ? `Chỉnh sửa gói: ${editingId}` : "Thêm gói hội viên mới"}</h3>
        <form onSubmit={handleSubmit}>
          <div className="grid-3">
            <div className="form-group">
              <label>Kênh sở hữu *</label>
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
              <label>Tên gói hội viên *</label>
              <input
                className="form-control"
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
              />
              {fieldErrors.name && <span className="field-error">{fieldErrors.name}</span>}
            </div>
            <div className="form-group">
              <label>Giá / tháng (VNĐ) *</label>
              <input
                type="number"
                className="form-control"
                value={form.price}
                onChange={(e) => setForm({ ...form, price: e.target.value })}
              />
              {fieldErrors.price && <span className="field-error">{fieldErrors.price}</span>}
            </div>
          </div>

          <div className="form-group">
            <label>Quyền lợi hội viên</label>
            <input
              className="form-control"
              value={form.perks}
              onChange={(e) => setForm({ ...form, perks: e.target.value })}
            />
          </div>

          <div className="form-group">
            <label>
              <input
                type="checkbox"
                checked={form.active}
                onChange={(e) => setForm({ ...form, active: e.target.checked })}
                style={{ marginRight: 8 }}
              />
              Đang mở đăng ký (Active)
            </label>
          </div>

          <div className="flex-gap">
            <button type="submit" className="btn btn-primary">
              {editingId ? "Lưu thay đổi" : "Thêm gói mới"}
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
              <th>Kênh</th>
              <th>Tên gói</th>
              <th>Giá / tháng</th>
              <th>Quyền lợi</th>
              <th>Trạng thái</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {tiers.map((t) => (
              <tr key={t.id}>
                <td><code>{t.id}</code></td>
                <td>{t.channelId}</td>
                <td><strong>{t.name}</strong></td>
                <td>{Number(t.price).toLocaleString("vi-VN")} ₫</td>
                <td className="muted">{t.perks}</td>
                <td>
                  <span className={`badge ${t.active ? "badge-green" : "badge-red"}`}>
                    {t.active ? "Active" : "Inactive"}
                  </span>
                </td>
                <td>
                  <div className="flex-gap">
                    <button type="button" className="btn btn-outline btn-sm" onClick={() => startEdit(t)}>
                      Sửa
                    </button>
                    <button type="button" className="btn btn-danger btn-sm" onClick={() => handleDelete(t.id)}>
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
