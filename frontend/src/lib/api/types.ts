/** Codigos de error estables que devuelve el backend. */
export type ErrorCode =
  | 'VALIDATION_ERROR'
  | 'RESOURCE_NOT_FOUND'
  | 'BUSINESS_RULE_VIOLATION'
  | 'CONFLICT'
  | 'UNAUTHORIZED'
  | 'FORBIDDEN'
  | 'INTERNAL_ERROR'
  | 'NETWORK_ERROR';

export interface FieldError {
  field: string;
  message: string;
}

/** Cuerpo de error de la API (RFC 9457 mas las extensiones de LUMA). */
export interface ApiProblem {
  type?: string;
  title?: string;
  status: number;
  detail?: string;
  instance?: string;
  timestamp?: string;
  errorCode?: ErrorCode;
  traceId?: string;
  errors?: FieldError[];
}

/**
 * Error normalizado que consume la interfaz.
 *
 * Todo fallo (HTTP, red o timeout) llega a los componentes con esta forma, para
 * que no existan dos maneras de mostrar un problema.
 */
export class ApiError extends Error {
  readonly status: number;
  readonly code: ErrorCode;
  readonly fieldErrors: FieldError[];
  readonly traceId?: string;

  constructor(params: {
    message: string;
    status: number;
    code: ErrorCode;
    fieldErrors?: FieldError[];
    traceId?: string;
  }) {
    super(params.message);
    this.name = 'ApiError';
    this.status = params.status;
    this.code = params.code;
    this.fieldErrors = params.fieldErrors ?? [];
    this.traceId = params.traceId;
  }

  /** Errores de validacion indexados por campo, listos para React Hook Form. */
  get fieldErrorMap(): Record<string, string> {
    return Object.fromEntries(this.fieldErrors.map((e) => [e.field, e.message]));
  }
}

/** Envoltura de paginacion comun a toda la API. */
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface SystemInfo {
  name: string;
  version: string;
  profiles: string[];
  serverTime: string;
}
