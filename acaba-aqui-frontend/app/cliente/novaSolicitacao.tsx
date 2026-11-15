import { useEffect, useState } from 'react';
import { Pressable, StyleSheet, TextInput, View } from 'react-native';
import { useRouter } from 'expo-router';

import { AcabaAquiButton } from '@/components/AcabaAquiButton';
import { AcabaAquiHeader } from '@/components/AcabaAquiHeader';
import { ScrollAreaView } from '@/components/ScrollAreaView';
import { Text } from '@/components/Themed';
import { catalogApi, type CatalogService } from '@/lib/catalogApi';
import { getAuthUserId } from '@/lib/authToken';
import {
  serviceRequestApi,
  type RequestAddress,
  type VisitAvailabilityInput,
} from '@/lib/serviceRequestApi';
import { AcabaAquiDropdown } from '@/components/AcabaAquiDropdown';
import { AcabaAquiInput } from '@/components/AcabaAquiInput';

interface SavedAddress extends RequestAddress {
  principal: boolean;
}

export default function NovaSolicitacaoRoute() {
  const router = useRouter();
  const [userId, setUserId] = useState<string | null>(null);
  const [services, setServices] = useState<CatalogService[]>([]);
  const [addresses, setAddresses] = useState<SavedAddress[]>([]);
  const [selectedServiceId, setSelectedServiceId] = useState<number | null>(null);
  const [selectedAddressId, setSelectedAddressId] = useState<number | null>(null);
  const [area, setArea] = useState('');
  const [timeZoneId, setTimeZoneId] = useState('America/Sao_Paulo');
  const [visitDate, setVisitDate] = useState('');
  const [visitHour, setVisitHour] = useState('');
  const [visitSlots, setVisitSlots] = useState<VisitAvailabilityInput[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    const deviceTimeZone = Intl.DateTimeFormat().resolvedOptions().timeZone;
    if (brazilianTimeZones.some((zone) => zone.value === deviceTimeZone)) {
      setTimeZoneId(deviceTimeZone);
    }
  }, []);

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
    if (visitSlots.length === 0) {
      setError('Adicione pelo menos um horário disponível para a visita.');
      return;
    }

    setError('');
    setSaving(true);
    try {
      await serviceRequestApi.create(userId, {
        servicoId: selectedServiceId,
        enderecoId: selectedAddressId,
        areaM2: areaValue,
        horariosDisponiveis: visitSlots,
      });
      router.replace('/(cliente)/solicitacoes');
    } catch (saveError) {
      setError(saveError instanceof Error ? saveError.message : 'Não foi possível criar a solicitação.');
    } finally {
      setSaving(false);
    }
  }

  function addVisitSlot() {
    const dateParts = visitDate.split('-').map(Number);
    if (!/^\d{4}-\d{2}-\d{2}$/.test(visitDate)
      || dateParts.length !== 3
      || !isValidCalendarDate(dateParts[0], dateParts[1], dateParts[2])) {
      setError('Informe uma data válida no formato AAAA-MM-DD.');
      return;
    }
    if (!/^\d{1,2}$/.test(visitHour) || Number(visitHour) < 0 || Number(visitHour) > 23) {
      setError('Informe uma hora válida entre 0 e 23.');
      return;
    }
    const inicioLocal = `${visitDate}T${String(Number(visitHour)).padStart(2, '0')}:00:00`;
    if (visitSlots.some((slot) => slot.inicioLocal === inicioLocal)) {
      setError('Esse horário já foi adicionado.');
      return;
    }

    setVisitSlots((current) => [...current, { inicioLocal, timeZoneId }]
      .sort((first, second) => first.inicioLocal.localeCompare(second.inicioLocal)));
    setError('');
    setVisitHour('');
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
            : <AcabaAquiDropdown
              data={services.map((service) => ({ label: service.nome, value: service.id.toString() }))}
              value={selectedServiceId?.toString() ?? null}
              onChange={(value) => setSelectedServiceId(Number(value))}
            />
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
              onPress={() => {
                if (visitSlots.length > 0 && selectedAddressId !== address.id) {
                  setError('Remova os horários antes de trocar o endereço do serviço.');
                  return;
                }
                setSelectedAddressId(address.id);
              }}
              style={[styles.option, selectedAddressId === address.id && styles.optionSelected]}
            >
              <Text style={styles.optionTitle}>
                {address.apelido || 'Endereço'}{address.id === addresses.find((item) => item.principal)?.id
                  ? ' · Principal' : ''}
              </Text>
              <Text style={styles.muted}>{formatAddress(address)}</Text>
            </Pressable>
          ))}

          <Text style={styles.sectionTitle}>Horários disponíveis para a visita</Text>
          <Text style={styles.muted}>
            Escolha blocos de uma hora. O fuso deve corresponder ao endereço do serviço.
          </Text>
          <AcabaAquiDropdown
            data={brazilianTimeZones}
            value={timeZoneId}
            onChange={(value) => {
              if (visitSlots.length > 0 && value !== timeZoneId) {
                setError('Remova os horários antes de trocar o fuso.');
                return;
              }
              setTimeZoneId(value);
            }}
          />
          <View style={styles.timeInputs}>
            <View style={styles.timeInput}>
              <Text style={styles.label}>Data (AAAA-MM-DD)</Text>
              <AcabaAquiInput
                placeholder="2030-05-10"
                value={visitDate}
                onChangeText={setVisitDate}
              />
            </View>
            <View style={styles.hourInput}>
              <Text style={styles.label}>Hora (0-23)</Text>
              <AcabaAquiInput
                placeholder="9"
                value={visitHour}
                onChangeText={setVisitHour}
              />
            </View>
          </View>
          <Pressable onPress={addVisitSlot} style={styles.addSlotButton}>
            <Text style={styles.addSlotText}>Adicionar horário</Text>
          </Pressable>
          {visitSlots.map((slot) => (
            <View key={slot.inicioLocal} style={styles.slot}>
              <Text style={styles.slotText}>{formatLocalVisitTime(slot.inicioLocal)}</Text>
              <Pressable onPress={() => setVisitSlots((current) =>
                current.filter((item) => item.inicioLocal !== slot.inicioLocal))}>
                <Text style={styles.removeSlot}>Remover</Text>
              </Pressable>
            </View>
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

const brazilianTimeZones = [
  { label: 'Fernando de Noronha', value: 'America/Noronha' },
  { label: 'Belém', value: 'America/Belem' },
  { label: 'Fortaleza', value: 'America/Fortaleza' },
  { label: 'Recife', value: 'America/Recife' },
  { label: 'Araguaína', value: 'America/Araguaina' },
  { label: 'Maceió', value: 'America/Maceio' },
  { label: 'Salvador', value: 'America/Bahia' },
  { label: 'São Paulo', value: 'America/Sao_Paulo' },
  { label: 'Campo Grande', value: 'America/Campo_Grande' },
  { label: 'Cuiabá', value: 'America/Cuiaba' },
  { label: 'Santarém', value: 'America/Santarem' },
  { label: 'Porto Velho', value: 'America/Porto_Velho' },
  { label: 'Boa Vista', value: 'America/Boa_Vista' },
  { label: 'Manaus', value: 'America/Manaus' },
  { label: 'Eirunepé', value: 'America/Eirunepe' },
  { label: 'Rio Branco', value: 'America/Rio_Branco' },
];

function isValidCalendarDate(year: number, month: number, day: number) {
  const date = new Date(Date.UTC(year, month - 1, day));
  return date.getUTCFullYear() === year
    && date.getUTCMonth() === month - 1
    && date.getUTCDate() === day;
}

function formatLocalVisitTime(value: string) {
  const [date, time] = value.split('T');
  return `${date} às ${time.slice(0, 5)}`;
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
  option: { borderWidth: 1, borderColor: '#ddd', borderRadius: 10, gap: 5, padding: 12 },
  optionSelected: { borderColor: '#a43434', backgroundColor: '#fff8f8' },
  optionTitle: { color: '#222', fontSize: 16, fontWeight: '600' },
  muted: { color: '#666', lineHeight: 21 },
  error: { color: '#a43434' },
  emptyAddress: { gap: 14, paddingTop: 12 },
  timeInputs: { flexDirection: 'row', gap: 10 },
  timeInput: { flex: 2, gap: 6 },
  hourInput: { flex: 1, gap: 6 },
  addSlotButton: { alignItems: 'center', borderColor: '#a43434', borderRadius: 9,
    borderWidth: 1, padding: 10 },
  addSlotText: { color: '#a43434', fontWeight: '700' },
  slot: { alignItems: 'center', borderColor: '#e1e1e1', borderRadius: 9,
    borderWidth: 1, flexDirection: 'row', justifyContent: 'space-between', padding: 11 },
  slotText: { color: '#333' },
  removeSlot: { color: '#a43434', fontWeight: '600' },
});
