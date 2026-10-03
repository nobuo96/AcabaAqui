import { useEffect, useState } from 'react';
import { ScrollView, StyleSheet, TextInput, View } from 'react-native';

import { AcabaAquiButton } from '@/components/AcabaAquiButton';
import { AcabaAquiHeader } from '@/components/AcabaAquiHeader';
import { Text } from '@/components/Themed';
import { apiFetch } from '@/lib/api';
import { getAuthUserId } from '@/lib/authToken';

interface UserDetails {
  nome: string;
  email: string;
  telefone: string | null;
  perfil: string;
  dataNascimento: string | null;
}

interface InformacoesPessoaisFormProps {
  onBack: () => void;
}

function formatBirthDate(value: string | null) {
  if (!value) {
    return '';
  }
  const [year, month, day] = value.slice(0, 10).split('-');
  return year && month && day ? `${day}/${month}/${year}` : '';
}

function maskBirthDate(value: string) {
  const digits = value.replace(/\D/g, '').slice(0, 8);
  if (digits.length <= 2) {
    return digits;
  }
  if (digits.length <= 4) {
    return `${digits.slice(0, 2)}/${digits.slice(2)}`;
  }
  return `${digits.slice(0, 2)}/${digits.slice(2, 4)}/${digits.slice(4)}`;
}

function parseBirthDate(value: string): string | null | undefined {
  if (!value.trim()) {
    return null;
  }

  const match = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(value);
  if (!match) {
    return undefined;
  }

  const day = Number(match[1]);
  const month = Number(match[2]);
  const year = Number(match[3]);
  const date = new Date(Date.UTC(year, month - 1, day));
  if (year < 1900 || date.getUTCFullYear() !== year
      || date.getUTCMonth() !== month - 1 || date.getUTCDate() !== day) {
    return undefined;
  }

  const isoDate = `${match[3]}-${match[2]}-${match[1]}`;
  const today = new Date().toISOString().slice(0, 10);
  return isoDate <= today ? isoDate : undefined;
}

export function InformacoesPessoaisForm({ onBack }: InformacoesPessoaisFormProps) {
  const [user, setUser] = useState<UserDetails | null>(null);
  const [nome, setNome] = useState('');
  const [telefone, setTelefone] = useState('');
  const [dataNascimento, setDataNascimento] = useState('');
  const [carregando, setCarregando] = useState(true);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState('');
  const [mensagem, setMensagem] = useState('');

  useEffect(() => {
    let ativo = true;

    async function carregarUsuario() {
      try {
        const userId = await getAuthUserId();
        if (!userId) {
          throw new Error('Sessão não encontrada. Entre novamente.');
        }

        const response = await apiFetch(`/user/${encodeURIComponent(userId)}`);
        if (!response.ok) {
          throw new Error(`Não foi possível carregar os dados (${response.status}).`);
        }

        const details: UserDetails = await response.json();
        if (ativo) {
          setUser(details);
          setNome(details.nome ?? '');
          setTelefone(details.telefone ?? '');
          setDataNascimento(formatBirthDate(details.dataNascimento));
        }
      } catch (error) {
        if (ativo) {
          setErro(error instanceof Error ? error.message : 'Não foi possível carregar os dados.');
        }
      } finally {
        if (ativo) {
          setCarregando(false);
        }
      }
    }

    void carregarUsuario();
    return () => {
      ativo = false;
    };
  }, []);

  async function salvar() {
    if (!user || salvando) {
      return;
    }

    setErro('');
    setMensagem('');
    if (!nome.trim()) {
      setErro('Informe seu nome.');
      return;
    }
    if (!telefone.trim()) {
      setErro('Informe seu telefone.');
      return;
    }

    const parsedBirthDate = parseBirthDate(dataNascimento);
    if (parsedBirthDate === undefined) {
      setErro('Informe uma data válida no formato DD/MM/AAAA, não futura.');
      return;
    }

    setSalvando(true);
    try {
      const response = await apiFetch(`/user/${encodeURIComponent(await getAuthUserId() ?? '')}`, {
        method: 'PUT',
        body: JSON.stringify({
          nome: nome.trim(),
          telefone: telefone.trim(),
          dataNascimento: parsedBirthDate,
        }),
      });
      const body = await response.json().catch(() => null);
      if (!response.ok) {
        setErro(body?.message ?? `Não foi possível salvar os dados (${response.status}).`);
        return;
      }

      setUser(body);
      setNome(body.nome ?? '');
      setTelefone(body.telefone ?? '');
      setDataNascimento(formatBirthDate(body.dataNascimento));
      setMensagem('Informações salvas.');
    } catch {
      setErro('Não foi possível conectar ao servidor. Tente novamente.');
    } finally {
      setSalvando(false);
    }
  }

  const perfil = user?.perfil === 'prestador' ? 'Prestador'
    : user?.perfil === 'cliente' ? 'Cliente' : user?.perfil ?? '';

  return (
    <View style={styles.container}>
      <AcabaAquiHeader title="Informações Pessoais" backButton onBackPress={onBack} />
      <ScrollView style={styles.inner} keyboardShouldPersistTaps="handled">
        <Text style={styles.sectionLabel}>Dados pessoais</Text>

        {carregando ? <Text style={styles.status}>Carregando informações...</Text> : null}
        {erro ? <Text style={styles.error}>{erro}</Text> : null}
        {mensagem ? <Text style={styles.success}>{mensagem}</Text> : null}

        {!carregando && user ? <>
          <View style={styles.fieldGroup}>
            <Text style={styles.label}>Nome</Text>
            <TextInput style={styles.input} value={nome} onChangeText={setNome} placeholder="Nome completo" />
          </View>

          <View style={styles.fieldGroup}>
            <Text style={styles.label}>Data de nascimento (opcional)</Text>
            <TextInput
              style={styles.input}
              value={dataNascimento}
              onChangeText={(value) => setDataNascimento(maskBirthDate(value))}
              placeholder="DD/MM/AAAA"
              keyboardType="numeric"
              maxLength={10}
            />
          </View>

          <View style={styles.fieldGroup}>
            <Text style={styles.label}>Telefone</Text>
            <TextInput
              style={styles.input}
              value={telefone}
              onChangeText={setTelefone}
              placeholder="Telefone"
              keyboardType="phone-pad"
              maxLength={20}
            />
          </View>

          <View style={styles.fieldGroup}>
            <Text style={styles.label}>E-mail</Text>
            <TextInput style={[styles.input, styles.readOnlyInput]} value={user.email} editable={false} />
          </View>

          <View style={styles.fieldGroup}>
            <Text style={styles.label}>Perfil</Text>
            <TextInput style={[styles.input, styles.readOnlyInput]} value={perfil} editable={false} />
          </View>

          <AcabaAquiButton text={salvando ? 'Salvando...' : 'Salvar'} onPress={() => { void salvar(); }} />
        </> : null}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  inner: {
    width: '100%',
    paddingHorizontal: 20,
    paddingVertical: 16,
  },
  sectionLabel: {
    fontSize: 20,
    fontWeight: '700',
    marginBottom: 12,
  },
  fieldGroup: {
    marginBottom: 18,
  },
  label: {
    fontSize: 16,
    marginBottom: 8,
    color: '#333',
    fontWeight: '600',
  },
  input: {
    borderWidth: 1,
    borderColor: '#d9d9d9',
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 12,
    fontSize: 16,
    backgroundColor: '#fff',
  },
  readOnlyInput: {
    backgroundColor: '#f3f3f3',
    color: '#666',
  },
  status: {
    marginBottom: 16,
    color: '#666',
  },
  error: {
    marginBottom: 16,
    color: '#a43434',
  },
  success: {
    marginBottom: 16,
    color: '#426341',
  },
});