import { useCallback, useState } from 'react';
import { Pressable, StyleSheet, View } from 'react-native';
import { useFocusEffect, useRouter } from 'expo-router';

import { Text } from '@/components/Themed';
import { inAppNotificationApi, type InAppNotification } from '@/lib/communicationApi';

export function InAppNotificationInbox() {
  const router = useRouter();
  const [notifications, setNotifications] = useState<InAppNotification[]>([]);
  const [error, setError] = useState('');
  const [workingId, setWorkingId] = useState<number | null>(null);
  const [expanded, setExpanded] = useState(false);

  const load = useCallback(async () => {
    try {
      setNotifications(await inAppNotificationApi.list());
      setError('');
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : 'Não foi possível carregar as notificações.');
    }
  }, []);

  useFocusEffect(useCallback(() => {
    void load();
  }, [load]));

  async function markRead(notificationId: number) {
    if (workingId !== null) return;
    setWorkingId(notificationId);
    setError('');
    try {
      await inAppNotificationApi.markRead(notificationId);
      setNotifications((current) => current.map((item) => item.id === notificationId
        ? { ...item, lida: true } : item));
    } catch (markError) {
      setError(markError instanceof Error ? markError.message : 'Não foi possível atualizar a notificação.');
    } finally {
      setWorkingId(null);
    }
  }

  return (
    <View style={styles.container}>
      <Pressable accessibilityRole="button" accessibilityState={{ expanded }}
        onPress={() => setExpanded((current) => !current)} style={styles.headingRow}>
        <Text style={styles.heading}>Notificações</Text>
        <Text style={styles.action}>{expanded ? 'Recolher' : `${notifications.filter((item) => !item.lida).length} nova(s)`}</Text>
      </Pressable>
      {!expanded ? null : <>
      {error ? <Text style={styles.error}>{error}</Text> : null}
      {!error && notifications.length === 0
        ? <Text style={styles.muted}>Você não tem notificações.</Text> : null}
      {notifications.map((notification) => (
        <View key={notification.id} style={[styles.item, !notification.lida && styles.unread]}>
          <View style={styles.message}>
            <Text style={styles.text}>{notification.mensagem}</Text>
            <Text style={styles.date}>
              {new Date(notification.dataCriacao).toLocaleString('pt-BR')}
            </Text>
          </View>
          {notification.solicitacaoId ? (
            <Pressable accessibilityRole="link" onPress={() => {
              void markRead(notification.id);
              router.push('/(cliente)/solicitacoes');
            }}>
              <Text style={styles.action}>Abrir solicitação</Text>
            </Pressable>
          ) : null}
          {!notification.lida ? (
            <Pressable
              disabled={workingId !== null}
              onPress={() => { void markRead(notification.id); }}
            >
              <Text style={styles.action}>
                {workingId === notification.id ? 'Salvando...' : 'Marcar lida'}
              </Text>
            </Pressable>
          ) : <Text style={styles.readLabel}>Lida</Text>}
        </View>
      ))}
      </>}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { borderColor: '#e1e1e1', borderRadius: 12, borderWidth: 1, gap: 8, padding: 14 },
  headingRow: { alignItems: 'center', flexDirection: 'row', justifyContent: 'space-between' },
  heading: { color: '#333', fontSize: 16, fontWeight: '700' },
  item: { alignItems: 'center', borderTopColor: '#eee', borderTopWidth: 1, flexDirection: 'row',
    gap: 10, justifyContent: 'space-between', paddingTop: 9 },
  unread: { backgroundColor: '#fffaf0' },
  message: { flex: 1, gap: 4 },
  text: { color: '#444', lineHeight: 20 },
  date: { color: '#777', fontSize: 12 },
  action: { color: '#a43434', fontSize: 12, fontWeight: '700' },
  readLabel: { color: '#777', fontSize: 12 },
  muted: { color: '#666' },
  error: { color: '#a43434' },
});
