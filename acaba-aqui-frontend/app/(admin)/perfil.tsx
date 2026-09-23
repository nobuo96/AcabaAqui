import { ScrollView, StyleSheet } from 'react-native';

import { Text, View } from '@/components/Themed';
import { useRouter } from 'expo-router';
import { AcabaAquiHeader } from '@/components/AcabaAquiHeader';
import { AcabaAquiOption } from '@/components/AcabaAquiOption';

export default function PerfilScreen() {
  const router = useRouter();

  const handleLogout = () => {
    router.replace('/(auth)/login');
  };

  return (
    <View style={styles.container}>
      <AcabaAquiHeader title="Perfil" />

      <ScrollView style={styles.inner}>
        <Text style={styles.title}>Mais opções</Text>
        <AcabaAquiOption text="Informações Pessoais" onPress={() => router.push('/(cliente)/informacoesPessoais')} />
        <AcabaAquiOption text="Sair" onPress={handleLogout} />
      </ScrollView>
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
