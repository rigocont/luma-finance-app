import MoreVertIcon from '@mui/icons-material/MoreVert';
import Card from '@mui/material/Card';
import Chip from '@mui/material/Chip';
import IconButton from '@mui/material/IconButton';
import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import Stack from '@mui/material/Stack';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Typography from '@mui/material/Typography';
import { useState, type MouseEvent } from 'react';

import { formatMoney } from '@/lib/money';
import { testIds } from '@/lib/testids';

import { FLEXIBILITY_SHORT, FREQUENCY_LABELS, type Expense } from './types';

interface ExpenseListProps {
  expenses: Expense[];
  onEdit: (expense: Expense) => void;
  onToggleActive: (expense: Expense) => void;
  onDelete: (expense: Expense) => void;
}

function calendario(expense: Expense): string {
  const cada = FREQUENCY_LABELS[expense.frequency];
  if (expense.dueDay === null) return cada;
  return `${cada}, dia ${expense.dueDay}`;
}

export function ExpenseList({ expenses, onEdit, onToggleActive, onDelete }: ExpenseListProps) {
  const [menuAnchor, setMenuAnchor] = useState<HTMLElement | null>(null);
  const [seleccionado, setSeleccionado] = useState<Expense | null>(null);

  function abrirMenu(event: MouseEvent<HTMLElement>, expense: Expense) {
    setMenuAnchor(event.currentTarget);
    setSeleccionado(expense);
  }

  function cerrarMenu() {
    setMenuAnchor(null);
    setSeleccionado(null);
  }

  function ejecutar(accion: (expense: Expense) => void) {
    if (seleccionado) accion(seleccionado);
    cerrarMenu();
  }

  return (
    <Card variant="outlined">
      <TableContainer>
        <Table data-testid={testIds.expenses.list}>
          <TableHead>
            <TableRow>
              <TableCell>Nombre</TableCell>
              <TableCell>Categoria</TableCell>
              <TableCell>Cuando</TableCell>
              <TableCell align="right">Monto</TableCell>
              <TableCell align="right" />
            </TableRow>
          </TableHead>

          <TableBody>
            {expenses.map((expense) => (
              <TableRow key={expense.id} data-testid={testIds.expenses.row(expense.id)} hover>
                <TableCell>
                  <Stack spacing={1}>
                    <Typography
                      variant="body1"
                      data-testid={testIds.expenses.rowName}
                      sx={{ color: expense.active ? 'text.primary' : 'text.disabled' }}
                    >
                      {expense.name}
                    </Typography>

                    <Stack direction="row" spacing={2} sx={{ flexWrap: 'wrap', gap: 1 }}>
                      <Chip
                        size="small"
                        variant="outlined"
                        label={FLEXIBILITY_SHORT[expense.flexibility]}
                        // Un gasto critico se marca en negativo: es el que no se
                        // puede mover cuando el ciclo no alcanza.
                        color={expense.flexibility === 'CRITICAL' ? 'error' : 'default'}
                        data-testid={testIds.expenses.rowFlexibility}
                      />
                      {!expense.active && (
                        <Chip
                          size="small"
                          label="Sin contar"
                          data-testid={testIds.expenses.rowInactive}
                        />
                      )}
                      {expense.requiresReview && (
                        <Chip
                          size="small"
                          color="warning"
                          variant="outlined"
                          label="Pide revision"
                          data-testid={testIds.expenses.rowReview}
                        />
                      )}
                    </Stack>
                  </Stack>
                </TableCell>

                <TableCell data-testid={testIds.expenses.rowCategory}>
                  <Typography variant="body2" color="text.secondary">
                    {expense.category?.name ?? 'Sin categoria'}
                  </Typography>
                </TableCell>

                <TableCell data-testid={testIds.expenses.rowSchedule}>
                  <Typography variant="body2" color="text.secondary">
                    {calendario(expense)}
                  </Typography>
                </TableCell>

                <TableCell align="right" data-testid={testIds.expenses.rowAmount}>
                  <Typography
                    variant="body1"
                    sx={{
                      fontVariantNumeric: 'tabular-nums',
                      color: expense.active ? 'text.primary' : 'text.disabled',
                    }}
                  >
                    {formatMoney(expense.amount)}
                  </Typography>
                </TableCell>

                <TableCell align="right">
                  <IconButton
                    aria-label={`Acciones de ${expense.name}`}
                    onClick={(event) => abrirMenu(event, expense)}
                    data-testid={testIds.expenses.rowMenu}
                  >
                    <MoreVertIcon fontSize="small" />
                  </IconButton>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      <Menu anchorEl={menuAnchor} open={Boolean(menuAnchor)} onClose={cerrarMenu}>
        <MenuItem onClick={() => ejecutar(onEdit)} data-testid={testIds.expenses.editAction}>
          Editar
        </MenuItem>
        <MenuItem
          onClick={() => ejecutar(onToggleActive)}
          data-testid={testIds.expenses.toggleActiveAction}
        >
          {seleccionado?.active ? 'Dejar de contarlo' : 'Volver a contarlo'}
        </MenuItem>
        <MenuItem onClick={() => ejecutar(onDelete)} data-testid={testIds.expenses.deleteAction}>
          Eliminar
        </MenuItem>
      </Menu>
    </Card>
  );
}
