const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "";

export class ApiError extends Error {
  status: number;
  errorCode: string;
  fieldErrors?: Record<string, string>;

  constructor(status: number, errorCode: string, message: string, fieldErrors?: Record<string, string>) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.errorCode = errorCode;
    this.fieldErrors = fieldErrors;
  }
}

interface ErrorResponseBody {
  code: string;
  message: string;
  fieldErrors?: Record<string, string>;
}

let refreshPromise: Promise<boolean> | null = null;

async function parseErrorResponse(res: Response): Promise<ApiError> {
  try {
    const body = (await res.json()) as ErrorResponseBody;
    return new ApiError(res.status, body.code, body.message, body.fieldErrors);
  } catch {
    return new ApiError(res.status, "UNKNOWN_ERROR", `요청 실패: ${res.status}`);
  }
}

async function requestOnce(path: string, init?: RequestInit): Promise<Response> {
  return fetch(`${API_BASE_URL}${path}`, {
    ...init,
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...init?.headers,
    },
  });
}

/** 동시에 여러 요청이 401을 맞아도 refresh 호출은 한 번만 공유되도록 in-flight promise를 재사용한다. */
function refreshOnce(): Promise<boolean> {
  if (!refreshPromise) {
    refreshPromise = requestOnce("/auth/refresh", { method: "POST" })
      .then((res) => res.ok)
      .catch(() => false)
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

/**
 * 백엔드 API 호출 공통 래퍼. 401 응답을 받으면 refresh를 시도해 원요청을 1회 재시도한다.
 * 실패 응답은 `ApiError`로 throw한다.
 */
export async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  let res = await requestOnce(path, init);

  if (res.status === 401 && path !== "/auth/refresh") {
    const refreshed = await refreshOnce();
    if (refreshed) {
      res = await requestOnce(path, init);
    }
  }

  if (!res.ok) {
    throw await parseErrorResponse(res);
  }

  if (res.status === 204) {
    return undefined as T;
  }

  // 200이어도 바디가 비어있을 수 있다(예: nullable 응답을 명시적 JSON null 대신 빈 바디로
  // 내려주는 경우). res.json()은 빈 문자열을 파싱하지 못해 SyntaxError를 던지므로,
  // 텍스트로 먼저 읽어 비어있으면 undefined로, 아니면 JSON.parse로 안전하게 처리한다.
  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
}
