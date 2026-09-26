import { useState } from 'react';
import { ActivityIndicator, Pressable, StyleSheet, Text, View } from 'react-native';

import { logout } from '@/features/auth/authService';
import { useAuthStore } from '@/features/auth/authStore';
import { colors } from '@/theme/colors';

/**
 * Placeholder de M0: solo confirma que el wiring de autenticacion funciona
 * de punta a punta (login, sesion persistida, logout). El resumen
 * financiero real -"Este ciclo" en modo lectura- es el contenido de M1, ver
 * docs/roadmap-mobile.md.
 */
export default function HomeScreen() {
  const user = useAuthStore((state) => state.user);
  const [loggingOut, setLoggingOut] = useState(false);

  async function handleLogout() {
    setLoggingOut(true);
    try {
      await logout();
      // RootLayout observa el status y vuelve solo al grupo (auth).
    } finally {
      setLoggingOut(false);
    }
  }

  return (
    <View style={styles.screen}>
      <View style={styles.card}>
        <Text style={styles.greeting}>Hola, {user?.name ?? 'de nuevo'}</Text>
        <Text style={styles.subtitle}>{user?.email}</Text>
        <Text style={styles.note}>
          Esta es la base de la app movil de LUMA (M0). Las pantallas de ciclos, gastos y ahorros
          llegan en las siguientes fases.
        </Text>
      </View>

      <Pressable
        style={[styles.button, loggingOut && styles.buttonDisabled]}
        onPress={handleLogout}
        disabled={loggingOut}
      >
        {loggingOut ? (
          <ActivityIndicator color={colors.ink} />
        ) : (
          <Text style={styles.buttonText}>Cerrar sesion</Text>
        )}
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: colors.ground,
    padding: 24,
    justifyContent: 'space-between',
  },
  card: {
    backgroundColor: colors.surface,
    borderRadius: 16,
    padding: 24,
    borderWidth: 1,
    borderColor: colors.line,
    marginTop: 24,
  },
  greeting: {
    fontSize: 22,
    fontWeight: '700',
    color: colors.ink,
  },
  subtitle: {
    fontSize: 14,
    color: colors.muted,
    marginTop: 4,
  },
  note: {
    fontSize: 14,
    color: colors.text,
    marginTop: 16,
    lineHeight: 20,
  },
  button: {
    backgroundColor: colors.surface2,
    borderRadius: 10,
    paddingVertical: 14,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: colors.line,
    marginBottom: 24,
  },
  buttonDisabled: {
    opacity: 0.6,
  },
  buttonText: {
    color: colors.ink,
    fontSize: 16,
    fontWeight: '600',
  },
});
