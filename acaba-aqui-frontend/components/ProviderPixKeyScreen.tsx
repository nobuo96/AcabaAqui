import { useCallback, useState } from 'react';
import { Pressable, StyleSheet, TextInput, View } from 'react-native';
import { useFocusEffect } from 'expo-router';

import { AcabaAquiHeader } from '@/components/AcabaAquiHeader';
import { ScrollAreaView } from '@/components/ScrollAreaView';
import { Text } from '@/components/Themed';
import { providerPixKeyApi, type PixKeyType, type ProviderPixKey } from '@/lib/communicationApi';

const keyTypes: { value: PixKeyType; label: string }[] = [
  { value: 'CPF', label: 'CPF' },
  { value: 'CNPJ', label: 'CNPJ' },
  { value: 'EMAIL', label: 'E-mail' },
  { value: 'TELEFONE', label: 'Telefone' },
  { value: 'ALEATORIA', label: 'Aleatória' },
];

interface ProviderPixKeyScreenProps {
  onBack: () => void;
}

export function ProviderPixKeyScreen({ onBack }: ProviderPixKeyScreenProps) {
  const [pixKey, setPixKey] = useState<ProviderPixKey | null>(null);
  const [type, setType] = useState<PixKeyType>('CPF');
  const [value, setValue] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const current = await providerPixKeyApi.get();
      setPixKey(current);
      if (current.type) setType(current.type);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : 'Não foi possível carregar a chave Pix.');
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(useCallback(() => {
    void load();
  }, [load]));

  async function save() {
    if (saving) return;
    setSaving(true);
    setError('');
    setMessage('');
    try {
      const updated = await providerPixKeyApi.update(type, value.trim());
      setPixKey(updated);
      setValue('');
      setMessage('Chave Pix atualizada.');
    } catch (saveError) {
      setError(saveError instanceof Error ? saveError.message : 'Não foi possível salvar a chave Pix.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <View style={styles.container}>
      <AcabaAquiHeader title="Chave Pix" backButton onBackPress={onBack} />
      <ScrollAreaView contentContainerStyle={styles.content}>
        <Text style={styles.info}>
          Cadastre a chave que a equipe da plataforma usará para o repasse manual. A chave fica criptografada
          no backend e aparece mascarada nesta tela.
        </Text>
        {loading ? <Text style={styles.muted}>Carregando chave...</Text> : null}
        {!loading && pixKey?.hasKey ? (
          <View style={styles.savedKey}>
            <Text style={styles.savedTitle}>Chave cadastrada</Text>
            <Text style={styles.savedValue}>
              {keyTypes.find((item) => item.value === pixKey.type)?.label}: {pixKey.maskedValue}
            </Text>
          </View>
        ) : null}
        <Text style={styles.label}>Tipo de chave</Text>
        <View style={styles.typeChoices}>
          {keyTypes.map((item) => (
            <Pressable
              key={item.value}
              onPress={() => setType(item.value)}
              style={[styles.typeChoice, type === item.value && styles.typeChoiceSelected]}
            >
              <Text style={[styles.typeText, type === item.value && styles.typeTextSelected]}>
                {item.label}
              </Text>
            </Pressable>
          ))}
        </View>
        <Text style={styles.label}>{type === 'ALEATORIA' ? 'Chave aleatória (UUID)' : keyTypes.find((item) => item.value === type)?.label}</Text>
        <TextInput
          style={styles.input}
          value={value}
          onChangeText={setValue}
          autoCapitalize="none"
          autoCorrect={false}
          keyboardType={type === 'EMAIL' ? 'email-address' : type === 'TELEFONE' ? 'phone-pad' : 'default'}
          placeholder={type === 'EMAIL' ? 'nome@exemplo.com' : 'Digite a chave Pix'}
          maxLength={255}
          secureTextEntry={type === 'CPF' || type === 'CNPJ'}
        />
        {error ? <Text accessibilityRole="alert" style={styles.error}>{error}</Text> : null}
        {message ? <Text style={styles.success}>{message}</Text> : null}
        <Pressable
          disabled={loading || saving || !value.trim()}
          onPress={() => { void save(); }}
          style={[styles.saveButton, (loading || saving || !value.trim()) && styles.disabled]}
        >
          <Text style={styles.saveButtonText}>{saving ? 'Salvando...' : pixKey?.hasKey ? 'Atualizar chave' : 'Salvar chave'}</Text>
        </Pressable>
        <Text style={styles.muted}>
          A alteração passa a valer para repasses ainda pendentes. Repasses registrados como pagos mantêm os dados usados na operação.
        </Text>
      </ScrollAreaView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f5f5f5' },
  content: { padding: 18, paddingBottom: 32, gap: 12 },
  info: { color: '#444', fontSize: 15, lineHeight: 22 },
  muted: { color: '#666', fontSize: 13, lineHeight: 19 },
  savedKey: { backgroundColor: '#fff', borderRadius: 10, padding: 14, gap: 5 },
  savedTitle: { color: '#333', fontWeight: '700' },
  savedValue: { color: '#555', fontSize: 15 },
  label: { color: '#333', fontWeight: '600', marginTop: 6 },
  typeChoices: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  typeChoice: { borderWidth: 1, borderColor: '#ccc', borderRadius: 18, paddingHorizontal: 12, paddingVertical: 8 },
  typeChoiceSelected: { backgroundColor: '#a43434', borderColor: '#a43434' },
  typeText: { color: '#444', fontSize: 13 },
  typeTextSelected: { color: '#fff', fontWeight: '700' },
  input: { borderWidth: 1, borderColor: '#ccc', backgroundColor: '#fff', borderRadius: 9, padding: 12, color: '#222' },
  error: { color: '#a43434', fontSize: 14 },
  success: { color: '#246b31', fontSize: 14 },
  saveButton: { backgroundColor: '#a43434', borderRadius: 9, padding: 13, alignItems: 'center' },
  saveButtonText: { color: '#fff', fontWeight: '700' },
  disabled: { opacity: 0.55 },
});
