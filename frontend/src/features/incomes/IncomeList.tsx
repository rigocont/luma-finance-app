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

import { FREQUENCY_LABELS, INCOME_TYPE_LABELS, type Income } from './types';

interface IncomeListProps {
  incomes: Income[];
  onEdit: (income: Income) => void;
  onToggleActive: (income: Income) => void;
  onDelete: (income: Income) => void;
}

function calendario(income: Income): string {
  const cada = FREQUENCY_LABELS[income.frequency];
  if (income.expectedDay === null) return cada;
  return `${cada}, dia ${income.expectedDay}`;
}

export function IncomeList({ incomes, onEdit, onToggleActive, onDelete }: IncomeListProps) {
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
              <TableCell>Nombre</TableCell>
              <TableCell>Tipo</TableCell>
              <TableCell>Cada cuando</TableCell>
              <TableCell align="right">Monto</TableCell>
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
                          label="Sin contar"
                          data-testid={testIds.incomes.rowInactive}
                        />
                      )}
                      {income.requiresReview && (
                        <Chip
                          size="small"
                          color="warning"
                          variant="outlined"
                          label="Pide revision"
                          data-testid={testIds.incomes.rowReview}
                        />
                      )}
                    </Stack>
                  </Stack>
                </TableCell>

                <TableCell data-testid={testIds.incomes.rowType}>
                  <Typography variant="body2" color="text.secondary">
                    {INCOME_TYPE_LABELS[income.type]}
                  </Typography>
                </TableCell>

                <TableCell data-testid={testIds.incomes.rowSchedule}>
                  <Typography variant="body2" color="text.secondary">
                    {calendario(income)}
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
                    aria-label={`Acciones de ${income.name}`}
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
          Editar
        </MenuItem>
        <MenuItem
          onClick={() => ejecutar(onToggleActive)}
          data-testid={testIds.incomes.toggleActiveAction}
        >
          {seleccionado?.active ? 'Dejar de contarlo' : 'Volver a contarlo'}
        </MenuItem>
        <MenuItem onClick={() => ejecutar(onDelete)} data-testid={testIds.incomes.deleteAction}>
          Eliminar
        </MenuItem>
      </Menu>
    </Card>
  );
}
