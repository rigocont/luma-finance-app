export const paths = {
  login: '/entrar',
  register: '/crear-cuenta',
  forgotPassword: '/recuperar',
  // El backend construye los enlaces de correo contra esta ruta: si cambia,
  // hay que cambiar tambien luma.app.base-url y el texto del correo.
  resetPassword: '/restablecer',

  // El asistente vive fuera del shell: sin menu ni secciones, porque todavia
  // no hay nada que navegar.
  onboarding: '/bienvenida',

  dashboard: '/',
  // La revision del ciclo tiene seccion propia y no una pestana dentro de
  // Gastos: no es una vista mas de la lista de gastos sino la bandeja de lo que
  // el ciclo en curso espera de ti, y cambia de contenido cada quincena.
  currentCycle: '/este-ciclo',
  incomes: '/ingresos',
  // Una sola seccion para gastos fijos y variables: viven en la misma tabla y
  // solo los distingue un campo. Separarlos en dos pantallas duplicaria lista,
  // formulario y confirmacion por un booleano.
  expenses: '/gastos',
  savings: '/ahorros',
  settings: '/ajustes',
} as const;

export type AppPath = (typeof paths)[keyof typeof paths];
