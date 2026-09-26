import AddIcon from '@mui/icons-material/Add';
import Button from '@mui/material/Button';
import MenuItem from '@mui/material/MenuItem';
import Stack from '@mui/material/Stack';
import TablePagination from '@mui/material/TablePagination';
import TextField from '@mui/material/TextField';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';

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
  incomeSortLabel,
  INCOME_TYPES,
  incomeTypeLabel,
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
  const { t } = useTranslation();
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
        eyebrow={t('incomes.eyebrow')}
        title={t('incomes.title')}
        description={t('incomes.description')}
        action={
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={abrirNuevo}
            data-testid={testIds.incomes.createButton}
          >
            {t('incomes.create')}
          </Button>
        }
      />

      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={3} sx={{ pb: 6 }}>
        <TextField
          select
          size="small"
          label={t('incomes.filters.type')}
          value={filters.type ?? ''}
          onChange={(event) =>
            cambiarFiltro({ type: (event.target.value || undefined) as IncomeType | undefined })
          }
          sx={{ minWidth: 200 }}
          data-testid={testIds.incomes.filterType}
        >
          <MenuItem value="">{t('common.all')}</MenuItem>
          {INCOME_TYPES.map((type) => (
            <MenuItem key={type} value={type}>
              {incomeTypeLabel(type)}
            </MenuItem>
          ))}
        </TextField>

        <TextField
          select
          size="small"
          label={t('incomes.filters.status')}
          value={filters.active === undefined ? '' : String(filters.active)}
          onChange={(event) =>
            cambiarFiltro({
              active: event.target.value === '' ? undefined : event.target.value === 'true',
            })
          }
          sx={{ minWidth: 180 }}
          data-testid={testIds.incomes.filterActive}
        >
          <MenuItem value="">{t('common.all')}</MenuItem>
          <MenuItem value="true">{t('incomes.filters.active')}</MenuItem>
          <MenuItem value="false">{t('incomes.filters.inactive')}</MenuItem>
        </TextField>

        <TextField
          select
          size="small"
          label={t('incomes.filters.sort')}
          value={filters.sort}
          onChange={(event) => cambiarFiltro({ sort: event.target.value as IncomeSort })}
          sx={{ minWidth: 180 }}
          data-testid={testIds.incomes.sort}
        >
          {INCOME_SORTS.map((sort) => (
            <MenuItem key={sort} value={sort}>
              {incomeSortLabel(sort)}
            </MenuItem>
          ))}
        </TextField>
      </Stack>

      {consulta.isPending && <LoadingState rows={4} />}

      {consulta.isError && <ErrorState error={consulta.error} onRetry={() => consulta.refetch()} />}

      {pagina && pagina.content.length === 0 && (
        <EmptyState
          title={hayFiltros ? t('incomes.empty.filteredTitle') : t('incomes.empty.title')}
          description={
            hayFiltros ? t('incomes.empty.filteredDescription') : t('incomes.empty.description')
          }
          action={
            hayFiltros ? (
              <Button onClick={() => setFilters(FILTROS_INICIALES)}>
                {t('common.clearFilters')}
              </Button>
            ) : (
              <Button variant="contained" onClick={abrirNuevo}>
                {t('incomes.captureFirst')}
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
            labelRowsPerPage={t('incomes.pagination.perPage')}
            labelDisplayedRows={({ from, to, count }) =>
              t('incomes.pagination.displayedRows', { from, to, count })
            }
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
        title={t('incomes.deleteConfirm.title', { name: porEliminar?.name ?? '' })}
        description={t('incomes.deleteConfirm.description')}
        confirmLabel={t('common.delete')}
        destructive
        pending={eliminar.isPending}
        onConfirm={confirmarEliminacion}
        onCancel={() => setPorEliminar(null)}
      />
    </Stack>
  );
}
