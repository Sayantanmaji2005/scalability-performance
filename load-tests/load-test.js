import http from "k6/http";
import { check, sleep } from "k6";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";

export const options = {
  stages: [
    { duration: "30s", target: 20 },
    { duration: "1m", target: 80 },
    { duration: "30s", target: 0 }
  ],
  thresholds: {
    http_req_duration: ["p(95)<200"],
    http_req_failed: ["rate<0.01"]
  }
};

export function setup() {
  const username = `load-${Date.now()}@scalemart.dev`;
  const password = "DemoPass123!";

  const loginPayload = JSON.stringify({ username, password });
  let response = http.post(`${BASE_URL}/api/v1/auth/login`, loginPayload, {
    headers: { "Content-Type": "application/json" }
  });

  if (response.status !== 200) {
    // Fallback to demo account configured in docker-compose.
    response = http.post(
      `${BASE_URL}/api/v1/auth/login`,
      JSON.stringify({
        username: "demo@scalemart.dev",
        password: "DemoPass123!"
      }),
      { headers: { "Content-Type": "application/json" } }
    );
  }

  check(response, {
    "login success": (res) => res.status === 200
  });

  return { token: response.json("token") };
}

export default function (data) {
  const headers = {
    "Content-Type": "application/json",
    Authorization: `Bearer ${data.token}`
  };

  const trending = http.get(`${BASE_URL}/api/v1/products/trending`, { headers });
  check(trending, {
    "trending 200": (res) => res.status === 200
  });

  const product = http.get(`${BASE_URL}/api/v1/products/1`, { headers });
  check(product, {
    "product 200": (res) => res.status === 200
  });

  const order = http.post(
    `${BASE_URL}/api/v1/orders`,
    JSON.stringify({ productId: 1, quantity: 1 }),
    { headers }
  );
  check(order, {
    "order accepted": (res) => res.status === 202
  });

  sleep(1);
}
