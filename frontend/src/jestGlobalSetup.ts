// Runs in the main jest process before workers spawn.
// Setting NODE_OPTIONS here is inherited by worker child processes,
// which suppresses Node.js DeprecationWarnings (e.g. punycode).
export default function globalSetup() {
  const existing = process.env.NODE_OPTIONS ?? '';
  if (!existing.includes('--no-deprecation')) {
    process.env.NODE_OPTIONS = `${existing} --no-deprecation`.trim();
  }
}
