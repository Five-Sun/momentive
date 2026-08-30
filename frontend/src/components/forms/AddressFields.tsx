import type { UseFormRegister, FieldErrors } from "react-hook-form";
import { TextField } from "./TextField";

export interface AddressFormValues {
  recipient: string;
  phone: string;
  zipcode: string;
  address1: string;
  address2?: string;
}

interface AddressFieldsProps {
  register: UseFormRegister<AddressFormValues>;
  errors: FieldErrors<AddressFormValues>;
}

/** 배송지 신규 입력 폼 필드 묶음. `/checkout`에서 새 배송지 추가 시 사용. */
export function AddressFields({ register, errors }: AddressFieldsProps) {
  return (
    <div className="flex flex-col gap-3">
      <TextField
        label="받는 사람"
        placeholder="이름을 입력해주세요"
        error={errors.recipient?.message}
        {...register("recipient")}
      />
      <TextField
        label="연락처"
        placeholder="010-0000-0000"
        error={errors.phone?.message}
        {...register("phone")}
      />
      <TextField
        label="우편번호"
        placeholder="12345"
        error={errors.zipcode?.message}
        {...register("zipcode")}
      />
      <TextField
        label="주소"
        placeholder="도로명 주소"
        error={errors.address1?.message}
        {...register("address1")}
      />
      <TextField
        label="상세주소 (선택)"
        placeholder="상세주소를 입력해주세요"
        error={errors.address2?.message}
        {...register("address2")}
      />
    </div>
  );
}
