import { useCallback, useState } from 'react';
import { Pressable, StyleSheet } from 'react-native';
import { useFocusEffect, useRouter } from 'expo-router';
import { SafeAreaView } from 'react-native-safe-area-context';

import { AcabaAquiButton } from '@/components/AcabaAquiButton';
import { ScrollAreaView } from '@/components/ScrollAreaView';
import { Text, View } from '@/components/Themed';
import { communicationApi, serviceExecutionApi } from '@/lib/communicationApi';
import { useCurrentUser } from '@/lib/useCurrentUser';

export default function ProviderHomeScreen() {
  const router = useRouter();
  const { nome, carregando, erro } = useCurrentUser();
  const [pendingOpportunities, setPendingOpportunities] = useState(0);
  const [activeServices, setActiveServices] = useState(0);
  const [conversationCount, setConversationCount] = useState(0);
  const [summaryLoading, setSummaryLoading] = useState(true);
  const [summaryError, setSummaryError] = useState('');

  useFocusEffect(useCallback(() => {
    let active = true;
    async function loadSummary() {
      setSummaryLoading(true);
      setSummaryError('');
      try {
        const [opportunities, executions, conversations] = await Promise.all([
          communicationApi.listOpportunities(),
          serviceExecutionApi.list(),
          communicationApi.listConversations(),
        ]);
        if (active) {
          setPendingOpportunities(opportunities.filter((item) => item.status === 'PENDENTE').length);
          setActiveServices(executions.filter((item) =>
            !['APROVADA', 'CANCELADA'].includes(item.status)).length);
          setConversationCount(conversations.length);
        }
      } catch (loadError) {
        if (active) {
          setSummaryError(loadError instanceof Error
            ? loadError.message : 'Não foi possível carregar o resumo.');
        }
      } finally {
        if (active) setSummaryLoading(false);
      }
    }
    void loadSummary();
    return () => { active = false; };
  }, []));

  return (
    <SafeAreaView edges={['top']} style={styles.safeArea}>
      <ScrollAreaView contentContainerStyle={styles.content}>
        <Text style={styles.eyebrow}>Painel do prestador</Text>
        <Text style={styles.title}>
          {carregando ? 'Carregando...' : `Olá${nome ? `, ${nome}` : ''}`}
        </Text>
        {erro ? <Text accessibilityRole="alert" style={styles.error}>{erro}</Text> : null}
        <View style={styles.hero}>
          <Text style={styles.heroTitle}>Encontre seu próximo serviço</Text>
          <Text style={styles.heroText}>
            Veja solicitações compatíveis com seus serviços e regiões de atendimento.
          </Text>
          <AcabaAquiButton
            text="Ver oportunidades"
            onPress={() => router.push('/(prestador)/oportunidades')}
          />
        </View>

        <Text style={styles.sectionTitle}>Resumo</Text>
        {summaryError ? <Text accessibilityRole="alert" style={styles.error}>{summaryError}</Text> : null}
        <View style={styles.summaryGrid}>
          <SummaryCard
            label="Novas oportunidades"
            value={summaryLoading ? '...' : String(pendingOpportunities)}
            onPress={() => router.push('/(prestador)/oportunidades')}
          />
          <SummaryCard
            label="Serviços ativos"
            value={summaryLoading ? '...' : String(activeServices)}
            onPress={() => router.push('/(prestador)/servicos')}
          />
          <SummaryCard
            label="Conversas"
            value={summaryLoading ? '...' : String(conversationCount)}
            onPress={() => router.push('/(prestador)/conversas')}
          />
        </View>

        <Text style={styles.sectionTitle}>Seu perfil profissional</Text>
        <Text style={styles.muted}>
          Mantenha serviços e regiões atualizados para receber oportunidades adequadas.
        </Text>
        <AcabaAquiButton
          text="Configurar serviços e regiões"
          variant="secondary"
          onPress={() => router.push('/prestador/cobertura')}
        />
      </ScrollAreaView>
    </SafeAreaView>
  );
}

function SummaryCard({ label, value, onPress }: {
  label: string;
  value: string;
  onPress: () => void;
}) {
  return (
    <Pressable accessibilityRole="button" onPress={onPress} style={styles.summaryCard}>
      <Text style={styles.summaryValue}>{value}</Text>
      <Text style={styles.summaryLabel}>{label}</Text>
      <Text style={styles.summaryAction}>Abrir</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: '#fff' },
  content: { padding: 20, gap: 14 },
  eyebrow: { color: '#a43434', fontSize: 13, fontWeight: '700' },
  title: { color: '#222', fontSize: 24, fontWeight: '800' },
  hero: { backgroundColor: '#fff8f8', borderColor: '#efdada', borderWidth: 1,
    borderRadius: 16, gap: 10, padding: 16 },
  heroTitle: { color: '#333', fontSize: 19, fontWeight: '800' },
  heroText: { color: '#666', lineHeight: 21 },
  sectionTitle: { color: '#333', fontSize: 18, fontWeight: '800', marginTop: 4 },
  summaryGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 10 },
  summaryCard: { backgroundColor: '#fafafa', borderColor: '#e1e1e1', borderWidth: 1,
    borderRadius: 12, gap: 5, minHeight: 112, minWidth: 145, padding: 12, flexGrow: 1 },
  summaryValue: { color: '#a43434', fontSize: 28, fontWeight: '800' },
  summaryLabel: { color: '#444', flex: 1, fontWeight: '600' },
  summaryAction: { color: '#a43434', fontSize: 13, fontWeight: '700' },
  muted: { color: '#666', lineHeight: 21 },
  error: { color: '#a43434', lineHeight: 20 },
});
