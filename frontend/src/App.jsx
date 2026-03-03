import { useState, useCallback, useEffect } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";

const DEFAULT_API_BASE = "/api/v1";
const REQUEST_TIMEOUT_MS = 12000;
const UI_BUILD_VERSION = "2026.02.28.1";

function normalizeApiBase(value) {
  if (typeof value !== "string") {
    return "";
  }
  return value.trim().replace(/\/+$/, "");
}

const INITIAL_API_BASE = normalizeApiBase(import.meta.env.VITE_API_BASE) || DEFAULT_API_BASE;

const currencyFormatter = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "USD"
});

function formatCurrency(value) {
  const amount = Number(value);
  return Number.isFinite(amount) ? currencyFormatter.format(amount) : "-";
}

function formatNumber(value) {
  const num = Number(value);
  return Number.isFinite(num) ? num.toFixed(2) : "-";
}

function parsePositiveInteger(value) {
  const parsed = Number.parseInt(value, 10);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
}

function parseNonNegativeInteger(value) {
  const parsed = Number.parseInt(value, 10);
  return Number.isInteger(parsed) && parsed >= 0 ? parsed : null;
}

function toIsoDateTimeOrEmpty(value) {
  if (!value) {
    return "";
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "" : date.toISOString();
}

function buildAuditQueryString(params) {
  const query = new URLSearchParams();
  query.set("page", String(params.page ?? 0));
  query.set("size", String(params.size ?? 25));
  if (params.actor) {
    query.set("actor", params.actor);
  }
  if (params.target) {
    query.set("target", params.target);
  }
  if (params.action) {
    query.set("action", params.action);
  }
  if (params.from) {
    query.set("from", params.from);
  }
  if (params.to) {
    query.set("to", params.to);
  }
  if (params.limit) {
    query.set("limit", String(params.limit));
  }
  return query.toString();
}

async function getErrorMessage(response) {
  const fallback = `HTTP ${response.status}`;
  let rawBody = "";

  try {
    rawBody = await response.text();
  } catch {
    return fallback;
  }

  if (!rawBody) {
    return fallback;
  }

  try {
    const data = JSON.parse(rawBody);
    if (typeof data?.message === "string" && data.message.length > 0) {
      return data.message;
    }
    if (typeof data?.error === "string" && data.error.length > 0) {
      return data.error;
    }
    if (typeof data === "string" && data.length > 0) {
      return data;
    }
    return fallback;
  } catch {
    return rawBody;
  }
}

function getNetworkErrorMessage(error) {
  if (error?.name === "AbortError") {
    return "Request timed out. Verify backend is running on port 8080.";
  }
  return "Network issue. Verify backend is running and reachable.";
}

function resolveErrorMessage(error) {
  if (typeof error?.message === "string" && error.message.length > 0 && error.message !== "Failed to fetch") {
    return error.message;
  }
  return getNetworkErrorMessage(error);
}

async function fetchWithTimeout(url, options) {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
  try {
    return await fetch(url, { ...options, signal: controller.signal });
  } finally {
    clearTimeout(timeoutId);
  }
}

// API Functions using React Query patterns
async function login(apiBase, credentials) {
  const response = await fetchWithTimeout(`${apiBase}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(credentials),
  });

  if (!response.ok) {
    const message = await getErrorMessage(response);
    throw new Error(message);
  }

  return response.json();
}

async function register(apiBase, credentials) {
  const response = await fetchWithTimeout(`${apiBase}/auth/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(credentials),
  });

  if (!response.ok) {
    const message = await getErrorMessage(response);
    throw new Error(message);
  }

  return response.json();
}

async function verifyEmail(apiBase, payload) {
  const response = await fetchWithTimeout(`${apiBase}/auth/verify-email`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const message = await getErrorMessage(response);
    throw new Error(message);
  }

  return response.json();
}

async function resendVerification(apiBase, payload) {
  const response = await fetchWithTimeout(`${apiBase}/auth/resend-verification`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const message = await getErrorMessage(response);
    throw new Error(message);
  }

  return response.json();
}

async function forgotPassword(apiBase, payload) {
  const response = await fetchWithTimeout(`${apiBase}/auth/forgot-password`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const message = await getErrorMessage(response);
    throw new Error(message);
  }

  return response.json();
}

async function resetPassword(apiBase, payload) {
  const response = await fetchWithTimeout(`${apiBase}/auth/reset-password`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const message = await getErrorMessage(response);
    throw new Error(message);
  }

  return response.json();
}

async function changePassword(apiBase, token, payload) {
  const response = await fetchWithTimeout(`${apiBase}/auth/change-password`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "Authorization": `Bearer ${token}`
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const message = await getErrorMessage(response);
    throw new Error(message);
  }

  return response.json();
}

async function refreshSessionToken(apiBase, refreshToken) {
  const response = await fetchWithTimeout(`${apiBase}/auth/refresh`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken }),
  });

  if (!response.ok) {
    throw new Error("Session expired. Please login again.");
  }

  return response.json();
}

async function logout(apiBase, token) {
  const response = await fetchWithTimeout(`${apiBase}/auth/logout`, {
    method: "POST",
    headers: { 
      "Content-Type": "application/json",
      "Authorization": `Bearer ${token}`
    },
  });

  if (!response.ok) {
    console.warn("Logout request failed");
  }
}

async function fetchTrending(apiBase, token) {
  const startTime = performance.now();
  const response = await fetchWithTimeout(`${apiBase}/products/trending`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  const endTime = performance.now();
  
  if (!response.ok) {
    throw new Error(await getErrorMessage(response));
  }

  return { data: await response.json(), responseTime: endTime - startTime };
}

async function fetchProduct(apiBase, token, productId) {
  const startTime = performance.now();
  const response = await fetchWithTimeout(`${apiBase}/products/${productId}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  const endTime = performance.now();
  
  if (!response.ok) {
    throw new Error(await getErrorMessage(response));
  }

  return { data: await response.json(), responseTime: endTime - startTime };
}

async function createOrder(apiBase, token, orderData, idempotencyKey) {
  const startTime = performance.now();
  const response = await fetchWithTimeout(`${apiBase}/orders`, {
    method: "POST",
    headers: { 
      "Content-Type": "application/json",
      "Authorization": `Bearer ${token}`,
      "Idempotency-Key": idempotencyKey
    },
    body: JSON.stringify(orderData),
  });
  const endTime = performance.now();

  if (!response.ok) {
    const message = await getErrorMessage(response);
    throw new Error(message);
  }

  return { data: await response.json(), responseTime: endTime - startTime, idempotencyKey };
}

async function fetchMetrics(apiBase, token) {
  const response = await fetchWithTimeout(`${apiBase}/metrics/business`, {
    headers: { Authorization: `Bearer ${token}` },
  });

  if (!response.ok) {
    throw new Error(await getErrorMessage(response));
  }

  return response.json();
}

async function fetchOrderHistory(apiBase, token, statusFilter) {
  const path =
    statusFilter && statusFilter !== "ALL"
      ? `/orders/status/${encodeURIComponent(statusFilter)}`
      : "/orders";
  const response = await fetchWithTimeout(`${apiBase}${path}`, {
    headers: { Authorization: `Bearer ${token}` },
  });

  if (!response.ok) {
    throw new Error(await getErrorMessage(response));
  }

  return response.json();
}

async function fetchAdminUsers(apiBase, token, filters) {
  const params = new URLSearchParams();
  if (filters.query) {
    params.set("query", filters.query);
  }
  if (filters.role) {
    params.set("role", filters.role);
  }
  if (filters.enabled) {
    params.set("enabled", filters.enabled);
  }

  const suffix = params.toString() ? `?${params.toString()}` : "";
  const response = await fetchWithTimeout(`${apiBase}/admin/users${suffix}`, {
    headers: { Authorization: `Bearer ${token}` },
  });

  if (!response.ok) {
    throw new Error(await getErrorMessage(response));
  }

  return response.json();
}

async function fetchAdminAuditLogs(apiBase, token, filters) {
  const suffix = buildAuditQueryString(filters);
  const response = await fetchWithTimeout(`${apiBase}/admin/audit-logs?${suffix}`, {
    headers: { Authorization: `Bearer ${token}` },
  });

  if (!response.ok) {
    throw new Error(await getErrorMessage(response));
  }

  return response.json();
}

async function exportAdminAuditLogs(apiBase, token, filters) {
  const suffix = buildAuditQueryString(filters);
  const response = await fetchWithTimeout(`${apiBase}/admin/audit-logs/export?${suffix}`, {
    headers: { Authorization: `Bearer ${token}` },
  });

  if (!response.ok) {
    throw new Error(await getErrorMessage(response));
  }

  return response.text();
}

async function updateAdminUserEnabled(apiBase, token, userId, enabled) {
  const response = await fetchWithTimeout(`${apiBase}/admin/users/${userId}/enabled`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
      "Authorization": `Bearer ${token}`
    },
    body: JSON.stringify({ enabled }),
  });

  if (!response.ok) {
    throw new Error(await getErrorMessage(response));
  }

  return response.json();
}

async function updateAdminUserRole(apiBase, token, userId, role) {
  const response = await fetchWithTimeout(`${apiBase}/admin/users/${userId}/role`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
      "Authorization": `Bearer ${token}`
    },
    body: JSON.stringify({ role }),
  });

  if (!response.ok) {
    throw new Error(await getErrorMessage(response));
  }

  return response.json();
}

export default function App() {
  const [apiBase, setApiBase] = useState(INITIAL_API_BASE);
  const [credentials, setCredentials] = useState({
    username: "demo@scalemart.dev",
    password: "DemoPass123!"
  });
  const [confirmPassword, setConfirmPassword] = useState("DemoPass123!");
  const [verifyForm, setVerifyForm] = useState({ username: "demo@scalemart.dev", token: "" });
  const [forgotUsername, setForgotUsername] = useState("demo@scalemart.dev");
  const [resetForm, setResetForm] = useState({
    username: "demo@scalemart.dev",
    token: "",
    newPassword: "",
    confirmNewPassword: "",
  });
  const [changePasswordForm, setChangePasswordForm] = useState({
    currentPassword: "",
    newPassword: "",
    confirmNewPassword: "",
  });
  const [debugVerificationToken, setDebugVerificationToken] = useState("");
  const [debugResetToken, setDebugResetToken] = useState("");
  const [currentUsername, setCurrentUsername] = useState("");
  const [userRole, setUserRole] = useState("");
  const [token, setToken] = useState("");
  const [refreshTokenValue, setRefreshTokenValue] = useState("");
  const [tokenExpiry, setTokenExpiry] = useState(null);
  const [productId, setProductId] = useState("1");
  const [quantity, setQuantity] = useState("1");
  const [orderStatusFilter, setOrderStatusFilter] = useState("ALL");
  const [lastIdempotencyKey, setLastIdempotencyKey] = useState("");
  const [auditPage, setAuditPage] = useState(0);
  const [auditPageSize, setAuditPageSize] = useState("25");
  const [auditExportLimit, setAuditExportLimit] = useState("500");
  const [auditFilters, setAuditFilters] = useState({
    actor: "",
    target: "",
    action: "",
    from: "",
    to: "",
  });
  const [adminFilters, setAdminFilters] = useState({
    query: "",
    role: "",
    enabled: "",
  });
  const [product, setProduct] = useState(null);
  const [latestOrder, setLatestOrder] = useState(null);
  const [notice, setNotice] = useState({ type: "info", text: "" });
  const [lastResponseTime, setLastResponseTime] = useState(null);
  
  const queryClient = useQueryClient();
  const isAdmin = userRole === "ADMIN";
  const parsedAuditPage = parseNonNegativeInteger(String(auditPage));
  const auditPageValue = parsedAuditPage == null ? 0 : parsedAuditPage;
  const parsedAuditPageSize = parsePositiveInteger(auditPageSize);
  const auditPageSizeValue = parsedAuditPageSize ? Math.min(parsedAuditPageSize, 200) : 25;
  const parsedAuditExportLimit = parsePositiveInteger(auditExportLimit);
  const auditExportLimitValue = parsedAuditExportLimit ? Math.min(parsedAuditExportLimit, 5000) : 500;
  const auditFromIso = toIsoDateTimeOrEmpty(auditFilters.from);
  const auditToIso = toIsoDateTimeOrEmpty(auditFilters.to);
  const auditQueryParams = {
    page: auditPageValue,
    size: auditPageSizeValue,
    actor: auditFilters.actor.trim(),
    target: auditFilters.target.trim(),
    action: auditFilters.action.trim(),
    from: auditFromIso,
    to: auditToIso,
  };
  const auditExportParams = {
    ...auditQueryParams,
    limit: auditExportLimitValue,
  };

  // Validate token expiry
  const isTokenValid =
    typeof token === "string" &&
    token.length > 0 &&
    typeof tokenExpiry === "number" &&
    tokenExpiry * 1000 > Date.now();

  // Login mutation
  const loginMutation = useMutation({
    mutationFn: () => login(apiBase, credentials),
    onSuccess: (data) => {
      setToken(data.token);
      setRefreshTokenValue(data.refreshToken);
      setTokenExpiry(data.expiresAtEpochSeconds);
      setUserRole(data.role || "USER");
      const username = credentials.username.trim();
      setCurrentUsername(username);
      setVerifyForm((prev) => ({ ...prev, username }));
      setForgotUsername(username);
      setResetForm((prev) => ({ ...prev, username }));
      setDebugVerificationToken("");
      setDebugResetToken("");
      setNotice({ type: "success", text: `Logged in as ${credentials.username}` });
    },
    onError: (error) => {
      setNotice({ type: "error", text: resolveErrorMessage(error) });
    },
  });

  // Register mutation
  const registerMutation = useMutation({
    mutationFn: () => register(apiBase, credentials),
    onSuccess: (data) => {
      const username = credentials.username.trim();
      setDebugVerificationToken(data.debugToken || "");
      setVerifyForm((prev) => ({
        ...prev,
        username,
        token: data.debugToken || prev.token,
      }));
      setForgotUsername(username);
      setResetForm((prev) => ({
        ...prev,
        username,
      }));
      setNotice({
        type: "success",
        text: data.message || `Account created for ${username}. Verify email before login.`,
      });
    },
    onError: (error) => {
      setNotice({ type: "error", text: resolveErrorMessage(error) });
    },
  });

  const verifyEmailMutation = useMutation({
    mutationFn: () => verifyEmail(apiBase, {
      username: verifyForm.username,
      token: verifyForm.token,
    }),
    onSuccess: (data) => {
      setDebugVerificationToken("");
      setNotice({ type: "success", text: data.message || "Email verified successfully." });
    },
    onError: (error) => {
      setNotice({ type: "error", text: resolveErrorMessage(error) });
    },
  });

  const resendVerificationMutation = useMutation({
    mutationFn: () => resendVerification(apiBase, { username: verifyForm.username }),
    onSuccess: (data) => {
      if (data.debugToken) {
        setDebugVerificationToken(data.debugToken);
        setVerifyForm((prev) => ({ ...prev, token: data.debugToken }));
      }
      setNotice({ type: "info", text: data.message || "Verification token sent." });
    },
    onError: (error) => {
      setNotice({ type: "error", text: resolveErrorMessage(error) });
    },
  });

  const forgotPasswordMutation = useMutation({
    mutationFn: () => forgotPassword(apiBase, { username: forgotUsername }),
    onSuccess: (data) => {
      setDebugResetToken(data.debugToken || "");
      setResetForm((prev) => ({
        ...prev,
        username: forgotUsername.trim(),
        token: data.debugToken || prev.token,
      }));
      setNotice({
        type: "info",
        text: data.message || "Password reset instructions generated.",
      });
    },
    onError: (error) => {
      setNotice({ type: "error", text: resolveErrorMessage(error) });
    },
  });

  const resetPasswordMutation = useMutation({
    mutationFn: () => resetPassword(apiBase, {
      username: resetForm.username,
      token: resetForm.token,
      newPassword: resetForm.newPassword,
    }),
    onSuccess: (data) => {
      setDebugResetToken("");
      setResetForm((prev) => ({
        ...prev,
        token: "",
        newPassword: "",
        confirmNewPassword: "",
      }));
      setNotice({
        type: "success",
        text: data.message || "Password reset successful. Login with new password.",
      });
    },
    onError: (error) => {
      setNotice({ type: "error", text: resolveErrorMessage(error) });
    },
  });

  const changePasswordMutation = useMutation({
    mutationFn: () =>
      changePassword(apiBase, token, {
        currentPassword: changePasswordForm.currentPassword,
        newPassword: changePasswordForm.newPassword,
      }),
    onSuccess: (data) => {
      setChangePasswordForm({
        currentPassword: "",
        newPassword: "",
        confirmNewPassword: "",
      });
      setNotice({
        type: "success",
        text: data.message || "Password changed successfully.",
      });
    },
    onError: (error) => {
      setNotice({ type: "error", text: resolveErrorMessage(error) });
    },
  });

  // Logout mutation
  const logoutMutation = useMutation({
    mutationFn: () => logout(apiBase, token),
    onSuccess: () => {
      setToken("");
      setRefreshTokenValue("");
      setTokenExpiry(null);
      setUserRole("");
      setCurrentUsername("");
      setDebugVerificationToken("");
      setDebugResetToken("");
      setProduct(null);
      setLatestOrder(null);
      setNotice({ type: "info", text: "Logged out successfully" });
    },
  });

  // Trending products query
  const trendingQuery = useQuery({
    queryKey: ["trending", apiBase, token],
    queryFn: () => fetchTrending(apiBase, token),
    enabled: isTokenValid,
  });

  // Product query
  const productQuery = useQuery({
    queryKey: ["product", apiBase, token, productId],
    queryFn: () => fetchProduct(apiBase, token, productId),
    enabled: isTokenValid && !!productId,
  });

  // Metrics query
  const metricsQuery = useQuery({
    queryKey: ["metrics", apiBase, token],
    queryFn: () => fetchMetrics(apiBase, token),
    enabled: isTokenValid,
    refetchInterval: 10000, // Refresh every 10 seconds
  });

  const orderHistoryQuery = useQuery({
    queryKey: ["orders", apiBase, token, orderStatusFilter],
    queryFn: () => fetchOrderHistory(apiBase, token, orderStatusFilter),
    enabled: isTokenValid,
  });

  const adminUsersQuery = useQuery({
    queryKey: ["admin-users", apiBase, token, adminFilters],
    queryFn: () => fetchAdminUsers(apiBase, token, adminFilters),
    enabled: isTokenValid && isAdmin,
  });

  const adminAuditQuery = useQuery({
    queryKey: ["admin-audit", apiBase, token, auditQueryParams],
    queryFn: () => fetchAdminAuditLogs(apiBase, token, auditQueryParams),
    enabled: isTokenValid && isAdmin,
  });

  // Order mutation
  const orderMutation = useMutation({
    mutationFn: () => {
      const parsedId = parsePositiveInteger(productId);
      const parsedQty = parsePositiveInteger(quantity);
      if (!parsedId || !parsedQty) {
        throw new Error("Product ID and quantity must be positive numbers.");
      }
      const idempotencyKey =
        typeof globalThis.crypto?.randomUUID === "function"
          ? globalThis.crypto.randomUUID()
          : `${Date.now()}-${Math.random().toString(16).slice(2)}`;
      setLastIdempotencyKey(idempotencyKey);
      return createOrder(apiBase, token, { 
        productId: parsedId, 
        quantity: parsedQty 
      }, idempotencyKey);
    },
    onSuccess: (result) => {
      setLatestOrder(result.data);
      setLastResponseTime(result.responseTime);
      setNotice({ type: "success", text: "Order submitted successfully!" });
      // Refresh metrics after order
      queryClient.invalidateQueries({ queryKey: ["metrics"] });
      queryClient.invalidateQueries({ queryKey: ["orders"] });
    },
    onError: (error) => {
      setNotice({ type: "error", text: resolveErrorMessage(error) });
    },
  });

  const updateUserEnabledMutation = useMutation({
    mutationFn: ({ userId, enabled }) => updateAdminUserEnabled(apiBase, token, userId, enabled),
    onSuccess: (updated) => {
      setNotice({
        type: "success",
        text: `${updated.username} is now ${updated.enabled ? "enabled" : "disabled"}.`
      });
      queryClient.invalidateQueries({ queryKey: ["admin-users"] });
      queryClient.invalidateQueries({ queryKey: ["admin-audit"] });
    },
    onError: (error) => {
      setNotice({ type: "error", text: resolveErrorMessage(error) });
    },
  });

  const updateUserRoleMutation = useMutation({
    mutationFn: ({ userId, role }) => updateAdminUserRole(apiBase, token, userId, role),
    onSuccess: (updated) => {
      setNotice({
        type: "success",
        text: `${updated.username} role changed to ${updated.role}.`
      });
      queryClient.invalidateQueries({ queryKey: ["admin-users"] });
      queryClient.invalidateQueries({ queryKey: ["admin-audit"] });
    },
    onError: (error) => {
      setNotice({ type: "error", text: resolveErrorMessage(error) });
    },
  });

  const exportAdminAuditMutation = useMutation({
    mutationFn: () => exportAdminAuditLogs(apiBase, token, auditExportParams),
    onSuccess: (csvText) => {
      const blob = new Blob([csvText], { type: "text/csv;charset=utf-8" });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = "admin-audit-logs.csv";
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
      setNotice({ type: "success", text: "Audit CSV exported." });
    },
    onError: (error) => {
      setNotice({ type: "error", text: resolveErrorMessage(error) });
    },
  });

  // Handle login
  const handleLogin = useCallback(() => {
    loginMutation.mutate();
  }, [loginMutation]);

  // Handle register
  const handleRegister = useCallback(() => {
    const username = credentials.username.trim();
    if (!username) {
      setNotice({ type: "error", text: "Username is required." });
      return;
    }

    if (!credentials.password) {
      setNotice({ type: "error", text: "Password is required." });
      return;
    }

    if (credentials.password !== confirmPassword) {
      setNotice({ type: "error", text: "Passwords do not match." });
      return;
    }

    registerMutation.mutate();
  }, [credentials, confirmPassword, registerMutation]);

  const handleVerifyEmail = useCallback(() => {
    if (!verifyForm.username.trim()) {
      setNotice({ type: "error", text: "Username is required for email verification." });
      return;
    }
    if (!verifyForm.token.trim()) {
      setNotice({ type: "error", text: "Verification token is required." });
      return;
    }
    verifyEmailMutation.mutate();
  }, [verifyForm, verifyEmailMutation]);

  const handleResendVerification = useCallback(() => {
    if (!verifyForm.username.trim()) {
      setNotice({ type: "error", text: "Username is required to resend verification." });
      return;
    }
    resendVerificationMutation.mutate();
  }, [verifyForm.username, resendVerificationMutation]);

  const handleForgotPassword = useCallback(() => {
    if (!forgotUsername.trim()) {
      setNotice({ type: "error", text: "Username is required for password reset." });
      return;
    }
    forgotPasswordMutation.mutate();
  }, [forgotUsername, forgotPasswordMutation]);

  const handleResetPassword = useCallback(() => {
    if (!resetForm.username.trim()) {
      setNotice({ type: "error", text: "Username is required." });
      return;
    }
    if (!resetForm.token.trim()) {
      setNotice({ type: "error", text: "Reset token is required." });
      return;
    }
    if (!resetForm.newPassword) {
      setNotice({ type: "error", text: "New password is required." });
      return;
    }
    if (resetForm.newPassword.length < 8) {
      setNotice({ type: "error", text: "New password must be at least 8 characters." });
      return;
    }
    if (resetForm.newPassword !== resetForm.confirmNewPassword) {
      setNotice({ type: "error", text: "New passwords do not match." });
      return;
    }
    resetPasswordMutation.mutate();
  }, [resetForm, resetPasswordMutation]);

  const handleChangePassword = useCallback(() => {
    if (!changePasswordForm.currentPassword) {
      setNotice({ type: "error", text: "Current password is required." });
      return;
    }
    if (!changePasswordForm.newPassword) {
      setNotice({ type: "error", text: "New password is required." });
      return;
    }
    if (changePasswordForm.newPassword.length < 8) {
      setNotice({ type: "error", text: "New password must be at least 8 characters." });
      return;
    }
    if (changePasswordForm.newPassword !== changePasswordForm.confirmNewPassword) {
      setNotice({ type: "error", text: "New passwords do not match." });
      return;
    }
    if (changePasswordForm.currentPassword === changePasswordForm.newPassword) {
      setNotice({ type: "error", text: "New password must be different from current password." });
      return;
    }
    changePasswordMutation.mutate();
  }, [changePasswordForm, changePasswordMutation]);

  // Handle logout
  const handleLogout = useCallback(() => {
    logoutMutation.mutate();
  }, [logoutMutation]);

  // Handle token refresh
  const handleRefreshToken = useCallback(async () => {
    try {
      const data = await refreshSessionToken(apiBase, refreshTokenValue);
      setToken(data.token);
      setRefreshTokenValue(data.refreshToken);
      setTokenExpiry(data.expiresAtEpochSeconds);
      setUserRole(data.role || userRole || "USER");
      setNotice({ type: "success", text: "Session refreshed" });
    } catch (error) {
      setNotice({ type: "error", text: resolveErrorMessage(error) });
      setToken("");
      setRefreshTokenValue("");
      setTokenExpiry(null);
      setUserRole("");
      setCurrentUsername("");
    }
  }, [apiBase, refreshTokenValue, userRole]);

  // Handle product fetch
  const handleGetProduct = useCallback(() => {
    if (productId) {
      queryClient.invalidateQueries({ queryKey: ["product", productId] });
    }
  }, [productId, queryClient]);

  // Handle trending fetch
  const handleGetTrending = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: ["trending"] });
  }, [queryClient]);

  // Handle order creation
  const handleCreateOrder = useCallback(() => {
    orderMutation.mutate();
  }, [orderMutation]);

  const handleRefreshOrderHistory = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: ["orders"] });
  }, [queryClient]);

  const handleRefreshAdminUsers = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: ["admin-users"] });
  }, [queryClient]);

  const handleRefreshAdminAudit = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: ["admin-audit"] });
  }, [queryClient]);

  const handleExportAdminAudit = useCallback(() => {
    exportAdminAuditMutation.mutate();
  }, [exportAdminAuditMutation]);

  const handleAuditPageSizeChange = useCallback((value) => {
    setAuditPageSize(value);
    setAuditPage(0);
  }, []);

  const handleAuditFilterChange = useCallback((key, value) => {
    setAuditFilters((prev) => ({ ...prev, [key]: value }));
    setAuditPage(0);
  }, []);

  const handleAuditPrevPage = useCallback(() => {
    setAuditPage((prev) => Math.max(0, prev - 1));
  }, []);

  const handleAuditNextPage = useCallback(() => {
    setAuditPage((prev) => prev + 1);
  }, []);

  // Update product and response time from query
  useEffect(() => {
    if (productQuery.data) {
      setProduct(productQuery.data.data);
      setLastResponseTime(productQuery.data.responseTime);
    }
  }, [productQuery.data]);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const username = params.get("username");
    const token = params.get("token");
    const isVerify = params.get("verify") === "1";
    const isReset = params.get("reset") === "1";

    if (!username || !token) {
      return;
    }

    if (isVerify) {
      setVerifyForm((prev) => ({ ...prev, username, token }));
      setNotice({ type: "info", text: "Verification token loaded from URL. Click Verify Email." });
    }

    if (isReset) {
      setResetForm((prev) => ({ ...prev, username, token }));
      setForgotUsername(username);
      setNotice({ type: "info", text: "Reset token loaded from URL. Enter new password and submit." });
    }
  }, []);

  const loading = {
    login: loginMutation.isPending,
    register: registerMutation.isPending,
    verifyEmail: verifyEmailMutation.isPending,
    resendVerification: resendVerificationMutation.isPending,
    forgotPassword: forgotPasswordMutation.isPending,
    resetPassword: resetPasswordMutation.isPending,
    changePassword: changePasswordMutation.isPending,
    logout: logoutMutation.isPending,
    product: productQuery.isFetching,
    trending: trendingQuery.isFetching,
    order: orderMutation.isPending,
    metrics: metricsQuery.isFetching,
    orders: orderHistoryQuery.isFetching,
    adminUsers: adminUsersQuery.isFetching,
    adminAudit: adminAuditQuery.isFetching,
    adminAuditExport: exportAdminAuditMutation.isPending,
    adminUpdate: updateUserEnabledMutation.isPending || updateUserRoleMutation.isPending,
  };
  const auditEntries = adminAuditQuery.data?.content || [];
  const auditTotalPages = typeof adminAuditQuery.data?.totalPages === "number"
    ? adminAuditQuery.data.totalPages
    : 0;
  const auditTotalElements = typeof adminAuditQuery.data?.totalElements === "number"
    ? adminAuditQuery.data.totalElements
    : 0;
  const isLastAuditPage = auditTotalPages === 0 || auditPageValue >= auditTotalPages - 1;

  return (
    <main className="app-shell" data-build={UI_BUILD_VERSION}>
      <div className="orb orb-left" aria-hidden="true" />
      <div className="orb orb-right" aria-hidden="true" />
      <header className="panel-head">
        <h1>Scalemart Performance Test Suite</h1>
        <div className="header-controls">
          <label className="compact-field">
            API Base
            <input
              type="text"
              value={apiBase}
              onChange={(event) => setApiBase(normalizeApiBase(event.target.value))}
              placeholder={DEFAULT_API_BASE}
            />
          </label>
          {isTokenValid ? (
            <button className="btn btn-secondary" onClick={handleLogout} disabled={loading.logout}>
              {loading.logout ? "Logging out..." : "Logout"}
            </button>
          ) : null}
        </div>
      </header>

      {!isTokenValid ? (
        <section className="panel reveal delay-1">
          <div className="panel-head">
            <h2>Authentication</h2>
          </div>

          <div className="control-row">
            <label className="compact-field">
              Username
              <input
                type="text"
                value={credentials.username}
                onChange={(event) => setCredentials((prev) => ({ ...prev, username: event.target.value }))}
              />
            </label>

            <label className="compact-field">
              Password
              <input
                type="password"
                value={credentials.password}
                onChange={(event) => setCredentials((prev) => ({ ...prev, password: event.target.value }))}
              />
            </label>

            <label className="compact-field">
              Confirm Password
              <input
                type="password"
                value={confirmPassword}
                onChange={(event) => setConfirmPassword(event.target.value)}
              />
            </label>

            <button className="btn btn-primary" onClick={handleLogin} disabled={loading.login || loading.register}>
              {loading.login ? "Logging in..." : "Login"}
            </button>

            <button className="btn btn-alt" onClick={handleRegister} disabled={loading.register || loading.login}>
              {loading.register ? "Creating..." : "Register"}
            </button>
          </div>

          <p className="token-info">
            New user? Register first, then verify email before login.
          </p>

          <section className="auth-extensions">
            <article className="auth-subpanel">
              <h3>Email Verification</h3>
              <div className="control-row">
                <label className="compact-field">
                  Username
                  <input
                    type="text"
                    value={verifyForm.username}
                    onChange={(event) =>
                      setVerifyForm((prev) => ({ ...prev, username: event.target.value }))
                    }
                    placeholder="user@example.com"
                  />
                </label>
                <label className="compact-field">
                  Verification Token
                  <input
                    type="text"
                    value={verifyForm.token}
                    onChange={(event) =>
                      setVerifyForm((prev) => ({ ...prev, token: event.target.value }))
                    }
                    placeholder="Paste token"
                  />
                </label>
                <button
                  className="btn btn-secondary"
                  onClick={handleVerifyEmail}
                  disabled={loading.verifyEmail || loading.resendVerification}
                >
                  {loading.verifyEmail ? "Verifying..." : "Verify Email"}
                </button>
                <button
                  className="btn btn-alt"
                  onClick={handleResendVerification}
                  disabled={loading.resendVerification || loading.verifyEmail}
                >
                  {loading.resendVerification ? "Sending..." : "Resend Token"}
                </button>
              </div>
              {debugVerificationToken ? (
                <p className="token-info">
                  Debug verification token: <code>{debugVerificationToken}</code>
                </p>
              ) : null}
            </article>

            <article className="auth-subpanel">
              <h3>Password Recovery</h3>
              <div className="control-row">
                <label className="compact-field">
                  Username
                  <input
                    type="text"
                    value={forgotUsername}
                    onChange={(event) => setForgotUsername(event.target.value)}
                    placeholder="user@example.com"
                  />
                </label>
                <button
                  className="btn btn-secondary"
                  onClick={handleForgotPassword}
                  disabled={loading.forgotPassword}
                >
                  {loading.forgotPassword ? "Sending..." : "Generate Reset Token"}
                </button>
              </div>

              <div className="control-row">
                <label className="compact-field">
                  Username
                  <input
                    type="text"
                    value={resetForm.username}
                    onChange={(event) =>
                      setResetForm((prev) => ({ ...prev, username: event.target.value }))
                    }
                    placeholder="user@example.com"
                  />
                </label>
                <label className="compact-field">
                  Reset Token
                  <input
                    type="text"
                    value={resetForm.token}
                    onChange={(event) =>
                      setResetForm((prev) => ({ ...prev, token: event.target.value }))
                    }
                    placeholder="Paste token"
                  />
                </label>
                <label className="compact-field">
                  New Password
                  <input
                    type="password"
                    value={resetForm.newPassword}
                    onChange={(event) =>
                      setResetForm((prev) => ({ ...prev, newPassword: event.target.value }))
                    }
                  />
                </label>
                <label className="compact-field">
                  Confirm New Password
                  <input
                    type="password"
                    value={resetForm.confirmNewPassword}
                    onChange={(event) =>
                      setResetForm((prev) => ({ ...prev, confirmNewPassword: event.target.value }))
                    }
                  />
                </label>
                <button
                  className="btn btn-alt"
                  onClick={handleResetPassword}
                  disabled={loading.resetPassword}
                >
                  {loading.resetPassword ? "Resetting..." : "Reset Password"}
                </button>
              </div>
              {debugResetToken ? (
                <p className="token-info">
                  Debug reset token: <code>{debugResetToken}</code>
                </p>
              ) : null}
            </article>
          </section>
        </section>
      ) : (
        <>
          {/* Performance Metrics Panel */}
          <section className="panel reveal delay-0">
            <div className="panel-head">
              <h2>📊 Performance Metrics</h2>
              <button 
                className="btn btn-secondary" 
                onClick={() => queryClient.invalidateQueries({ queryKey: ["metrics"] })}
                disabled={loading.metrics}
              >
                {loading.metrics ? "Refreshing..." : "Refresh"}
              </button>
            </div>

            <div className="metrics-grid">
              <div className="metric-card">
                <span className="metric-label">Last Response Time</span>
                <strong className="metric-value">
                  {lastResponseTime ? `${formatNumber(lastResponseTime)} ms` : "-"}
                </strong>
              </div>
              <div className="metric-card">
                <span className="metric-label">Avg Login Time</span>
                <strong className="metric-value">
                  {metricsQuery.data ? `${formatNumber(metricsQuery.data.avgLoginTimeMs)} ms` : "-"}
                </strong>
              </div>
              <div className="metric-card">
                <span className="metric-label">Avg Order Time</span>
                <strong className="metric-value">
                  {metricsQuery.data ? `${formatNumber(metricsQuery.data.avgOrderTimeMs)} ms` : "-"}
                </strong>
              </div>
              <div className="metric-card">
                <span className="metric-label">Cache Hit Ratio</span>
                <strong className="metric-value">
                  {metricsQuery.data ? `${formatNumber(metricsQuery.data.cacheHitRatio * 100)}%` : "-"}
                </strong>
              </div>
              <div className="metric-card">
                <span className="metric-label">Login Success</span>
                <strong className="metric-value success">
                  {metricsQuery.data ? metricsQuery.data.loginSuccess : 0}
                </strong>
              </div>
              <div className="metric-card">
                <span className="metric-label">Orders Processed</span>
                <strong className="metric-value success">
                  {metricsQuery.data ? metricsQuery.data.orderSuccess : 0}
                </strong>
              </div>
            </div>
          </section>

          <section className="panel reveal delay-1">
            <div className="panel-head">
              <h2>Session Info</h2>
              <button className="btn btn-secondary" onClick={handleRefreshToken}>
                Refresh Token
              </button>
            </div>

            <div className="session-info">
              <div>
                <span>User</span>
                <strong>{currentUsername || credentials.username}</strong>
              </div>
              <div>
                <span>Role</span>
                <strong>{userRole || "-"}</strong>
              </div>
              <div>
                <span>Status</span>
                <strong>Authenticated</strong>
              </div>
              <div>
                <span>Expires</span>
                <strong>{tokenExpiry ? new Date(tokenExpiry * 1000).toLocaleString() : "N/A"}</strong>
              </div>
            </div>
          </section>

          <section className="panel reveal delay-1">
            <div className="panel-head">
              <h2>Security</h2>
              <span className="hint">Change account password</span>
            </div>

            <div className="control-row">
              <label className="compact-field">
                Current Password
                <input
                  type="password"
                  value={changePasswordForm.currentPassword}
                  onChange={(event) =>
                    setChangePasswordForm((prev) => ({ ...prev, currentPassword: event.target.value }))
                  }
                />
              </label>
              <label className="compact-field">
                New Password
                <input
                  type="password"
                  value={changePasswordForm.newPassword}
                  onChange={(event) =>
                    setChangePasswordForm((prev) => ({ ...prev, newPassword: event.target.value }))
                  }
                />
              </label>
              <label className="compact-field">
                Confirm New Password
                <input
                  type="password"
                  value={changePasswordForm.confirmNewPassword}
                  onChange={(event) =>
                    setChangePasswordForm((prev) => ({ ...prev, confirmNewPassword: event.target.value }))
                  }
                />
              </label>
              <button
                className="btn btn-primary"
                onClick={handleChangePassword}
                disabled={loading.changePassword}
              >
                {loading.changePassword ? "Updating..." : "Change Password"}
              </button>
            </div>
          </section>

          <section className="panel reveal delay-2">
            <div className="panel-head">
              <h2>Product Lookup</h2>
              <span className="hint">GET /products/:id</span>
            </div>

            <div className="control-row">
              <label className="compact-field">
                Product ID
                <input
                  type="number"
                  min="1"
                  value={productId}
                  onChange={(event) => setProductId(event.target.value)}
                  placeholder="1"
                />
              </label>

              <button
                className="btn btn-primary"
                onClick={handleGetProduct}
                disabled={loading.product}
              >
                {loading.product ? "Loading..." : "Get Product"}
              </button>
            </div>

            {product ? (
              <article className="product-card">
                <div>
                  <span>ID</span>
                  <strong>#{product.id}</strong>
                </div>
                <div>
                  <span>Name</span>
                  <strong>{product.name}</strong>
                </div>
                <div>
                  <span>Category</span>
                  <strong>{product.category}</strong>
                </div>
                <div>
                  <span>Price</span>
                  <strong>{formatCurrency(product.price)}</strong>
                </div>
              </article>
            ) : (
              <article className="result-placeholder">
                No product loaded. Click Get Product to load data.
              </article>
            )}
          </section>

          <section className="panel reveal delay-3">
            <div className="panel-head">
              <h2>Trending Products</h2>
              <span className="hint">GET /products/trending</span>
            </div>

            <button
              className="btn btn-secondary"
              onClick={handleGetTrending}
              disabled={loading.trending}
            >
              {loading.trending ? "Loading..." : "Get Trending"}
            </button>

            {trendingQuery.isSuccess && trendingQuery.data.data.length > 0 ? (
              <article className="product-list">
                <ul>
                  {trendingQuery.data.data.map((item) => (
                    <li key={item.id}>
                      <span>{item.name}</span>
                      <span>{formatCurrency(item.price)}</span>
                    </li>
                  ))}
                </ul>
              </article>
            ) : (
              <article className="result-placeholder">
                Trending feed is empty. Use Get Trending to load data.
              </article>
            )}
          </section>

          <section className="panel reveal delay-4">
            <div className="panel-head">
              <h2>Async Order Composer</h2>
              <span className="hint">POST /orders (Kafka + Worker)</span>
            </div>

            <div className="control-row">
              <label className="compact-field">
                Product ID
                <input
                  type="number"
                  min="1"
                  value={productId}
                  onChange={(event) => setProductId(event.target.value)}
                  placeholder="1"
                />
              </label>

              <label className="compact-field">
                Quantity
                <input
                  type="number"
                  min="1"
                  value={quantity}
                  onChange={(event) => setQuantity(event.target.value)}
                  placeholder="1"
                />
              </label>

              <button
                className="btn btn-primary"
                onClick={handleCreateOrder}
                disabled={loading.order}
              >
                {loading.order ? "Submitting..." : "Submit Order"}
              </button>
            </div>

            {lastIdempotencyKey ? (
              <p className="token-info">Last Idempotency Key: <code>{lastIdempotencyKey}</code></p>
            ) : null}

            {latestOrder ? (
              <article className="order-card">
                <div>
                  <span>Order ID</span>
                  <strong>#{latestOrder.orderId}</strong>
                </div>
                <div>
                  <span>Status</span>
                  <strong>{latestOrder.status}</strong>
                </div>
                <div>
                  <span>Total</span>
                  <strong>{formatCurrency(latestOrder.totalAmount)}</strong>
                </div>
                <div>
                  <span>Created</span>
                  <strong>{new Date(latestOrder.createdAt).toLocaleString()}</strong>
                </div>
              </article>
            ) : (
              <article className="result-placeholder">
                No order submitted in this session yet.
              </article>
            )}
          </section>

          <section className="panel reveal delay-4">
            <div className="panel-head">
              <h2>Order History</h2>
              <button className="btn btn-secondary" onClick={handleRefreshOrderHistory} disabled={loading.orders}>
                {loading.orders ? "Refreshing..." : "Refresh"}
              </button>
            </div>

            <div className="control-row">
              <label className="compact-field">
                Status
                <select
                  value={orderStatusFilter}
                  onChange={(event) => setOrderStatusFilter(event.target.value)}
                >
                  <option value="ALL">All</option>
                  <option value="PENDING">Pending</option>
                  <option value="PROCESSING">Processing</option>
                  <option value="COMPLETED">Completed</option>
                  <option value="FAILED">Failed</option>
                </select>
              </label>
            </div>

            {orderHistoryQuery.data && orderHistoryQuery.data.length > 0 ? (
              <article className="history-list">
                <ul>
                  {orderHistoryQuery.data.slice(0, 12).map((order) => (
                    <li key={order.orderId}>
                      <span>#{order.orderId}</span>
                      <span>{order.status}</span>
                      <span>{formatCurrency(order.totalAmount)}</span>
                      <span>{new Date(order.createdAt).toLocaleString()}</span>
                    </li>
                  ))}
                </ul>
              </article>
            ) : (
              <article className="result-placeholder">
                No orders found for this filter.
              </article>
            )}
          </section>

          {isAdmin ? (
            <section className="panel reveal delay-4">
              <div className="panel-head">
                <h2>Admin: User Management</h2>
                <button className="btn btn-secondary" onClick={handleRefreshAdminUsers} disabled={loading.adminUsers}>
                  {loading.adminUsers ? "Refreshing..." : "Refresh"}
                </button>
              </div>

              <div className="control-row">
                <label className="compact-field">
                  Search
                  <input
                    type="text"
                    value={adminFilters.query}
                    onChange={(event) => setAdminFilters((prev) => ({ ...prev, query: event.target.value }))}
                    placeholder="username or email"
                  />
                </label>

                <label className="compact-field">
                  Role
                  <select
                    value={adminFilters.role}
                    onChange={(event) => setAdminFilters((prev) => ({ ...prev, role: event.target.value }))}
                  >
                    <option value="">All</option>
                    <option value="USER">USER</option>
                    <option value="ADMIN">ADMIN</option>
                  </select>
                </label>

                <label className="compact-field">
                  Enabled
                  <select
                    value={adminFilters.enabled}
                    onChange={(event) => setAdminFilters((prev) => ({ ...prev, enabled: event.target.value }))}
                  >
                    <option value="">All</option>
                    <option value="true">Enabled</option>
                    <option value="false">Disabled</option>
                  </select>
                </label>
              </div>

              {adminUsersQuery.data && adminUsersQuery.data.length > 0 ? (
                <article className="admin-list">
                  <ul>
                    {adminUsersQuery.data.map((user) => (
                      <li key={user.id}>
                        <div className="admin-meta">
                          <strong>{user.username}</strong>
                          <span>{user.email || "-"}</span>
                          <span>Role: <code>{user.role}</code></span>
                          <span>Status: <code>{user.enabled ? "ENABLED" : "DISABLED"}</code></span>
                        </div>
                        <div className="admin-actions">
                          <button
                            className="btn btn-secondary"
                            disabled={loading.adminUpdate}
                            onClick={() => updateUserEnabledMutation.mutate({ userId: user.id, enabled: !user.enabled })}
                          >
                            {user.enabled ? "Disable" : "Enable"}
                          </button>
                          <button
                            className="btn btn-alt"
                            disabled={loading.adminUpdate}
                            onClick={() =>
                              updateUserRoleMutation.mutate({
                                userId: user.id,
                                role: user.role === "ADMIN" ? "USER" : "ADMIN"
                              })
                            }
                          >
                            {user.role === "ADMIN" ? "Make USER" : "Make ADMIN"}
                          </button>
                        </div>
                      </li>
                    ))}
                  </ul>
                </article>
              ) : (
                <article className="result-placeholder">
                  No users found for current filters.
                </article>
              )}
            </section>
          ) : null}

          {isAdmin ? (
            <section className="panel reveal delay-4">
              <div className="panel-head">
                <h2>Admin: Audit Logs</h2>
                <div className="admin-actions">
                  <button className="btn btn-secondary" onClick={handleRefreshAdminAudit} disabled={loading.adminAudit}>
                    {loading.adminAudit ? "Refreshing..." : "Refresh"}
                  </button>
                  <button className="btn btn-alt" onClick={handleExportAdminAudit} disabled={loading.adminAuditExport}>
                    {loading.adminAuditExport ? "Exporting..." : "Export CSV"}
                  </button>
                </div>
              </div>

              <div className="control-row">
                <label className="compact-field">
                  Actor
                  <input
                    type="text"
                    value={auditFilters.actor}
                    onChange={(event) => handleAuditFilterChange("actor", event.target.value)}
                    placeholder="admin username"
                  />
                </label>
                <label className="compact-field">
                  Target
                  <input
                    type="text"
                    value={auditFilters.target}
                    onChange={(event) => handleAuditFilterChange("target", event.target.value)}
                    placeholder="target username"
                  />
                </label>
                <label className="compact-field">
                  Action
                  <select
                    value={auditFilters.action}
                    onChange={(event) => handleAuditFilterChange("action", event.target.value)}
                  >
                    <option value="">All</option>
                    <option value="USER_ENABLED_UPDATED">USER_ENABLED_UPDATED</option>
                    <option value="USER_ROLE_UPDATED">USER_ROLE_UPDATED</option>
                  </select>
                </label>
                <label className="compact-field">
                  From
                  <input
                    type="datetime-local"
                    value={auditFilters.from}
                    onChange={(event) => handleAuditFilterChange("from", event.target.value)}
                  />
                </label>
                <label className="compact-field">
                  To
                  <input
                    type="datetime-local"
                    value={auditFilters.to}
                    onChange={(event) => handleAuditFilterChange("to", event.target.value)}
                  />
                </label>
                <label className="compact-field">
                  Page Size
                  <input
                    type="number"
                    min="1"
                    max="200"
                    value={auditPageSize}
                    onChange={(event) => handleAuditPageSizeChange(event.target.value)}
                  />
                </label>
                <label className="compact-field">
                  Export Limit
                  <input
                    type="number"
                    min="1"
                    max="5000"
                    value={auditExportLimit}
                    onChange={(event) => setAuditExportLimit(event.target.value)}
                  />
                </label>
              </div>

              <div className="audit-toolbar">
                <span>Total Records: <strong>{auditTotalElements}</strong></span>
                <span>
                  Page <strong>{auditTotalPages === 0 ? 0 : auditPageValue + 1}</strong> / <strong>{auditTotalPages}</strong>
                </span>
                <div className="admin-actions">
                  <button className="btn btn-secondary" onClick={handleAuditPrevPage} disabled={auditPageValue <= 0}>
                    Previous
                  </button>
                  <button className="btn btn-secondary" onClick={handleAuditNextPage} disabled={isLastAuditPage}>
                    Next
                  </button>
                </div>
              </div>

              {auditEntries.length > 0 ? (
                <article className="audit-list">
                  <ul>
                    {auditEntries.map((entry) => (
                      <li key={entry.id}>
                        <div className="audit-meta">
                          <strong>{entry.action}</strong>
                          <span>Actor: <code>{entry.actorUsername}</code></span>
                          <span>Target: <code>{entry.targetUsername || "-"}</code></span>
                          <span>At: {new Date(entry.createdAt).toLocaleString()}</span>
                        </div>
                        <div className="audit-detail">
                          {entry.details || "-"}
                        </div>
                      </li>
                    ))}
                  </ul>
                </article>
              ) : (
                <article className="result-placeholder">
                  No audit records yet.
                </article>
              )}
            </section>
          ) : null}
        </>
      )}

      {notice.text && (
        <section className={`notice notice-${notice.type} reveal delay-5`}>
          {notice.text}
        </section>
      )}
    </main>
  );
}
