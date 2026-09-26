import AddIcon from '@mui/icons-material/Add';
import Button from '@mui/material/Button';
import MenuItem from '@mui/material/MenuItem';
import Stack from '@mui/material/Stack';
import TablePagination from '@mui/material/TablePagination';
import TextField from '@mui/material/TextField';
import { useState } from 'react';

import { PageHeader } from '@/components/layout/PageHeader';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorState } from '@/components/ui/ErrorState';
import { LoadingState } from '@/components/ui/LoadingState';
import { testIds } from '@/lib/testids';

import { IncomeFormDrawer } from './IncomeFormDrawer';
import { IncomeList } from './IncomeList';
import {
  INCOME_SORTS,
  INCOME_SORT_LABELS,
  INCOME_TYPES,
  INCOME_TYPE_LABELS,
  type Income,
  type IncomeFilters,
  type IncomePayload,
  type IncomeSort,
  type IncomeType,
} from './types';
import {
  useCreateIncome,
  useDeleteIncome,
  useIncomes,
  useSetIncomeActive,
  useUpdateIncome,
} from './useIncomes';

const FILTROS_INICIALES: IncomeFilters = { sort: 'NEWEST', page: 0, size: 20 };

/** Coincide con MAX_PAGE_SIZE del backend. */
const TAMANOS_DE_PAGINA = [10, 20, 50];

export function IncomesPage() {
  const [filters, setFilters] = useState<IncomeFilters>(FILTROS_INICIALES);
  const [enEdicion, setEnEdicion] = useState<Income | null>(null);
  const [cajonAbierto, setCajonAbierto] = useState(false);
  const [porEliminar, setPorEliminar] = useState<Income | null>(null);

  const consulta = useIncomes(filters);
  const crear = useCreateIncome();
  const actualizar = useUpdateIncome();
  const cambiarActivo = useSetIncomeActive();
  const eliminar = useDeleteIncome();

  const guardando = crear.isPending || actualizar.isPending;

  /** Cambiar cualquier filtro vuelve a la primera pagina: la cuarta pagina de
   *  un listado filtrado casi siempre esta vacia y parece un error. */
  function cambiarFiltro(cambio: Partial<IncomeFilters>) {
    setFilters((previos) => ({ ...previos, ...cambio, page: 0 }));
  }

  function abrirNuevo() {
    setEnEdicion(null);
    crear.reset();
    actualizar.reset();
    setCajonAbierto(true);
  }

  function abrirEdicion(income: Income) {
    setEnEdicion(income);
    crear.reset();
    actualizar.reset();
    setCajonAbierto(true);
  }

  function guardar(payload: IncomePayload) {
    const mutacion = enEdicion
      ? actualizar.mutateAsync({ id: enEdicion.id, payload })
      : crear.mutateAsync(payload);

    // El cajon se cierra solo si la peticion salio bien. Si el backend rechazo
    // algo, se queda abierto con el error junto a su campo y sin perder lo
    // capturado.
    void mutacion.then(() => setCajonAbierto(false)).catch(() => undefined);
  }

  function confirmarEliminacion() {
    if (!porEliminar) return;
    void eliminar
      .mutateAsync({ id: porEliminar.id, name: porEliminar.name })
      .then(() => setPorEliminar(null))
      .catch(() => setPorEliminar(null));
  }

  const pagina = consulta.data;
  const hayFiltros = filters.type !== undefined || filters.active !== undefined;

  return (
    <Stack data-testid={testIds.incomes.page}>
      <PageHeader
        eyebrow="Tu dinero"
        title="Ingresos"
        description="De donde viene el dinero con el que armas cada ciclo."
        action={
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={abrirNuevo}
            data-testid={testIds.incomes.createButton}
          >
            Capturar ingreso
          </Button>
        }
      />

      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={3} sx={{ pb: 6 }}>
        <TextField
          select
          size="small"
          label="Tipo"
          value={filters.type ?? ''}
          onChange={(event) =>
            cambiarFiltro({ type: (event.target.value || undefined) as IncomeType | undefined })
          }
          sx={{ minWidth: 200 }}
          data-testid={testIds.incomes.filterType}
        >
          <MenuItem value="">Todos</MenuItem>
          {INCOME_TYPES.map((type) => (
            <MenuItem key={type} value={type}>
              {INCOME_TYPE_LABELS[type]}
            </MenuItem>
          ))}
        </TextField>

        <TextField
          select
          size="small"
          label="Estado"
          value={filters.active === undefined ? '' : String(filters.active)}
          onChange={(event) =>
            cambiarFiltro({
              active: event.target.value === '' ? undefined : event.target.value === 'true',
            })
          }
          sx={{ minWidth: 180 }}
          data-testid={testIds.incomes.filterActive}
        >
          <MenuItem value="">Todos</MenuItem>
          <MenuItem value="true">Cuentan</MenuItem>
          <MenuItem value="false">Sin contar</MenuItem>
        </TextField>

        <TextField
          select
          size="small"
          label="Ordenar por"
          value={filters.sort}
          onChange={(event) => cambiarFiltro({ sort: event.target.value as IncomeSort })}
          sx={{ minWidth: 180 }}
          data-testid={testIds.incomes.sort}
        >
          {INCOME_SORTS.map((sort) => (
            <MenuItem key={sort} value={sort}>
              {INCOME_SORT_LABELS[sort]}
            </MenuItem>
          ))}
        </TextField>
      </Stack>

      {consulta.isPending && <LoadingState rows={4} />}

      {consulta.isError && <ErrorState error={consulta.error} onRetry={() => consulta.refetch()} />}

      {pagina && pagina.content.length === 0 && (
        <EmptyState
          title={hayFiltros ? 'Nada con esos filtros' : 'Todavia no capturas ningun ingreso'}
          description={
            hayFiltros
              ? 'Prueba quitando alguno para ver el resto.'
              : 'Empieza por tu sueldo. Si ya tienes un ciclo abierto, aparecera ahi de inmediato.'
          }
          action={
            hayFiltros ? (
              <Button onClick={() => setFilters(FILTROS_INICIALES)}>Quitar filtros</Button>
            ) : (
              <Button variant="contained" onClick={abrirNuevo}>
                Capturar el primero
              </Button>
            )
          }
        />
      )}

      {pagina && pagina.content.length > 0 && (
        <Stack spacing={4}>
          <IncomeList
            incomes={pagina.content}
            onEdit={abrirEdicion}
            onToggleActive={(income) =>
              cambiarActivo.mutate({ id: income.id, active: !income.active })
            }
            onDelete={setPorEliminar}
          />

          <TablePagination
            component="div"
            count={pagina.totalElements}
            page={pagina.page}
            rowsPerPage={pagina.size}
            rowsPerPageOptions={TAMANOS_DE_PAGINA}
            onPageChange={(_event, page) => setFilters((previos) => ({ ...previos, page }))}
            onRowsPerPageChange={(event) => cambiarFiltro({ size: Number(event.target.value) })}
            labelRowsPerPage="Por pagina"
            labelDisplayedRows={({ from, to, count }) => `${from}-${to} de ${count}`}
            data-testid={testIds.incomes.pagination}
          />
        </Stack>
      )}

      <IncomeFormDrawer
        open={cajonAbierto}
        income={enEdicion}
        pending={guardando}
        error={enEdicion ? actualizar.error : crear.error}
        onSubmit={guardar}
        onClose={() => setCajonAbierto(false)}
      />

      <ConfirmDialog
        open={porEliminar !== null}
        title={`Eliminar "${porEliminar?.name ?? ''}"`}
        description={
          'Deja de aparecer en tus listas y no se puede recuperar. Los ciclos ' +
          'que ya lo tenian no cambian. Si solo quieres que deje de contar, ' +
          'usa "Dejar de contarlo".'
        }
        confirmLabel="Eliminar"
        destructive
        pending={eliminar.isPending}
        onConfirm={confirmarEliminacion}
        onCancel={() => setPorEliminar(null)}
      />
    </Stack>
  );
}
