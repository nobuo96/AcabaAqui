import { StyleSheet } from 'react-native';

import { Text, View } from '@/components/Themed';
import { useRouter } from 'expo-router';
import { AcabaAquiHeader } from '@/components/AcabaAquiHeader';
import { AcabaAquiOption } from '@/components/AcabaAquiOption';
import { ScrollAreaView } from '@/components/ScrollAreaView';
import { removeAuthToken } from '@/lib/authToken';

export default function PerfilScreen() {
  const router = useRouter();

  const handleLogout = async () => {
    await removeAuthToken();
    router.replace('/(auth)/login');
  };

  return (
    <View style={styles.container}>
      <AcabaAquiHeader title="Perfil" />

      <ScrollAreaView contentContainerStyle={styles.inner}>
        <Text style={styles.title}>Mais opções</Text>
        <AcabaAquiOption text="Informações Pessoais" onPress={() => router.push('/(cliente)/informacoesPessoais')} />
        <AcabaAquiOption text="Sair" onPress={handleLogout} />
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
    width: '100%',
    paddingHorizontal: 20,
    paddingVertical: 8,
    gap: 8,
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 8,
  },
});
