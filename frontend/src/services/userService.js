import { userApi } from "./api";

export const userService = {
  getAll: async (role) => {
    const params = role ? { role } : {};
    const response = await userApi.get("/users", { params });
    return response.data;
  },
  getById: async (id) => {
    const response = await userApi.get(`/users/${id}`);
    return response.data;
  },
  create: async (data) => {
    const response = await userApi.post("/users", data);
    return response.data;
  },
  update: async (id, data) => {
    const response = await userApi.put(`/users/${id}`, data);
    return response.data;
  },
  delete: async (id) => {
    await userApi.delete(`/users/${id}`);
  },
};
