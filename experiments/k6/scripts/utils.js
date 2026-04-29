import http from "k6/http";

const DEFAULT_DEMO_SERVER = "http://demo-server:8090";

function demoServerUrl() {
  return __ENV.DEMO_SERVER_URL || DEFAULT_DEMO_SERVER;
}

export function setMode(mode, params) {
  const query = Object.entries(params || {})
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
    .join("&");

  const url = `${demoServerUrl()}/demo/mode/${mode}${query ? `?${query}` : ""}`;
  const response = http.post(url);
  if (response.status !== 200) {
    throw new Error(
      `setMode(${mode}) failed: status=${response.status}, body=${response.body}`,
    );
  }
}

export function setCustomMode({ mode, delayMs, status, errorRate }) {
  const params = {};
  if (mode != null) params.mode = mode;
  if (delayMs != null) params.delayMs = delayMs;
  if (status != null) params.status = status;
  if (errorRate != null) params.errorRate = errorRate;

  const query = Object.entries(params)
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
    .join("&");

  const response = http.post(`${demoServerUrl()}/demo/mode?${query}`);
  if (response.status !== 200) {
    throw new Error(
      `setCustomMode failed: status=${response.status}, body=${response.body}`,
    );
  }
}

export function resetMode() {
  setMode("ok");
}

export function applyScenarioMode() {
  const scenario = __ENV.SCENARIO_MODE || "ok";

  switch (scenario) {
    case "ok":
      setMode("ok");
      return;
    case "slow":
      setMode("slow", {
        delayMs: __ENV.SCENARIO_DELAY_MS || "5000",
      });
      return;
    case "error":
      setMode("error", {
        status: __ENV.SCENARIO_STATUS || "500",
      });
      return;
    case "flaky":
      setCustomMode({
        mode: "FLAKY",
        delayMs: __ENV.SCENARIO_DELAY_MS || "0",
        status: __ENV.SCENARIO_STATUS || "500",
        errorRate: __ENV.SCENARIO_ERROR_RATE || "0.5",
      });
      return;
    default:
      throw new Error(`Unsupported SCENARIO_MODE: ${scenario}`);
  }
}
