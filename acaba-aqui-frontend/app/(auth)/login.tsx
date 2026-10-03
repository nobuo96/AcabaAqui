import { Image, StyleSheet } from 'react-native';
import { useState } from 'react';

import { Text, View } from '@/components/Themed';
import { AcabaAquiButton } from '@/components/AcabaAquiButton';
import { Link, useRouter } from 'expo-router';
import { AcabaAquiInput } from '@/components/AcabaAquiInput';
import { SocialLoginButton } from '@/components/SocialLoginButton';
import { apiFetch } from '@/lib/api';
import { saveAuthToken, saveAuthUserId } from '@/lib/authToken';

export default function LoginScreen() {
  const router = useRouter();
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [erroMensagem, setErroMensagem] = useState('');

  const handleLogin = async () => {
    setErroMensagem('');
    try {
      const resposta = await apiFetch('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, senha }),
      });

      if (!resposta.ok) {
        setErroMensagem(resposta.status === 401 ? 'E-mail ou senha inválidos.' : 'Não foi possível entrar.');
        return;
      }

      const auth = await resposta.json();
      await saveAuthToken(auth.token);
      await saveAuthUserId(auth.userId);

      if (auth.perfil === 'prestador') {
        router.replace('/(prestador)');
      } else if (auth.perfil === 'administrador') {
        router.replace('/(admin)');
      } else {
        router.replace('/(cliente)');
      }
    } catch {
      setErroMensagem('Não foi possível conectar ao servidor.');
    }
  };

  return (
    <View style={styles.container}>
      <View style={styles.inner}>
        <View style={styles.imageContainer}>
          <Image
            source={require('@/assets/images/logo.jpg')}
            style={styles.image}
            resizeMode="contain"
          />
          <Image
            source={require('@/assets/images/nomeApp.jpg')}
            style={styles.imageNome}
            resizeMode="contain"
          />
        </View>

        <Text style={styles.title}>Acesse sua conta</Text>
        <Text style={styles.subTitle}>Insira seu e-mail e senha para acessar sua conta.</Text>

        {erroMensagem ? <Text style={styles.errorText}>{erroMensagem}</Text> : null}

        <View style={styles.inputContainer}>
          <AcabaAquiInput
            placeholder="E-mail"
            keyboardType="email-address"
            autoCapitalize="none"
            value={email}
            onChangeText={setEmail}
          />
          <AcabaAquiInput
            placeholder="Senha"
            secureTextEntry={true}
            value={senha}
            onChangeText={setSenha}
          />
        </View>

        <AcabaAquiButton text="Entrar" onPress={handleLogin} />

        <View style={styles.optionsContainer}>
          <Link href="/(auth)/cadastro" style={{ textDecorationLine: 'underline' }}>
            <Text>Cadastrar-se</Text>
          </Link>
          <Link href="/(auth)/recuperarSenha" style={{ textDecorationLine: 'underline' }}>
            <Text>Esqueceu sua senha?</Text>
          </Link>
        </View>

        <View style={styles.socialContainer}>
          <SocialLoginButton
            provider="google"
            text="Continuar com Google"
            onPress={() => setErroMensagem('Login com Google ainda não está configurado.')}
          />
          <SocialLoginButton
            provider="facebook"
            text="Continuar com Facebook"
            onPress={() => setErroMensagem('Login com Facebook ainda não está configurado.')}
          />
        </View>
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
    marginVertical: 8,
    gap: 8,
  },
  imageContainer: {
    flexDirection: 'row',
  },
  image: {
    width: 80,
    height: 80,
    marginVertical: 20,
  },
  imageNome: {
    width: 200,
    height: 100,
    marginVertical: 20,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
  },
  subTitle: {
    fontSize: 16,
  },
  errorText: { color: '#a43434', fontSize: 14 },
  inputContainer: {
    marginVertical: 8,
    gap: 8,
  },
  socialContainer: {
    width: '100%',
    gap: 12,
    marginTop: 8,
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
