import { useEffect, useState } from 'react';
import { Pressable, StyleSheet, View } from 'react-native';
import { useRouter } from 'expo-router';

import { AcabaAquiButton } from '@/components/AcabaAquiButton';
import { AcabaAquiDropdown } from '@/components/AcabaAquiDropdown';
import { AcabaAquiHeader } from '@/components/AcabaAquiHeader';
import { AcabaAquiInput } from '@/components/AcabaAquiInput';
import { ScrollAreaView } from '@/components/ScrollAreaView';
import { Text } from '@/components/Themed';
import { getAuthUserId } from '@/lib/authToken';
import { catalogApi, type CatalogService } from '@/lib/catalogApi';
import {
  serviceRequestApi,
  type RequestAddress,
  type VisitAvailabilityInput,
} from '@/lib/serviceRequestApi';

interface SavedAddress extends RequestAddress {
  principal: boolean;
}

const steps = ['Serviço', 'Detalhes', 'Disponibilidade', 'Revisão'];

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
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [step, setStep] = useState(0);

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
        if (!id) throw new Error('Sessão não encontrada. Entre novamente.');
        const [availableServices, savedAddresses] = await Promise.all([
          catalogApi.listServices(),
          serviceRequestApi.listAddresses(id),
        ]);
        if (active) {
          setUserId(id);
          setServices(availableServices);
          setAddresses(savedAddresses);
          setSelectedAddressId(savedAddresses.find((address) => address.principal)?.id
            ?? savedAddresses[0]?.id ?? null);
        }
      } catch (loadError) {
        if (active) {
          setError(loadError instanceof Error ? loadError.message : 'Não foi possível carregar os dados.');
        }
      } finally {
        if (active) setLoading(false);
      }
    }
    void load();
    return () => { active = false; };
  }, []);

  function validateCurrentStep() {
    const errors: Record<string, string> = {};
    if (step === 0 && !selectedServiceId) {
      errors.service = 'Escolha um serviço para continuar.';
    }
    if (step === 1) {
      const areaValue = Number(area.trim().replace(',', '.'));
      if (!selectedAddressId) errors.address = 'Selecione o endereço do serviço.';
      if (!Number.isFinite(areaValue) || areaValue <= 0) {
        errors.area = 'Informe uma área maior que zero.';
      }
    }
    if (step === 2 && visitSlots.length === 0) {
      errors.slots = 'Adicione pelo menos um horário disponível.';
    }
    setFieldErrors(errors);
    setError('');
    return Object.keys(errors).length === 0;
  }

  function goNext() {
    if (validateCurrentStep()) setStep((current) => Math.min(current + 1, 3));
  }

  function addVisitSlot() {
    const dateMatch = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(visitDate);
    if (!dateMatch) {
      setFieldErrors((current) => ({ ...current, slotDate: 'Informe uma data válida.' }));
      return;
    }
    const [, dayText, monthText, yearText] = dateMatch;
    const day = Number(dayText);
    const month = Number(monthText);
    const year = Number(yearText);
    if (!isValidCalendarDate(year, month, day)) {
      setFieldErrors((current) => ({ ...current, slotDate: 'Confira o dia, mês e ano informados.' }));
      return;
    }
    const dateForApi = `${yearText}-${monthText}-${dayText}`;
    if (dateForApi < getDateInTimeZone(timeZoneId)) {
      setFieldErrors((current) => ({ ...current, slotDate: 'Não use uma data passada.' }));
      return;
    }
    if (!/^\d{1,2}$/.test(visitHour) || Number(visitHour) < 0 || Number(visitHour) > 23) {
      setFieldErrors((current) => ({ ...current, slotHour: 'Informe uma hora entre 0 e 23.' }));
      return;
    }
    const hour = Number(visitHour);
    if (dateForApi === getDateInTimeZone(timeZoneId) && hour <= getHourInTimeZone(timeZoneId)) {
      setFieldErrors((current) => ({ ...current, slotDate: 'O horário precisa ser futuro.' }));
      return;
    }
    const inicioLocal = `${dateForApi}T${String(hour).padStart(2, '0')}:00:00`;
    if (visitSlots.some((slot) => slot.inicioLocal === inicioLocal)) {
      setFieldErrors((current) => ({ ...current, slots: 'Esse horário já foi adicionado.' }));
      return;
    }
    setVisitSlots((current) => [...current, { inicioLocal, timeZoneId }]
      .sort((first, second) => first.inicioLocal.localeCompare(second.inicioLocal)));
    setFieldErrors({});
    setVisitHour('');
  }

  async function submit() {
    if (!userId || saving) return;
    const areaValue = Number(area.trim().replace(',', '.'));
    setSaving(true);
    setError('');
    try {
      await serviceRequestApi.create(userId, {
        servicoId: selectedServiceId as number,
        enderecoId: selectedAddressId as number,
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

  const selectedService = services.find((item) => item.id === selectedServiceId);
  const selectedAddress = addresses.find((item) => item.id === selectedAddressId);

  return (
    <View style={styles.container}>
      <AcabaAquiHeader title="Nova solicitação" backButton onBackPress={() => router.back()} />
      <ScrollAreaView contentContainerStyle={styles.content}>
        <View style={styles.stepper} accessibilityRole="header">
          {steps.map((label, index) => (
            <Pressable key={label} disabled={index > step} onPress={() => setStep(index)}
              accessibilityRole="button" style={styles.stepItem}>
              <View style={[styles.stepCircle, index <= step && styles.stepCircleActive]}>
                <Text style={[styles.stepNumber, index <= step && styles.stepNumberActive]}>{index + 1}</Text>
              </View>
              <Text style={[styles.stepLabel, index === step && styles.stepLabelActive]}>{label}</Text>
            </Pressable>
          ))}
        </View>

        {loading ? <Text style={styles.muted}>Carregando serviços e endereços...</Text> : null}
        {error ? <Text accessibilityRole="alert" style={styles.error}>{error}</Text> : null}
        {addresses.length === 0 && !loading ? (
          <View style={styles.emptyAddress}>
            <Text style={styles.sectionTitle}>Cadastre um endereço para continuar</Text>
            <Text style={styles.muted}>É necessário ter pelo menos um endereço cadastrado.</Text>
            <AcabaAquiButton text="Cadastrar endereço" onPress={() => router.push('/enderecos')} />
          </View>
        ) : null}

        {!loading && addresses.length > 0 && step === 0 ? (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>Qual serviço você precisa?</Text>
            {services.length === 0
              ? <Text style={styles.muted}>Não há serviços disponíveis no momento.</Text>
              : <AcabaAquiDropdown
                  label="Tipo de serviço"
                  placeholder="Escolha um serviço"
                  data={services.map((item) => ({ label: item.nome, value: item.id.toString() }))}
                  value={selectedServiceId?.toString() ?? null}
                  onChange={(value) => setSelectedServiceId(Number(value))}
                  error={fieldErrors.service}
                />}
          </View>
        ) : null}

        {!loading && addresses.length > 0 && step === 1 ? (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>Área e endereço</Text>
            <AcabaAquiInput label="Área aproximada em m²" placeholder="Ex.: 45,5"
              keyboardType="decimal-pad" value={area} onChangeText={setArea}
              error={fieldErrors.area} />
            <Text style={styles.label}>Endereço do serviço</Text>
            {addresses.map((address) => (
              <Pressable key={address.id} accessibilityRole="radio"
                accessibilityState={{ selected: selectedAddressId === address.id }}
                onPress={() => setSelectedAddressId(address.id)}
                style={[styles.option, selectedAddressId === address.id && styles.optionSelected]}>
                <Text style={styles.optionTitle}>{address.apelido || 'Endereço'}
                  {address.principal ? ' · Principal' : ''}</Text>
                <Text style={styles.muted}>{formatAddress(address)}</Text>
              </Pressable>
            ))}
            {fieldErrors.address ? <Text style={styles.error}>{fieldErrors.address}</Text> : null}
          </View>
        ) : null}

        {!loading && addresses.length > 0 && step === 2 ? (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>Horários para a visita</Text>
            <Text style={styles.muted}>Adicione blocos de uma hora no horário local do endereço.</Text>
            <AcabaAquiDropdown
              label="Fuso horário"
              placeholder="Escolha o fuso"
              data={brazilianTimeZones}
              value={timeZoneId}
              onChange={setTimeZoneId}
            />
            <View style={styles.timeInputs}>
              <View style={styles.timeInput}>
                <AcabaAquiInput label="Data (DD/MM/AAAA)" placeholder="DD/MM/AAAA"
                  keyboardType="numeric" value={visitDate}
                  onChangeText={(value) => setVisitDate(formatDateInput(value))}
                  error={fieldErrors.slotDate} />
              </View>
              <View style={styles.hourInput}>
                <AcabaAquiInput label="Hora" placeholder="9" keyboardType="numeric"
                  value={visitHour} onChangeText={setVisitHour} error={fieldErrors.slotHour} />
              </View>
            </View>
            <AcabaAquiButton text="Adicionar horário" variant="secondary" onPress={addVisitSlot} />
            {fieldErrors.slots ? <Text style={styles.error}>{fieldErrors.slots}</Text> : null}
            {visitSlots.map((slot) => (
              <View key={slot.inicioLocal} style={styles.slot}>
                <Text style={styles.slotText}>{formatLocalVisitTime(slot.inicioLocal)}
                  {' · '}{brazilianTimeZones.find((zone) => zone.value === slot.timeZoneId)?.label ?? slot.timeZoneId}</Text>
                <Pressable accessibilityRole="button" onPress={() =>
                  setVisitSlots((current) => current.filter((item) => item.inicioLocal !== slot.inicioLocal))}>
                  <Text style={styles.removeSlot}>Remover</Text>
                </Pressable>
              </View>
            ))}
          </View>
        ) : null}

        {!loading && addresses.length > 0 && step === 3 ? (
          <View style={styles.section}>
            <Text style={styles.sectionTitle}>Revise antes de enviar</Text>
            <View style={styles.reviewCard}>
              <Text style={styles.reviewLabel}>Serviço</Text>
              <Text style={styles.reviewValue}>{selectedService?.nome ?? 'Não selecionado'}</Text>
              <AcabaAquiButton text="Editar serviço" variant="secondary" onPress={() => setStep(0)} />
              <Text style={styles.reviewLabel}>Área</Text>
              <Text style={styles.reviewValue}>{area || '—'} m²</Text>
              <Text style={styles.reviewLabel}>Endereço</Text>
              <Text style={styles.reviewValue}>{selectedAddress ? formatAddress(selectedAddress) : '—'}</Text>
              <AcabaAquiButton text="Editar área e endereço" variant="secondary" onPress={() => setStep(1)} />
              <Text style={styles.reviewLabel}>Horários</Text>
              {visitSlots.map((slot) => (
                <Text key={slot.inicioLocal} style={styles.reviewValue}>{formatLocalVisitTime(slot.inicioLocal)}</Text>
              ))}
              <AcabaAquiButton text="Editar horários" variant="secondary" onPress={() => setStep(2)} />
            </View>
          </View>
        ) : null}

        {!loading && addresses.length > 0 ? (
          <View style={styles.navigation}>
            {step > 0
              ? <AcabaAquiButton text="Voltar" variant="secondary" onPress={() => setStep((current) => current - 1)} />
              : null}
            {step < 3
              ? <AcabaAquiButton text="Continuar" onPress={goNext} />
              : <AcabaAquiButton text={saving ? 'Enviando...' : 'Enviar solicitação'}
                  loading={saving} onPress={() => { void submit(); }} />}
          </View>
        ) : null}
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

function formatDateInput(value: string) {
  const digits = value.replace(/\D/g, '').slice(0, 8);
  if (digits.length <= 2) return digits;
  if (digits.length <= 4) return `${digits.slice(0, 2)}/${digits.slice(2)}`;
  return `${digits.slice(0, 2)}/${digits.slice(2, 4)}/${digits.slice(4)}`;
}

function getDateInTimeZone(timeZoneId: string) {
  const parts = new Intl.DateTimeFormat('en', {
    timeZone: timeZoneId,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(new Date());
  const values = Object.fromEntries(parts.map((part) => [part.type, part.value]));
  return `${values.year}-${values.month}-${values.day}`;
}

function getHourInTimeZone(timeZoneId: string) {
  const hour = new Intl.DateTimeFormat('en', {
    timeZone: timeZoneId,
    hour: '2-digit',
    hourCycle: 'h23',
  }).formatToParts(new Date()).find((part) => part.type === 'hour')?.value;
  return Number(hour);
}

function formatLocalVisitTime(value: string) {
  const [date, time] = value.split('T');
  const [year, month, day] = date.split('-');
  return `${day}/${month}/${year} às ${time.slice(0, 5)}`;
}

function formatAddress(address: RequestAddress) {
  const street = [address.logradouro, address.numero, address.complemento, address.bairro]
    .filter(Boolean).join(', ');
  return [address.apelido, street, `${address.cidade} - ${address.uf}`].filter(Boolean).join(' · ');
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff' },
  content: { paddingHorizontal: 20, paddingVertical: 18, gap: 14 },
  stepper: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 4 },
  stepItem: { alignItems: 'center', flex: 1, gap: 5 },
  stepCircle: { alignItems: 'center', backgroundColor: '#eee', borderRadius: 15,
    height: 30, justifyContent: 'center', width: 30 },
  stepCircleActive: { backgroundColor: '#a43434' },
  stepNumber: { color: '#666', fontWeight: '700' },
  stepNumberActive: { color: '#fff' },
  stepLabel: { color: '#777', fontSize: 11, textAlign: 'center' },
  stepLabelActive: { color: '#333', fontWeight: '700' },
  section: { gap: 12 },
  sectionTitle: { color: '#222', fontSize: 21, fontWeight: '800' },
  label: { color: '#444', fontSize: 15, fontWeight: '600' },
  muted: { color: '#666', lineHeight: 21 },
  error: { color: '#a43434', lineHeight: 20 },
  emptyAddress: { gap: 12 },
  option: { borderWidth: 1, borderColor: '#ddd', borderRadius: 10, gap: 5, padding: 12 },
  optionSelected: { borderColor: '#a43434', backgroundColor: '#fff8f8' },
  optionTitle: { color: '#222', fontSize: 16, fontWeight: '700' },
  timeInputs: { flexDirection: 'row', gap: 10 },
  timeInput: { flex: 2 },
  hourInput: { flex: 1 },
  slot: { alignItems: 'center', borderColor: '#e1e1e1', borderRadius: 9,
    borderWidth: 1, flexDirection: 'row', justifyContent: 'space-between', padding: 11 },
  slotText: { color: '#333', flex: 1 },
  removeSlot: { color: '#a43434', fontWeight: '700' },
  reviewCard: { backgroundColor: '#fafafa', borderColor: '#e1e1e1', borderRadius: 12,
    borderWidth: 1, gap: 8, padding: 14 },
  reviewLabel: { color: '#777', fontSize: 13, marginTop: 4 },
  reviewValue: { color: '#222', fontSize: 16, fontWeight: '600' },
  navigation: { flexDirection: 'row', flexWrap: 'wrap', gap: 10, marginTop: 4 },
});
