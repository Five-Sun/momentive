"use client";

import { forwardRef, useState } from "react";
import type { InputHTMLAttributes } from "react";
import { Eye, EyeOff } from "lucide-react";

interface PasswordFieldProps extends Omit<InputHTMLAttributes<HTMLInputElement>, "type"> {
  label: string;
  error?: string;
}

export const PasswordField = forwardRef<HTMLInputElement, PasswordFieldProps>(function PasswordField(
  { label, error, id, ...inputProps },
  ref,
) {
  const [visible, setVisible] = useState(false);
  const fieldId = id ?? inputProps.name;

  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={fieldId} className="text-body-sm text-body">
        {label}
      </label>
      <div
        className={`bg-surface-soft border-hairline focus-within:border-brand-pink flex h-12 items-center rounded-md border px-4 ${
          error ? "border-error" : ""
        }`}
      >
        <input
          id={fieldId}
          ref={ref}
          type={visible ? "text" : "password"}
          className="text-body text-ink placeholder:text-muted h-full flex-1 border-none bg-transparent outline-none"
          aria-invalid={error ? true : undefined}
          {...inputProps}
        />
        <button
          type="button"
          onClick={() => setVisible((v) => !v)}
          aria-label={visible ? "비밀번호 숨기기" : "비밀번호 표시"}
          className="text-muted flex-shrink-0"
        >
          {visible ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
        </button>
      </div>
      {error && <span className="text-body-sm text-error">{error}</span>}
    </div>
  );
});
