/**
 * Returns the API base URL.
 * Set NEXT_PUBLIC_API_URL at build time for production (Next.js inlines it into the bundle).
 * Without it, the client targets the local development server.
 */
export function getApiDomain(): string {
  return process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";
}
