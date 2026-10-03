import { useEffect, useState } from 'react';
import { Pressable, StyleSheet } from 'react-native';

import { Text, View } from '@/components/Themed';
import { AcabaAquiInput } from '@/components/AcabaAquiInput';
import { AcabaAquiButton } from '@/components/AcabaAquiButton';
import { useRouter } from 'expo-router';
import { apiFetch } from '@/lib/api';

export default function RecuperarSenhaScreen() {
    const router = useRouter();
    const [etapa, setEtapa] = useState<'email' | 'codigo'>('email');
    const [email, setEmail] = useState('');
    const [codigo, setCodigo] = useState('');
    const [novaSenha, setNovaSenha] = useState('');
    const [confirmarSenha, setConfirmarSenha] = useState('');
    const [mensagem, setMensagem] = useState('');
    const [erroMensagem, setErroMensagem] = useState('');
    const [ocupado, setOcupado] = useState(false);
    const [cooldown, setCooldown] = useState(0);

    useEffect(() => {
        if (cooldown <= 0) {
            return;
        }
        const timer = setTimeout(() => setCooldown((seconds) => seconds - 1), 1000);
        return () => clearTimeout(timer);
    }, [cooldown]);

    const solicitarCodigo = async () => {
        if (ocupado) {
            return;
        }
        setErroMensagem('');
        setMensagem('');
        setOcupado(true);
        try {
            const response = await apiFetch('/auth/password-reset/request', {
                method: 'POST',
                body: JSON.stringify({ email }),
            });
            const body = await response.json().catch(() => null);
            if (!response.ok) {
                setErroMensagem(body?.message ?? 'Informe um e-mail válido.');
                return;
            }

            setEtapa('codigo');
            setCodigo('');
            setCooldown(60);
            setMensagem(body?.message ?? 'Se houver uma conta elegível, enviaremos um código para o e-mail informado.');
        } catch {
            setErroMensagem('Não foi possível conectar ao servidor. Tente novamente.');
        } finally {
            setOcupado(false);
        }
    };

    const reenviarCodigo = async () => {
        if (ocupado || cooldown > 0) {
            return;
        }
        setErroMensagem('');
        setMensagem('');
        setOcupado(true);
        try {
            const response = await apiFetch('/auth/password-reset/resend', {
                method: 'POST',
                body: JSON.stringify({ email }),
            });
            const body = await response.json().catch(() => null);
            if (!response.ok) {
                setErroMensagem(body?.message ?? 'Não foi possível reenviar o código.');
                return;
            }
            setCooldown(60);
            setMensagem(body?.message ?? 'Se houver uma solicitação ativa, enviaremos um novo código.');
        } catch {
            setErroMensagem('Não foi possível conectar ao servidor. Tente novamente.');
        } finally {
            setOcupado(false);
        }
    };

    const confirmarRedefinicao = async () => {
        if (ocupado) {
            return;
        }
        setErroMensagem('');
        setMensagem('');
        if (!/^\d{6}$/.test(codigo)) {
            setErroMensagem('Informe o código de 6 dígitos recebido por e-mail.');
            return;
        }
        if (novaSenha.length < 8) {
            setErroMensagem('A senha deve ter pelo menos 8 caracteres.');
            return;
        }
        if (novaSenha !== confirmarSenha) {
            setErroMensagem('As senhas não coincidem.');
            return;
        }

        setOcupado(true);
        try {
            const response = await apiFetch('/auth/password-reset/confirm', {
                method: 'POST',
                body: JSON.stringify({ email, code: codigo, newPassword: novaSenha }),
            });
            const body = await response.json().catch(() => null);
            if (!response.ok) {
                setErroMensagem(body?.message ?? 'Código inválido ou expirado.');
                return;
            }

            router.replace('/(auth)/login');
        } catch {
            setErroMensagem('Não foi possível redefinir a senha. Tente novamente.');
        } finally {
            setOcupado(false);
        }
    };

    return (
        <View style={styles.container}>
            <View style={styles.inner}>
                <Pressable
                    style={styles.backButton}
                    onPress={() => router.replace('../')}
                >
                    <Text style={styles.backButtonText}>←</Text>
                </Pressable>

                <Text style={styles.title}>Recuperar Senha</Text>
                                <Text style={styles.subTitle}>
                                        {etapa === 'email'
                                                ? 'Informe o e-mail da sua conta para receber um código de recuperação.'
                                                : `Informe o código enviado para ${email} e escolha uma nova senha.`}
                                </Text>

                                {erroMensagem ? (
                                        <View style={styles.errorCard}>
                                                <Text style={styles.errorText}>{erroMensagem}</Text>
                                        </View>
                                ) : null}
                                {mensagem ? <Text style={styles.infoText}>{mensagem}</Text> : null}

                                {etapa === 'email' ? <>
                                    <View style={styles.inputContainer}>
                    <AcabaAquiInput
                        placeholder="E-mail"
                        keyboardType="email-address"
                                                autoCapitalize="none"
                                                value={email}
                                                onChangeText={setEmail}
                    />
                                    </View>
                                    <AcabaAquiButton
                                        text={ocupado ? 'Enviando...' : 'Enviar código'}
                                        onPress={solicitarCodigo}
                                    />
                                </> : <>
                                    <View style={styles.inputContainer}>
                                        <AcabaAquiInput
                                                placeholder="Código de 6 dígitos"
                                                keyboardType="numeric"
                                                autoCapitalize="none"
                                                value={codigo}
                                                onChangeText={(value) => setCodigo(value.replace(/\D/g, '').slice(0, 6))}
                                        />
                                        <AcabaAquiInput
                                                placeholder="Nova senha"
                                                secureTextEntry
                                                value={novaSenha}
                                                onChangeText={setNovaSenha}
                                        />
                                        <AcabaAquiInput
                                                placeholder="Confirmar nova senha"
                                                secureTextEntry
                                                value={confirmarSenha}
                                                onChangeText={setConfirmarSenha}
                                        />
                                    </View>
                                    <AcabaAquiButton
                                        text={ocupado ? 'Redefinindo...' : 'Redefinir senha'}
                                        onPress={confirmarRedefinicao}
                                    />
                                    <Pressable disabled={ocupado || cooldown > 0} onPress={reenviarCodigo} style={styles.secondaryAction}>
                                        <Text style={[styles.secondaryActionText, cooldown > 0 && styles.disabledActionText]}>
                                                {cooldown > 0 ? `Reenviar código em ${cooldown}s` : 'Reenviar código'}
                                        </Text>
                                    </Pressable>
                                    <Pressable
                                        disabled={ocupado}
                                        onPress={() => {
                                                setEtapa('email');
                                                setMensagem('');
                                                setErroMensagem('');
                                        }}
                                        style={styles.secondaryAction}
                                    >
                                        <Text style={styles.secondaryActionText}>Usar outro e-mail</Text>
                                    </Pressable>
                                </>}
            </View>
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        alignItems: 'flex-start',
        justifyContent: 'flex-start',
    },
    inner: {
        marginLeft: 20,
        marginRight: 20,
        marginVertical: 50,
        gap: 8,
    },
    backButton: {
        alignSelf: 'flex-start',
        marginBottom: 8,
    },
    backButtonText: {
        color: '#a43434',
        fontSize: 32,
        fontWeight: 'bold',
    },
    title: {
        fontSize: 24,
        fontWeight: 'bold',
    },
    subTitle: {
        fontSize: 16,
    },
    errorCard: {
        backgroundColor: '#fdecea',
        borderWidth: 1,
        borderColor: '#f5c2c7',
        borderRadius: 8,
        padding: 12,
    },
    errorText: {
        color: '#a43434',
        fontSize: 14,
    },
    infoText: {
        color: '#426341',
        fontSize: 14,
    },
    secondaryAction: {
        alignSelf: 'center',
        paddingVertical: 8,
    },
    secondaryActionText: {
        color: '#a43434',
        textDecorationLine: 'underline',
    },
    disabledActionText: {
        color: '#7a7a7a',
        textDecorationLine: 'none',
    },
    inputContainer: {
        marginVertical: 20,
        gap: 8,
    },
    separator: {
        marginVertical: 30,
        height: 1,
        width: '80%',
    },
    optionsContainer: {
        flexDirection: 'row',
        justifyContent: 'space-around',
        marginTop: 20,
    },
});
