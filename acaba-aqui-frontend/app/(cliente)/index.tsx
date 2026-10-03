import { StyleSheet } from 'react-native';

import { Text, View } from '@/components/Themed';
import { useCurrentUser } from '@/lib/useCurrentUser';

export default function HomeScreen() {
  const { nome, carregando, erro } = useCurrentUser();

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Tela Inicial Cliente</Text>
      {carregando ? <Text>Carregando usuário...</Text> : null}
      {!carregando && !erro ? <Text style={styles.name}>Olá, {nome}</Text> : null}
      {erro ? <Text style={styles.error}>{erro}</Text> : null}
      <View style={styles.separator} lightColor="#eee" darkColor="rgba(255,255,255,0.1)" />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  title: {
    fontSize: 20,
    fontWeight: 'bold',
  },
  name: {
    fontSize: 18,
    marginTop: 12,
  },
  error: {
    color: '#a43434',
    marginTop: 12,
  },
  separator: {
    marginVertical: 30,
    height: 1,
    width: '80%',
  },
});
