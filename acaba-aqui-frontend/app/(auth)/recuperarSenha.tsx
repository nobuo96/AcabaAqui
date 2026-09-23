import { Pressable, StyleSheet } from 'react-native';

import { Text, View } from '@/components/Themed';
import { AcabaAquiInput } from '@/components/AcabaAquiInput';
import { AcabaAquiButton } from '@/components/AcabaAquiButton';
import { useRouter } from 'expo-router';

export default function RecuperarSenhaScreen() {
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

                <Text style={styles.title}>Recuperar Senha</Text>
                <Text style={styles.subTitle}>Insira seu e-mail para receber instruções de recuperação de senha.</Text>

                <View style={styles.inputContainer}>
                    <AcabaAquiInput
                        placeholder="E-mail"
                        keyboardType="email-address"
                    />
                </View>

                <AcabaAquiButton
                    text="Recuperar Senha"
                    onPress={() => router.replace('/(auth)/login')}
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
