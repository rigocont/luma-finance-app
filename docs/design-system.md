# LUMA — Sistema de diseño v1

**Estado:** propuesta aprobada en Fase 0 · se implementa en Fase 3
**Documento visual:** ver el artefacto "Identidad LUMA"

---

## 1. Concepto

LUMA significa luz. La identidad toma la palabra literalmente.

**Decisión central: la calidez viene de los neutros, no del acento.** Fondos color pergamino, grises con temperatura, esquinas suaves. Sobre esa base, un solo acento de miel y dos colores que únicamente significan dinero.

Esto evita el azul frío y vigilante de casi toda app financiera sin caer en decoración. La app se siente cálida por su base, no por sus adornos — que es también lo que la hace barata de mantener.

---

## 2. Las seis reglas KISS

Un desarrollador debe poder decidir sin consultar a nadie:

1. **Una familia tipográfica, siete tamaños.** Si un tamaño no está en la escala, no existe.
2. **Toda acción primaria es tinta** (`#1B1917`). Nunca hay que elegir color de botón. La miel no se usa para acciones: solo identidad, foco y progreso.
3. **Verde y rojo solo significan dinero.** Sin íconos rojos decorativos, sin ilustraciones verdes.
4. **Dos niveles de sombra, y uno es «ninguna».** Casi todo se separa con borde de 1 px.
5. **Una acción primaria por pantalla.** Dos botones de tinta compitiendo = uno está mal clasificado.
6. **La interfaz habla como una persona.** «Te alcanza», no «Superávit». Los enums viven en la API, no en la pantalla.

---

## 3. Color

### 3.1 Decisión: el ámbar NO es advertencia

Casi toda app financiera gasta amarillo en «atención». LUMA no:

| Situación | Color |
|---|---|
| Pago próximo | Neutro — es información, no alarma |
| Pago vencido | Negativo (rojo) |
| Déficit | Negativo (rojo) |
| Pagado / superávit | Positivo (verde) |
| Balanceado | Tinta / neutro |

Eso **elimina un color semántico completo del sistema** y libera la miel para ser el color de la marca.

### 3.2 Tokens — tema claro

| Token | Hex | Uso |
|---|---|---|
| `--luma-ground` | `#FBF8F3` | Fondo de la aplicación |
| `--luma-surface` | `#FFFFFF` | Tarjetas, modales, tablas |
| `--luma-surface-2` | `#F5F0E8` | Fondos sutiles, pistas de progreso |
| `--luma-line` | `#E8E0D4` | Bordes y separadores |
| `--luma-line-strong` | `#D6CCBC` | Bordes de énfasis |
| `--luma-ink` | `#1B1917` | Texto principal · **toda acción primaria** |
| `--luma-text` | `#2C2825` | Texto de cuerpo |
| `--luma-muted` | `#6F6862` | Texto secundario |
| `--luma-faint` | `#9A928A` | Etiquetas, metadatos |
| `--luma-honey` | `#C98A2B` | Marca sobre claro, foco, % de metas |
| `--luma-honey-fill` | `#E8AE4C` | Rellenos, barras de progreso |
| `--luma-honey-wash` | `#FBF0DC` | Fondos de acento |
| `--luma-positive` | `#2E7D52` | Te alcanza · pagado |
| `--luma-positive-wash` | `#E4F2E9` | Fondo positivo |
| `--luma-negative` | `#B3402F` | Déficit · vencido |
| `--luma-negative-wash` | `#FAE8E4` | Fondo negativo |
| `--luma-on-ink` | `#FBF8F3` | Texto sobre tinta |

### 3.3 Tokens — tema oscuro

| Token | Hex |
|---|---|
| `--luma-ground` | `#1A1815` |
| `--luma-surface` | `#221F1A` |
| `--luma-surface-2` | `#2A2620` |
| `--luma-line` | `#35302A` |
| `--luma-line-strong` | `#464037` |
| `--luma-ink` | `#F0EBE3` |
| `--luma-text` | `#E7E1D8` |
| `--luma-muted` | `#A69E94` |
| `--luma-faint` | `#7C746B` |
| `--luma-honey` | `#EFB959` |
| `--luma-honey-fill` | `#D99930` |
| `--luma-honey-wash` | `#33291A` |
| `--luma-positive` | `#6ABE8E` |
| `--luma-positive-wash` | `#1E3128` |
| `--luma-negative` | `#E8806C` |
| `--luma-negative-wash` | `#3A231E` |
| `--luma-on-ink` | `#1A1815` |

> El tema oscuro es **cálido**, no gris neutro. Los fondos conservan el sesgo hacia la miel.

---

## 4. Tipografía

**Familia única: Figtree** (Google Fonts). Geométrica de terminaciones suaves — legible a 12 px y amable a 34 px.

```
font-family: "Figtree", ui-sans-serif, system-ui, -apple-system, "Segoe UI", Roboto, sans-serif;
```

Pesos cargados: 400, 500, 600, 700, 800. Nada más.

### Escala

| Rol | Tamaño / Peso | Tracking | Uso |
|---|---|---|---|
| Display | 34 / 800 | −0.025em | Cifra de balance |
| Título | 25 / 700 | −0.018em | Título de página |
| Sección | 19 / 600 | −0.01em | Encabezado de sección |
| Cuerpo | 15 / 400 | — | Texto general |
| Menor | 13 / 500 | — | Metadatos, fechas |
| Etiqueta | 11 / 700 | +0.10em, MAYÚS | Eyebrows, encabezados de tabla |
| Cifra | 30 / 700 | −0.02em | Importes destacados |

### Regla de las cifras

**Todo importe lleva `font-variant-numeric: tabular-nums`.** Sin excepción. Las columnas de dinero deben alinearse.

> ⚠️ **Verificar en Fase 3:** confirmar que Figtree expone figuras tabulares (`tnum`). Si no, el plan B es **Manrope**, que las tiene y conserva el carácter amable. El resto del sistema no cambia.

---

## 5. Forma y espacio

### Radios

| Token | Valor | Uso |
|---|---|---|
| `--luma-radius-sm` | 10px | Botones, inputs, filas |
| `--luma-radius-md` | 14px | Tarjetas |
| `--luma-radius-lg` | 20px | Contenedores grandes, modales |
| `--luma-radius-full` | 999px | Pastillas, barras de progreso, avatares |

### Espacio — base 4

| Contexto | Valor |
|---|---|
| Interior de controles | 8 · 12 |
| Interior de tarjetas | 16 · 20 |
| Entre tarjetas | 16 |
| Entre secciones | 32 · 48 |
| Margen de página | 24 (16 en móvil) |

### Elevación

| Token | Valor | Uso |
|---|---|---|
| — (borde) | `1px solid var(--luma-line)` | **El caso por defecto.** Todo se separa con línea. |
| `--luma-shadow-1` | `0 1px 2px rgba(43,33,20,.05)` | Elementos ligeramente levantados |
| `--luma-shadow-2` | `0 6px 20px -6px rgba(43,33,20,.14)` | Tarjeta de balance, modales, toasts, dropdowns |

Las sombras son **de tinta cálida**, nunca negro puro.

---

## 6. Voz de la interfaz

La API devuelve enums; la pantalla habla español normal.

| Estado del motor | Texto en pantalla | Color |
|---|---|---|
| `DEFICIT` | «Te falta para cerrar» | Negativo |
| `BALANCED` | «Justo» | Neutro |
| `SURPLUS` | «Te alcanza» | Positivo |

| Estado del item | Texto | Color |
|---|---|---|
| `PAID` | Pagado | Positivo |
| `PENDING` | Pendiente | Apagado |
| `OVERDUE` | Vencido | Negativo |
| `NEEDS_REVIEW` | Revisar | Miel |
| `PARTIAL` | Pago parcial | Apagado |
| `SKIPPED` | Omitido | Tenue |

**Reglas de copy:**

- Segunda persona, voz activa.
- Un botón dice exactamente qué pasa: «Registrar pago» → toast «Pago registrado».
- Los errores explican qué pasó y cómo arreglarlo. Sin disculpas, sin vaguedad.
- El déficit **siempre** se acompaña de explicación. Nunca solo el número en rojo.
- Cifras en formato `es-MX`: `$12,500.00` · `−$740.00` (menos tipográfico, no guion).

---

## 7. Marca

**Wordmark:** `LUMA` en Figtree 800, tracking `+0.14em`, en tinta.

**Símbolo:** círculo con un degradado radial desplegado desde arriba a la izquierda — miel clara → miel → tinta. Una fuente de luz cálida, sin ilustración ni metáfora extra.

```css
background: radial-gradient(circle at 34% 30%,
  var(--luma-honey-fill) 0%,
  var(--luma-honey) 52%,
  var(--luma-ink) 100%);
```

---

## 8. Implementación en MUI

Los tokens viven en `frontend/src/theme/tokens.ts` como **fuente única de verdad** y alimentan tanto `createTheme` como las variables CSS.

```ts
// frontend/src/theme/tokens.ts  (esqueleto — se crea en Fase 1)
export const tokens = {
  light: {
    ground: '#FBF8F3', surface: '#FFFFFF', surface2: '#F5F0E8',
    line: '#E8E0D4', lineStrong: '#D6CCBC',
    ink: '#1B1917', text: '#2C2825', muted: '#6F6862', faint: '#9A928A',
    honey: '#C98A2B', honeyFill: '#E8AE4C', honeyWash: '#FBF0DC',
    positive: '#2E7D52', positiveWash: '#E4F2E9',
    negative: '#B3402F', negativeWash: '#FAE8E4',
    onInk: '#FBF8F3',
  },
  dark: { /* ver §3.3 */ },
  radius: { sm: 10, md: 14, lg: 20, full: 999 },
  space:  (n: number) => n * 4,
} as const;
```

Puntos clave de los overrides de MUI (`theme/components.ts`):

- `MuiButton`: `disableElevation`, `textTransform: 'none'`, radio `sm`, `contained` = tinta
- `MuiPaper`: `variant="outlined"` por defecto; sombra solo donde se pide explícitamente
- `MuiCard`: radio `md`, borde `line`, sin sombra
- `MuiTextField`: `outlined`, radio `sm`, anillo de foco en miel
- `MuiChip`: radio `full`, solo los tres tonos semánticos
- `MuiCssBaseline`: fondo `ground`, `color-scheme` sincronizado con el tema

---

## 9. Accesibilidad

- Contraste mínimo AA (4.5:1) para texto; verificado en ambos temas.
- **El color nunca es el único portador de significado**: cada estado lleva texto además de color (una fila vencida dice «Vencido», no solo se pinta de rojo).
- Foco visible en todos los controles: `outline: 2px solid var(--luma-honey); outline-offset: 2px`.
- `prefers-reduced-motion` respetado.
- Objetivos táctiles ≥ 44 px en móvil.
