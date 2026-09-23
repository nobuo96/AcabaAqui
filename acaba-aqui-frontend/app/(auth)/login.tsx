import { Image, StyleSheet } from 'react-native';
import { useState } from 'react';

import { Text, View } from '@/components/Themed';
import { AcabaAquiButton } from '@/components/AcabaAquiButton';
import { Link, useRouter } from 'expo-router';
import { AcabaAquiInput } from '@/components/AcabaAquiInput';
import { SocialLoginButton } from '@/components/SocialLoginButton';
import { AcabaAquiSwitch } from '@/components/AcabaAquiSwitch';

export default function LoginScreen() {
  const router = useRouter();
  const [role, setRole] = useState('Cliente');

  const handleLogin = () => {
    if (role === 'Fornecedor') {
      router.replace('/(prestador)');
      return;
    }

    if (role === 'Administrador') {
      router.replace('/(admin)');
      return;
    }

    router.replace('/(cliente)');
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

        <View style={styles.roleContainer}>
          <Text style={styles.roleLabel}>Eu sou</Text>
          <AcabaAquiSwitch
            options={['Cliente', 'Fornecedor', 'Administrador']}
            value={role}
            onChange={setRole}
          />
        </View>

        <View style={styles.inputContainer}>
          <AcabaAquiInput
            placeholder="E-mail"
            keyboardType="email-address"
          />
          <AcabaAquiInput
            placeholder="Senha"
            secureTextEntry={true}
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
            onPress={handleLogin}
          />
          <SocialLoginButton
            provider="facebook"
            text="Continuar com Facebook"
            onPress={handleLogin}
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
  roleContainer: {
    marginTop: 8,
    marginBottom: 12,
    gap: 8,
  },
  roleLabel: {
    fontSize: 15,
    fontWeight: '600',
    color: '#333333',
  },
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
