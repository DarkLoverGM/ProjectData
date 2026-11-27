import { paymentApi } from "./api";

export const tierService = {
  getAll: async (params = {}) => {
    const response = await paymentApi.get("/tiers", { params });
    return response.data;
  },
  getById: async (id) => {
    const response = await paymentApi.get(`/tiers/${id}`);
    return response.data;
  },
  create: async (data) => {
    const response = await paymentApi.post("/tiers", data);
    return response.data;
  },
  update: async (id, data) => {
    const response = await paymentApi.put(`/tiers/${id}`, data);
    return response.data;
  },
  delete: async (id) => {
    await paymentApi.delete(`/tiers/${id}`);
  },
};
