import { Pressable, StyleSheet } from 'react-native';

import { Text, View } from '@/components/Themed';
import { AcabaAquiInput } from '@/components/AcabaAquiInput';
import { AcabaAquiButton } from '@/components/AcabaAquiButton';
import { useRouter } from 'expo-router';

export default function CadastroScreen() {
    const router = useRouter();

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
                        placeholder="E-mail"
                        keyboardType="email-address"
                    />
                    <AcabaAquiInput
                        placeholder="Senha"
                        secureTextEntry={true}
                    />
                    <AcabaAquiInput
                        placeholder="Confirmar Senha"
                        secureTextEntry={true}
                    />
                </View>

                <AcabaAquiButton
                    text="Cadastrar"
                    onPress={() => router.replace('/(cliente)')}
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
