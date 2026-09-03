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
import { useAuth } from "@/lib/auth/AuthProvider";
import { ApiError } from "@/lib/api/client";

const loginSchema = z.object({
  email: z.string().min(1, "이메일을 입력해주세요").email("올바른 이메일 형식이 아니에요"),
  password: z.string().min(1, "비밀번호를 입력해주세요"),
});

type LoginFormValues = z.infer<typeof loginSchema>;

export default function LoginPage() {
  const router = useRouter();
  const { login } = useAuth();
  const [formError, setFormError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
  });

  async function onSubmit(values: LoginFormValues) {
    setFormError(null);
    try {
      await login(values);
      router.push("/mypage");
    } catch (err) {
      if (err instanceof ApiError && err.errorCode === "INVALID_CREDENTIALS") {
        setFormError("이메일 또는 비밀번호가 일치하지 않아요");
        return;
      }
      setFormError("로그인에 실패했어요. 잠시 후 다시 시도해주세요");
    }
  }

  return (
    <main className="bg-canvas flex min-h-screen flex-col px-4 py-8 lg:items-center lg:justify-center lg:px-0">
      <div className="w-full lg:max-w-[480px]">
        <h1 className="text-title text-ink mb-6">로그인</h1>

        {formError && (
          <div className="bg-brand-pink-tint text-error text-body-sm mb-4 rounded-md px-4 py-3">{formError}</div>
        )}

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
            placeholder="비밀번호를 입력해주세요"
            error={errors.password?.message}
            {...register("password")}
          />

          <Button type="submit" fullWidth disabled={isSubmitting}>
            로그인
          </Button>
        </form>

        <p className="text-body-sm text-muted mt-6 text-center">
          아직 계정이 없으신가요?{" "}
          <Link href="/signup" className="text-ink font-semibold underline">
            회원가입
          </Link>
        </p>
      </div>
    </main>
  );
}
