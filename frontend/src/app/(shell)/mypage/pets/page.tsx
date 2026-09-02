"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { ArrowLeft, Pencil, PawPrint, Trash2 } from "lucide-react";
import { Button } from "@/components/core/Button";
import { IconButton } from "@/components/core/IconButton";
import { Toast } from "@/components/feedback/Toast";
import { TextField } from "@/components/forms/TextField";
import {
  createPet,
  deletePet,
  getPets,
  updatePet,
  type PetGender,
  type PetRequest,
  type PetResponse,
} from "@/lib/api/pets";
import { ApiError } from "@/lib/api/client";

const petSchema = z.object({
  name: z.string().min(1, "이름을 입력해주세요"),
  breed: z.string(),
  birthDate: z.string(),
  gender: z.enum(["MALE", "FEMALE", ""]),
  weightKg: z.string(),
});

type PetFormValues = z.infer<typeof petSchema>;

const EMPTY_FORM_VALUES: PetFormValues = {
  name: "",
  breed: "",
  birthDate: "",
  gender: "",
  weightKg: "",
};

const GENDER_LABEL: Record<PetGender, string> = {
  MALE: "수컷",
  FEMALE: "암컷",
};

function petToFormValues(pet: PetResponse): PetFormValues {
  return {
    name: pet.name,
    breed: pet.breed ?? "",
    birthDate: pet.birthDate ?? "",
    gender: pet.gender ?? "",
    weightKg: pet.weightKg != null ? String(pet.weightKg) : "",
  };
}

function formValuesToRequest(values: PetFormValues): PetRequest {
  const weight = values.weightKg.trim() ? Number(values.weightKg) : undefined;
  return {
    name: values.name.trim(),
    breed: values.breed.trim() || undefined,
    birthDate: values.birthDate || undefined,
    gender: values.gender || undefined,
    weightKg: weight != null && !Number.isNaN(weight) ? weight : undefined,
  };
}

function formatBirthDate(birthDate: string) {
  return birthDate.replaceAll("-", ".");
}

export default function MyPetsPage() {
  const router = useRouter();
  const [pets, setPets] = useState<PetResponse[]>([]);
  const [loaded, setLoaded] = useState(false);
  const [loadFailed, setLoadFailed] = useState(false);
  const [showForm, setShowForm] = useState(false);
  const [editingPet, setEditingPet] = useState<PetResponse | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<PetFormValues>({
    resolver: zodResolver(petSchema),
    defaultValues: EMPTY_FORM_VALUES,
  });

  useEffect(() => {
    getPets()
      .then((res) => setPets(res.pets))
      .catch(() => {
        setLoadFailed(true);
        showToast("반려견 목록을 불러오지 못했어요");
      })
      .finally(() => setLoaded(true));
  }, []);

  function showToast(message: string) {
    setToastMessage(message);
    setTimeout(() => setToastMessage(null), 1800);
  }

  function openCreateForm() {
    setEditingPet(null);
    reset(EMPTY_FORM_VALUES);
    setShowForm(true);
  }

  function openEditForm(pet: PetResponse) {
    setEditingPet(pet);
    reset(petToFormValues(pet));
    setShowForm(true);
  }

  function closeForm() {
    setShowForm(false);
    setEditingPet(null);
  }

  async function onSubmit(values: PetFormValues) {
    setSubmitting(true);
    try {
      const request = formValuesToRequest(values);
      if (editingPet) {
        const updated = await updatePet(editingPet.id, request);
        setPets((prev) => prev.map((pet) => (pet.id === updated.id ? updated : pet)));
        showToast("반려견 정보를 수정했어요");
      } else {
        const created = await createPet(request);
        setPets((prev) => [created, ...prev]);
        showToast("반려견을 등록했어요");
      }
      closeForm();
    } catch (err) {
      if (err instanceof ApiError && err.errorCode === "VALIDATION_FAILED" && err.fieldErrors) {
        for (const [field, message] of Object.entries(err.fieldErrors)) {
          if (field === "name" || field === "breed" || field === "birthDate" || field === "gender" || field === "weightKg") {
            setError(field, { message });
          }
        }
        return;
      }
      showToast(err instanceof ApiError ? err.message : "저장에 실패했어요. 잠시 후 다시 시도해주세요");
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(pet: PetResponse) {
    if (!window.confirm(`${pet.name}을(를) 삭제할까요?`)) return;
    try {
      await deletePet(pet.id);
      setPets((prev) => prev.filter((p) => p.id !== pet.id));
      if (editingPet?.id === pet.id) closeForm();
      showToast("반려견을 삭제했어요");
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "삭제에 실패했어요");
    }
  }

  return (
    <div className="bg-canvas relative flex min-h-screen flex-col">
      <div className="border-hairline bg-surface-card flex h-13 flex-shrink-0 items-center border-b px-4 lg:hidden">
        <button onClick={() => router.back()} aria-label="뒤로가기" className="text-ink">
          <ArrowLeft className="h-5 w-5" />
        </button>
        <span className="text-title-sm text-ink flex-1 text-center">반려견 프로필 관리</span>
        <div className="h-5 w-5" />
      </div>

      <div className="hidden px-0 pt-7 pb-4 lg:block">
        <span className="text-title-sm text-ink">반려견 프로필 관리</span>
      </div>

      <div className="flex flex-1 flex-col gap-3 p-4 pb-28 lg:px-0 lg:pb-16">
        {loaded && pets.length === 0 && !showForm && (
          <div className="flex flex-1 flex-col items-center justify-center gap-4 py-20">
            <span className="text-body text-muted">
              {loadFailed ? "반려견 목록을 불러오지 못했어요" : "등록된 반려견이 없어요"}
            </span>
            {!loadFailed && (
              <Button variant="primary" onClick={openCreateForm}>
                반려견 등록
              </Button>
            )}
          </div>
        )}

        {pets.length > 0 && (
          <div className="flex flex-col gap-3 lg:grid lg:grid-cols-2">
            {pets.map((pet) => (
              <div key={pet.id} className="border-hairline bg-surface-card flex gap-3 rounded-md border p-3.5">
                <div
                  className={`flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-full ${
                    pet.gender === "FEMALE" ? "bg-brand-pink-tint" : "bg-surface-strong"
                  }`}
                >
                  <PawPrint className="text-brand-pink-deep h-6 w-6" />
                </div>
                <div className="flex flex-1 flex-col gap-0.5">
                  <span className="text-body-sm text-ink font-semibold">{pet.name}</span>
                  <div className="text-caption text-muted flex flex-wrap gap-x-2">
                    {pet.breed && <span>{pet.breed}</span>}
                    {pet.birthDate && <span>{formatBirthDate(pet.birthDate)}</span>}
                    {pet.gender && <span>{GENDER_LABEL[pet.gender]}</span>}
                    {pet.weightKg != null && <span>{pet.weightKg}kg</span>}
                  </div>
                </div>
                <div className="flex flex-shrink-0 items-start gap-1.5">
                  <IconButton size={32} onClick={() => openEditForm(pet)}>
                    <Pencil className="h-4 w-4" />
                  </IconButton>
                  <IconButton size={32} onClick={() => handleDelete(pet)}>
                    <Trash2 className="h-4 w-4" />
                  </IconButton>
                </div>
              </div>
            ))}
          </div>
        )}

        {loaded && pets.length > 0 && !showForm && (
          <Button variant="secondary" fullWidth onClick={openCreateForm}>
            반려견 등록
          </Button>
        )}

        {showForm && (
          <form
            onSubmit={handleSubmit(onSubmit)}
            className="border-hairline bg-surface-card flex flex-col gap-3 rounded-md border p-3.5 lg:max-w-[480px]"
          >
            <span className="text-title-sm text-ink">{editingPet ? "반려견 정보 수정" : "반려견 등록"}</span>
            <TextField label="이름" placeholder="반려견 이름" error={errors.name?.message} {...register("name")} />
            <TextField
              label="품종 (선택)"
              placeholder="예: 말티즈"
              error={errors.breed?.message}
              {...register("breed")}
            />
            <TextField label="생일 (선택)" type="date" error={errors.birthDate?.message} {...register("birthDate")} />
            <div className="flex flex-col gap-1.5">
              <label htmlFor="pet-gender" className="text-body-sm text-body">
                성별 (선택)
              </label>
              <select
                id="pet-gender"
                className="bg-surface-soft border-hairline text-body text-ink h-12 rounded-md border px-4 outline-none focus:border-brand-pink"
                {...register("gender")}
              >
                <option value="">선택 안함</option>
                <option value="MALE">수컷</option>
                <option value="FEMALE">암컷</option>
              </select>
              {errors.gender && <span className="text-body-sm text-error">{errors.gender.message}</span>}
            </div>
            <TextField
              label="몸무게 (kg, 선택)"
              type="number"
              step="0.1"
              min="0"
              placeholder="예: 4.5"
              error={errors.weightKg?.message}
              {...register("weightKg")}
            />
            <div className="flex gap-2.5">
              <Button type="button" variant="secondary" onClick={closeForm}>
                취소
              </Button>
              <div className="flex-1">
                <Button type="submit" variant="primary" fullWidth disabled={submitting}>
                  {editingPet ? "수정 완료" : "등록하기"}
                </Button>
              </div>
            </div>
          </form>
        )}
      </div>

      {toastMessage && <Toast message={toastMessage} visible={!!toastMessage} />}
    </div>
  );
}
