export const paths = {
  login: '/entrar',
  register: '/crear-cuenta',
  forgotPassword: '/recuperar',
  // El backend construye los enlaces de correo contra esta ruta: si cambia,
  // hay que cambiar tambien luma.app.base-url y el texto del correo.
  resetPassword: '/restablecer',

  dashboard: '/',
  incomes: '/ingresos',
  fixedExpenses: '/gastos-fijos',
  variableExpenses: '/gastos-variables',
  savings: '/ahorros',
  settings: '/ajustes',
} as const;

export type AppPath = (typeof paths)[keyof typeof paths];
