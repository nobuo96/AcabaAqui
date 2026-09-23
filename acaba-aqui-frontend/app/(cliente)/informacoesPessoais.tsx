import { ScrollView, StyleSheet, TextInput, View } from 'react-native';
import { useRouter } from 'expo-router';
import { AcabaAquiHeader } from '@/components/AcabaAquiHeader';
import { AcabaAquiButton } from '@/components/AcabaAquiButton';
import { Text } from '@/components/Themed';

export default function InformacoesPessoaisScreen() {
  const router = useRouter();

  const handleBack = () => {
    router.replace('/(cliente)/perfil');
  };

  return (
    <View style={styles.container}>
      <AcabaAquiHeader
        title="Informações Pessoais"
        backButton
        onBackPress={handleBack}
      />

      <ScrollView style={styles.inner}>
        <Text style={styles.sectionLabel}>Dados pessoais</Text>

        <View style={styles.fieldGroup}>
          <Text style={styles.label}>Nome</Text>
          <TextInput style={styles.input} value="Victor Hugo" placeholder="Nome" />
        </View>

        <View style={styles.fieldGroup}>
          <Text style={styles.label}>Apelido</Text>
          <TextInput style={styles.input} value="Victor" placeholder="Apelido" />
        </View>

        <View style={styles.fieldGroup}>
          <Text style={styles.label}>Data de nascimento</Text>
          <TextInput style={styles.input} value="12/04/1997" placeholder="Data de nascimento" />
        </View>

        <View style={styles.fieldGroup}>
          <Text style={styles.label}>Gênero</Text>
          <TextInput style={styles.input} value="Masculino" placeholder="Gênero" />
        </View>

        <View style={styles.fieldGroup}>
          <Text style={styles.label}>Telefone</Text>
          <TextInput style={styles.input} value="(11) 99999-9999" placeholder="Telefone" keyboardType="phone-pad" />
        </View>

        <View style={styles.fieldGroup}>
          <Text style={styles.label}>E-mail</Text>
          <TextInput
            style={[styles.input, styles.disabledInput]}
            value="victor@email.com"
            editable={false}
          />
        </View>

        <AcabaAquiButton text="Salvar" onPress={handleBack} />
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  inner: {
    width: '100%',
    paddingHorizontal: 20,
    paddingVertical: 16,
  },
  sectionLabel: {
    fontSize: 20,
    fontWeight: '700',
    marginBottom: 12,
  },
  fieldGroup: {
    marginBottom: 18,
  },
  label: {
    fontSize: 16,
    marginBottom: 8,
    color: '#333',
    fontWeight: '600',
  },
  input: {
    borderWidth: 1,
    borderColor: '#d9d9d9',
    borderRadius: 12,
    paddingHorizontal: 12,
    paddingVertical: 12,
    fontSize: 16,
    backgroundColor: '#fff',
  },
  disabledInput: {
    backgroundColor: '#f3f3f3',
    color: '#666',
  },
});
