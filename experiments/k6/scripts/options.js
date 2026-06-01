export function makeOptions() {
  // Open-model load needs enough VUs to sustain the target rate when latency
  // rises (at 50 req/s and up to 3s read-timeout, ~150 VUs); defaults give
  // headroom so the rate holds and dropped iterations reflect genuine capacity
  // limits, not VU starvation.
  const preAllocatedVUs = parseInt(__ENV.PRE_VUS || "50", 10);
  const maxVUs = parseInt(__ENV.MAX_VUS || "300", 10);
  const trendStats = ["avg", "min", "med", "max", "p(90)", "p(95)", "p(99)"];

  // Phase 3 (pre-emptive degradation): a ramping arrival rate
  // (RAMP_STAGES="target:dur,target:dur,...") drives the dependency through its
  // saturation point so a proactive policy can pre-empt the latency spike that a
  // circuit breaker only reacts to after the fact. Falls back to the open-model
  // constant rate (scenarios 2-5) when RAMP_STAGES is unset.
  if (__ENV.RAMP_STAGES) {
    const stages = __ENV.RAMP_STAGES.split(",").map((s) => {
      const [target, duration] = s.split(":");
      return { target: parseInt(target, 10), duration };
    });
    return {
      summaryTrendStats: trendStats,
      scenarios: {
        load: {
          executor: "ramping-arrival-rate",
          startRate: parseInt(__ENV.RAMP_START || "5", 10),
          timeUnit: "1s",
          preAllocatedVUs,
          maxVUs,
          stages,
        },
      },
    };
  }

  const rps = parseInt(__ENV.RPS || "50", 10);
  const duration = __ENV.DURATION || "60s";
  return {
    summaryTrendStats: trendStats,
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
