import { apiFetch } from "./client";

export interface AddressRequest {
  recipient: string;
  phone: string;
  zipcode: string;
  address1: string;
  address2?: string;
  isDefault: boolean;
}

export interface AddressResponse {
  id: number;
  recipient: string;
  phone: string;
  zipcode: string;
  address1: string;
  address2: string | null;
  isDefault: boolean;
}

export function getAddresses(): Promise<AddressResponse[]> {
  return apiFetch<AddressResponse[]>("/addresses");
}

export function createAddress(request: AddressRequest): Promise<AddressResponse> {
  return apiFetch<AddressResponse>("/addresses", {
    method: "POST",
    body: JSON.stringify(request),
  });
}

export function updateAddress(addressId: number, request: AddressRequest): Promise<AddressResponse> {
  return apiFetch<AddressResponse>(`/addresses/${addressId}`, {
    method: "PATCH",
    body: JSON.stringify(request),
  });
}
