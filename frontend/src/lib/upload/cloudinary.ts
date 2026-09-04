import type { ImageUploadSignature } from "@/lib/api/admin";

/**
 * Cloudinary signed upload. 파일 바이트는 백엔드를 거치지 않고 브라우저가 직접 올린다.
 *
 * 백엔드 호출이 아니라 외부 서비스 직접 호출이므로 공통 `apiFetch`를 쓰지 않는다
 * (`apiFetch`는 우리 API의 쿠키 인증·401 refresh·`ErrorResponse` 포맷을 전제로 한다).
 * 그래서 `src/lib/api/` 밖에 두어 "도메인 API 파일은 fetch를 직접 호출하지 않는다"는
 * 컨벤션의 경계를 흐리지 않는다.
 */

/** Cloudinary 업로드 응답 중 실제로 쓰는 필드만 좁혀 둔다. */
interface CloudinaryUploadResponse {
  secure_url?: string;
}

export async function uploadToCloudinary(
  file: File,
  signature: ImageUploadSignature,
): Promise<string> {
  const form = new FormData();
  form.append("file", file);
  form.append("api_key", signature.apiKey);
  form.append("timestamp", String(signature.timestamp));
  form.append("signature", signature.signature);
  // 서명 대상 파라미터(folder, timestamp)는 발급받은 값 그대로 실어야 서명이 맞는다.
  form.append("folder", signature.folder);

  // Content-Type을 직접 지정하지 않는다 — multipart boundary는 브라우저가 붙여야 한다.
  const res = await fetch(`https://api.cloudinary.com/v1_1/${signature.cloudName}/image/upload`, {
    method: "POST",
    body: form,
  });

  if (!res.ok) {
    throw new Error(`Cloudinary 업로드 실패: ${res.status}`);
  }

  const body = (await res.json()) as CloudinaryUploadResponse;
  if (!body.secure_url) {
    throw new Error("Cloudinary 응답에 secure_url이 없습니다.");
  }
  return body.secure_url;
}
