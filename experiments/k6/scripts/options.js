export function makeOptions() {
  const rps = parseInt(__ENV.RPS || "50", 10);
  const duration = __ENV.DURATION || "60s";
  // Open-model (constant-arrival-rate) needs enough VUs to sustain the target
  // rate when latency rises. At 50 req/s and up to 3s read-timeout, ~150 VUs
  // are required; defaults give headroom so the rate holds (and dropped
  // iterations, if any, reflect genuine capacity limits, not VU starvation).
  const preAllocatedVUs = parseInt(__ENV.PRE_VUS || "50", 10);
  const maxVUs = parseInt(__ENV.MAX_VUS || "300", 10);

  return {
    summaryTrendStats: ["avg", "min", "med", "max", "p(90)", "p(95)", "p(99)"],

    scenarios: {
      load: {
        executor: "constant-arrival-rate",
        rate: rps,
        timeUnit: "1s",
        duration,
        preAllocatedVUs,
        maxVUs,
      },
    },
  };
}
