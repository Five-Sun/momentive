import { forwardRef } from "react";
import type { InputHTMLAttributes } from "react";

interface TextFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
}

export const TextField = forwardRef<HTMLInputElement, TextFieldProps>(function TextField(
  { label, error, id, ...inputProps },
  ref,
) {
  const fieldId = id ?? inputProps.name;

  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={fieldId} className="text-body-sm text-body">
        {label}
      </label>
      <input
        id={fieldId}
        ref={ref}
        className={`bg-surface-soft border-hairline text-body text-ink placeholder:text-muted h-12 rounded-md border px-4 outline-none focus:border-brand-pink ${
          error ? "border-error" : ""
        }`}
        aria-invalid={error ? true : undefined}
        {...inputProps}
      />
      {error && <span className="text-body-sm text-error">{error}</span>}
    </div>
  );
});
