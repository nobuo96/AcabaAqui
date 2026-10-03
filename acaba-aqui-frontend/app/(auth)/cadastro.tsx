import { useEffect, useState } from 'react';
import { Pressable, StyleSheet } from 'react-native';

import { Text, View } from '@/components/Themed';
import { AcabaAquiInput } from '@/components/AcabaAquiInput';
import { AcabaAquiButton } from '@/components/AcabaAquiButton';
import { useRouter } from 'expo-router';
import { apiFetch } from '@/lib/api';
import { saveAuthToken, saveAuthUserId } from '@/lib/authToken';

export default function CadastroScreen() {
    const router = useRouter();
    const [etapa, setEtapa] = useState<'dados' | 'codigo'>('dados');
    const [senha, setSenha] = useState('');
    const [confirmarSenha, setConfirmarSenha] = useState('');
    const [codigo, setCodigo] = useState('');
    const [erroMensagem, setErroMensagem] = useState('');
    const [mensagem, setMensagem] = useState('');
    const [ocupado, setOcupado] = useState(false);
    const [cooldown, setCooldown] = useState(0);
    const [useData, setUseData] = useState({
        nome: '',
        telefone: '',
        email: '',
        perfil: 'cliente',
        ativo: true,
    });

    const handleInputChange = (field: keyof typeof useData, value: string) => {
        setUseData((prev) => ({
            ...prev,
            [field]: value,
        }));
    };

    useEffect(() => {
        if (cooldown <= 0) {
            return;
        }

        const timer = setTimeout(() => setCooldown((seconds) => seconds - 1), 1000);
        return () => clearTimeout(timer);
    }, [cooldown]);

    const handleCadastro = async () => {
        setErroMensagem('');
        setMensagem('');

        if (ocupado) {
            return;
        }

        if (senha !== confirmarSenha) {
            setErroMensagem('As senhas não coincidem.');
            return;
        }

        const payload = {
            nome: useData.nome,
            email: useData.email,
            telefone: useData.telefone,
            perfil: useData.perfil,
            senha,
        };

        setOcupado(true);
        try {
            const resposta = await apiFetch('/auth/email-verification/request', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload),
            });

            if (resposta.ok) {
                setEtapa('codigo');
                setCodigo('');
                setCooldown(60);
                setMensagem('Se o cadastro puder prosseguir, enviaremos um código para o e-mail informado.');
            } else {
                const body = await resposta.json().catch(() => null);
                setErroMensagem(body?.message ?? `Não foi possível iniciar o cadastro (${resposta.status}).`);
            }
        } catch {
            setErroMensagem('Não foi possível cadastrar o usuário. Verifique os dados informados e tente novamente.');
        } finally {
            setOcupado(false);
        }
    };

    const handleReenviarCodigo = async () => {
        if (ocupado || cooldown > 0) {
            return;
        }

        setOcupado(true);
        setErroMensagem('');
        setMensagem('');
        try {
            const resposta = await apiFetch('/auth/email-verification/resend', {
                method: 'POST',
                body: JSON.stringify({ email: useData.email }),
            });
            if (resposta.ok) {
                setCooldown(60);
                setMensagem('Se o cadastro puder prosseguir, enviaremos um novo código.');
            } else {
                const body = await resposta.json().catch(() => null);
                setErroMensagem(body?.message ?? 'Não foi possível reenviar o código.');
            }
        } catch {
            setErroMensagem('Não foi possível conectar ao servidor.');
        } finally {
            setOcupado(false);
        }
    };

    const handleConfirmarCodigo = async () => {
        if (ocupado) {
            return;
        }
        if (!/^\d{6}$/.test(codigo)) {
            setErroMensagem('Informe o código de 6 dígitos recebido por e-mail.');
            return;
        }

        setOcupado(true);
        setErroMensagem('');
        setMensagem('');
        try {
            const resposta = await apiFetch('/auth/email-verification/confirm', {
                method: 'POST',
                body: JSON.stringify({ email: useData.email, code: codigo }),
            });

            if (!resposta.ok) {
                const body = await resposta.json().catch(() => null);
                setErroMensagem(body?.message ?? (resposta.status === 429
                    ? 'Muitas tentativas. Solicite um novo código.'
                    : 'Código inválido ou expirado.'));
                return;
            }

            const auth = await resposta.json();
            await saveAuthToken(auth.token);
            await saveAuthUserId(auth.userId);
            router.replace(auth.perfil === 'prestador' ? '/(prestador)' : '/(cliente)');
        } catch {
            setErroMensagem('Não foi possível confirmar o código. Tente novamente.');
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

                <Text style={styles.title}>Cadastre-se</Text>
                <Text style={styles.subTitle}>
                    {etapa === 'dados'
                        ? 'Insira seus dados para receber um código de confirmação.'
                        : `Informe o código de 6 dígitos enviado para ${useData.email}.`}
                </Text>

                {erroMensagem ? (
                    <View style={styles.errorCard}>
                        <Text style={styles.errorText}>{erroMensagem}</Text>
                    </View>
                ) : null}
                                {mensagem ? <Text style={styles.infoText}>{mensagem}</Text> : null}

                                {etapa === 'dados' ? <>
                                    <View style={styles.inputContainer}>
                    <AcabaAquiInput
                        placeholder="Nome Completo"
                        value={useData.nome}
                        onChangeText={(text) => handleInputChange('nome', text)}
                    />
                    <AcabaAquiInput
                        placeholder="Telefone"
                        keyboardType="phone-pad"
                        value={useData.telefone}
                        onChangeText={(text) => handleInputChange('telefone', text)}
                    />
                    <AcabaAquiInput
                        placeholder="E-mail"
                        keyboardType="email-address"
                        value={useData.email}
                        onChangeText={(text) => handleInputChange('email', text)}
                    />
                    
                    <Text style={styles.roleLabel}>Escolha seu Perfil</Text>
                    <View style={styles.profileSelector}>
                        <Pressable
                            style={[
                                styles.profileOption,
                                useData.perfil === 'cliente' && styles.profileOptionSelected,
                            ]}
                            onPress={() => handleInputChange('perfil', 'cliente')}
                        >
                            <Text style={styles.profileOptionText}>Cliente</Text>
                        </Pressable>

                        <Pressable
                            style={[
                                styles.profileOption,
                                useData.perfil === 'prestador' && styles.profileOptionSelected,
                            ]}
                            onPress={() => handleInputChange('perfil', 'prestador')}
                        >
                            <Text style={styles.profileOptionText}>Prestador</Text>
                        </Pressable>
                    </View>

                    <AcabaAquiInput
                        placeholder="Senha"
                        secureTextEntry={true}
                        value={senha}
                        onChangeText={setSenha}
                    />
                    <AcabaAquiInput
                        placeholder="Confirmar Senha"
                        secureTextEntry={true}
                        value={confirmarSenha}
                        onChangeText={setConfirmarSenha}
                    />
                  </View>
                  <AcabaAquiButton
                      text={ocupado ? 'Enviando código...' : 'Enviar código'}
                      onPress={handleCadastro}
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
                  </View>
                  <AcabaAquiButton
                      text={ocupado ? 'Validando...' : 'Confirmar e-mail'}
                      onPress={handleConfirmarCodigo}
                  />
                  <Pressable
                      disabled={ocupado || cooldown > 0}
                      onPress={handleReenviarCodigo}
                      style={styles.secondaryAction}
                  >
                      <Text style={[styles.secondaryActionText, cooldown > 0 && styles.disabledActionText]}>
                          {cooldown > 0 ? `Reenviar código em ${cooldown}s` : 'Reenviar código'}
                      </Text>
                  </Pressable>
                  <Pressable
                      disabled={ocupado}
                      onPress={() => {
                          setEtapa('dados');
                          setErroMensagem('');
                          setMensagem('');
                      }}
                      style={styles.secondaryAction}
                  >
                      <Text style={styles.secondaryActionText}>Corrigir dados ou e-mail</Text>
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
        width: '100%',
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
    inputContainer: {
        marginVertical: 20,
        gap: 8,
    },
    errorCard: {
        backgroundColor: '#fdecea',
        borderWidth: 1,
        borderColor: '#f5c2c7',
        borderRadius: 10,
        padding: 12,
        marginBottom: 12,
    },
    errorText: {
        color: '#a43434',
        fontSize: 14,
        fontWeight: '600',
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
    roleLabel: {
        fontSize: 20,
        fontWeight: '600',
        color: '#333333',
        textAlign: 'center',
        marginTop: 20,
    },
    profileSelector: {
        flexDirection: 'row',
        gap: 12,
        marginVertical: 8,
    },
    profileOption: {
        flex: 1,
        paddingVertical: 12,
        paddingHorizontal: 16,
        borderRadius: 10,
        borderWidth: 1,
        borderColor: '#d9d9d9',
        alignItems: 'center',
    },
    profileOptionSelected: {
        backgroundColor: '#a43434',
        borderColor: '#a43434',
    },
    profileOptionText: {
        color: '#333',
        fontWeight: '600',
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
