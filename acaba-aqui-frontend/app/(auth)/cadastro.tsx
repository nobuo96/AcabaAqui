import { useState } from 'react';
import { Pressable, StyleSheet } from 'react-native';

import { Text, View } from '@/components/Themed';
import { AcabaAquiInput } from '@/components/AcabaAquiInput';
import { AcabaAquiButton } from '@/components/AcabaAquiButton';
import { useRouter } from 'expo-router';

export default function CadastroScreen() {
    const router = useRouter();
    const [senha, setSenha] = useState('');
    const [confirmarSenha, setConfirmarSenha] = useState('');
    const [useData, setUseData] = useState({
        nome: '',
        telefone: '',
        email: '',
        perfil: 'Usuario',
        dataCriacao: new Date().toISOString(),
        ultimaAtualizacao: new Date().toISOString(),
        ativo: 'S',
    });

    const handleInputChange = (field: keyof typeof useData, value: string) => {
        setUseData((prev) => ({
            ...prev,
            [field]: value,
        }));
    };

    const handleCadastro = async () => {
        const url = 'http://localhost:8080/user';

        if (senha !== confirmarSenha) {
            console.error('As senhas não coincidem');
            return;
        }

        const payload = {
            nome: useData.nome,
            email: useData.email,
            telefone: useData.telefone,
            perfil: useData.perfil,
            dataCriacao: useData.dataCriacao,
            ultimaAtualizacao: useData.ultimaAtualizacao,
            ativo: useData.ativo,
        };

        try {
            const resposta = await fetch(url, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(payload),
            });

            if (resposta.ok) {
                router.replace('/(cliente)');
            } else {
                console.error('Erro ao cadastrar usuário');
            }
        } catch (error) {
            console.error('Erro de rede:', error);
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
                <Text style={styles.subTitle}>Insira seu e-mail e senha para criar sua conta. É Rápido e Fácil</Text>

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
                    text="Cadastrar"
                    onPress={handleCadastro}
                />
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
