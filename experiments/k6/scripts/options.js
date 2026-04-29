export function makeOptions() {
  const rps = parseInt(__ENV.RPS || "50", 10);
  const duration = __ENV.DURATION || "60s";
  const preAllocatedVUs = parseInt(__ENV.PRE_VUS || "20", 10);
  const maxVUs = parseInt(__ENV.MAX_VUS || "200", 10);

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
