import { useCallback, useState } from 'react';
import { Pressable, StyleSheet } from 'react-native';
import { useRouter } from 'expo-router';
import { useFocusEffect } from 'expo-router';

import { Text, View } from '@/components/Themed';
import { AcabaAquiButton } from '@/components/AcabaAquiButton';
import { useCurrentUser } from '@/lib/useCurrentUser';
import { getAuthUserId } from '@/lib/authToken';
import { serviceRequestApi } from '@/lib/serviceRequestApi';
import { communicationApi, serviceExecutionApi } from '@/lib/communicationApi';
import { ScrollAreaView } from '@/components/ScrollAreaView';

export default function HomeScreen() {
  const router = useRouter();
  const { nome, carregando, erro } = useCurrentUser();
  const [activeRequests, setActiveRequests] = useState(0);
  const [activeServices, setActiveServices] = useState(0);
  const [conversationCount, setConversationCount] = useState(0);
  const [summaryLoading, setSummaryLoading] = useState(true);

  useFocusEffect(useCallback(() => {
    let active = true;
    async function loadSummary() {
      setSummaryLoading(true);
      try {
        const userId = await getAuthUserId();
        if (!userId) return;
        const [requests, conversations, services] = await Promise.all([
          serviceRequestApi.list(userId),
          communicationApi.listConversations(),
          serviceExecutionApi.list(),
        ]);
        if (active) {
          setActiveRequests(requests.filter((item) => item.status !== 'ENCERRADA').length);
          setConversationCount(conversations.length);
          setActiveServices(services.filter((item) => item.status !== 'APROVADA' && item.status !== 'CANCELADA').length);
        }
      } catch {
        // Keep the main shortcuts usable even if the summary cannot be loaded.
      } finally {
        if (active) setSummaryLoading(false);
      }
    }
    void loadSummary();
    return () => { active = false; };
  }, []));

  return (
    <View style={styles.container}>
      <ScrollAreaView contentContainerStyle={styles.content}>
        {carregando ? <Text style={styles.muted}>Carregando seu painel...</Text> : null}
        {!carregando && !erro ? <Text style={styles.title}>Olá, {nome || 'cliente'}</Text> : null}
        {erro ? <Text style={styles.error}>{erro}</Text> : null}

        <View style={styles.hero}>
          <Text style={styles.heroTitle}>Precisa resolver um serviço?</Text>
          <Text style={styles.heroText}>Conte o que precisa e escolha um horário para receber visitas de orçamento.</Text>
          <AcabaAquiButton text="Criar nova solicitação"
            onPress={() => router.push('/cliente/novaSolicitacao')} />
        </View>

        <Text style={styles.sectionTitle}>Seu acompanhamento</Text>
        <View style={styles.summaryGrid}>
          <SummaryCard label="Solicitações abertas" value={summaryLoading ? '...' : String(activeRequests)}
            onPress={() => router.push('/(cliente)/solicitacoes')} />
          <SummaryCard label="Serviços em andamento" value={summaryLoading ? '...' : String(activeServices)}
            onPress={() => router.push('/(cliente)/servicos')} />
          <SummaryCard label="Conversas" value={summaryLoading ? '...' : String(conversationCount)}
            onPress={() => router.push('/(cliente)/conversas')} />
        </View>
      </ScrollAreaView>
    </View>
  );
}

function SummaryCard({ label, value, onPress }: { label: string; value: string; onPress: () => void }) {
  return (
    <Pressable accessibilityRole="button" onPress={onPress} style={styles.summaryCard}>
      <Text style={styles.summaryValue}>{value}</Text>
      <Text style={styles.summaryLabel}>{label}</Text>
      <Text style={styles.summaryAction}>Abrir</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff' },
  content: { paddingHorizontal: 20, paddingVertical: 18, gap: 14 },
  title: {
    fontSize: 24,
    fontWeight: '800',
    color: '#222',
  },
  error: {
    color: '#a43434',
    marginTop: 12,
  },
  hero: { backgroundColor: '#fff8f8', borderColor: '#efdada', borderWidth: 1,
    borderRadius: 16, gap: 10, padding: 16 },
  heroTitle: { color: '#333', fontSize: 19, fontWeight: '800' },
  heroText: { color: '#666', lineHeight: 21 },
  sectionTitle: { color: '#333', fontSize: 18, fontWeight: '800', marginTop: 4 },
  summaryGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 10 },
  summaryCard: { backgroundColor: '#fafafa', borderColor: '#e1e1e1', borderWidth: 1,
    borderRadius: 12, gap: 5, minHeight: 112, minWidth: 150, padding: 12, flexGrow: 1 },
  summaryValue: { color: '#a43434', fontSize: 28, fontWeight: '800' },
  summaryLabel: { color: '#444', flex: 1, fontWeight: '600' },
  summaryAction: { color: '#a43434', fontSize: 13, fontWeight: '700' },
  muted: { color: '#666' },
});
