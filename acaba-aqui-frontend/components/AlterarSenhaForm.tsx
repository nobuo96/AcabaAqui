import { useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';

import { AcabaAquiButton } from '@/components/AcabaAquiButton';
import { AcabaAquiHeader } from '@/components/AcabaAquiHeader';
import { AcabaAquiInput } from '@/components/AcabaAquiInput';
import { Text } from '@/components/Themed';
import { useRouter } from 'expo-router';
import { apiFetch } from '@/lib/api';
import { removeAuthToken } from '@/lib/authToken';

interface InformacoesPessoaisFormProps {
    onBack: () => void;
}

export function AlterarSenhaForm({ onBack }: InformacoesPessoaisFormProps) {
    const router = useRouter();
    const [senhaAtual, setSenhaAtual] = useState('');
    const [novaSenha, setNovaSenha] = useState('');
    const [confirmacaoSenha, setConfirmacaoSenha] = useState('');
    const [alterando, setAlterando] = useState(false);
    const [erro, setErro] = useState('');
    const [mensagem, setMensagem] = useState('');

    async function alterar() {
        if (alterando) {
            return;
        }

        setErro('');
        setMensagem('');

        if (senhaAtual.trim().length === 0 || novaSenha.length === 0 || confirmacaoSenha.length === 0) {
            setErro('Preencha todos os campos.');
            return;
        }
        if (novaSenha.length < 8) {
            setErro('A nova senha deve ter pelo menos 8 caracteres.');
            return;
        }
        if (novaSenha !== confirmacaoSenha) {
            setErro('A confirmação da nova senha não confere.');
            return;
        }

        setAlterando(true);
        try {
            const response = await apiFetch('/auth/change-password', {
                method: 'POST',
                body: JSON.stringify({ senhaAtual, novaSenha }),
            });
            const body = await response.json().catch(() => null);
            if (!response.ok) {
                setErro(body?.message ?? `Não foi possível alterar a senha (${response.status}).`);
                return;
            }

            await removeAuthToken();
            setMensagem('Senha alterada. Faça login novamente.');
            setTimeout(() => {
                router.replace('/(auth)/login');
            }, 500);
        } catch {
            setErro('Não foi possível conectar ao servidor. Tente novamente.');
        } finally {
            setAlterando(false);
        }
    }

    return (
        <View style={styles.container}>
            <AcabaAquiHeader title="Alterar Senha" backButton onBackPress={onBack} />
            <ScrollView style={styles.inner} keyboardShouldPersistTaps="handled">
                {erro ? <Text style={styles.error}>{erro}</Text> : null}
                {mensagem ? <Text style={styles.success}>{mensagem}</Text> : null}

                <View style={styles.fieldGroup}>
                    <Text style={styles.label}>Senha Atual</Text>
                    <AcabaAquiInput
                        placeholder="Digite sua senha atual"
                        secureTextEntry={true}
                        value={senhaAtual}
                        onChangeText={setSenhaAtual}
                    />
                </View>

                <View style={styles.fieldGroup}>
                    <Text style={styles.label}>Nova Senha</Text>
                    <AcabaAquiInput
                        placeholder="Mínimo de 8 caracteres"
                        secureTextEntry={true}
                        value={novaSenha}
                        onChangeText={setNovaSenha}
                    />
                </View>

                <View style={styles.fieldGroup}>
                    <Text style={styles.label}>Confirmar Nova Senha</Text>
                    <AcabaAquiInput
                        placeholder="Repita a nova senha"
                        secureTextEntry={true}
                        value={confirmacaoSenha}
                        onChangeText={setConfirmacaoSenha}
                    />
                </View>

                <AcabaAquiButton text={alterando ? 'Alterando...' : 'Alterar'} onPress={() => { void alterar(); }} />
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