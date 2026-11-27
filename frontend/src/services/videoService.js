import { videoApi } from "./api";

export const videoService = {
  getAll: async (filters = {}) => {
    const response = await videoApi.get("/videos", { params: filters });
    return response.data;
  },
  getById: async (id) => {
    const response = await videoApi.get(`/videos/${id}`);
    return response.data;
  },
  create: async (data) => {
    const response = await videoApi.post("/videos", data);
    return response.data;
  },
  update: async (id, data) => {
    const response = await videoApi.put(`/videos/${id}`, data);
    return response.data;
  },
  incrementViews: async (id) => {
    const response = await videoApi.patch(`/videos/${id}/view`);
    return response.data;
  },
  delete: async (id) => {
    await videoApi.delete(`/videos/${id}`);
  },
};
