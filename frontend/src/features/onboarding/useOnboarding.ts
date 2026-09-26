import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router';

import { markOnboardingCompleted } from '@/features/auth/authStore';
import i18n from '@/i18n';
import { showToast } from '@/store/toastStore';
import { paths } from '@/routes/paths';

import { completeOnboarding, fetchOnboardingState, onboardingKeys, skipOnboarding } from './api';

export function useOnboardingState() {
  return useQuery({
    queryKey: onboardingKeys.state,
    queryFn: fetchOnboardingState,
  });
}

/**
 * Termina el alta.
 *
 * <p>Se limpia TODA la cache al salir. El asistente creo ingresos, gastos, metas
 * y un ciclo; cualquier lista que se hubiera consultado antes esta vieja, y la
 * aplicacion se abre justo despues.
 */
export function useCompleteOnboarding() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  return useMutation({
    mutationFn: completeOnboarding,
    onSuccess: async () => {
      markOnboardingCompleted();
      await queryClient.invalidateQueries();
      showToast(i18n.t('onboarding.toast.completed'));
      navigate(paths.dashboard, { replace: true });
    },
  });
}

export function useSkipOnboarding() {
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  return useMutation({
    mutationFn: skipOnboarding,
    onSuccess: async () => {
      markOnboardingCompleted();
      await queryClient.invalidateQueries();
      navigate(paths.dashboard, { replace: true });
    },
  });
}
