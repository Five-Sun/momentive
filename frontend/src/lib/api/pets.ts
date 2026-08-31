import { apiFetch } from "./client";

export type PetGender = "MALE" | "FEMALE";

export interface PetRequest {
  name: string;
  breed?: string | null;
  birthDate?: string | null;
  gender?: PetGender | null;
  weightKg?: number | null;
}

export interface PetResponse {
  id: number;
  name: string;
  breed: string | null;
  birthDate: string | null;
  gender: PetGender | null;
  weightKg: number | null;
}

export interface PetListResponse {
  pets: PetResponse[];
}

export function getPets(): Promise<PetListResponse> {
  return apiFetch<PetListResponse>("/pets");
}

export function createPet(request: PetRequest): Promise<PetResponse> {
  return apiFetch<PetResponse>("/pets", {
    method: "POST",
    body: JSON.stringify(request),
  });
}

export function updatePet(petId: number, request: PetRequest): Promise<PetResponse> {
  return apiFetch<PetResponse>(`/pets/${petId}`, {
    method: "PATCH",
    body: JSON.stringify(request),
  });
}

export function deletePet(petId: number): Promise<void> {
  return apiFetch<void>(`/pets/${petId}`, {
    method: "DELETE",
  });
}
