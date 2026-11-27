import { NavLink, Outlet } from "react-router-dom";

export default function AdminLayout() {
  return (
    <div className="admin-shell">
      <aside className="admin-sidebar">
        <div className="brand">
          <span className="brand-badge">ADMIN</span>
          <span>YT Microservices</span>
        </div>

        <nav className="admin-nav">
          <NavLink to="/admin" end className={({ isActive }) => (isActive ? "active" : "")}>
            Dashboard
          </NavLink>
          <NavLink to="/admin/users" className={({ isActive }) => (isActive ? "active" : "")}>
            Quản lý Người dùng / Kênh
          </NavLink>
          <NavLink to="/admin/videos" className={({ isActive }) => (isActive ? "active" : "")}>
            Quản lý Video
          </NavLink>
          <NavLink to="/admin/tiers" className={({ isActive }) => (isActive ? "active" : "")}>
            Quản lý Gói hội viên
          </NavLink>
          <NavLink to="/admin/orders" className={({ isActive }) => (isActive ? "active" : "")}>
            Quản lý Đơn thanh toán
          </NavLink>
        </nav>

        <div style={{ marginTop: "auto" }}>
          <NavLink to="/" className="btn btn-outline" style={{ width: "100%" }}>
            ← Về giao diện Client
          </NavLink>
        </div>
      </aside>

      <main className="admin-content">
        <Outlet />
      </main>
    </div>
  );
}
