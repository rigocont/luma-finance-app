/** Codigos de error estables que devuelve el backend (mismo contrato que el frontend web). */
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
 * Error normalizado que consume la app.
 *
 * Todo fallo (HTTP, red o timeout) llega a las pantallas con esta forma, para
 * que no existan dos maneras de mostrar un problema. Los mensajes por defecto
 * estan en espanol: la interfaz movil de M0 no trae i18n propio todavia (ver
 * docs/roadmap-mobile.md); cuando lo tenga, esta es la capa que los traduce.
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

  get fieldErrorMap(): Record<string, string> {
    return Object.fromEntries(this.fieldErrors.map((e) => [e.field, e.message]));
  }
}
