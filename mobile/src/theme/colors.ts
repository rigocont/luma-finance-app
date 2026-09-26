/**
 * Subconjunto de los tokens de `docs/design-system.md` que usan las
 * pantallas de M0.
 *
 * Movil todavia no decidio si replica el sistema de diseno 1 a 1 o adapta
 * algunos patrones a gestos tactiles (ver docs/roadmap-mobile.md, seccion
 * 5): esto es lo minimo para que login y la pantalla inicial se vean como
 * LUMA, no el sistema completo.
 */
export const colors = {
  ground: '#FBF8F3',
  surface: '#FFFFFF',
  surface2: '#F5F0E8',
  line: '#E8E0D4',
  ink: '#1B1917',
  text: '#2C2825',
  muted: '#6F6862',
  faint: '#9A928A',
  honey: '#C98A2B',
  honeyWash: '#FBF0DC',
  positive: '#2E7D52',
  negative: '#B3402F',
  negativeWash: '#FAE8E4',
  onInk: '#FBF8F3',
} as const;
