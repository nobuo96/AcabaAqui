import { useCallback, useState } from 'react';
import { Pressable, StyleSheet, TextInput, View } from 'react-native';
import { useFocusEffect } from 'expo-router';

import { AcabaAquiButton } from '@/components/AcabaAquiButton';
import { AcabaAquiHeader } from '@/components/AcabaAquiHeader';
import { ScrollAreaView } from '@/components/ScrollAreaView';
import { Text } from '@/components/Themed';
import { catalogApi, type CatalogService } from '@/lib/catalogApi';
import { communicationApi, type ProviderRegion } from '@/lib/communicationApi';

export function ProviderCoverageScreen({ onBack }: { onBack: () => void }) {
  const [services, setServices] = useState<CatalogService[]>([]);
  const [selectedServiceIds, setSelectedServiceIds] = useState<number[]>([]);
  const [coverage, setCoverage] = useState<ProviderRegion[]>([]);
  const [city, setCity] = useState('');
  const [uf, setUf] = useState('');
  const [loading, setLoading] = useState(true);
  const [savingServices, setSavingServices] = useState(false);
  const [savingRegion, setSavingRegion] = useState(false);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [availableServices, areas, selectedServices] = await Promise.all([
        catalogApi.listServices(),
        communicationApi.listCoverage(),
        communicationApi.listProviderServices(),
      ]);
      setServices(availableServices.filter((service) => service.ativo));
      setCoverage(areas);
      setSelectedServiceIds(selectedServices);
    } catch (loadError) {
      setError(loadError instanceof Error
        ? loadError.message : 'Não foi possível carregar serviços e regiões.');
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(useCallback(() => {
    void load();
  }, [load]));

  function toggleService(serviceId: number) {
    if (loading || savingServices) {
      return;
    }
    setSelectedServiceIds((current) => current.includes(serviceId)
      ? current.filter((id) => id !== serviceId)
      : [...current, serviceId]);
  }

  async function saveServices() {
    if (loading || savingServices) {
      return;
    }
    setSavingServices(true);
    setError('');
    try {
      setSelectedServiceIds(await communicationApi.saveProviderServices(selectedServiceIds));
    } catch (saveError) {
      setError(saveError instanceof Error ? saveError.message : 'Não foi possível salvar os serviços.');
    } finally {
      setSavingServices(false);
    }
  }

  async function addCoverage() {
    if (savingRegion) {
      return;
    }
    if (!city.trim() || !/^[a-z]{2}$/i.test(uf.trim())) {
      setError('Informe uma cidade e uma UF válida.');
      return;
    }
    setSavingRegion(true);
    setError('');
    try {
      await communicationApi.addCoverage({ cidade: city.trim(), uf: uf.trim().toUpperCase() });
      setCity('');
      setUf('');
      setCoverage(await communicationApi.listCoverage());
    } catch (saveError) {
      setError(saveError instanceof Error ? saveError.message : 'Não foi possível adicionar a região.');
    } finally {
      setSavingRegion(false);
    }
  }

  async function removeCoverage(id: number) {
    setError('');
    try {
      await communicationApi.removeCoverage(id);
      setCoverage((current) => current.filter((area) => area.id !== id));
    } catch (removeError) {
      setError(removeError instanceof Error ? removeError.message : 'Não foi possível remover a região.');
    }
  }

  return (
    <View style={styles.container}>
      <AcabaAquiHeader title="Serviços e regiões" backButton onBackPress={onBack} />
      <ScrollAreaView contentContainerStyle={styles.content}>
        <Text style={styles.description}>
          Os serviços selecionados se aplicam a todas as regiões cadastradas.
        </Text>
        {loading ? <Text style={styles.muted}>Carregando serviços e regiões...</Text> : null}
        <Text style={styles.sectionTitle}>Serviços que você atende</Text>
        {services.map((service) => (
          <Pressable key={service.id} onPress={() => toggleService(service.id)}
            style={[styles.serviceOption, selectedServiceIds.includes(service.id) && styles.selectedOption]}>
            <Text style={styles.optionText}>
              {selectedServiceIds.includes(service.id) ? '[x] ' : '[ ] '}{service.nome}
            </Text>
          </Pressable>
        ))}
        {services.length === 0 && !loading
          ? <Text style={styles.muted}>Não há serviços disponíveis no catálogo.</Text> : null}
        <AcabaAquiButton
          text={savingServices ? 'Salvando serviços...' : 'Salvar serviços'}
          onPress={() => { void saveServices(); }}
        />

        <Text style={styles.sectionTitle}>Regiões atendidas</Text>
        <View style={styles.fields}>
          <TextInput value={city} onChangeText={setCity} placeholder="Cidade" style={[styles.input, styles.cityInput]} />
          <TextInput value={uf} onChangeText={setUf} placeholder="UF" maxLength={2}
            autoCapitalize="characters" style={[styles.input, styles.ufInput]} />
        </View>
        {error ? <Text style={styles.error}>{error}</Text> : null}
        <AcabaAquiButton
          text={savingRegion ? 'Salvando região...' : 'Adicionar região'}
          onPress={() => { void addCoverage(); }}
        />

        {!loading && coverage.length === 0
          ? <Text style={styles.muted}>Cadastre uma região para começar a receber oportunidades.</Text> : null}
        {coverage.map((area) => (
          <View key={area.id} style={styles.card}>
            <View style={styles.cardText}>
              <Text style={styles.optionText}>{area.cidade} - {area.uf}</Text>
            </View>
            <Pressable onPress={() => { void removeCoverage(area.id); }}>
              <Text style={styles.remove}>Remover</Text>
            </Pressable>
          </View>
        ))}
      </ScrollAreaView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff' },
  content: { padding: 20, gap: 12 },
  description: { color: '#555', lineHeight: 21 },
  sectionTitle: { color: '#222', fontSize: 18, fontWeight: '700', marginTop: 10 },
  serviceOption: { borderWidth: 1, borderColor: '#ddd', borderRadius: 9, padding: 11 },
  selectedOption: { borderColor: '#a43434', backgroundColor: '#fff8f8' },
  optionText: { color: '#222', fontSize: 15, fontWeight: '600' },
  fields: { flexDirection: 'row', gap: 10 },
  input: { borderColor: '#ccc', borderRadius: 9, borderWidth: 1, color: '#222', padding: 11 },
  cityInput: { flex: 1 },
  ufInput: { width: 70 },
  card: { alignItems: 'center', borderColor: '#e1e1e1', borderRadius: 10, borderWidth: 1,
    flexDirection: 'row', justifyContent: 'space-between', padding: 12 },
  cardText: { gap: 5 },
  muted: { color: '#666', lineHeight: 21 },
  error: { color: '#a43434' },
  remove: { color: '#a43434', fontWeight: '700' },
});
