import { getAuthToken, logout, refreshAuthToken } from './auth';

// Em produção (Docker), usa /api/v1 (Nginx remove /api e envia para backend:8080/v1)
// Em desenvolvimento, usa http://localhost:8080/v1 (acesso direto ao backend sem /api)
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/v1';

// ==========================================
// TYPE DEFINITIONS
// ==========================================

export interface Client {
  id: number;
  name: string;
  document: string;
  phone: string | null;
  address: string;
  bank: string | null;
  lateFeeRate: number | null;
  monthlyInterestRate: number | null;
}

export interface PaymentResponse {
  id: number;
  clientId: number;
  paymentGroupId: number;
  groupName: string;
  payerName: string;
  payerPhone: string | null;
  installmentNumber: number;
  totalInstallments: number;
  originalValue: number;
  overdueValue: number;
  dueDate: string;
  paymentDate: string | null;
  paymentStatus: "PENDING" | "PAID" | "PAID_LATE" | "OVERDUE";
  observation: string;
}

export interface GroupedPaymentResponse {
  mainPayment: PaymentResponse;
  overduePayments: PaymentResponse[];
}

export interface PaymentGroupData {
  id: number;
  payerName: string;
  payerDocument: string;
  payerPhone: string | null;
  monthlyValue: number;
  totalInstallments: number;
  paidInstallments: number;
  lateFeeRate: number;
  monthlyInterestRate: number;
  observation: string | null;
  client: Client;
}

// Helper function to add auth headers to requests
function getAuthHeaders(): HeadersInit {
  const token = getAuthToken();
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
  };

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  return headers;
}

// Helper function to handle API responses
async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    throw new Error(`Request failed with status ${response.status}`);
  }

  // Some endpoints return 204 No Content
  if (response.status === 204) {
    return undefined as T;
  }

  const contentType = response.headers.get('content-type') || '';
  const text = await response.text();

  if (!text) {
    return undefined as T;
  }

  if (contentType.includes('application/json')) {
    return JSON.parse(text) as T;
  }

  // Fallback for non-JSON responses
  return text as unknown as T;
}

// Flag to prevent multiple concurrent refresh attempts
let isRefreshing = false;
let refreshPromise: Promise<string | null> | null = null;

// Wrapper that handles 401 with automatic token refresh and retry
async function fetchWithAuth(url: string, options: RequestInit = {}): Promise<Response> {
  const response = await fetch(url, {
    ...options,
    headers: getAuthHeaders(),
  });

  if (response.status !== 401) {
    return response;
  }

  // Avoid multiple concurrent refresh calls
  if (!isRefreshing) {
    isRefreshing = true;
    refreshPromise = refreshAuthToken();
  }

  const newToken = await refreshPromise;
  isRefreshing = false;
  refreshPromise = null;

  if (!newToken) {
    logout();
    throw new Error('Sessão expirada. Faça login novamente.');
  }

  // Retry the original request with new token
  return fetch(url, {
    ...options,
    headers: getAuthHeaders(),
  });
}

export async function fetchClients(): Promise<Client[]> {
  const response = await fetchWithAuth(`${API_BASE_URL}/client`);
  return handleResponse<Client[]>(response);
}

export async function fetchGroupedPayments(filters: {
  clientId?: string;
  status?: string;
  month?: number;
  year?: number;
}): Promise<GroupedPaymentResponse[]> {
  const params = new URLSearchParams();
  if (filters.clientId && filters.clientId !== 'all') params.append('clientId', filters.clientId);
  if (filters.status && filters.status !== 'all') params.append('status', filters.status);
  if (filters.month) params.append('month', filters.month.toString());
  if (filters.year) params.append('year', filters.year.toString());

  const response = await fetchWithAuth(`${API_BASE_URL}/payment?${params.toString()}`);
  return handleResponse<GroupedPaymentResponse[]>(response);
}

export async function markPaymentAsPaid(paymentId: number): Promise<void> {
  const response = await fetchWithAuth(`${API_BASE_URL}/payment/${paymentId}/mark-as-paid`, {
    method: 'PATCH',
  });
  return handleResponse<void>(response);
}

export async function fetchClientById(id: number): Promise<Client> {
  const response = await fetchWithAuth(`${API_BASE_URL}/client/${id}`);
  return handleResponse<Client>(response);
}

export async function createPaymentGroup(data: {
  clientId: number;
  payerName: string;
  payerDocument: string;
  payerPhone?: string;
  monthlyValue: number;
  totalInstallments: number;
  lateFeeRate?: number;
  monthlyInterestRate?: number;
  firstInstallmentDueDate: string; // ISO format YYYY-MM-DD
  observation?: string;
  generateBoletos?: boolean;
}): Promise<PaymentGroupData> {
  const response = await fetchWithAuth(`${API_BASE_URL}/payment-group`, {
    method: 'POST',
    body: JSON.stringify(data),
  });
  return handleResponse<PaymentGroupData>(response);
}

// Boleto related functions
export interface BoletoResponse {
  id: number;
  paymentId: number;
  bankType: string;
  bankBoletoId: string | null;
  barcode: string | null;
  digitableLine: string | null;
  pdfUrl: string | null;
  status: string;
  errorMessage: string | null;
  createdAt: string;
}

export async function fetchBoletoByPaymentId(paymentId: number): Promise<BoletoResponse> {
  const response = await fetchWithAuth(`${API_BASE_URL}/boletos/payment/${paymentId}`);
  return handleResponse<BoletoResponse>(response);
}

export interface PaymentGroupListItem {
  id: number;
  groupName: string;
  payerName: string | null;
  clientName: string;
  clientId: number;
  payerDocument: string;
  payerPhone: string | null;
  totalInstallments: number;
  paidInstallments: number;
  lateFeeRate: number | null;
  monthlyInterestRate: number | null;
  monthlyValue: number;
  totalPaid: number;
  totalRemaining: number;
  creationDate: string | null;
  observation: string | null;
}

export async function fetchPaymentGroups(filters?: {
  clientId?: string;
}): Promise<PaymentGroupListItem[]> {
  const params = new URLSearchParams();
  if (filters?.clientId && filters.clientId !== 'all') params.append('clientId', filters.clientId);

  const response = await fetchWithAuth(`${API_BASE_URL}/payment-group?${params.toString()}`);
  return handleResponse<PaymentGroupListItem[]>(response);
}

export async function deletePaymentGroup(id: number): Promise<void> {
  const response = await fetchWithAuth(`${API_BASE_URL}/payment-group/${id}`, {
    method: 'DELETE',
  });
  return handleResponse<void>(response);
}

export async function updatePayment(id: number, data: {
  originalValue: number;
  dueDate: string; // ISO format YYYY-MM-DD
  paymentDate?: string; // ISO format YYYY-MM-DD
  observation?: string;
}): Promise<PaymentResponse> {
  const response = await fetchWithAuth(`${API_BASE_URL}/payment/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
  return handleResponse<PaymentResponse>(response);
}

export async function fetchAllClients(): Promise<Client[]> {
  const response = await fetchWithAuth(`${API_BASE_URL}/client`);
  return handleResponse<Client[]>(response);
}

export async function createClient(data: {
  clientName: string;
  address: string;
  phone?: string;
  document: string;
  bank?: string;
  lateFeeRate?: number;
  monthlyInterestRate?: number;
}): Promise<Client> {
  const response = await fetchWithAuth(`${API_BASE_URL}/client`, {
    method: 'POST',
    body: JSON.stringify(data),
  });
  return handleResponse<Client>(response);
}

export async function updateClient(id: number, data: {
  clientName: string;
  address: string;
  phone?: string;
  document: string;
  bank?: string;
  lateFeeRate?: number;
  monthlyInterestRate?: number;
}): Promise<Client> {
  const response = await fetchWithAuth(`${API_BASE_URL}/client/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
  return handleResponse<Client>(response);
}

export async function deleteClient(id: number): Promise<void> {
  const response = await fetchWithAuth(`${API_BASE_URL}/client/${id}`, {
    method: 'DELETE',
  });
  return handleResponse<void>(response);
}

export async function downloadMonthlyReport(clientId: number, month: number, year: number): Promise<Blob> {
  const response = await fetchWithAuth(
    `${API_BASE_URL}/report/client/${clientId}/monthly?month=${month}&year=${year}`
  );
  if (!response.ok) {
    throw new Error(`Failed to generate report: ${response.status}`);
  }
  return response.blob();
}
