import http from "k6/http";
import { check } from "k6";
import { Rate } from "k6/metrics";
import { makeOptions } from "./options.js";
import { applyScenarioMode, resetMode } from "./utils.js";

export const options = makeOptions();

// Quality metric: share of responses served from the generic fallback chart
// (degraded) rather than live personalized recommendations. The client sets
// `degraded: true` when it returns the chart — uniform across all configs.
const fallbackRate = new Rate("fallback_rate");

const TARGET_HOST = __ENV.TARGET_HOST || "demo-client";
const TARGET_PORT = __ENV.TARGET_PORT || "8091";
const TARGET_BASE = `http://${TARGET_HOST}:${TARGET_PORT}/api/recommendations`;

export function setup() {
  console.log(
    `Setup: TARGET=${TARGET_HOST} SCENARIO_MODE=${__ENV.SCENARIO_MODE || "ok"}`,
  );
  applyScenarioMode();
}

export default function () {
  const userId = Math.floor(Math.random() * 100000) + 1;
  const response = http.get(`${TARGET_BASE}?userId=${userId}`);
  check(response, {
    "status is 2xx": (r) => r.status >= 200 && r.status < 300,
    "has recommendations": (r) => {
      try {
        return Array.isArray(r.json("recommendations"));
      } catch (_) {
        return false;
      }
    },
  });

  // Record quality: a degraded (fallback) response carries `degraded: true`.
  // Non-2xx / unparseable bodies count as non-degraded (they are failures, not
  // fallbacks — captured separately by http_req_failed).
  let degraded = false;
  try {
    degraded = response.json("degraded") === true;
  } catch (_) {
    degraded = false;
  }
  fallbackRate.add(degraded);
}

export function teardown() {
  // During a warmup→measurement sequence the fault mode must persist across
  // jobs so the proactive degradation level does not decay between samples.
  // run.sh sets RESET_ON_TEARDOWN=false for those jobs and runs a final reset.
  if (__ENV.RESET_ON_TEARDOWN === "false") {
    console.log("Teardown: keeping demo-server mode (sequence)");
    return;
  }
  console.log("Teardown: resetting demo-server to OK mode");
  resetMode();
}

export function handleSummary(data) {
  return {
    stdout:
      "=== JSON SUMMARY START ===\n" +
      JSON.stringify(data) +
      "\n=== JSON SUMMARY END ===\n",
  };
}
