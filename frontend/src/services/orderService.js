import { paymentApi } from "./api";

export const orderService = {
  getAll: async (params = {}) => {
    const response = await paymentApi.get("/orders", { params });
    return response.data;
  },
  getById: async (id) => {
    const response = await paymentApi.get(`/orders/${id}`);
    return response.data;
  },
  create: async (data) => {
    const response = await paymentApi.post("/orders", data);
    return response.data;
  },
  updateStatus: async (id, status) => {
    const response = await paymentApi.patch(`/orders/${id}/status`, { status });
    return response.data;
  },
  delete: async (id) => {
    await paymentApi.delete(`/orders/${id}`);
  },
  // Kiểm tra giao tiếp đồng bộ RestClient (Câu B)
  getVideoViaPayment: async (videoId) => {
    const response = await paymentApi.get(`/orders/video/${videoId}`);
    return response.data;
  },
  getUserViaPayment: async (userId) => {
    const response = await paymentApi.get(`/orders/user/${userId}`);
    return response.data;
  },
};
