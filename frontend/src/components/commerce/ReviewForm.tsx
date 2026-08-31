"use client";

import { useEffect } from "react";
import { Star } from "lucide-react";
import { useForm, useWatch, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Button } from "@/components/core/Button";
import type { ApiError } from "@/lib/api/client";

const reviewSchema = z.object({
  rating: z.number().min(1, "별점을 선택해주세요").max(5),
  text: z
    .string()
    .min(10, "리뷰 내용은 10자 이상 입력해주세요")
    .max(500, "리뷰 내용은 500자 이하로 입력해주세요"),
});

export type ReviewFormValues = z.infer<typeof reviewSchema>;

interface ReviewFormProps {
  initialValues?: ReviewFormValues;
  submitting: boolean;
  onSubmit: (values: ReviewFormValues) => Promise<void> | void;
  onCancel?: () => void;
  onApiError?: (
    err: ApiError,
    setError: (field: keyof ReviewFormValues, message: string) => void,
  ) => void;
}

/** 리뷰 작성/수정 공용 폼. 별점(1~5) 탭 선택 + 텍스트(10~500자) 입력. */
export function ReviewForm({ initialValues, submitting, onSubmit, onCancel, onApiError }: ReviewFormProps) {
  const {
    control,
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<ReviewFormValues>({
    resolver: zodResolver(reviewSchema),
    defaultValues: initialValues ?? { rating: 0, text: "" },
  });

  useEffect(() => {
    reset(initialValues ?? { rating: 0, text: "" });
  }, [initialValues, reset]);

  const text = useWatch({ control, name: "text" }) ?? "";

  async function handleFormSubmit(values: ReviewFormValues) {
    try {
      await onSubmit(values);
    } catch (err) {
      if (onApiError) {
        onApiError(err as ApiError, (field, message) => setError(field, { message }));
      } else {
        throw err;
      }
    }
  }

  return (
    <form onSubmit={handleSubmit(handleFormSubmit)} className="flex flex-col gap-3">
      <div className="flex flex-col gap-1.5">
        <span className="text-body-sm text-body">별점</span>
        <Controller
          control={control}
          name="rating"
          render={({ field }) => (
            <div className="flex items-center gap-1">
              {[1, 2, 3, 4, 5].map((value) => (
                <button
                  key={value}
                  type="button"
                  aria-label={`별점 ${value}점`}
                  onClick={() => field.onChange(value)}
                  className={field.value >= value ? "text-brand-pink" : "text-muted-soft"}
                >
                  <Star className="h-6 w-6" fill="currentColor" />
                </button>
              ))}
            </div>
          )}
        />
        {errors.rating && <span className="text-body-sm text-error">{errors.rating.message}</span>}
      </div>

      <div className="flex flex-col gap-1.5">
        <label htmlFor="review-text" className="text-body-sm text-body">
          리뷰 내용
        </label>
        <textarea
          id="review-text"
          rows={4}
          maxLength={500}
          placeholder="상품에 대한 솔직한 리뷰를 남겨주세요 (10~500자)"
          className={`bg-surface-soft border-hairline text-body text-ink placeholder:text-muted rounded-md border p-3 outline-none focus:border-brand-pink ${
            errors.text ? "border-error" : ""
          }`}
          aria-invalid={errors.text ? true : undefined}
          {...register("text")}
        />
        <div className="flex items-center justify-between">
          {errors.text ? (
            <span className="text-body-sm text-error">{errors.text.message}</span>
          ) : (
            <span />
          )}
          <span className="text-caption text-muted">{text.length}/500</span>
        </div>
      </div>

      <div className="flex gap-2.5">
        {onCancel && (
          <Button type="button" variant="secondary" onClick={onCancel}>
            취소
          </Button>
        )}
        <div className="flex-1">
          <Button type="submit" variant="primary" fullWidth disabled={submitting}>
            {initialValues ? "수정 완료" : "리뷰 등록"}
          </Button>
        </div>
      </div>
    </form>
  );
}
