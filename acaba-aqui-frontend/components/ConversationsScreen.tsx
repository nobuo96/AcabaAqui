import { useCallback, useState } from 'react';
import { Pressable, StyleSheet, View } from 'react-native';
import { useFocusEffect, useRouter } from 'expo-router';

import { AcabaAquiHeader } from '@/components/AcabaAquiHeader';
import { ScrollAreaView } from '@/components/ScrollAreaView';
import { Text } from '@/components/Themed';
import { communicationApi, type Conversation } from '@/lib/communicationApi';
import { getAuthUserId } from '@/lib/authToken';

export function ConversationsScreen() {
  const router = useRouter();
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [userId, setUserId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [items, currentUserId] = await Promise.all([
        communicationApi.listConversations(),
        getAuthUserId(),
      ]);
      setConversations(items);
      setUserId(currentUserId);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : 'Não foi possível carregar as conversas.');
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(useCallback(() => {
    void load();
  }, [load]));

  return (
    <View style={styles.container}>
      <AcabaAquiHeader title="Conversas" />
      <ScrollAreaView contentContainerStyle={styles.content}>
        {loading ? <Text style={styles.muted}>Carregando conversas...</Text> : null}
        {error ? <Text style={styles.error}>{error}</Text> : null}
        {!loading && !error && conversations.length === 0
          ? <Text style={styles.muted}>Após a confirmação de uma visita, a conversa aparecerá aqui.</Text>
          : null}
        {conversations.map((conversation) => {
          const otherParticipant = String(conversation.clienteId) === userId
            ? conversation.prestadorNome : conversation.clienteNome;
          return (
            <Pressable key={conversation.id} onPress={() => router.push({
              pathname: '/chat/[conversationId]',
              params: { conversationId: String(conversation.id) },
            })} style={styles.card}>
              <View style={styles.heading}>
                <Text style={styles.title}>{otherParticipant}</Text>
                <Text style={styles.date}>{new Date(conversation.dataCriacao).toLocaleDateString('pt-BR')}</Text>
              </View>
              <Text style={styles.detail}>{conversation.servicoNome}</Text>
              <Text style={styles.muted}>{conversation.cidade} - {conversation.uf}</Text>
            </Pressable>
          );
        })}
      </ScrollAreaView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff' },
  content: { padding: 20, gap: 12 },
  card: { borderColor: '#e1e1e1', borderRadius: 12, borderWidth: 1, gap: 7, padding: 14 },
  heading: { alignItems: 'center', flexDirection: 'row', justifyContent: 'space-between' },
  title: { color: '#222', flex: 1, fontSize: 17, fontWeight: '700' },
  detail: { color: '#444' },
  date: { color: '#777', fontSize: 12 },
  muted: { color: '#666', lineHeight: 21 },
  error: { color: '#a43434' },
});
