import { useCallback, useEffect, useState } from 'react';
import { Pressable, StyleSheet, Switch, TextInput, View } from 'react-native';

import { AcabaAquiButton } from '@/components/AcabaAquiButton';
import { AcabaAquiHeader } from '@/components/AcabaAquiHeader';
import { ScrollAreaView } from '@/components/ScrollAreaView';
import { Text } from '@/components/Themed';
import { apiFetch } from '@/lib/api';
import { getAuthUserId } from '@/lib/authToken';

interface Address {
  id: number;
  cep: string;
  logradouro: string | null;
  numero: string;
  complemento: string | null;
  bairro: string | null;
  cidade: string;
  uf: string;
  apelido: string | null;
  principal: boolean;
}

interface AddressForm {
  cep: string;
  logradouro: string;
  numero: string;
  complemento: string;
  bairro: string;
  cidade: string;
  uf: string;
  apelido: string;
  principal: boolean;
}

interface AddressManagementProps {
  onBack: () => void;
}

const emptyForm: AddressForm = {
  cep: '',
  logradouro: '',
  numero: '',
  complemento: '',
  bairro: '',
  cidade: '',
  uf: '',
  apelido: '',
  principal: false,
};

function formatCep(value: string) {
  const digits = value.replace(/\D/g, '').slice(0, 8);
  return digits.length > 5 ? `${digits.slice(0, 5)}-${digits.slice(5)}` : digits;
}

function addressPath(userId: string) {
  return `/user/${encodeURIComponent(userId)}/addresses`;
}

export function AddressManagement({ onBack }: AddressManagementProps) {
  const [userId, setUserId] = useState<string | null>(null);
  const [addresses, setAddresses] = useState<Address[]>([]);
  const [form, setForm] = useState<AddressForm>(emptyForm);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [confirmingId, setConfirmingId] = useState<number | null>(null);
  const [editing, setEditing] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [consultingCep, setConsultingCep] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  const loadAddresses = useCallback(async (id: string) => {
    const response = await apiFetch(addressPath(id));
    if (!response.ok) {
      const body = await response.json().catch(() => null);
      throw new Error(body?.message ?? `Não foi possível carregar os endereços (${response.status}).`);
    }
    setAddresses(await response.json());
  }, []);

  useEffect(() => {
    let active = true;
    async function load() {
      try {
        const id = await getAuthUserId();
        if (!id) {
          throw new Error('Sessão não encontrada. Entre novamente.');
        }
        if (active) {
          setUserId(id);
          await loadAddresses(id);
        }
      } catch (loadError) {
        if (active) {
          setError(loadError instanceof Error ? loadError.message : 'Não foi possível carregar os endereços.');
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
  }, [loadAddresses]);

  function setField<K extends keyof AddressForm>(key: K, value: AddressForm[K]) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  function startCreate() {
    setForm(emptyForm);
    setEditingId(null);
    setEditing(true);
    setError('');
    setMessage('');
  }

  function startEdit(address: Address) {
    setForm({
      cep: formatCep(address.cep),
      logradouro: address.logradouro ?? '',
      numero: address.numero,
      complemento: address.complemento ?? '',
      bairro: address.bairro ?? '',
      cidade: address.cidade,
      uf: address.uf,
      apelido: address.apelido ?? '',
      principal: address.principal,
    });
    setEditingId(address.id);
    setEditing(true);
    setError('');
    setMessage('');
  }

  async function consultarCep() {
    const cep = form.cep.replace(/\D/g, '');
    if (cep.length !== 8) {
      setError('Informe um CEP válido com 8 dígitos.');
      return;
    }
    setError('');
    setMessage('');
    setConsultingCep(true);
    try {
      const response = await apiFetch(`/addresses/cep/${encodeURIComponent(cep)}`);
      const body = await response.json().catch(() => null);
      if (!response.ok) {
        throw new Error(body?.message ?? `Não foi possível consultar o CEP (${response.status}).`);
      }
      setForm((current) => ({
        ...current,
        cep: formatCep(body.cep ?? cep),
        logradouro: body.logradouro ?? '',
        complemento: body.complemento ?? '',
        bairro: body.bairro ?? '',
        cidade: body.cidade ?? '',
        uf: body.uf ?? '',
      }));
      setMessage('Endereço preenchido pelo CEP. Confira os dados e complete o número.');
    } catch (lookupError) {
      setError(lookupError instanceof Error
        ? `${lookupError.message} Você pode preencher o endereço manualmente.`
        : 'Não foi possível consultar o CEP. Preencha o endereço manualmente.');
    } finally {
      setConsultingCep(false);
    }
  }

  async function save() {
    if (!userId || saving) {
      return;
    }
    setError('');
    setMessage('');
    if (form.cep.replace(/\D/g, '').length !== 8) {
      setError('Informe um CEP válido com 8 dígitos.');
      return;
    }
    if (!form.numero.trim() || !form.cidade.trim() || !/^[a-z]{2}$/i.test(form.uf.trim())) {
      setError('Informe número (ou s/n), cidade e UF válida.');
      return;
    }

    setSaving(true);
    try {
      const path = `${addressPath(userId)}${editingId ? `/${editingId}` : ''}`;
      const response = await apiFetch(path, {
        method: editingId ? 'PUT' : 'POST',
        body: JSON.stringify({ ...form, cep: form.cep.replace(/\D/g, ''), uf: form.uf.trim().toUpperCase() }),
      });
      const body = await response.json().catch(() => null);
      if (!response.ok) {
        throw new Error(body?.message ?? `Não foi possível salvar o endereço (${response.status}).`);
      }
      await loadAddresses(userId);
      setEditing(false);
      setEditingId(null);
      setForm(emptyForm);
      setMessage('Endereço salvo.');
    } catch (saveError) {
      setError(saveError instanceof Error ? saveError.message : 'Não foi possível salvar o endereço.');
    } finally {
      setSaving(false);
    }
  }

  async function definirPrincipal(address: Address) {
    if (!userId) {
      return;
    }
    setError('');
    try {
      const response = await apiFetch(`${addressPath(userId)}/${address.id}/principal`, { method: 'PUT' });
      if (!response.ok) {
        throw new Error(`Não foi possível definir o endereço principal (${response.status}).`);
      }
      await loadAddresses(userId);
    } catch (principalError) {
      setError(principalError instanceof Error ? principalError.message : 'Não foi possível atualizar o endereço.');
    }
  }

  async function desativar(address: Address) {
    if (!userId) {
      return;
    }
    setError('');
    try {
      const response = await apiFetch(`${addressPath(userId)}/${address.id}`, { method: 'DELETE' });
      if (!response.ok) {
        throw new Error(`Não foi possível remover o endereço (${response.status}).`);
      }
      await loadAddresses(userId);
      setConfirmingId(null);
    } catch (deactivateError) {
      setError(deactivateError instanceof Error ? deactivateError.message : 'Não foi possível remover o endereço.');
    }
  }

  return (
    <View style={styles.container}>
      <AcabaAquiHeader title="Meus endereços" backButton onBackPress={onBack} />
      <ScrollAreaView contentContainerStyle={styles.inner}>
        {loading ? <Text style={styles.status}>Carregando endereços...</Text> : null}
        {error ? <Text style={styles.error}>{error}</Text> : null}
        {message ? <Text style={styles.success}>{message}</Text> : null}

        {!loading && !editing ? <>
          {addresses.length === 0
            ? <Text style={styles.status}>Você ainda não cadastrou endereços.</Text>
            : addresses.map((address) => (
              <View key={address.id} style={styles.card}>
                <Text style={styles.cardTitle}>{address.apelido || 'Endereço'}</Text>
                {address.principal ? <Text style={styles.principal}>Principal</Text> : null}
                <Text style={styles.addressText}>
                  {[address.logradouro, address.numero, address.complemento, address.bairro]
                    .filter(Boolean).join(', ')}
                </Text>
                <Text style={styles.addressText}>{address.cidade} - {address.uf} · {formatCep(address.cep)}</Text>
                <View style={styles.actions}>
                  <Pressable onPress={() => startEdit(address)}><Text style={styles.action}>Editar</Text></Pressable>
                  {!address.principal
                    ? <Pressable onPress={() => { void definirPrincipal(address); }}>
                      <Text style={styles.action}>Definir como principal</Text>
                    </Pressable> : null}
                  {confirmingId === address.id ? <>
                    <Pressable onPress={() => { void desativar(address); }}>
                      <Text style={styles.dangerAction}>Confirmar desativação</Text>
                    </Pressable>
                    <Pressable onPress={() => setConfirmingId(null)}>
                      <Text style={styles.action}>Cancelar</Text>
                    </Pressable>
                  </> : <Pressable onPress={() => setConfirmingId(address.id)}>
                    <Text style={styles.dangerAction}>Desativar</Text>
                  </Pressable>}
                </View>
              </View>
            ))}
          <AcabaAquiButton text="Adicionar endereço" onPress={startCreate} />
        </> : null}

        {!loading && editing ? <>
          <Text style={styles.sectionTitle}>{editingId ? 'Editar endereço' : 'Novo endereço'}</Text>
          <View style={styles.fieldGroup}>
            <Text style={styles.label}>CEP</Text>
            <View style={styles.cepRow}>
              <TextInput
                style={[styles.input, styles.cepInput]}
                value={form.cep}
                onChangeText={(value) => setField('cep', formatCep(value))}
                placeholder="00000-000"
                keyboardType="numeric"
                maxLength={9}
              />
              <Pressable style={styles.lookupButton} onPress={() => { void consultarCep(); }}>
                <Text style={styles.lookupText}>{consultingCep ? 'Buscando...' : 'Buscar'}</Text>
              </Pressable>
            </View>
          </View>

          <AddressField label="Apelido (opcional)" value={form.apelido} onChangeText={(value) => setField('apelido', value)} placeholder="Casa, trabalho..." />
          <AddressField label="Logradouro" value={form.logradouro} onChangeText={(value) => setField('logradouro', value)} placeholder="Rua ou avenida" />
          <AddressField label="Número" value={form.numero} onChangeText={(value) => setField('numero', value)} placeholder="Número ou s/n" />
          <AddressField label="Complemento (opcional)" value={form.complemento} onChangeText={(value) => setField('complemento', value)} placeholder="Apartamento, bloco..." />
          <AddressField label="Bairro" value={form.bairro} onChangeText={(value) => setField('bairro', value)} placeholder="Bairro" />
          <AddressField label="Cidade" value={form.cidade} onChangeText={(value) => setField('cidade', value)} placeholder="Cidade" />
          <AddressField label="UF" value={form.uf} onChangeText={(value) => setField('uf', value.slice(0, 2).toUpperCase())} placeholder="SP" maxLength={2} />

          <View style={styles.switchRow}>
            <Text style={styles.label}>Definir como endereço principal</Text>
            <Switch value={form.principal} onValueChange={(value) => setField('principal', value)} />
          </View>
          <AcabaAquiButton text={saving ? 'Salvando...' : 'Salvar endereço'} onPress={() => { void save(); }} />
          <Pressable style={styles.cancelButton} onPress={() => { setEditing(false); setError(''); }}>
            <Text style={styles.cancelText}>Cancelar</Text>
          </Pressable>
        </> : null}
      </ScrollAreaView>
    </View>
  );
}

function AddressField({ label, value, onChangeText, placeholder, maxLength }: {
  label: string;
  value: string;
  onChangeText: (value: string) => void;
  placeholder: string;
  maxLength?: number;
}) {
  return (
    <View style={styles.fieldGroup}>
      <Text style={styles.label}>{label}</Text>
      <TextInput
        style={styles.input}
        value={value}
        onChangeText={onChangeText}
        placeholder={placeholder}
        maxLength={maxLength}
        autoCapitalize="words"
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff' },
  inner: { width: '100%', paddingHorizontal: 20, paddingVertical: 16 },
  status: { marginBottom: 16, color: '#666' },
  error: { marginBottom: 16, color: '#a43434' },
  success: { marginBottom: 16, color: '#426341' },
  sectionTitle: { fontSize: 20, fontWeight: '700', marginBottom: 16 },
  fieldGroup: { marginBottom: 16 },
  label: { fontSize: 16, marginBottom: 8, color: '#333', fontWeight: '600' },
  input: {
    borderWidth: 1, borderColor: '#d9d9d9', borderRadius: 8,
    paddingHorizontal: 12, paddingVertical: 12, fontSize: 16, backgroundColor: '#fff',
  },
  cepRow: { flexDirection: 'row', gap: 8 },
  cepInput: { flex: 1 },
  lookupButton: {
    justifyContent: 'center', paddingHorizontal: 16, borderRadius: 8, backgroundColor: '#a43434',
  },
  lookupText: { color: '#fff', fontWeight: '600' },
  switchRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: 20 },
  card: { padding: 16, marginBottom: 14, borderRadius: 10, borderWidth: 1, borderColor: '#e0e0e0' },
  cardTitle: { fontSize: 18, fontWeight: '700', marginBottom: 5 },
  principal: { color: '#426341', fontWeight: '600', marginBottom: 8 },
  addressText: { color: '#444', marginBottom: 4 },
  actions: { flexDirection: 'row', flexWrap: 'wrap', gap: 16, marginTop: 14 },
  action: { color: '#a43434', fontWeight: '600' },
  dangerAction: { color: '#8d2525', fontWeight: '600' },
  cancelButton: { padding: 14, alignItems: 'center' },
  cancelText: { color: '#555', fontSize: 16 },
});
