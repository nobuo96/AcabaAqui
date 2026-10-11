import { Image, Platform, Pressable, StyleSheet } from 'react-native';
import { useEffect, useRef, useState } from 'react';
import * as AuthSession from 'expo-auth-session';
import { useIdTokenAuthRequest } from 'expo-auth-session/providers/google';
import * as WebBrowser from 'expo-web-browser';

import { Text, View } from '@/components/Themed';
import { AcabaAquiButton } from '@/components/AcabaAquiButton';
import { Link, useRouter } from 'expo-router';
import { AcabaAquiInput } from '@/components/AcabaAquiInput';
import { SocialLoginButton } from '@/components/SocialLoginButton';
import { ScrollAreaView } from '@/components/ScrollAreaView';
import { apiFetch } from '@/lib/api';
import { saveAuthToken, saveAuthUserId } from '@/lib/authToken';

WebBrowser.maybeCompleteAuthSession();

type SocialProvider = 'google' | 'facebook';
type SocialLoginResponse = {
  requerPerfil: boolean;
  nome: string | null;
  email: string | null;
  token: string | null;
  userId: number | null;
  perfil: string | null;
  expiresInSeconds: number;
};

const facebookAppId = process.env.EXPO_PUBLIC_FACEBOOK_APP_ID?.trim() ?? '';
const facebookRedirectUri = AuthSession.makeRedirectUri({
  native: `fb${facebookAppId || 'development'}://authorize`,
});
const facebookDiscovery = {
  authorizationEndpoint: 'https://www.facebook.com/v25.0/dialog/oauth',
};

export default function LoginScreen() {
  const router = useRouter();
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [erroMensagem, setErroMensagem] = useState('');
  const [socialBusy, setSocialBusy] = useState(false);
  const [socialDraft, setSocialDraft] = useState<{
    provider: SocialProvider;
    credential: string;
    nome: string;
    email: string;
  } | null>(null);
  const [socialPhone, setSocialPhone] = useState('');
  const [socialName, setSocialName] = useState('');
  const [socialProfile, setSocialProfile] = useState<'cliente' | 'prestador'>('cliente');
  const processedGoogleToken = useRef('');
  const processedFacebookToken = useRef('');

  const googleWebClientId = process.env.EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID?.trim() ?? '';
  const googleIosClientId = process.env.EXPO_PUBLIC_GOOGLE_IOS_CLIENT_ID?.trim() ?? '';
  const googleAndroidClientId = process.env.EXPO_PUBLIC_GOOGLE_ANDROID_CLIENT_ID?.trim() ?? '';
  const facebookConfigured = Boolean(facebookAppId);
  const googleConfigured = Boolean(Platform.select({
    web: googleWebClientId,
    ios: googleIosClientId,
    android: googleAndroidClientId,
    default: '',
  }));

  const [googleRequest, googleResponse, promptGoogleAsync] = useIdTokenAuthRequest({
    webClientId: googleWebClientId || 'google-client-id-not-configured',
    iosClientId: googleIosClientId || 'google-client-id-not-configured',
    androidClientId: googleAndroidClientId || 'google-client-id-not-configured',
    selectAccount: true,
  });

  const [facebookRequest, facebookResponse, promptFacebookAsync] = AuthSession.useAuthRequest({
    clientId: facebookAppId || 'facebook-app-id-not-configured',
    redirectUri: facebookRedirectUri,
    responseType: AuthSession.ResponseType.Token,
    scopes: ['public_profile', 'email'],
    usePKCE: false,
  }, facebookDiscovery);

  const navigateAfterLogin = async (auth: SocialLoginResponse) => {
    if (!auth.token || auth.userId == null || !auth.perfil) {
      setErroMensagem('O provedor não retornou uma sessão válida.');
      return;
    }
    await saveAuthToken(auth.token);
    await saveAuthUserId(auth.userId);

    if (auth.perfil === 'prestador') {
      router.replace('/(prestador)');
    } else if (auth.perfil === 'administrador') {
      router.replace('/(admin)');
    } else {
      router.replace('/(cliente)');
    }
  };

  const sendSocialCredential = async (
    provider: SocialProvider,
    credential: string,
    perfil?: 'cliente' | 'prestador',
    nome?: string,
    telefone?: string,
  ) => {
    const response = await apiFetch('/auth/social', {
      method: 'POST',
      body: JSON.stringify({ provedor: provider, credencial: credential, perfil, nome, telefone }),
    });
    const body = await response.json().catch(() => null);
    if (!response.ok) {
      throw new Error(body?.message ?? 'Não foi possível entrar com esta conta.');
    }
    return body as SocialLoginResponse;
  };

  const handleSocialCredential = async (provider: SocialProvider, credential: string) => {
    setSocialBusy(true);
    setErroMensagem('');
    setSocialDraft(null);
    try {
      const auth = await sendSocialCredential(provider, credential);
      if (auth.requerPerfil) {
        if (!auth.email) {
          setErroMensagem('O provedor não retornou um e-mail válido.');
          return;
        }
        setSocialDraft({ provider, credential, nome: auth.nome ?? '', email: auth.email });
        setSocialName(auth.nome ?? '');
        setSocialProfile('cliente');
        setSocialPhone('');
        return;
      }
      await navigateAfterLogin(auth);
    } catch (error) {
      setErroMensagem(error instanceof Error ? error.message : 'Não foi possível entrar com esta conta.');
    } finally {
      setSocialBusy(false);
    }
  };

  useEffect(() => {
    if (googleResponse?.type === 'error') {
      setErroMensagem('O Google recusou a autenticação. Confira a configuração OAuth e tente novamente.');
      return;
    }
    if (googleResponse?.type !== 'success') {
      return;
    }
    const credential = googleResponse.params.id_token || googleResponse.authentication?.idToken;
    if (!credential || processedGoogleToken.current === credential) {
      return;
    }
    processedGoogleToken.current = credential;
    void handleSocialCredential('google', credential);
  }, [googleResponse]);

  useEffect(() => {
    if (facebookResponse?.type === 'error') {
      setErroMensagem('O Facebook recusou a autenticação. Confira a configuração OAuth e tente novamente.');
      return;
    }
    if (facebookResponse?.type !== 'success') {
      return;
    }
    const credential = facebookResponse.params.access_token || facebookResponse.authentication?.accessToken;
    if (!credential || processedFacebookToken.current === credential) {
      return;
    }
    processedFacebookToken.current = credential;
    void handleSocialCredential('facebook', credential);
  }, [facebookResponse]);

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
      await navigateAfterLogin(auth);
    } catch {
      setErroMensagem('Não foi possível conectar ao servidor.');
    }
  };

  const handleSocialProfileSubmit = async () => {
    if (!socialDraft || socialBusy) {
      return;
    }
    setSocialBusy(true);
    setErroMensagem('');
    try {
      const auth = await sendSocialCredential(
        socialDraft.provider,
        socialDraft.credential,
        socialProfile,
        socialName,
        socialPhone,
      );
      await navigateAfterLogin(auth);
    } catch (error) {
      setErroMensagem(error instanceof Error ? error.message : 'Não foi possível concluir o cadastro.');
    } finally {
      setSocialBusy(false);
    }
  };

  const handleGoogleLogin = async () => {
    if (!googleConfigured) {
      setErroMensagem('Configure os IDs OAuth de desenvolvimento do Google antes de testar.');
      return;
    }
    setErroMensagem('');
    try {
      await promptGoogleAsync();
    } catch {
      setErroMensagem('Não foi possível abrir o login do Google.');
    }
  };

  const handleFacebookLogin = async () => {
    if (!facebookConfigured) {
      setErroMensagem('Configure o ID do aplicativo de desenvolvimento do Facebook antes de testar.');
      return;
    }
    setErroMensagem('');
    try {
      await promptFacebookAsync();
    } catch {
      setErroMensagem('Não foi possível abrir o login do Facebook.');
    }
  };

  return (
    <View style={styles.container}>
      <ScrollAreaView contentContainerStyle={styles.inner}>
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

        {socialDraft ? (
          <View style={styles.socialContainer}>
            <Text style={styles.socialTitle}>Complete seu cadastro</Text>
            {socialDraft.nome ? <Text>{socialDraft.nome}</Text> : null}
            <Text>{socialDraft.email}</Text>
            <AcabaAquiInput
              placeholder="Nome completo"
              value={socialName}
              onChangeText={setSocialName}
            />
            <AcabaAquiInput
              placeholder="Telefone"
              keyboardType="phone-pad"
              value={socialPhone}
              onChangeText={setSocialPhone}
            />
            <Text style={styles.roleLabel}>Escolha seu perfil</Text>
            <View style={styles.profileSelector}>
              <Pressable
                style={[styles.profileOption, socialProfile === 'cliente' && styles.profileOptionSelected]}
                onPress={() => setSocialProfile('cliente')}
              >
                <Text>Cliente</Text>
              </Pressable>
              <Pressable
                style={[styles.profileOption, socialProfile === 'prestador' && styles.profileOptionSelected]}
                onPress={() => setSocialProfile('prestador')}
              >
                <Text>Prestador</Text>
              </Pressable>
            </View>
            <AcabaAquiButton
              text={socialBusy ? 'Concluindo...' : 'Concluir cadastro'}
              onPress={handleSocialProfileSubmit}
            />
            <Pressable onPress={() => setSocialDraft(null)} style={styles.cancelSocial}>
              <Text style={styles.cancelSocialText}>Cancelar</Text>
            </Pressable>
          </View>
        ) : (
          <View style={styles.socialContainer}>
            <SocialLoginButton
              provider="google"
              text="Continuar com Google"
              onPress={handleGoogleLogin}
            />
            <SocialLoginButton
              provider="facebook"
              text="Continuar com Facebook"
              onPress={handleFacebookLogin}
            />
            {socialBusy ? <Text style={styles.socialStatus}>Validando sua conta...</Text> : null}
            {!googleRequest || !facebookRequest ? (
              <Text style={styles.socialStatus}>Carregando opções de login...</Text>
            ) : null}
            {__DEV__ ? (
              <View>
                <Text style={styles.redirectHint}>Retorno Google: {googleRequest?.redirectUri ?? 'carregando'}</Text>
                <Text style={styles.redirectHint}>Retorno Facebook: {facebookRequest?.redirectUri ?? 'carregando'}</Text>
              </View>
            ) : null}
          </View>
        )}
      </ScrollAreaView>
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
  socialTitle: {
    fontSize: 18,
    fontWeight: '600',
  },
  socialStatus: {
    fontSize: 14,
    textAlign: 'center',
  },
  redirectHint: {
    fontSize: 11,
  },
  roleLabel: {
    fontSize: 16,
    fontWeight: '600',
    textAlign: 'center',
  },
  profileSelector: {
    flexDirection: 'row',
    gap: 12,
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
    backgroundColor: '#f5dada',
    borderColor: '#a43434',
  },
  cancelSocial: {
    alignSelf: 'center',
    padding: 8,
  },
  cancelSocialText: {
    color: '#a43434',
    textDecorationLine: 'underline',
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
