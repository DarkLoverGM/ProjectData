import axios from "axios";

// Kết nối User Service (Port 3003)
export const userApi = axios.create({
  baseURL: import.meta.env.VITE_USER_API_URL || "http://localhost:3003/api",
  timeout: 10000,
});

// Kết nối Video Service (Port 3004)
export const videoApi = axios.create({
  baseURL: import.meta.env.VITE_VIDEO_API_URL || "http://localhost:3004/api",
  timeout: 10000,
});

// Kết nối Payment Service (Port 3005)
export const paymentApi = axios.create({
  baseURL: import.meta.env.VITE_PAYMENT_API_URL || "http://localhost:3005/api",
  timeout: 10000,
});

export function extractErrorMessage(error, fallback = "Đã xảy ra lỗi không mong đợi") {
  if (error?.response?.data) {
    const data = error.response.data;
    if (data.errors && typeof data.errors === "object") {
      const details = Object.values(data.errors).join("; ");
      return `${data.message || "Dữ liệu không hợp lệ"}: ${details}`;
    }
    if (data.message) {
      return data.message;
    }
  }
  return error?.message || fallback;
}
