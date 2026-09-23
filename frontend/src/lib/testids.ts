/**
 * Catalogo central de `data-testid`.
 *
 * Convencion: `<feature>-<elemento>-<rol>`, en minusculas y con guiones.
 *
 * Estos identificadores son CONTRATO: se tratan como API publica de la interfaz.
 * No se renombran sin anotarlo en docs/testids.md, porque la automatizacion de
 * pruebas depende de ellos.
 *
 * Este archivo NO contiene pruebas. La automatizacion se desarrolla aparte.
 */
export const testIds = {
  layout: {
    appShell: 'layout-app-shell',
    sidebar: 'layout-sidebar',
    sidebarToggle: 'layout-sidebar-toggle',
    header: 'layout-header',
    themeToggle: 'layout-theme-toggle',
    navItem: (key: string) => `layout-nav-${key}`,
    pageTitle: 'layout-page-title',
  },

  state: {
    loading: 'state-loading',
    empty: 'state-empty',
    error: 'state-error',
    errorRetry: 'state-error-retry',
  },

  auth: {
    bootSplash: 'auth-boot-splash',
    loginPage: 'auth-login-page',
    registerPage: 'auth-register-page',
    nameInput: 'auth-name-input',
    emailInput: 'auth-email-input',
    passwordInput: 'auth-password-input',
    submitButton: 'auth-submit-button',
    formError: 'auth-form-error',
    goToRegister: 'auth-go-to-register',
    goToLogin: 'auth-go-to-login',
    forgotPasswordPage: 'auth-forgot-password-page',
    resetLinkSent: 'auth-reset-link-sent',
    resetPasswordPage: 'auth-reset-password-page',
    invalidResetLink: 'auth-invalid-reset-link',
    requestNewLink: 'auth-request-new-link',
    goToForgotPassword: 'auth-go-to-forgot-password',
    currentPasswordInput: 'auth-current-password-input',
    newPasswordInput: 'auth-new-password-input',
    confirmPasswordInput: 'auth-confirm-password-input',
    loginNotice: 'auth-login-notice',
    userMenu: 'auth-user-menu',
    userName: 'auth-user-name',
    logoutButton: 'auth-logout-button',
  },

  settings: {
    page: 'settings-page',
    email: 'settings-email',
    themeCard: 'settings-theme-card',
    themeOption: (value: string) => `settings-theme-${value}`,
    changePasswordCard: 'settings-change-password-card',
    changePasswordSubmit: 'settings-change-password-submit',
    changePasswordError: 'settings-change-password-error',
  },

  dashboard: {
    page: 'dashboard-page',
    connectionCard: 'dashboard-connection-card',
    connectionStatus: 'dashboard-connection-status',
    apiVersion: 'dashboard-api-version',
    greeting: 'dashboard-greeting',
  },
} as const;
