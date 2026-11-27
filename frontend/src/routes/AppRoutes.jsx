import { Route, Routes } from "react-router-dom";
import ClientLayout from "../layouts/ClientLayout";
import AdminLayout from "../layouts/AdminLayout";

import HomePage from "../pages/client/HomePage";
import VideoListPage from "../pages/client/VideoListPage";
import VideoDetailPage from "../pages/client/VideoDetailPage";
import MembershipPage from "../pages/client/MembershipPage";
import OrderSuccessPage from "../pages/client/OrderSuccessPage";

import DashboardPage from "../pages/admin/DashboardPage";
import UsersAdminPage from "../pages/admin/UsersAdminPage";
import VideosAdminPage from "../pages/admin/VideosAdminPage";
import TiersAdminPage from "../pages/admin/TiersAdminPage";
import OrdersAdminPage from "../pages/admin/OrdersAdminPage";

export default function AppRoutes() {
  return (
    <Routes>
      {/* Giao diện Client */}
      <Route element={<ClientLayout />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/videos" element={<VideoListPage />} />
        <Route path="/videos/:id" element={<VideoDetailPage />} />
        <Route path="/memberships" element={<MembershipPage />} />
        <Route path="/order-success" element={<OrderSuccessPage />} />
      </Route>

      {/* Giao diện Admin */}
      <Route path="/admin" element={<AdminLayout />}>
        <Route index element={<DashboardPage />} />
        <Route path="users" element={<UsersAdminPage />} />
        <Route path="videos" element={<VideosAdminPage />} />
        <Route path="tiers" element={<TiersAdminPage />} />
        <Route path="orders" element={<OrdersAdminPage />} />
      </Route>
    </Routes>
  );
}
