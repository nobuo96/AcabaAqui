import { useCallback, useState } from 'react';
import { Alert, Pressable, StyleSheet, TextInput, View } from 'react-native';
import { useFocusEffect } from 'expo-router';

import { AcabaAquiHeader } from '@/components/AcabaAquiHeader';
import { ScrollAreaView } from '@/components/ScrollAreaView';
import { Text } from '@/components/Themed';
import { servicePayoutApi, type ServicePayout } from '@/lib/communicationApi';

export function AdminPayoutsScreen() {
  const [payouts, setPayouts] = useState<ServicePayout[]>([]);
  const [selected, setSelected] = useState<ServicePayout | null>(null);
  const [amount, setAmount] = useState('');
  const [transferReference, setTransferReference] = useState('');
  const [receiptReference, setReceiptReference] = useState('');
  const [loading, setLoading] = useState(true);
  const [savingId, setSavingId] = useState<number | null>(null);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setPayouts(await servicePayoutApi.list());
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : 'Não foi possível carregar os repasses.');
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(useCallback(() => {
    void load();
  }, [load]));

  async function toggleDetails(payoutId: number) {
    if (selected?.id === payoutId) {
      setSelected(null);
      return;
    }
    setError('');
    try {
      const detail = await servicePayoutApi.get(payoutId);
      setSelected(detail);
      setAmount(detail.paymentAmount === null ? '' : String(detail.paymentAmount));
      setTransferReference('');
      setReceiptReference('');
    } catch (detailError) {
      setError(detailError instanceof Error ? detailError.message : 'Não foi possível carregar o repasse.');
    }
  }

  function confirmPayment(payout: ServicePayout) {
    Alert.alert(
      'Registrar repasse Pix',
      'Faça a transferência pela conta da plataforma e confira a chave do destinatário antes de confirmar. Este registro não executa o Pix.',
      [
        { text: 'Voltar', style: 'cancel' },
        { text: 'Confirmar transferência', onPress: () => { void markPaid(payout); } },
      ],
    );
  }

  async function markPaid(payout: ServicePayout) {
    if (savingId !== null) return;
    const parsedAmount = Number(amount.replace(',', '.'));
    if (!Number.isFinite(parsedAmount) || parsedAmount <= 0 || !transferReference.trim()) {
      setError('Informe um valor válido e o identificador/comprovante da transferência.');
      return;
    }
    setSavingId(payout.id);
    setError('');
    try {
      const updated = await servicePayoutApi.markPaid(payout.id, {
        amount: parsedAmount,
        transferReference: transferReference.trim(),
        expectedPixKey: payout.pixKeyValue ?? '',
        ...(receiptReference.trim() ? { receiptReference: receiptReference.trim() } : {}),
      });
      setSelected(updated);
      await load();
    } catch (paymentError) {
      const message = paymentError instanceof Error
        ? paymentError.message
        : 'Não foi possível registrar o repasse.';
      await load();
      setError(message);
    } finally {
      setSavingId(null);
    }
  }

  return (
    <View style={styles.container}>
      <AcabaAquiHeader title="Repasses Pix" />
      <ScrollAreaView contentContainerStyle={styles.content}>
        <Text style={styles.intro}>
          Repasse manual pelo saldo da plataforma. A pendência é criada após aprovação ou ao fim dos 7 dias sem ação/disputa;
          o prazo operacional é de até 7 dias úteis.
        </Text>
        {loading ? <Text style={styles.muted}>Carregando repasses...</Text> : null}
        {error ? <Text accessibilityRole="alert" style={styles.error}>{error}</Text> : null}
        {!loading && payouts.length === 0
          ? <Text style={styles.muted}>Não há repasses registrados.</Text>
          : null}
        {payouts.map((payout) => (
          <View key={payout.id} style={styles.card}>
            <View style={styles.heading}>
              <Text style={styles.title}>{payout.serviceName}</Text>
              <Text style={[
                styles.badge,
                payout.status === 'PAGO' && styles.paidBadge,
                payout.status === 'CANCELADO' && styles.canceledBadge,
              ]}>
                {payout.status === 'PAGO' ? 'Pago' : payout.status === 'CANCELADO' ? 'Cancelado' : 'Pendente'}
              </Text>
            </View>
            <Text style={styles.line}>Prestador: {payout.providerName}</Text>
            <Text style={styles.line}>Valor pago pelo cliente: {formatCurrency(payout.paymentAmount)}</Text>
            <Text style={styles.line}>Elegível desde: {formatDate(payout.eligibleAt)}</Text>
            <Text style={styles.line}>Motivo: {reasonLabel(payout.reason)}</Text>
            <Pressable onPress={() => { void toggleDetails(payout.id); }} style={styles.detailButton}>
              <Text style={styles.detailButtonText}>
                {selected?.id === payout.id ? 'Ocultar detalhes' : 'Ver chave e detalhes'}
              </Text>
            </Pressable>
            {selected?.id === payout.id ? (
              <PayoutDetails
                payout={selected}
                amount={amount}
                transferReference={transferReference}
                receiptReference={receiptReference}
                saving={savingId === payout.id}
                onAmountChange={setAmount}
                onTransferReferenceChange={setTransferReference}
                onReceiptReferenceChange={setReceiptReference}
                onPay={() => confirmPayment(selected)}
              />
            ) : null}
          </View>
        ))}
      </ScrollAreaView>
    </View>
  );
}

function PayoutDetails({
  payout,
  amount,
  transferReference,
  receiptReference,
  saving,
  onAmountChange,
  onTransferReferenceChange,
  onReceiptReferenceChange,
  onPay,
}: {
  payout: ServicePayout;
  amount: string;
  transferReference: string;
  receiptReference: string;
  saving: boolean;
  onAmountChange: (value: string) => void;
  onTransferReferenceChange: (value: string) => void;
  onReceiptReferenceChange: (value: string) => void;
  onPay: () => void;
}) {
  return (
    <View style={styles.details}>
      {payout.pixKeyValue ? (
        <>
          <Text style={styles.label}>Chave Pix ({pixTypeLabel(payout.pixKeyType)}):</Text>
          <Text selectable style={styles.pixKey}>{payout.pixKeyValue}</Text>
        </>
      ) : (
        <Text style={styles.warning}>
          {payout.pixKeyMasked
            ? `Chave cadastrada (${payout.pixKeyMasked}), mas o valor não pode ser revelado. Verifique a configuração de criptografia.`
            : 'O prestador ainda não cadastrou uma chave Pix.'}
        </Text>
      )}
      {payout.disputeOpen ? (
        <Text style={styles.error}>Repasse bloqueado: há uma disputa aberta.</Text>
      ) : null}
      {payout.paymentStatus
        && payout.paymentStatus !== 'REPASSE_PENDENTE'
        && payout.paymentStatus !== 'REPASSADO' ? (
        <Text style={styles.error}>
          Revise o estado financeiro: {paymentStatusLabel(payout.paymentStatus)}.
        </Text>
      ) : null}
      {payout.status === 'PAGO' ? (
        <>
          <Text style={styles.line}>Transferido: {formatCurrency(payout.transferredAmount)}</Text>
          <Text style={styles.line}>Referência: {payout.transferReference}</Text>
          {payout.receiptReference ? <Text style={styles.line}>Comprovante: {payout.receiptReference}</Text> : null}
          {payout.paidAt ? <Text style={styles.line}>Registrado em: {formatDate(payout.paidAt)}</Text> : null}
          {payout.administratorName ? <Text style={styles.line}>Administrador: {payout.administratorName}</Text> : null}
        </>
      ) : payout.status === 'CANCELADO' ? (
        <Text style={styles.warning}>Este repasse foi cancelado após a confirmação do reembolso ao cliente.</Text>
      ) : (
        <>
          <Text style={styles.label}>Valor transferido (máximo {formatCurrency(payout.paymentAmount)}):</Text>
          <TextInput
            style={styles.input}
            value={amount}
            onChangeText={onAmountChange}
            keyboardType="decimal-pad"
            placeholder="0,00"
          />
          <Text style={styles.label}>ID da transação Pix (obrigatório):</Text>
          <TextInput
            style={styles.input}
            value={transferReference}
            onChangeText={onTransferReferenceChange}
            maxLength={500}
            placeholder="Identificador retornado pelo banco"
          />
          <Text style={styles.label}>Referência do comprovante (opcional):</Text>
          <TextInput
            style={styles.input}
            value={receiptReference}
            onChangeText={onReceiptReferenceChange}
            maxLength={1000}
            placeholder="Número ou localização do comprovante"
          />
          <Pressable
            disabled={!payout.canPay || saving || !payout.pixKeyValue}
            onPress={onPay}
            style={[styles.payButton, (!payout.canPay || saving || !payout.pixKeyValue) && styles.disabled]}
          >
            <Text style={styles.payButtonText}>{saving ? 'Registrando...' : 'Registrar como pago'}</Text>
          </Pressable>
        </>
      )}
    </View>
  );
}

function reasonLabel(reason: ServicePayout['reason']) {
  return reason === 'APROVACAO_CLIENTE' ? 'Aprovação do cliente' : 'Fim do prazo de 7 dias';
}

function pixTypeLabel(type: ServicePayout['pixKeyType']) {
  switch (type) {
    case 'CPF': return 'CPF';
    case 'CNPJ': return 'CNPJ';
    case 'EMAIL': return 'e-mail';
    case 'TELEFONE': return 'telefone';
    case 'ALEATORIA': return 'chave aleatória';
    default: return 'chave Pix';
  }
}

function paymentStatusLabel(status: NonNullable<ServicePayout['paymentStatus']>) {
  switch (status) {
    case 'REPASSE_PENDENTE': return 'repasse pendente';
    case 'REPASSADO': return 'repasse registrado';
    case 'CONTESTADO': return 'contestado';
    case 'REEMBOLSO_PENDENTE': return 'reembolso pendente';
    case 'REEMBOLSADO': return 'reembolsado';
    case 'REEMBOLSADO_PARCIAL': return 'reembolso parcial';
    default: return status.toLowerCase().replace(/_/g, ' ');
  }
}

function formatCurrency(value: number | null) {
  return value === null
    ? '—'
    : new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
}

function formatDate(value: string) {
  return new Date(value).toLocaleString('pt-BR');
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f6f6f6' },
  content: { padding: 16, paddingBottom: 32, gap: 12 },
  intro: { color: '#444', fontSize: 14, lineHeight: 20 },
  muted: { color: '#666', fontSize: 14 },
  error: { color: '#a43434', fontSize: 14, lineHeight: 20 },
  warning: { color: '#854c00', fontSize: 14, lineHeight: 20 },
  card: { backgroundColor: '#fff', borderRadius: 12, padding: 16, gap: 8 },
  heading: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 8 },
  title: { color: '#222', fontSize: 17, fontWeight: '700', flexShrink: 1 },
  badge: { color: '#854c00', backgroundColor: '#fff0d6', borderRadius: 12, paddingHorizontal: 9, paddingVertical: 4, fontSize: 12, fontWeight: '700' },
  paidBadge: { color: '#246b31', backgroundColor: '#e5f4e8' },
  canceledBadge: { color: '#555', backgroundColor: '#eee' },
  line: { color: '#4a4a4a', fontSize: 14 },
  detailButton: { alignSelf: 'flex-start', paddingVertical: 6 },
  detailButtonText: { color: '#a43434', fontWeight: '700' },
  details: { borderTopWidth: 1, borderTopColor: '#eee', paddingTop: 10, gap: 8 },
  label: { color: '#333', fontSize: 13, fontWeight: '700' },
  pixKey: { color: '#222', fontSize: 15, fontWeight: '600', paddingVertical: 4 },
  input: { borderWidth: 1, borderColor: '#ccc', borderRadius: 8, padding: 11, color: '#222', backgroundColor: '#fff' },
  payButton: { backgroundColor: '#a43434', borderRadius: 8, padding: 12, alignItems: 'center', marginTop: 4 },
  payButtonText: { color: '#fff', fontWeight: '700' },
  disabled: { opacity: 0.55 },
});
