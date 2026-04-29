import http from "k6/http";
import { check } from "k6";
import { makeOptions } from "./options.js";
import { applyScenarioMode, resetMode } from "./utils.js";

export const options = makeOptions();

const TARGET_HOST = __ENV.TARGET_HOST || "demo-client-with-janus";
const TARGET_PORT = __ENV.TARGET_PORT || "8091";
const LIMIT = __ENV.LIMIT || "10";
const TARGET_URL = `http://${TARGET_HOST}:${TARGET_PORT}/api/recommendations?limit=${LIMIT}`;

export function setup() {
  console.log(
    `Setup: TARGET=${TARGET_HOST} SCENARIO_MODE=${__ENV.SCENARIO_MODE || "ok"}`,
  );
  applyScenarioMode();
}

export default function () {
  const response = http.get(TARGET_URL);
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
}

export function teardown() {
  console.log("Teardown: resetting demo-server to OK mode");
  resetMode();
}
