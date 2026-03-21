const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/v1';

export interface LoginResponse {
  token: string;
  type: string;
  expiresIn: number;
}

export async function login(email: string, password: string): Promise<LoginResponse> {
  const response = await fetch(`${API_BASE_URL}/auth/login`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ email, password }),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message || 'Falha ao fazer login');
  }

  return response.json();
}

export async function validateToken(token: string): Promise<boolean> {
  try {
    const response = await fetch(`${API_BASE_URL}/auth/validate`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`,
      },
    });
    return response.ok;
  } catch {
    return false;
  }
}

export function setAuthToken(token: string) {
  if (typeof window !== 'undefined') {
    localStorage.setItem('auth_token', token);
  }
}

export function setAuthTokenWithExpiry(token: string, expiresIn: number) {
  if (typeof window !== 'undefined') {
    localStorage.setItem('auth_token', token);
    localStorage.setItem('auth_token_expiry', String(Date.now() + expiresIn));
  }
}

export function isTokenExpired(): boolean {
  if (typeof window === 'undefined') return false;
  const expiry = localStorage.getItem('auth_token_expiry');
  if (!expiry) return false;
  return Date.now() > Number(expiry);
}

export function getAuthToken(): string | null {
  if (typeof window !== 'undefined') {
    return localStorage.getItem('auth_token');
  }
  return null;
}

export function removeAuthToken() {
  if (typeof window !== 'undefined') {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('auth_token_expiry');
  }
}

export function logout() {
  removeAuthToken();
  if (typeof window !== 'undefined') {
    window.location.href = '/login';
  }
}

export function isAuthenticated(): boolean {
  return getAuthToken() !== null && !isTokenExpired();
}

export async function refreshAuthToken(): Promise<string | null> {
  const currentToken = getAuthToken();
  if (!currentToken) return null;

  try {
    const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${currentToken}`,
      },
    });

    if (!response.ok) return null;

    const data: LoginResponse = await response.json();
    setAuthTokenWithExpiry(data.token, data.expiresIn);
    return data.token;
  } catch {
    return null;
  }
}
