import { useEffect, useState } from 'react';
import { Pressable, StyleSheet, TextInput, View } from 'react-native';
import { useRouter } from 'expo-router';

import { AcabaAquiButton } from '@/components/AcabaAquiButton';
import { AcabaAquiHeader } from '@/components/AcabaAquiHeader';
import { ScrollAreaView } from '@/components/ScrollAreaView';
import { Text } from '@/components/Themed';
import { catalogApi, type CatalogService } from '@/lib/catalogApi';
import { getAuthUserId } from '@/lib/authToken';
import { serviceRequestApi, type RequestAddress } from '@/lib/serviceRequestApi';
import { AcabaAquiDropdown } from '@/components/AcabaAquiDropdown';
import { AcabaAquiInput } from '@/components/AcabaAquiInput';

interface SavedAddress extends RequestAddress {
  principal: boolean;
}

export default function NovaSolicitacaoScreen() {
  const router = useRouter();
  const [userId, setUserId] = useState<string | null>(null);
  const [services, setServices] = useState<CatalogService[]>([]);
  const [addresses, setAddresses] = useState<SavedAddress[]>([]);
  const [selectedServiceId, setSelectedServiceId] = useState<number | null>(null);
  const [selectedAddressId, setSelectedAddressId] = useState<number | null>(null);
  const [area, setArea] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    async function load() {
      try {
        const id = await getAuthUserId();
        if (!id) {
          throw new Error('Sessão não encontrada. Entre novamente.');
        }
        const [availableServices, savedAddresses] = await Promise.all([
          catalogApi.listServices(),
          serviceRequestApi.listAddresses(id),
        ]);
        if (active) {
          setUserId(id);
          setServices(availableServices);
          setAddresses(savedAddresses);
          setSelectedServiceId(availableServices[0]?.id ?? null);
          setSelectedAddressId(savedAddresses.find((address) => address.principal)?.id
            ?? savedAddresses[0]?.id ?? null);
        }
      } catch (loadError) {
        if (active) {
          setError(loadError instanceof Error ? loadError.message : 'Não foi possível carregar os dados.');
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }
    void load();
    return () => {
      active = false;
    };
  }, []);

  async function submit() {
    if (!userId || saving) {
      return;
    }
    const areaValue = Number(area.trim().replace(',', '.'));
    if (!selectedServiceId) {
      setError('Selecione um serviço disponível.');
      return;
    }
    if (!selectedAddressId) {
      setError('Selecione um endereço.');
      return;
    }
    if (!Number.isFinite(areaValue) || areaValue <= 0) {
      setError('Informe uma área maior que zero.');
      return;
    }

    setError('');
    setSaving(true);
    try {
      await serviceRequestApi.create(userId, {
        servicoId: selectedServiceId,
        enderecoId: selectedAddressId,
        areaM2: areaValue,
      });
      router.replace('/(cliente)/solicitacoes');
    } catch (saveError) {
      setError(saveError instanceof Error ? saveError.message : 'Não foi possível criar a solicitação.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <View style={styles.container}>
      <AcabaAquiHeader title="Nova solicitação" backButton onBackPress={() => router.back()} />
      <ScrollAreaView contentContainerStyle={styles.content}>
        {loading ? <Text style={styles.muted}>Carregando serviços e endereços...</Text> : null}
        {error ? <Text style={styles.error}>{error}</Text> : null}

        {!loading && addresses.length === 0 ? (
          <View style={styles.emptyAddress}>
            <Text style={styles.sectionTitle}>Cadastre um endereço para continuar</Text>
            <Text style={styles.muted}>
              É necessário ter pelo menos um endereço cadastrado para criar uma solicitação.
            </Text>
            <AcabaAquiButton text="Cadastrar endereço" onPress={() => router.push('/enderecos')} />
          </View>
        ) : null}

        {!loading && addresses.length > 0 ? <>
          <Text style={styles.sectionTitle}>Serviço</Text>
          {services.length === 0
            ? <Text style={styles.muted}>Não há serviços disponíveis no momento.</Text>
            : <AcabaAquiDropdown data={services.map((service) => ({ label: service.nome, value: service.id.toString() }))} />
            // services.map((service) => (
            //   <Pressable
            //     key={service.id}
            //     onPress={() => setSelectedServiceId(service.id)}
            //     style={[styles.option, selectedServiceId === service.id && styles.optionSelected]}
            //   >
            //     <Text style={styles.optionTitle}>{service.nome}</Text>
            //     <Text style={styles.muted}>{service.descricao}</Text>
            //   </Pressable>
            // ))
          }

          <Text style={styles.sectionTitle}>Área aproximada</Text>
          <View style={styles.fieldGroup}>
            <Text style={styles.label}>Área em m²</Text>
            <AcabaAquiInput
              placeholder="Ex: 45,5"
              keyboardType="decimal-pad"
              value={area}
              onChangeText={setArea}
            />
          </View>

          <Text style={styles.sectionTitle}>Endereço do serviço</Text>
          {addresses.map((address) => (
            <Pressable
              key={address.id}
              onPress={() => setSelectedAddressId(address.id)}
              style={[styles.option, selectedAddressId === address.id && styles.optionSelected]}
            >
              <Text style={styles.optionTitle}>
                {address.apelido || 'Endereço'}{address.id === addresses.find((item) => item.principal)?.id
                  ? ' · Principal' : ''}
              </Text>
              <Text style={styles.muted}>{formatAddress(address)}</Text>
            </Pressable>
          ))}

          <AcabaAquiButton
            text={saving ? 'Enviando...' : 'Criar solicitação'}
            onPress={() => { void submit(); }}
          />
        </> : null}
      </ScrollAreaView>
    </View>
  );
}

function formatAddress(address: RequestAddress) {
  const street = [address.logradouro, address.numero, address.complemento, address.bairro]
    .filter(Boolean).join(', ');
  return [street, `${address.cidade} - ${address.uf}`].filter(Boolean).join(' · ');
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff' },
  content: { paddingHorizontal: 20, paddingVertical: 18, gap: 12 },
  sectionTitle: { color: '#1f1f1f', fontSize: 20, fontWeight: '700', marginTop: 10 },
  fieldGroup: { gap: 6 },
  label: { color: '#444', fontSize: 15, fontWeight: '600' },
  input: {
    borderWidth: 1,
    borderColor: '#ccc',
    borderRadius: 10,
    color: '#222',
    fontSize: 16,
    paddingHorizontal: 12,
    paddingVertical: 11,
  },
  option: { borderWidth: 1, borderColor: '#ddd', borderRadius: 10, gap: 5, padding: 12 },
  optionSelected: { borderColor: '#a43434', backgroundColor: '#fff8f8' },
  optionTitle: { color: '#222', fontSize: 16, fontWeight: '600' },
  muted: { color: '#666', lineHeight: 21 },
  error: { color: '#a43434' },
  emptyAddress: { gap: 14, paddingTop: 12 },
});
