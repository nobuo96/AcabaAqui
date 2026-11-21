import { useCallback, useState } from 'react';
import { Pressable, StyleSheet } from 'react-native';
import { useFocusEffect, useRouter } from 'expo-router';
import { SafeAreaView } from 'react-native-safe-area-context';

import { AcabaAquiButton } from '@/components/AcabaAquiButton';
import { ScrollAreaView } from '@/components/ScrollAreaView';
import { Text, View } from '@/components/Themed';
import { catalogApi } from '@/lib/catalogApi';
import { serviceDisputeApi, servicePayoutApi } from '@/lib/communicationApi';

export default function AdminHomeScreen() {
  const router = useRouter();
  const [pendingPayouts, setPendingPayouts] = useState(0);
  const [openDisputes, setOpenDisputes] = useState(0);
  const [activeServices, setActiveServices] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useFocusEffect(useCallback(() => {
    let active = true;
    async function loadSummary() {
      setLoading(true);
      setError('');
      try {
        const [payouts, disputes, services] = await Promise.all([
          servicePayoutApi.list(),
          serviceDisputeApi.list(),
          catalogApi.listServices(),
        ]);
        if (active) {
          setPendingPayouts(payouts.filter((item) => item.status === 'PENDENTE').length);
          setOpenDisputes(disputes.filter((item) => item.status === 'ABERTA').length);
          setActiveServices(services.filter((item) => item.ativo).length);
        }
      } catch (loadError) {
        if (active) {
          setError(loadError instanceof Error
            ? loadError.message : 'Não foi possível carregar o resumo administrativo.');
        }
      } finally {
        if (active) setLoading(false);
      }
    }
    void loadSummary();
    return () => { active = false; };
  }, []));

  return (
    <SafeAreaView edges={['top']} style={styles.safeArea}>
      <ScrollAreaView contentContainerStyle={styles.content}>
        <Text style={styles.eyebrow}>Administração</Text>
        <Text style={styles.title}>Painel de operações</Text>
        <Text style={styles.intro}>
          Acompanhe itens que exigem revisão e acesse rapidamente as áreas de gestão.
        </Text>

        {error ? <Text accessibilityRole="alert" style={styles.error}>{error}</Text> : null}
        <Text style={styles.sectionTitle}>Atenção prioritária</Text>
        <View style={styles.summaryGrid}>
          <SummaryCard
            label="Disputas abertas"
            value={loading ? '...' : String(openDisputes)}
            onPress={() => router.push('/(admin)/disputas')}
          />
          <SummaryCard
            label="Repasses pendentes"
            value={loading ? '...' : String(pendingPayouts)}
            onPress={() => router.push('/(admin)/repasses')}
          />
        </View>
        <AcabaAquiButton
          text="Revisar disputas"
          onPress={() => router.push('/(admin)/disputas')}
        />

        <Text style={styles.sectionTitle}>Gestão da plataforma</Text>
        <SummaryCard
          label="Serviços ativos no catálogo"
          value={loading ? '...' : String(activeServices)}
          onPress={() => router.push('/(admin)')}
        />
        <View style={styles.quickLinks}>
          <QuickLink label="Gerenciar serviços" onPress={() => router.push('/(admin)')} />
          <QuickLink label="Gerenciar categorias" onPress={() => router.push('/(admin)/categorias')} />
          <QuickLink label="Ver repasses" onPress={() => router.push('/(admin)/repasses')} />
          <QuickLink label="Ver disputas" onPress={() => router.push('/(admin)/disputas')} />
        </View>
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

function QuickLink({ label, onPress }: { label: string; onPress: () => void }) {
  return (
    <Pressable accessibilityRole="button" onPress={onPress} style={styles.quickLink}>
      <Text style={styles.quickLinkText}>{label}</Text>
      <Text style={styles.arrow}>›</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  safeArea: { flex: 1, backgroundColor: '#fff' },
  content: { padding: 20, gap: 14 },
  eyebrow: { color: '#a43434', fontSize: 13, fontWeight: '700' },
  title: { color: '#222', fontSize: 24, fontWeight: '800' },
  intro: { color: '#666', lineHeight: 21 },
  sectionTitle: { color: '#333', fontSize: 18, fontWeight: '800', marginTop: 4 },
  summaryGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 10 },
  summaryCard: { backgroundColor: '#fafafa', borderColor: '#e1e1e1', borderWidth: 1,
    borderRadius: 12, gap: 5, minHeight: 112, minWidth: 145, padding: 12, flexGrow: 1 },
  summaryValue: { color: '#a43434', fontSize: 28, fontWeight: '800' },
  summaryLabel: { color: '#444', flex: 1, fontWeight: '600' },
  summaryAction: { color: '#a43434', fontSize: 13, fontWeight: '700' },
  quickLinks: { borderColor: '#e1e1e1', borderWidth: 1, borderRadius: 12, overflow: 'hidden' },
  quickLink: { alignItems: 'center', borderBottomColor: '#eee', borderBottomWidth: 1,
    flexDirection: 'row', justifyContent: 'space-between', minHeight: 48, paddingHorizontal: 14 },
  quickLinkText: { color: '#333', fontWeight: '600' },
  arrow: { color: '#a43434', fontSize: 24 },
  error: { color: '#a43434', lineHeight: 20 },
});
