import { Stack } from 'expo-router';
import { ActivityIndicator, StyleSheet, View } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { useAuthStore } from '@/features/auth/authStore';
import { useBootstrapSession } from '@/features/auth/useBootstrapSession';
import { colors } from '@/theme/colors';

/**
 * Layout raiz: decide, una sola vez al abrir la app, si hay sesion o no.
 *
 * Mientras `useBootstrapSession` intenta renovarla con el refresh token
 * guardado en el dispositivo (`status === 'bootstrapping'`), no se pinta ni
 * el login ni la app: solo un indicador de carga. Ya resuelto, `Stack.
 * Protected` decide el grupo de rutas visible segun `status` — el mismo
 * patron de guardas que usa el frontend web con `ProtectedRoute`, adaptado a
 * la navegacion de expo-router.
 */
export default function RootLayout() {
  useBootstrapSession();
  const status = useAuthStore((state) => state.status);

  if (status === 'bootstrapping') {
    return (
      <View style={styles.loading}>
        <ActivityIndicator color={colors.honey} size="large" />
      </View>
    );
  }

  return (
    <SafeAreaProvider>
      <Stack screenOptions={{ headerShown: false }}>
        <Stack.Protected guard={status === 'authenticated'}>
          <Stack.Screen name="(app)" />
        </Stack.Protected>
        <Stack.Protected guard={status !== 'authenticated'}>
          <Stack.Screen name="(auth)" />
        </Stack.Protected>
      </Stack>
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({
  loading: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.ground,
  },
});
