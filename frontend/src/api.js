const TOKEN_KEY = 'lde_token'
const USER_KEY = 'lde_user'

export function getUser() {
  const raw = localStorage.getItem(USER_KEY)
  return raw ? JSON.parse(raw) : null
}

export function saveSession(data) {
  localStorage.setItem(TOKEN_KEY, data.token)
  localStorage.setItem(USER_KEY, JSON.stringify({ username: data.username, role: data.role }))
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export async function api(path, options = {}) {
  const token = localStorage.getItem(TOKEN_KEY)
  const res = await fetch('/api' + path, {
    method: options.method || 'GET',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: 'Bearer ' + token } : {}),
    },
    body: options.body ? JSON.stringify(options.body) : undefined,
  })

  if (res.status === 401 && path !== '/auth/login') {
    clearSession()
    window.location.href = '/login'
    throw new Error('Session expired')
  }
  if (!res.ok) {
    let message = 'Request failed (' + res.status + ')'
    try {
      const data = await res.json()
      if (data.message) message = data.message
    } catch (e) {
      if (res.status === 403) message = 'Your role is not allowed to do this'
    }
    throw new Error(message)
  }
  return res.json()
}

export const money = (n) =>
  Number(n).toLocaleString('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 })

export const pct = (p) => (p * 100).toFixed(1) + '%'
