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

import { frequencyLabel, incomeTypeLabel, type Income } from './types';

interface IncomeListProps {
  incomes: Income[];
  onEdit: (income: Income) => void;
  onToggleActive: (income: Income) => void;
  onDelete: (income: Income) => void;
}

function calendario(t: TFunction, income: Income): string {
  const cada = frequencyLabel(income.frequency);
  if (income.expectedDay === null) return cada;
  return t('incomes.schedule.withDay', { frequency: cada, day: income.expectedDay });
}

export function IncomeList({ incomes, onEdit, onToggleActive, onDelete }: IncomeListProps) {
  const { t } = useTranslation();
  const [menuAnchor, setMenuAnchor] = useState<HTMLElement | null>(null);
  const [seleccionado, setSeleccionado] = useState<Income | null>(null);

  function abrirMenu(event: MouseEvent<HTMLElement>, income: Income) {
    setMenuAnchor(event.currentTarget);
    setSeleccionado(income);
  }

  function cerrarMenu() {
    setMenuAnchor(null);
    setSeleccionado(null);
  }

  function ejecutar(accion: (income: Income) => void) {
    if (seleccionado) accion(seleccionado);
    cerrarMenu();
  }

  return (
    <Card variant="outlined">
      <TableContainer>
        <Table data-testid={testIds.incomes.list}>
          <TableHead>
            <TableRow>
              <TableCell>{t('incomes.table.name')}</TableCell>
              <TableCell>{t('incomes.table.type')}</TableCell>
              <TableCell>{t('incomes.table.schedule')}</TableCell>
              <TableCell align="right">{t('incomes.table.amount')}</TableCell>
              <TableCell align="right" />
            </TableRow>
          </TableHead>

          <TableBody>
            {incomes.map((income) => (
              <TableRow key={income.id} data-testid={testIds.incomes.row(income.id)} hover>
                <TableCell>
                  <Stack spacing={1}>
                    <Typography
                      variant="body1"
                      data-testid={testIds.incomes.rowName}
                      // Un ingreso inactivo se ve apagado, pero NO tachado: no
                      // esta borrado, solo no cuenta por ahora.
                      sx={{ color: income.active ? 'text.primary' : 'text.disabled' }}
                    >
                      {income.name}
                    </Typography>

                    <Stack direction="row" spacing={2} sx={{ flexWrap: 'wrap', gap: 1 }}>
                      {!income.active && (
                        <Chip
                          size="small"
                          label={t('incomes.inactiveChip')}
                          data-testid={testIds.incomes.rowInactive}
                        />
                      )}
                      {income.requiresReview && (
                        <Chip
                          size="small"
                          color="warning"
                          variant="outlined"
                          label={t('incomes.reviewChip')}
                          data-testid={testIds.incomes.rowReview}
                        />
                      )}
                    </Stack>
                  </Stack>
                </TableCell>

                <TableCell data-testid={testIds.incomes.rowType}>
                  <Typography variant="body2" color="text.secondary">
                    {incomeTypeLabel(income.type)}
                  </Typography>
                </TableCell>

                <TableCell data-testid={testIds.incomes.rowSchedule}>
                  <Typography variant="body2" color="text.secondary">
                    {calendario(t, income)}
                  </Typography>
                </TableCell>

                <TableCell align="right" data-testid={testIds.incomes.rowAmount}>
                  <Typography
                    variant="body1"
                    // Tabulares: las columnas de dinero tienen que alinearse.
                    sx={{
                      fontVariantNumeric: 'tabular-nums',
                      color: income.active ? 'text.primary' : 'text.disabled',
                    }}
                  >
                    {formatMoney(income.amount)}
                  </Typography>
                </TableCell>

                <TableCell align="right">
                  <IconButton
                    aria-label={t('incomes.actionsFor', { name: income.name })}
                    onClick={(event) => abrirMenu(event, income)}
                    data-testid={testIds.incomes.rowMenu}
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
        <MenuItem onClick={() => ejecutar(onEdit)} data-testid={testIds.incomes.editAction}>
          {t('incomes.menu.edit')}
        </MenuItem>
        <MenuItem
          onClick={() => ejecutar(onToggleActive)}
          data-testid={testIds.incomes.toggleActiveAction}
        >
          {seleccionado?.active ? t('incomes.menu.deactivate') : t('incomes.menu.activate')}
        </MenuItem>
        <MenuItem onClick={() => ejecutar(onDelete)} data-testid={testIds.incomes.deleteAction}>
          {t('incomes.menu.delete')}
        </MenuItem>
      </Menu>
    </Card>
  );
}
