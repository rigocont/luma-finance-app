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
import { useTranslation } from 'react-i18next';
import type { TFunction } from 'i18next';

import { formatMoney } from '@/lib/money';
import { testIds } from '@/lib/testids';

import { flexibilityShort, frequencyLabel, type Expense } from './types';

interface ExpenseListProps {
  expenses: Expense[];
  onEdit: (expense: Expense) => void;
  onToggleActive: (expense: Expense) => void;
  onDelete: (expense: Expense) => void;
}

function calendario(t: TFunction, expense: Expense): string {
  const cada = frequencyLabel(expense.frequency);
  if (expense.dueDay === null) return cada;
  return t('expenses.schedule.withDay', { frequency: cada, day: expense.dueDay });
}

export function ExpenseList({ expenses, onEdit, onToggleActive, onDelete }: ExpenseListProps) {
  const { t } = useTranslation();
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
              <TableCell>{t('expenses.table.name')}</TableCell>
              <TableCell>{t('expenses.table.category')}</TableCell>
              <TableCell>{t('expenses.table.schedule')}</TableCell>
              <TableCell align="right">{t('expenses.table.amount')}</TableCell>
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
                        label={flexibilityShort(expense.flexibility)}
                        // Un gasto critico se marca en negativo: es el que no se
                        // puede mover cuando el ciclo no alcanza.
                        color={expense.flexibility === 'CRITICAL' ? 'error' : 'default'}
                        data-testid={testIds.expenses.rowFlexibility}
                      />
                      {!expense.active && (
                        <Chip
                          size="small"
                          label={t('expenses.inactiveChip')}
                          data-testid={testIds.expenses.rowInactive}
                        />
                      )}
                      {expense.requiresReview && (
                        <Chip
                          size="small"
                          color="warning"
                          variant="outlined"
                          label={t('expenses.reviewChip')}
                          data-testid={testIds.expenses.rowReview}
                        />
                      )}
                    </Stack>
                  </Stack>
                </TableCell>

                <TableCell data-testid={testIds.expenses.rowCategory}>
                  <Typography variant="body2" color="text.secondary">
                    {expense.category?.name ?? t('expenses.noCategory')}
                  </Typography>
                </TableCell>

                <TableCell data-testid={testIds.expenses.rowSchedule}>
                  <Typography variant="body2" color="text.secondary">
                    {calendario(t, expense)}
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
                    aria-label={t('expenses.actionsFor', { name: expense.name })}
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
          {t('expenses.menu.edit')}
        </MenuItem>
        <MenuItem
          onClick={() => ejecutar(onToggleActive)}
          data-testid={testIds.expenses.toggleActiveAction}
        >
          {seleccionado?.active ? t('expenses.menu.deactivate') : t('expenses.menu.activate')}
        </MenuItem>
        <MenuItem onClick={() => ejecutar(onDelete)} data-testid={testIds.expenses.deleteAction}>
          {t('expenses.menu.delete')}
        </MenuItem>
      </Menu>
    </Card>
  );
}
