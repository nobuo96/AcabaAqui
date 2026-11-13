import { useCallback, useState } from 'react';
import { Pressable, StyleSheet, TextInput, View } from 'react-native';
import { useFocusEffect, useLocalSearchParams, useRouter } from 'expo-router';

import { AcabaAquiHeader } from '@/components/AcabaAquiHeader';
import { ScrollAreaView } from '@/components/ScrollAreaView';
import { Text } from '@/components/Themed';
import { communicationApi, type ChatMessage } from '@/lib/communicationApi';
import { getAuthUserId } from '@/lib/authToken';

export default function ChatRoute() {
  const router = useRouter();
  const params = useLocalSearchParams<{ conversationId: string | string[] }>();
  const conversationId = Number(Array.isArray(params.conversationId)
    ? params.conversationId[0] : params.conversationId);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [userId, setUserId] = useState<string | null>(null);
  const [draft, setDraft] = useState('');
  const [sending, setSending] = useState(false);
  const [error, setError] = useState('');

  useFocusEffect(useCallback(() => {
    let active = true;
    async function refresh() {
      if (!Number.isSafeInteger(conversationId) || conversationId <= 0) {
        setError('Conversa inválida.');
        return;
      }
      try {
        const [items, currentUserId] = await Promise.all([
          communicationApi.listMessages(conversationId),
          getAuthUserId(),
        ]);
        if (active) {
          setMessages(items);
          setUserId(currentUserId);
          setError('');
        }
      } catch (loadError) {
        if (active) {
          setError(loadError instanceof Error ? loadError.message : 'Não foi possível carregar as mensagens.');
        }
      }
    }
    void refresh();
    const timer = setInterval(() => { void refresh(); }, 5000);
    return () => {
      active = false;
      clearInterval(timer);
    };
  }, [conversationId]));

  async function send() {
    const content = draft.trim();
    if (!content || sending) {
      return;
    }
    setSending(true);
    setError('');
    try {
      const message = await communicationApi.sendMessage(conversationId, content);
      setMessages((current) => [...current, message]);
      setDraft('');
    } catch (sendError) {
      setError(sendError instanceof Error ? sendError.message : 'Não foi possível enviar a mensagem.');
    } finally {
      setSending(false);
    }
  }

  return (
    <View style={styles.container}>
      <AcabaAquiHeader title="Chat" backButton onBackPress={() => router.back()} />
      <ScrollAreaView contentContainerStyle={styles.messages}>
        {error ? <Text style={styles.error}>{error}</Text> : null}
        {messages.length === 0 && !error
          ? <Text style={styles.empty}>Envie uma mensagem para iniciar a conversa.</Text> : null}
        {messages.map((message) => {
          const mine = String(message.remetenteId) === userId;
          return (
            <View key={message.id} style={[styles.message, mine ? styles.ownMessage : styles.otherMessage]}>
              <Text style={styles.sender}>{mine ? 'Você' : message.remetenteNome}</Text>
              <Text style={styles.messageText}>{message.conteudo}</Text>
              <Text style={styles.time}>
                {new Date(message.dataCriacao).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })}
              </Text>
            </View>
          );
        })}
      </ScrollAreaView>
      <View style={styles.composer}>
        <TextInput value={draft} onChangeText={setDraft} placeholder="Digite sua mensagem"
          multiline maxLength={4000} style={styles.input} />
        <Pressable disabled={sending || !draft.trim()} onPress={() => { void send(); }}
          style={[styles.sendButton, (sending || !draft.trim()) && styles.disabledButton]}>
          <Text style={styles.sendText}>{sending ? '...' : 'Enviar'}</Text>
        </Pressable>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff' },
  messages: { flexGrow: 1, gap: 10, padding: 16 },
  empty: { color: '#666', textAlign: 'center', marginTop: 16 },
  message: { borderRadius: 12, maxWidth: '85%', padding: 11 },
  ownMessage: { alignSelf: 'flex-end', backgroundColor: '#f5e5e5' },
  otherMessage: { alignSelf: 'flex-start', backgroundColor: '#f1f1f1' },
  sender: { color: '#555', fontSize: 12, fontWeight: '700', marginBottom: 4 },
  messageText: { color: '#222', fontSize: 16 },
  time: { color: '#777', fontSize: 11, marginTop: 5, textAlign: 'right' },
  composer: { alignItems: 'flex-end', borderTopColor: '#eaeaea', borderTopWidth: 1,
    flexDirection: 'row', gap: 8, padding: 12 },
  input: { borderColor: '#ccc', borderRadius: 10, borderWidth: 1, color: '#222',
    flex: 1, maxHeight: 120, minHeight: 42, paddingHorizontal: 12, paddingVertical: 9 },
  sendButton: { backgroundColor: '#a43434', borderRadius: 9, paddingHorizontal: 15, paddingVertical: 12 },
  disabledButton: { opacity: 0.5 },
  sendText: { color: '#fff', fontWeight: '700' },
  error: { color: '#a43434', textAlign: 'center' },
});
