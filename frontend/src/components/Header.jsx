import { NavLink } from "react-router-dom";

export default function Header() {
  return (
    <header className="client-header">
      <div className="container client-header-inner">
        <NavLink to="/" className="brand">
          <span className="brand-badge">▶ YT</span>
          <span>Creator Platform</span>
        </NavLink>

        <nav className="nav-links">
          <NavLink to="/" end className={({ isActive }) => `nav-link ${isActive ? "active" : ""}`}>
            Trang chủ
          </NavLink>
          <NavLink to="/videos" className={({ isActive }) => `nav-link ${isActive ? "active" : ""}`}>
            Video & Super Thanks
          </NavLink>
          <NavLink to="/memberships" className={({ isActive }) => `nav-link ${isActive ? "active" : ""}`}>
            Gói hội viên
          </NavLink>
          <NavLink to="/admin" className="nav-link admin-btn">
            Quản trị (Admin)
          </NavLink>
        </nav>
      </div>
    </header>
  );
}
