import i18n from '@/i18n';

/**
 * Formato de dinero.
 *
 * Un solo lugar. Formatear importes a mano produce inconsistencias visibles
 * ("$1,100" junto a "$1,100.00") y errores de redondeo.
 *
 * El backend envia los importes como cadena para no perder precision en el
 * tipo numerico de JavaScript. Aqui se convierten solo para mostrarlos.
 *
 * El locale de formato (separadores, orden del signo) sigue el idioma de la
 * interfaz (Fase 15) y no un valor fijo: no se cachea en una constante de
 * modulo porque el idioma puede cambiar sin recargar la pagina.
 */

export interface MoneyValue {
  amount: string;
  currency: string;
}

/** es -> es-MX, en -> en-US. Es el mismo mapeo que usa lib/date. */
export function intlLocaleFor(uiLanguage: string): string {
  return uiLanguage === 'en' ? 'en-US' : 'es-MX';
}

function currentLocale(): string {
  return intlLocaleFor(i18n.language);
}

export function formatMoney(
  value: MoneyValue,
  options: { locale?: string; withDecimals?: boolean } = {},
): string {
  const { locale = currentLocale(), withDecimals = true } = options;

  const formatter = new Intl.NumberFormat(locale, {
    style: 'currency',
    currency: value.currency,
    minimumFractionDigits: withDecimals ? 2 : 0,
    maximumFractionDigits: withDecimals ? 2 : 0,
  });

  return formatter.format(Number(value.amount));
}

/** Version compacta para espacios estrechos: $12,500 en lugar de $12,500.00 */
export function formatMoneyCompact(value: MoneyValue, locale = currentLocale()): string {
  return formatMoney(value, { locale, withDecimals: false });
}

export function isNegative(value: MoneyValue): boolean {
  return Number(value.amount) < 0;
}

export function isPositive(value: MoneyValue): boolean {
  return Number(value.amount) > 0;
}

export function isZero(value: MoneyValue): boolean {
  return Number(value.amount) === 0;
}
