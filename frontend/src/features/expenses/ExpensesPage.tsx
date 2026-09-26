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

import { ExpenseFormDrawer } from './ExpenseFormDrawer';
import { ExpenseList } from './ExpenseList';
import {
  EXPENSE_KINDS,
  EXPENSE_KIND_LABELS,
  EXPENSE_SORTS,
  EXPENSE_SORT_LABELS,
  type Expense,
  type ExpenseFilters,
  type ExpenseKind,
  type ExpensePayload,
  type ExpenseSort,
} from './types';
import {
  useCreateExpense,
  useDeleteExpense,
  useExpenseCategories,
  useExpenses,
  useSetExpenseActive,
  useUpdateExpense,
} from './useExpenses';

const FILTROS_INICIALES: ExpenseFilters = { sort: 'NEWEST', page: 0, size: 20 };

const TAMANOS_DE_PAGINA = [10, 20, 50];

export function ExpensesPage() {
  const [filters, setFilters] = useState<ExpenseFilters>(FILTROS_INICIALES);
  const [enEdicion, setEnEdicion] = useState<Expense | null>(null);
  const [cajonAbierto, setCajonAbierto] = useState(false);
  const [porEliminar, setPorEliminar] = useState<Expense | null>(null);

  const consulta = useExpenses(filters);
  const categorias = useExpenseCategories();
  const crear = useCreateExpense();
  const actualizar = useUpdateExpense();
  const cambiarActivo = useSetExpenseActive();
  const eliminar = useDeleteExpense();

  const guardando = crear.isPending || actualizar.isPending;

  function cambiarFiltro(cambio: Partial<ExpenseFilters>) {
    setFilters((previos) => ({ ...previos, ...cambio, page: 0 }));
  }

  function abrirNuevo() {
    setEnEdicion(null);
    crear.reset();
    actualizar.reset();
    setCajonAbierto(true);
  }

  function abrirEdicion(expense: Expense) {
    setEnEdicion(expense);
    crear.reset();
    actualizar.reset();
    setCajonAbierto(true);
  }

  function guardar(payload: ExpensePayload) {
    const mutacion = enEdicion
      ? actualizar.mutateAsync({ id: enEdicion.id, payload })
      : crear.mutateAsync(payload);

    // Solo se cierra si salio bien. Si el backend rechazo algo, el cajon se
    // queda abierto con el error junto a su campo y sin perder lo capturado.
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
  const hayFiltros =
    filters.kind !== undefined || filters.active !== undefined || filters.categoryId !== undefined;

  return (
    <Stack data-testid={testIds.expenses.page}>
      <PageHeader
        eyebrow="Tu dinero"
        title="Gastos"
        description="Lo que sale cada ciclo: lo que siempre cuesta igual y lo que cambia."
        action={
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={abrirNuevo}
            data-testid={testIds.expenses.createButton}
          >
            Capturar gasto
          </Button>
        }
      />

      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={3} sx={{ pb: 6, flexWrap: 'wrap' }}>
        <TextField
          select
          size="small"
          label="El monto"
          value={filters.kind ?? ''}
          onChange={(event) =>
            cambiarFiltro({ kind: (event.target.value || undefined) as ExpenseKind | undefined })
          }
          sx={{ minWidth: 190 }}
          data-testid={testIds.expenses.filterKind}
        >
          <MenuItem value="">Todos</MenuItem>
          {EXPENSE_KINDS.map((kind) => (
            <MenuItem key={kind} value={kind}>
              {EXPENSE_KIND_LABELS[kind]}
            </MenuItem>
          ))}
        </TextField>

        <TextField
          select
          size="small"
          label="Categoria"
          value={filters.categoryId ?? ''}
          onChange={(event) => cambiarFiltro({ categoryId: event.target.value || undefined })}
          sx={{ minWidth: 190 }}
          data-testid={testIds.expenses.filterCategory}
        >
          <MenuItem value="">Todas</MenuItem>
          {(categorias.data ?? []).map((category) => (
            <MenuItem key={category.id} value={category.id}>
              {category.name}
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
          sx={{ minWidth: 160 }}
          data-testid={testIds.expenses.filterActive}
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
          onChange={(event) => cambiarFiltro({ sort: event.target.value as ExpenseSort })}
          sx={{ minWidth: 170 }}
          data-testid={testIds.expenses.sort}
        >
          {EXPENSE_SORTS.map((sort) => (
            <MenuItem key={sort} value={sort}>
              {EXPENSE_SORT_LABELS[sort]}
            </MenuItem>
          ))}
        </TextField>
      </Stack>

      {consulta.isPending && <LoadingState rows={4} />}

      {consulta.isError && <ErrorState error={consulta.error} onRetry={() => consulta.refetch()} />}

      {pagina && pagina.content.length === 0 && (
        <EmptyState
          title={hayFiltros ? 'Nada con esos filtros' : 'Todavia no capturas ningun gasto'}
          description={
            hayFiltros
              ? 'Prueba quitando alguno para ver el resto.'
              : 'Empieza por los que no fallan: renta, servicios, despensa. Si ya tienes un ciclo abierto, apareceran ahi de inmediato.'
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
          <ExpenseList
            expenses={pagina.content}
            onEdit={abrirEdicion}
            onToggleActive={(expense) =>
              cambiarActivo.mutate({ id: expense.id, active: !expense.active })
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
            data-testid={testIds.expenses.pagination}
          />
        </Stack>
      )}

      <ExpenseFormDrawer
        open={cajonAbierto}
        expense={enEdicion}
        categories={categorias.data ?? []}
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
