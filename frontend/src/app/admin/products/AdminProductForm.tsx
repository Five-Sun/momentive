"use client";

import { useRef, useState } from "react";
import type { ReactNode } from "react";
import { useFieldArray, useForm, type FieldPath } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { ArrowDown, ArrowUp, Plus, RotateCcw, Trash2 } from "lucide-react";
import { Button } from "@/components/core/Button";
import { TextField } from "@/components/forms/TextField";
import { CATEGORY_LIST } from "@/lib/categories";
import { ApiError } from "@/lib/api/client";
import {
  issueImageUploadSignature,
  type AdminProductDetail,
  type AdminProductRequest,
  type AdminProductVariantRequest,
} from "@/lib/api/admin";
import { uploadToCloudinary } from "@/lib/upload/cloudinary";
import { PRODUCT_STATUS_LABEL, PRODUCT_STATUS_VALUES } from "../productStatus";

/**
 * 상품 등록·수정 공용 폼. 등록(`/admin/products/new`)과 수정(`/admin/products/[id]`)이
 * 같은 요청 본문을 쓰므로 폼도 하나로 둔다.
 *
 * 저장 버튼은 화면 흐름 안(문서 하단)에 두고 `fixed`로 띄우지 않는다 — 고정 UI끼리 겹쳐
 * CTA가 클릭되지 않는 사고를 만들지 않기 위함이다
 * (`docs/backlog/2026-08-29-cart-order-payment-phase6-01.md` 재발 방지).
 */

const MAX_IMAGE_COUNT = 5;

/** 숫자 입력은 문자열로 다룬다 — 빈 칸이 NaN이 되는 경계를 만들지 않기 위함. */
const NON_NEGATIVE_INT = /^\s*\d+\s*$/;
const OPTIONAL_NON_NEGATIVE_INT = /^\s*\d*\s*$/;

const CATEGORY_VALUES = ["OUTER", "KNIT", "INNERWEAR", "ACCESSORY"] as const;

const variantRowSchema = z.object({
  /**
   * 서버가 돌려준 variant id(신규 행은 ""). hidden input으로 폼에 실려 다니며,
   * 사이즈 이름을 고쳐도 이 값이 유지되므로 수정은 항상 기존 행 in-place 갱신이 된다.
   */
  id: z.string(),
  size: z.string().max(50, "사이즈 이름은 50자까지 입력할 수 있어요"),
  stock: z.string().regex(NON_NEGATIVE_INT, "재고는 0 이상의 정수로 입력해주세요"),
});

const productFormSchema = z
  .object({
    name: z
      .string()
      .max(200, "상품명은 200자까지 입력할 수 있어요")
      .refine((value) => value.trim().length > 0, "상품명을 입력해주세요"),
    description: z.string().refine((value) => value.trim().length > 0, "상품 설명을 입력해주세요"),
    price: z.string().regex(NON_NEGATIVE_INT, "정가는 0 이상의 정수로 입력해주세요"),
    discountPrice: z
      .string()
      .regex(OPTIONAL_NON_NEGATIVE_INT, "할인가는 0 이상의 정수로 입력하거나 비워주세요"),
    category: z.enum(CATEGORY_VALUES),
    status: z.enum(PRODUCT_STATUS_VALUES),
    variants: z.array(variantRowSchema).min(1, "사이즈·재고를 최소 1행 이상 입력해주세요"),
  })
  .superRefine((values, ctx) => {
    // 서버의 DUPLICATE_VARIANT_SIZE를 맞기 전에 어느 행이 문제인지 먼저 알려준다.
    // 빈 사이즈("" = 사이즈 없음)도 같은 규칙으로 하나만 허용된다.
    const seen = new Set<string>();
    values.variants.forEach((variant, index) => {
      const key = variant.size.trim();
      if (seen.has(key)) {
        ctx.addIssue({
          code: "custom",
          path: ["variants", index, "size"],
          message:
            key === ""
              ? "사이즈를 비운 행은 하나만 둘 수 있어요"
              : "같은 사이즈 이름을 중복해서 등록할 수 없어요",
        });
        return;
      }
      seen.add(key);
    });
  });

type ProductFormValues = z.infer<typeof productFormSchema>;

/** 미리보기 한 장. 업로드 실패는 이 슬롯만 실패 상태가 되고 나머지는 그대로 남는다. */
interface ImageSlot {
  key: string;
  label: string;
  url: string | null;
  status: "uploading" | "done" | "failed";
  /** 재시도에 필요한 원본 파일. 서버에서 불러온 기존 이미지는 null */
  file: File | null;
}

/** 삭제된 기존 variant. 같은 사이즈가 다시 추가되면 이 id를 되살린다. */
interface RemovedVariant {
  id: number;
  size: string | null;
}

function blankToNull(value: string): string | null {
  const trimmed = value.trim();
  return trimmed === "" ? null : trimmed;
}

/** null(사이즈 없음)과 빈 문자열을 같은 키로 묶는다. */
function sizeKey(size: string | null): string {
  return size ?? "";
}

function fileNameOf(url: string): string {
  return url.split("/").pop() || url;
}

function toDefaultValues(product: AdminProductDetail | null): ProductFormValues {
  if (!product) {
    return {
      name: "",
      description: "",
      price: "",
      discountPrice: "",
      category: "OUTER",
      status: "ON_SALE",
      variants: [{ id: "", size: "", stock: "0" }],
    };
  }

  return {
    name: product.name,
    description: product.description,
    price: String(product.price),
    discountPrice: product.discountPrice != null ? String(product.discountPrice) : "",
    category: product.category,
    status: product.status,
    // 서버가 준 variant id를 그대로 보존한다. 버리고 재전송하면 서버가 삭제 후 재INSERT로
    // 처리해, 이미 주문에 쓰인 사이즈에서 VARIANT_IN_USE(400)가 난다.
    variants: product.variants.map((variant) => ({
      id: String(variant.id),
      size: variant.size ?? "",
      stock: String(variant.stock),
    })),
  };
}

function toInitialImageSlots(product: AdminProductDetail | null): ImageSlot[] {
  if (!product) return [];
  return product.images.map((image) => ({
    key: `saved-${image.id}`,
    label: fileNameOf(image.url),
    url: image.url,
    status: "done" as const,
    file: null,
  }));
}

/**
 * 폼 행을 요청 variant로 옮긴다.
 *
 * 기존 행을 지웠다가 같은 사이즈 이름을 새로 추가한 경우, 그대로 보내면 서버가
 * "DELETE + INSERT"로 처리해 (Hibernate가 INSERT를 먼저 수행하므로) 유니크 인덱스 위반이 난다.
 * 그래서 삭제된 행의 id를 사이즈 이름으로 되살려, 항상 기존 행 in-place 수정으로 만든다.
 */
function toVariantRequests(
  rows: ProductFormValues["variants"],
  removed: RemovedVariant[],
): AdminProductVariantRequest[] {
  const reusableIds = new Map<string, number>();
  for (const variant of removed) {
    if (!reusableIds.has(sizeKey(variant.size))) reusableIds.set(sizeKey(variant.size), variant.id);
  }

  return rows.map((row) => {
    const size = blankToNull(row.size);
    const stock = Number(row.stock.trim());

    if (row.id !== "") {
      return { id: Number(row.id), size, stock };
    }

    const reusedId = reusableIds.get(sizeKey(size));
    if (reusedId != null) {
      reusableIds.delete(sizeKey(size));
      return { id: reusedId, size, stock };
    }
    return { id: null, size, stock };
  });
}

/**
 * 이번 요청에서 삭제되는 기존 행이 아직 들고 있는 사이즈 이름을, 남는 다른 행이 그대로
 * 쓰려는 상황을 찾는다(예: "M" 행을 지우면서 다른 행 이름을 "M"으로 바꾼 경우).
 *
 * 서버는 한 요청 안에서 UPDATE를 DELETE보다 먼저 수행하므로 이대로 보내면 상품 내 사이즈
 * 유니크 제약과 충돌한다. 한 번에 표현할 방법이 없는 조합이라, 폼에서 미리 막고 두 번에 나눠
 * 저장하도록 안내한다. 지운 뒤 같은 이름을 새로 추가한 경우는 `toVariantRequests`가 id를
 * 되살려 in-place 수정으로 바꾸므로 여기에 걸리지 않는다.
 */
function findDeletedSizeConflict(
  requests: AdminProductVariantRequest[],
  removed: RemovedVariant[],
): number {
  const keptIds = new Set(requests.map((request) => request.id).filter((id) => id != null));
  const deletedSizes = new Set(
    removed.filter((variant) => !keptIds.has(variant.id)).map((variant) => sizeKey(variant.size)),
  );
  if (deletedSizes.size === 0) return -1;

  return requests.findIndex(
    (request) => request.id != null && deletedSizes.has(sizeKey(request.size)),
  );
}

const FORM_FIELD_ROOTS = ["name", "description", "price", "discountPrice", "category", "status", "variants"];

/** 서버 필드 경로("variants[0].stock")를 RHF 경로("variants.0.stock")로 바꾼다. */
function toFormPath(serverField: string): FieldPath<ProductFormValues> | null {
  const normalized = serverField.replace(/\[(\d+)\]/g, ".$1");
  const root = normalized.split(".")[0];
  return FORM_FIELD_ROOTS.includes(root) ? (normalized as FieldPath<ProductFormValues>) : null;
}

const FIELD_BOX_CLASS =
  "bg-surface-soft border-hairline text-body text-ink placeholder:text-muted rounded-md border px-4 outline-none focus:border-brand-pink";

function FieldShell({
  label,
  htmlFor,
  error,
  children,
}: {
  label: string;
  htmlFor: string;
  error?: string;
  children: ReactNode;
}) {
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={htmlFor} className="text-body-sm text-body">
        {label}
      </label>
      {children}
      {error && <span className="text-body-sm text-error">{error}</span>}
    </div>
  );
}

interface AdminProductFormProps {
  /** 수정 대상 상품. 등록 폼이면 null */
  product: AdminProductDetail | null;
  submitLabel: string;
  /** 실제 API 호출. `ApiError`를 그대로 던지면 이 폼이 인라인 에러로 매핑한다. */
  onSubmit: (request: AdminProductRequest) => Promise<AdminProductDetail>;
  onSaved: (saved: AdminProductDetail) => void;
  /** 인라인으로 표시할 자리가 없는 에러(네트워크 실패 등) */
  onUnhandledError: (message: string) => void;
  /** 수정 폼의 삭제 버튼 등, 액션 영역에 덧붙일 요소 */
  extraActions?: ReactNode;
}

export function AdminProductForm({
  product,
  submitLabel,
  onSubmit,
  onSaved,
  onUnhandledError,
  extraActions,
}: AdminProductFormProps) {
  const [imageSlots, setImageSlots] = useState<ImageSlot[]>(() => toInitialImageSlots(product));
  const [imageError, setImageError] = useState<string | null>(null);
  const [removedVariants, setRemovedVariants] = useState<RemovedVariant[]>([]);
  const slotSeq = useRef(0);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const {
    register,
    control,
    handleSubmit,
    getValues,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<ProductFormValues>({
    resolver: zodResolver(productFormSchema),
    defaultValues: toDefaultValues(product),
  });

  const { fields, append, remove } = useFieldArray({ control, name: "variants" });

  const uploading = imageSlots.some((slot) => slot.status === "uploading");

  function nextSlotKey() {
    slotSeq.current += 1;
    return `local-${slotSeq.current}`;
  }

  function updateSlot(key: string, patch: Partial<ImageSlot>) {
    setImageSlots((prev) => prev.map((slot) => (slot.key === key ? { ...slot, ...patch } : slot)));
  }

  async function uploadSlot(key: string, file: File) {
    try {
      const signature = await issueImageUploadSignature();
      const url = await uploadToCloudinary(file, signature);
      updateSlot(key, { url, status: "done" });
    } catch {
      // 실패한 장만 실패 표시하고 나머지 미리보기는 유지한다(spec 시나리오 B 예외).
      updateSlot(key, { url: null, status: "failed" });
    }
  }

  function handleFilesSelected(fileList: FileList | null) {
    if (!fileList || fileList.length === 0) return;
    setImageError(null);

    const selected = Array.from(fileList);
    const room = MAX_IMAGE_COUNT - imageSlots.length;
    if (room <= 0) {
      setImageError(`이미지는 최대 ${MAX_IMAGE_COUNT}장까지 등록할 수 있어요`);
      return;
    }
    if (selected.length > room) {
      setImageError(`이미지는 최대 ${MAX_IMAGE_COUNT}장까지 등록할 수 있어 ${room}장만 추가했어요`);
    }

    const accepted = selected.slice(0, room);
    const newSlots: ImageSlot[] = accepted.map((file) => ({
      key: nextSlotKey(),
      label: file.name,
      url: null,
      status: "uploading",
      file,
    }));

    setImageSlots((prev) => [...prev, ...newSlots]);
    newSlots.forEach((slot, index) => void uploadSlot(slot.key, accepted[index]));
  }

  function retrySlot(slot: ImageSlot) {
    if (!slot.file) return;
    updateSlot(slot.key, { status: "uploading" });
    void uploadSlot(slot.key, slot.file);
  }

  function removeSlot(key: string) {
    setImageSlots((prev) => prev.filter((slot) => slot.key !== key));
    setImageError(null);
  }

  function moveSlot(index: number, direction: -1 | 1) {
    const target = index + direction;
    setImageSlots((prev) => {
      if (target < 0 || target >= prev.length) return prev;
      const next = [...prev];
      [next[index], next[target]] = [next[target], next[index]];
      return next;
    });
  }

  function handleRemoveVariant(index: number) {
    const row = getValues(`variants.${index}`);
    if (row && row.id !== "") {
      // 같은 사이즈가 다시 추가되면 이 id를 되살려 삭제+재INSERT를 피한다.
      setRemovedVariants((prev) => [...prev, { id: Number(row.id), size: blankToNull(row.size) }]);
    }
    remove(index);
  }

  function applyServerError(err: unknown) {
    if (!(err instanceof ApiError)) {
      onUnhandledError("저장에 실패했어요. 잠시 후 다시 시도해주세요");
      return;
    }

    if (err.fieldErrors && Object.keys(err.fieldErrors).length > 0) {
      for (const [field, message] of Object.entries(err.fieldErrors)) {
        if (field.startsWith("imageUrls")) {
          setImageError(message);
          continue;
        }
        const path = toFormPath(field);
        if (path) setError(path, { message });
      }
      return;
    }

    switch (err.errorCode) {
      case "VARIANT_REQUIRED":
      case "DUPLICATE_VARIANT_SIZE":
      case "VARIANT_IN_USE":
      case "VARIANT_NOT_FOUND":
        setError("variants", { message: err.message });
        return;
      case "IMAGE_LIMIT_EXCEEDED":
        setImageError(err.message);
        return;
      default:
        onUnhandledError(err.message);
    }
  }

  async function handleFormSubmit(values: ProductFormValues) {
    if (uploading) {
      setImageError("이미지 업로드가 끝난 뒤 저장해주세요");
      return;
    }

    const variants = toVariantRequests(values.variants, removedVariants);
    const conflictIndex = findDeletedSizeConflict(variants, removedVariants);
    if (conflictIndex >= 0) {
      setError(`variants.${conflictIndex}.size`, {
        message: "삭제한 사이즈와 이름이 같아요. 삭제를 먼저 저장한 뒤 이름을 바꿔주세요",
      });
      return;
    }

    const request: AdminProductRequest = {
      name: values.name.trim(),
      description: values.description.trim(),
      price: Number(values.price.trim()),
      discountPrice: values.discountPrice.trim() === "" ? null : Number(values.discountPrice.trim()),
      category: values.category,
      status: values.status,
      // 업로드에 성공한 장만, 화면에 보이는 순서 그대로 보낸다(순서가 곧 displayOrder).
      imageUrls: imageSlots.flatMap((slot) =>
        slot.status === "done" && slot.url ? [slot.url] : [],
      ),
      variants,
    };

    try {
      const saved = await onSubmit(request);
      // 저장 응답의 id를 폼 상태에 그대로 되돌려 심는다 — 연속 저장에서도 같은 행이 유지된다.
      reset(toDefaultValues(saved));
      setImageSlots(toInitialImageSlots(saved));
      setRemovedVariants([]);
      setImageError(null);
      onSaved(saved);
    } catch (err) {
      applyServerError(err);
    }
  }

  const variantsError = errors.variants?.root?.message ?? errors.variants?.message;

  return (
    <form onSubmit={handleSubmit(handleFormSubmit)} className="flex flex-col gap-8">
      <section className="border-hairline bg-surface-card flex flex-col gap-4 rounded-md border p-5">
        <h2 className="text-title-sm text-ink">기본 정보</h2>

        <TextField
          label="상품명"
          placeholder="예: 겨울 패딩"
          error={errors.name?.message}
          {...register("name")}
        />

        <FieldShell label="상품 설명" htmlFor="description" error={errors.description?.message}>
          <textarea
            id="description"
            rows={5}
            placeholder="상품 설명을 입력해주세요"
            className={`${FIELD_BOX_CLASS} py-3`}
            aria-invalid={errors.description ? true : undefined}
            {...register("description")}
          />
        </FieldShell>

        <div className="grid gap-4 md:grid-cols-2">
          <TextField
            label="정가"
            inputMode="numeric"
            placeholder="28000"
            error={errors.price?.message}
            {...register("price")}
          />
          <TextField
            label="할인가 (없으면 비워두세요)"
            inputMode="numeric"
            placeholder="22400"
            error={errors.discountPrice?.message}
            {...register("discountPrice")}
          />
        </div>

        <div className="grid gap-4 md:grid-cols-2">
          <FieldShell label="카테고리" htmlFor="category" error={errors.category?.message}>
            <select id="category" className={`${FIELD_BOX_CLASS} h-12`} {...register("category")}>
              {CATEGORY_LIST.map((category) => (
                <option key={category.key} value={category.key}>
                  {category.label}
                </option>
              ))}
            </select>
          </FieldShell>

          <FieldShell label="판매 상태" htmlFor="status" error={errors.status?.message}>
            <select id="status" className={`${FIELD_BOX_CLASS} h-12`} {...register("status")}>
              {PRODUCT_STATUS_VALUES.map((status) => (
                <option key={status} value={status}>
                  {PRODUCT_STATUS_LABEL[status]}
                </option>
              ))}
            </select>
          </FieldShell>
        </div>
      </section>

      <section className="border-hairline bg-surface-card flex flex-col gap-4 rounded-md border p-5">
        <div className="flex items-center justify-between">
          <h2 className="text-title-sm text-ink">이미지</h2>
          <span className="text-body-sm text-muted">
            {imageSlots.length} / {MAX_IMAGE_COUNT} · 위에서부터 노출 순서
          </span>
        </div>

        <div>
          <input
            ref={fileInputRef}
            type="file"
            accept="image/*"
            multiple
            className="hidden"
            onChange={(event) => {
              handleFilesSelected(event.target.files);
              // 같은 파일을 연속으로 다시 고를 수 있게 입력값을 비운다.
              event.target.value = "";
            }}
          />
          <Button
            variant="secondary"
            size="sm"
            icon={<Plus className="h-4 w-4" />}
            disabled={imageSlots.length >= MAX_IMAGE_COUNT}
            onClick={() => fileInputRef.current?.click()}
          >
            이미지 추가
          </Button>
        </div>

        {imageError && <span className="text-body-sm text-error">{imageError}</span>}

        {imageSlots.length === 0 ? (
          <p className="text-body-sm text-muted">
            이미지 없이 저장해도 괜찮아요. 고객 화면에는 기본 이미지가 표시돼요
          </p>
        ) : (
          <ul className="flex flex-col gap-3">
            {imageSlots.map((slot, index) => (
              <li
                key={slot.key}
                className="border-hairline bg-surface-soft flex items-center gap-3 rounded-md border p-3"
              >
                <div className="h-16 w-16 flex-shrink-0">
                  {slot.status === "done" && slot.url ? (
                    // eslint-disable-next-line @next/next/no-img-element
                    <img
                      src={slot.url}
                      alt={slot.label}
                      className="aspect-square w-full rounded-2xl object-cover"
                    />
                  ) : (
                    <div className="bg-surface-strong text-caption text-muted flex aspect-square items-center justify-center rounded-2xl">
                      {slot.status === "uploading" ? "업로드중" : "실패"}
                    </div>
                  )}
                </div>

                <div className="flex min-w-0 flex-1 flex-col">
                  <span className="text-body-sm text-ink truncate">{slot.label}</span>
                  {slot.status === "failed" && (
                    <span className="text-body-sm text-error">
                      업로드에 실패했어요. 다시 시도하거나 이 장만 삭제해주세요
                    </span>
                  )}
                  {slot.status === "uploading" && (
                    <span className="text-body-sm text-muted">업로드 중...</span>
                  )}
                </div>

                <div className="flex flex-shrink-0 items-center gap-1">
                  {slot.status === "failed" && slot.file && (
                    <button
                      type="button"
                      aria-label="업로드 다시 시도"
                      onClick={() => retrySlot(slot)}
                      className="border-hairline bg-surface-card text-ink inline-flex h-9 w-9 items-center justify-center rounded-full border"
                    >
                      <RotateCcw className="h-4 w-4" />
                    </button>
                  )}
                  <button
                    type="button"
                    aria-label="위로 이동"
                    disabled={index === 0}
                    onClick={() => moveSlot(index, -1)}
                    className="border-hairline bg-surface-card text-ink inline-flex h-9 w-9 items-center justify-center rounded-full border disabled:opacity-40"
                  >
                    <ArrowUp className="h-4 w-4" />
                  </button>
                  <button
                    type="button"
                    aria-label="아래로 이동"
                    disabled={index === imageSlots.length - 1}
                    onClick={() => moveSlot(index, 1)}
                    className="border-hairline bg-surface-card text-ink inline-flex h-9 w-9 items-center justify-center rounded-full border disabled:opacity-40"
                  >
                    <ArrowDown className="h-4 w-4" />
                  </button>
                  <button
                    type="button"
                    aria-label="이미지 삭제"
                    onClick={() => removeSlot(slot.key)}
                    className="border-hairline bg-surface-card text-error inline-flex h-9 w-9 items-center justify-center rounded-full border"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="border-hairline bg-surface-card flex flex-col gap-4 rounded-md border p-5">
        <div className="flex items-center justify-between">
          <h2 className="text-title-sm text-ink">사이즈 · 재고</h2>
          <span className="text-body-sm text-muted">
            사이즈가 없는 상품은 사이즈를 비우고 재고만 입력하세요
          </span>
        </div>

        {variantsError && <span className="text-body-sm text-error">{variantsError}</span>}

        <ul className="flex flex-col gap-3">
          {fields.map((field, index) => (
            <li key={field.id} className="flex items-start gap-2">
              {/* 서버 variant id. 사이즈 이름을 바꿔도 이 값이 유지돼 in-place 수정이 된다. */}
              <input type="hidden" {...register(`variants.${index}.id`)} />
              <div className="flex-1">
                <TextField
                  label="사이즈"
                  placeholder="S / M / L (없으면 비워두세요)"
                  error={errors.variants?.[index]?.size?.message}
                  {...register(`variants.${index}.size`)}
                />
              </div>
              <div className="w-40">
                <TextField
                  label="재고"
                  inputMode="numeric"
                  placeholder="0"
                  error={errors.variants?.[index]?.stock?.message}
                  {...register(`variants.${index}.stock`)}
                />
              </div>
              <button
                type="button"
                aria-label="사이즈 행 삭제"
                disabled={fields.length <= 1}
                onClick={() => handleRemoveVariant(index)}
                className="border-hairline bg-surface-card text-error mt-7 inline-flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-full border disabled:opacity-40"
              >
                <Trash2 className="h-4 w-4" />
              </button>
            </li>
          ))}
        </ul>

        <div>
          <Button
            variant="secondary"
            size="sm"
            icon={<Plus className="h-4 w-4" />}
            onClick={() => append({ id: "", size: "", stock: "0" })}
          >
            사이즈 추가
          </Button>
        </div>

        <p className="text-body-sm text-muted">
          이미 주문에 사용된 사이즈는 삭제할 수 없어요. 재고를 0으로 두세요
        </p>
      </section>

      <div className="flex items-center gap-3">
        <Button type="submit" variant="primary" disabled={isSubmitting || uploading}>
          {submitLabel}
        </Button>
        {extraActions}
      </div>
    </form>
  );
}
