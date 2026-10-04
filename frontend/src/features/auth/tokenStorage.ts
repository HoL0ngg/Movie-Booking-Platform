const K = 'cine.tokens'
export type Tokens = { accessToken: string; refreshToken: string }
export const tokenStorage = {
  get: (): Tokens | null => { try { return JSON.parse(localStorage.getItem(K) ?? 'null') } catch { return null } },
  set: (t: Tokens) => localStorage.setItem(K, JSON.stringify(t)),
  clear: () => localStorage.removeItem(K),
}