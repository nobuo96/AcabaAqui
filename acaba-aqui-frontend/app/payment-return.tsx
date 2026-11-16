import { useCallback, useEffect, useRef, useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { useFocusEffect, useLocalSearchParams, useRouter } from 'expo-router';

import { AcabaAquiButton } from '@/components/AcabaAquiButton';
import { AcabaAquiHeader } from '@/components/AcabaAquiHeader';
import { ScrollAreaView } from '@/components/ScrollAreaView';
import { Text } from '@/components/Themed';
import { servicePaymentApi, type PaymentLookup } from '@/lib/communicationApi';

export default function PaymentReturnScreen() {
  const router = useRouter();
  const params = useLocalSearchParams<{ quoteId?: string | string[] }>();
  const quoteIdParam = Array.isArray(params.quoteId) ? params.quoteId[0] : params.quoteId;
  const quoteId = Number(quoteIdParam);
  const [lookup, setLookup] = useState<PaymentLookup | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const refreshing = useRef(false);

  const refresh = useCallback(async () => {
    if (refreshing.current) return;
    if (!Number.isSafeInteger(quoteId) || quoteId <= 0) {
      setError('Não foi possível identificar o orçamento deste pagamento.');
      setLookup(null);
      return;
    }
    refreshing.current = true;
    setLoading(true);
    setError('');
    try {
      const current = await servicePaymentApi.getForQuote(quoteId);
      setLookup(current);
      if (current.payment?.status === 'AGUARDANDO_PAGAMENTO'
          || current.payment?.status === 'REEMBOLSO_PENDENTE') {
        const payment = await servicePaymentApi.sync(current.payment.id);
        setLookup({ exists: true, payment });
      } else {
        setLookup(current);
      }
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : 'Não foi possível consultar o pagamento.');
    } finally {
      setLoading(false);
      refreshing.current = false;
    }
  }, [quoteId]);

  useFocusEffect(useCallback(() => {
    void refresh();
  }, [refresh]));

  const status = lookup?.payment?.status;
  useEffect(() => {
    if (status !== 'AGUARDANDO_PAGAMENTO') return undefined;
    let attempts = 0;
    const interval = setInterval(() => {
      attempts += 1;
      if (attempts >= 12) {
        clearInterval(interval);
        return;
      }
      void refresh();
    }, 5000);
    return () => clearInterval(interval);
  }, [status, refresh]);

  return (
    <View style={styles.container}>
      <AcabaAquiHeader title="Status do pagamento" />
      <ScrollAreaView contentContainerStyle={styles.content}>
        {loading ? <Text style={styles.muted}>Consultando o status...</Text> : null}
        {error ? <Text style={styles.error}>{error}</Text> : null}
        {!loading && !error && !lookup?.exists ? (
          <Text style={styles.muted}>Ainda não há um pagamento registrado para este orçamento.</Text>
        ) : null}
        {status ? (
          <>
            <Text style={styles.status}>{paymentStatusLabel(status)}</Text>
            <Text style={styles.muted}>
              Valor: {new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })
                .format(lookup.payment?.amount ?? 0)}
            </Text>
            {status === 'AGUARDANDO_PAGAMENTO' ? (
              <Text style={styles.muted}>
                O Mercado Pago ainda não confirmou o pagamento. Atualize em instantes; não use o retorno
                do checkout como confirmação.
              </Text>
            ) : null}
            {status === 'PAGO' ? (
              <Text style={styles.muted}>Pagamento confirmado pelo servidor.</Text>
            ) : null}
          </>
        ) : null}
        <AcabaAquiButton text="Atualizar status" onPress={() => { void refresh(); }} />
        <AcabaAquiButton
          text="Voltar para solicitações"
          onPress={() => router.replace('/(cliente)/solicitacoes')}
        />
      </ScrollAreaView>
    </View>
  );
}

function paymentStatusLabel(status: NonNullable<PaymentLookup['payment']>['status']) {
  return {
    AGUARDANDO_PAGAMENTO: 'Aguardando pagamento',
    PAGO: 'Pagamento confirmado',
    RECUSADO: 'Pagamento não aprovado',
    EXPIRADO: 'Pagamento expirado',
    CANCELADO: 'Pagamento cancelado',
    REEMBOLSO_PENDENTE: 'Reembolso em processamento',
    REEMBOLSADO: 'Pagamento reembolsado',
    REEMBOLSADO_PARCIAL: 'Pagamento reembolsado parcialmente',
    CONTESTADO: 'Pagamento em contestação',
  }[status];
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff' },
  content: { gap: 14, paddingHorizontal: 20, paddingVertical: 18 },
  status: { color: '#333', fontSize: 20, fontWeight: '700' },
  muted: { color: '#666', lineHeight: 21 },
  error: { color: '#a43434' },
});
