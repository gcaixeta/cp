import { getAuthToken, logout } from './auth';

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
  if (response.status === 401) {
    // Unauthorized - redirect to login
    logout();
    throw new Error('Sessão expirada. Faça login novamente.');
  }

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

export async function fetchClients(): Promise<Client[]> {
  const response = await fetch(`${API_BASE_URL}/client`, {
    headers: getAuthHeaders(),
  });
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

  const response = await fetch(`${API_BASE_URL}/payment?${params.toString()}`, {
    headers: getAuthHeaders(),
  });
  return handleResponse<GroupedPaymentResponse[]>(response);
}

export async function markPaymentAsPaid(paymentId: number): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/payment/${paymentId}/mark-as-paid`, {
    method: 'PATCH',
    headers: getAuthHeaders(),
  });
  return handleResponse<void>(response);
}

export async function fetchClientById(id: number): Promise<Client> {
  const response = await fetch(`${API_BASE_URL}/client/${id}`, {
    headers: getAuthHeaders(),
  });
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
  const response = await fetch(`${API_BASE_URL}/payment-group`, {
    method: 'POST',
    headers: getAuthHeaders(),
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
  const response = await fetch(`${API_BASE_URL}/boletos/payment/${paymentId}`, {
    method: 'GET',
    headers: getAuthHeaders(),
  });
  return handleResponse<BoletoResponse>(response);
}

export async function updatePayment(id: number, data: {
  originalValue: number;
  dueDate: string; // ISO format YYYY-MM-DD
  paymentDate?: string; // ISO format YYYY-MM-DD
  observation?: string;
}): Promise<PaymentResponse> {
  const response = await fetch(`${API_BASE_URL}/payment/${id}`, {
    method: 'PUT',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });
  return handleResponse<PaymentResponse>(response);
}

export async function fetchAllClients(): Promise<Client[]> {
  const response = await fetch(`${API_BASE_URL}/client`, {
    headers: getAuthHeaders(),
  });
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
  const response = await fetch(`${API_BASE_URL}/client`, {
    method: 'POST',
    headers: getAuthHeaders(),
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
  const response = await fetch(`${API_BASE_URL}/client/${id}`, {
    method: 'PUT',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  });
  return handleResponse<Client>(response);
}

export async function deleteClient(id: number): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/client/${id}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
  });
  return handleResponse<void>(response);
}
