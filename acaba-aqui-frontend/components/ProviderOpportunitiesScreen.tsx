import { useCallback, useState } from 'react';
import { Pressable, StyleSheet, View } from 'react-native';
import { useFocusEffect, useRouter } from 'expo-router';

import { AcabaAquiHeader } from '@/components/AcabaAquiHeader';
import { ScrollAreaView } from '@/components/ScrollAreaView';
import { Text } from '@/components/Themed';
import { communicationApi, type RequestOpportunity } from '@/lib/communicationApi';

export function ProviderOpportunitiesScreen() {
  const router = useRouter();
  const [opportunities, setOpportunities] = useState<RequestOpportunity[]>([]);
  const [loading, setLoading] = useState(true);
  const [workingId, setWorkingId] = useState<number | null>(null);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const items = await communicationApi.listOpportunities();
      setOpportunities(items);
      await communicationApi.markOpportunitiesRead();
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : 'Não foi possível carregar as oportunidades.');
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(useCallback(() => {
    void load();
  }, [load]));

  async function respond(item: RequestOpportunity, action: 'accept' | 'decline') {
    if (workingId !== null) {
      return;
    }
    setWorkingId(item.id);
    setError('');
    try {
      if (action === 'accept') {
        const conversation = await communicationApi.acceptOpportunity(item.id);
        router.push({ pathname: '/chat/[conversationId]', params: { conversationId: String(conversation.id) } });
      } else {
        await communicationApi.declineOpportunity(item.id);
        setOpportunities((current) => current.map((opportunity) => opportunity.id === item.id
          ? { ...opportunity, status: 'RECUSADA' } : opportunity));
      }
    } catch (responseError) {
      setError(responseError instanceof Error ? responseError.message : 'Não foi possível responder à oportunidade.');
      await load();
    } finally {
      setWorkingId(null);
    }
  }

  return (
    <View style={styles.container}>
      <AcabaAquiHeader title="Oportunidades" />
      <ScrollAreaView contentContainerStyle={styles.content}>
        {loading ? <Text style={styles.muted}>Carregando oportunidades...</Text> : null}
        {error ? <Text style={styles.error}>{error}</Text> : null}
        {!loading && opportunities.length === 0
          ? <Text style={styles.muted}>Ainda não há oportunidades para seus serviços e regiões.</Text> : null}
        {opportunities.map((item) => (
          <View key={item.id} style={styles.card}>
            <View style={styles.heading}>
              <Text style={styles.title}>{item.servicoNome}</Text>
              <Text style={[styles.status, item.status !== 'PENDENTE' && styles.closedStatus]}>
                {statusLabel(item.status)}
              </Text>
            </View>
            <Text style={styles.detail}>Área aproximada: {formatArea(item.areaM2)} m²</Text>
            <Text style={styles.detail}>Região: {item.cidade} - {item.uf}</Text>
            <Text style={styles.date}>Recebida em {new Date(item.dataCriacao).toLocaleString('pt-BR')}</Text>
            {item.status === 'PENDENTE' ? (
              <View style={styles.actions}>
                <Pressable disabled={workingId !== null} onPress={() => { void respond(item, 'accept'); }}
                  style={[styles.actionButton, styles.acceptButton]}>
                  <Text style={styles.actionText}>{workingId === item.id ? 'Aguarde...' : 'Aceitar orçamento'}</Text>
                </Pressable>
                <Pressable disabled={workingId !== null} onPress={() => { void respond(item, 'decline'); }}
                  style={[styles.actionButton, styles.declineButton]}>
                  <Text style={[styles.actionText, styles.declineText]}>Recusar</Text>
                </Pressable>
              </View>
            ) : null}
          </View>
        ))}
      </ScrollAreaView>
    </View>
  );
}

function statusLabel(status: RequestOpportunity['status']) {
  return { PENDENTE: 'Nova', ACEITA: 'Aceita', RECUSADA: 'Recusada', ENCERRADA: 'Encerrada' }[status];
}

function formatArea(value: number) {
  return new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 2 }).format(value);
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff' },
  content: { padding: 20, gap: 14 },
  card: { borderColor: '#e1e1e1', borderRadius: 12, borderWidth: 1, gap: 9, padding: 14 },
  heading: { alignItems: 'center', flexDirection: 'row', gap: 8, justifyContent: 'space-between' },
  title: { color: '#222', flex: 1, fontSize: 17, fontWeight: '700' },
  status: { backgroundColor: '#edf4eb', borderRadius: 12, color: '#426341', fontSize: 12,
    overflow: 'hidden', paddingHorizontal: 9, paddingVertical: 4 },
  closedStatus: { backgroundColor: '#eee', color: '#666' },
  detail: { color: '#444', lineHeight: 21 },
  date: { color: '#777', fontSize: 13 },
  actions: { flexDirection: 'row', gap: 10, marginTop: 4 },
  actionButton: { alignItems: 'center', borderRadius: 9, flex: 1, padding: 11 },
  acceptButton: { backgroundColor: '#a43434' },
  declineButton: { borderColor: '#a43434', borderWidth: 1 },
  actionText: { color: '#fff', fontWeight: '700' },
  declineText: { color: '#a43434' },
  muted: { color: '#666', lineHeight: 21 },
  error: { color: '#a43434' },
});
