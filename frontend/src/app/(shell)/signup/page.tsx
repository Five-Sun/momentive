"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { TextField } from "@/components/forms/TextField";
import { PasswordField } from "@/components/forms/PasswordField";
import { Button } from "@/components/core/Button";
import { Toast } from "@/components/feedback/Toast";
import { useAuth } from "@/lib/auth/AuthProvider";
import { ApiError } from "@/lib/api/client";

const signupSchema = z.object({
  email: z.string().min(1, "이메일을 입력해주세요").email("올바른 이메일 형식이 아니에요"),
  password: z
    .string()
    .min(8, "비밀번호는 최소 8자 이상이어야 해요")
    .regex(/^(?=.*[A-Za-z])(?=.*\d).+$/, "비밀번호는 영문/숫자를 조합해야 해요"),
  nickname: z.string().min(1, "닉네임을 입력해주세요"),
});

type SignupFormValues = z.infer<typeof signupSchema>;

export default function SignupPage() {
  const router = useRouter();
  const { signup } = useAuth();
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<SignupFormValues>({
    resolver: zodResolver(signupSchema),
  });

  async function onSubmit(values: SignupFormValues) {
    try {
      await signup(values);
      router.push("/mypage");
    } catch (err) {
      if (err instanceof ApiError && err.fieldErrors?.email) {
        setError("email", { message: err.fieldErrors.email });
        return;
      }
      setToastMessage("회원가입에 실패했어요. 잠시 후 다시 시도해주세요");
      setTimeout(() => setToastMessage(null), 1800);
    }
  }

  return (
    <main className="bg-canvas relative flex min-h-screen flex-col px-4 py-8 lg:items-center lg:justify-center lg:px-0">
      <div className="w-full lg:max-w-[480px]">
        <h1 className="text-title text-ink mb-6">회원가입</h1>

        <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
          <TextField
            label="이메일"
            type="email"
            placeholder="example@momentive.com"
            error={errors.email?.message}
            {...register("email")}
          />
          <PasswordField
            label="비밀번호"
            placeholder="영문/숫자 조합 8자 이상"
            error={errors.password?.message}
            {...register("password")}
          />
          <TextField
            label="닉네임"
            type="text"
            placeholder="닉네임을 입력해주세요"
            error={errors.nickname?.message}
            {...register("nickname")}
          />

          <Button type="submit" fullWidth disabled={isSubmitting}>
            회원가입
          </Button>
        </form>

        <p className="text-body-sm text-muted mt-6 text-center">
          이미 계정이 있으신가요?{" "}
          <Link href="/login" className="text-ink font-semibold underline">
            로그인
          </Link>
        </p>
      </div>

      {toastMessage && <Toast message={toastMessage} visible={!!toastMessage} />}
    </main>
  );
}
