/**
 * Formato de dinero.
 *
 * Un solo lugar. Formatear importes a mano produce inconsistencias visibles
 * ("$1,100" junto a "$1,100.00") y errores de redondeo.
 *
 * El backend envia los importes como cadena para no perder precision en el
 * tipo numerico de JavaScript. Aqui se convierten solo para mostrarlos.
 */

export interface MoneyValue {
  amount: string;
  currency: string;
}

const DEFAULT_LOCALE = 'es-MX';

export function formatMoney(
  value: MoneyValue,
  options: { locale?: string; withDecimals?: boolean } = {},
): string {
  const { locale = DEFAULT_LOCALE, withDecimals = true } = options;

  const formatter = new Intl.NumberFormat(locale, {
    style: 'currency',
    currency: value.currency,
    minimumFractionDigits: withDecimals ? 2 : 0,
    maximumFractionDigits: withDecimals ? 2 : 0,
  });

  return formatter.format(Number(value.amount));
}

/** Version compacta para espacios estrechos: $12,500 en lugar de $12,500.00 */
export function formatMoneyCompact(value: MoneyValue, locale = DEFAULT_LOCALE): string {
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
