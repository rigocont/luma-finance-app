/**
 * Formato de fechas.
 *
 * Un solo lugar, por la misma razon que el dinero: formatear a mano en cada
 * pantalla produce "15 mar 2026" junto a "2026-03-15" y nadie sabe cual es la
 * correcta.
 *
 * Las fechas de la API son LocalDate ("2026-03-15"), sin hora ni zona. Pasarlas
 * a `new Date(iso)` las interpreta en UTC, y en husos negativos —el de LUMA
 * entre ellos— eso muestra el dia ANTERIOR. Por eso la cadena se parte a mano y
 * se construye una fecha local.
 */

const DEFAULT_LOCALE = 'es-MX';

const CORTO = new Intl.DateTimeFormat(DEFAULT_LOCALE, {
  day: 'numeric',
  month: 'short',
  year: 'numeric',
});

/** Una fecha ISO sin hora, en la zona local. Nulo si la cadena no lo es. */
export function parseLocalDate(iso: string): Date | null {
  const partes = iso.split('-');
  if (partes.length !== 3) return null;

  const [ano, mes, dia] = partes.map(Number) as [number, number, number];
  if (!Number.isFinite(ano) || !Number.isFinite(mes) || !Number.isFinite(dia)) return null;

  return new Date(ano, mes - 1, dia);
}

/**
 * "15 mar 2026".
 *
 * Si la cadena no es una fecha reconocible se devuelve tal cual: es preferible
 * mostrar el dato crudo que un "Invalid Date" que no le dice nada a nadie.
 */
export function formatDay(iso: string): string {
  const fecha = parseLocalDate(iso);
  return fecha ? CORTO.format(fecha) : iso;
}
