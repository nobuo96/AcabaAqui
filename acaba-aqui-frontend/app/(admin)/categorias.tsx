import { useCallback, useState } from 'react';
import {
  ActivityIndicator,
  Alert,
  FlatList,
  Modal,
  Pressable,
  RefreshControl,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { useFocusEffect } from 'expo-router';
import { catalogApi, ServiceCategory } from '@/lib/catalogApi';

type StatusFilter = 'todos' | 'ativos' | 'inativos';

export default function AdminCategoriesScreen() {
  const [categories, setCategories] = useState<ServiceCategory[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState('');
  const [filter, setFilter] = useState<StatusFilter>('ativos');
  const [modalVisible, setModalVisible] = useState(false);
  const [editingCategory, setEditingCategory] = useState<ServiceCategory | null>(null);
  const [nome, setNome] = useState('');
  const [descricao, setDescricao] = useState('');
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  const loadCategories = useCallback(async (refresh = false) => {
    if (refresh) setRefreshing(true);
    else setLoading(true);
    setError('');
    try {
      setCategories(await catalogApi.listCategories());
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : 'Não foi possível carregar as categorias.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useFocusEffect(useCallback(() => {
    void loadCategories();
  }, [loadCategories]));

  const openForm = (category?: ServiceCategory) => {
    setEditingCategory(category ?? null);
    setNome(category?.nome ?? '');
    setDescricao(category?.descricao ?? '');
    setFormError('');
    setModalVisible(true);
  };

  const saveCategory = async () => {
    if (!nome.trim()) {
      setFormError('Informe o nome da categoria.');
      return;
    }
    setSaving(true);
    setFormError('');
    try {
      const input = {
        nome: nome.trim(),
        descricao: descricao.trim(),
        ...(editingCategory ? { ativo: editingCategory.ativo } : {}),
      };
      if (editingCategory) await catalogApi.updateCategory(editingCategory.id, input);
      else await catalogApi.createCategory(input);
      setModalVisible(false);
      await loadCategories(true);
    } catch (saveError) {
      setFormError(saveError instanceof Error ? saveError.message : 'Não foi possível salvar a categoria.');
    } finally {
      setSaving(false);
    }
  };

  const toggleCategory = (category: ServiceCategory) => {
    const action = category.ativo ? 'desativar' : 'reativar';
    Alert.alert(`${action[0].toUpperCase()}${action.slice(1)} categoria`, `Deseja ${action} "${category.nome}"?`, [
      { text: 'Cancelar', style: 'cancel' },
      {
        text: action[0].toUpperCase() + action.slice(1),
        style: category.ativo ? 'destructive' : 'default',
        onPress: async () => {
          try {
            if (category.ativo) await catalogApi.deactivateCategory(category.id);
            else await catalogApi.updateCategory(category.id, {
              nome: category.nome,
              descricao: category.descricao ?? '',
              ativo: true,
            });
            await loadCategories(true);
          } catch (actionError) {
            setError(actionError instanceof Error ? actionError.message : 'Não foi possível atualizar a categoria.');
          }
        },
      },
    ]);
  };

  const filteredCategories = categories.filter((category) => {
    if (filter === 'ativos') return category.ativo;
    if (filter === 'inativos') return !category.ativo;
    return true;
  });

  return (
    <View style={styles.screen}>
      <View style={styles.header}>
        <View>
          <Text style={styles.title}>Categorias</Text>
          <Text style={styles.subtitle}>Opções controladas para classificar serviços</Text>
        </View>
        <Pressable style={styles.primaryButton} onPress={() => openForm()}>
          <Text style={styles.primaryButtonText}>+ Nova</Text>
        </Pressable>
      </View>

      <View style={styles.filters}>
        {(['ativos', 'inativos', 'todos'] as StatusFilter[]).map((value) => (
          <Pressable
            key={value}
            onPress={() => setFilter(value)}
            style={[styles.filterButton, filter === value && styles.filterSelected]}
          >
            <Text style={[styles.filterText, filter === value && styles.filterTextSelected]}>
              {value[0].toUpperCase() + value.slice(1)}
            </Text>
          </Pressable>
        ))}
      </View>

      {!!error && <Text accessibilityRole="alert" style={styles.error}>{error}</Text>}
      {loading ? (
        <ActivityIndicator style={styles.loader} color="#a43434" />
      ) : (
        <FlatList
          data={filteredCategories}
          keyExtractor={(item) => String(item.id)}
          contentContainerStyle={styles.list}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => void loadCategories(true)} />}
          ListEmptyComponent={<Text style={styles.empty}>Nenhuma categoria encontrada.</Text>}
          renderItem={({ item }) => (
            <View style={styles.card}>
              <View style={styles.cardHeading}>
                <Text style={styles.cardTitle}>{item.nome}</Text>
                <Text style={[styles.badge, item.ativo ? styles.activeBadge : styles.inactiveBadge]}>
                  {item.ativo ? 'Ativa' : 'Inativa'}
                </Text>
              </View>
              {!!item.descricao && <Text style={styles.description}>{item.descricao}</Text>}
              <View style={styles.actions}>
                <Pressable onPress={() => openForm(item)} style={styles.secondaryButton}>
                  <Text style={styles.secondaryButtonText}>Editar</Text>
                </Pressable>
                <Pressable onPress={() => toggleCategory(item)} style={styles.secondaryButton}>
                  <Text style={item.ativo ? styles.dangerText : styles.secondaryButtonText}>
                    {item.ativo ? 'Desativar' : 'Reativar'}
                  </Text>
                </Pressable>
              </View>
            </View>
          )}
        />
      )}

      <Modal visible={modalVisible} animationType="slide" onRequestClose={() => setModalVisible(false)}>
        <View style={styles.modal}>
          <Text style={styles.modalTitle}>{editingCategory ? 'Editar categoria' : 'Nova categoria'}</Text>
          <Text style={styles.label}>Nome</Text>
          <TextInput
            style={styles.input}
            value={nome}
            onChangeText={setNome}
            maxLength={80}
            placeholder="Ex.: Pintura"
          />
          <Text style={styles.label}>Descrição (opcional)</Text>
          <TextInput
            style={[styles.input, styles.multilineInput]}
            value={descricao}
            onChangeText={setDescricao}
            maxLength={255}
            multiline
            textAlignVertical="top"
            placeholder="Descreva esta categoria"
          />
          {!!formError && <Text accessibilityRole="alert" style={styles.error}>{formError}</Text>}
          <View style={styles.modalActions}>
            <Pressable style={styles.secondaryButton} onPress={() => setModalVisible(false)}>
              <Text style={styles.secondaryButtonText}>Cancelar</Text>
            </Pressable>
            <Pressable
              style={[styles.primaryButton, saving && styles.disabledButton]}
              onPress={() => void saveCategory()}
              disabled={saving}
            >
              <Text style={styles.primaryButtonText}>{saving ? 'Salvando...' : 'Salvar'}</Text>
            </Pressable>
          </View>
        </View>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: '#f7f7f7' },
  header: { padding: 20, backgroundColor: '#fff', flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  title: { fontSize: 25, fontWeight: '700', color: '#1f1f1f' },
  subtitle: { marginTop: 4, color: '#6b6b6b' },
  primaryButton: { backgroundColor: '#a43434', paddingVertical: 11, paddingHorizontal: 16, borderRadius: 10 },
  primaryButtonText: { color: '#fff', fontWeight: '700' },
  filters: { flexDirection: 'row', gap: 8, paddingHorizontal: 16, paddingVertical: 12 },
  filterButton: { paddingVertical: 8, paddingHorizontal: 14, borderRadius: 20, backgroundColor: '#e9e9e9' },
  filterSelected: { backgroundColor: '#a43434' },
  filterText: { color: '#444', fontWeight: '600' },
  filterTextSelected: { color: '#fff' },
  list: { padding: 16, gap: 12, flexGrow: 1 },
  card: { backgroundColor: '#fff', padding: 16, borderRadius: 12, borderWidth: 1, borderColor: '#e8e8e8' },
  cardHeading: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 8 },
  cardTitle: { flex: 1, fontSize: 18, fontWeight: '700', color: '#222' },
  badge: { paddingVertical: 4, paddingHorizontal: 9, borderRadius: 12, overflow: 'hidden', fontSize: 12, fontWeight: '600' },
  activeBadge: { backgroundColor: '#e5f4e8', color: '#246b31' },
  inactiveBadge: { backgroundColor: '#eee', color: '#666' },
  description: { marginTop: 8, color: '#555', lineHeight: 21 },
  actions: { flexDirection: 'row', gap: 10, marginTop: 14 },
  secondaryButton: { borderWidth: 1, borderColor: '#d5d5d5', borderRadius: 9, paddingVertical: 9, paddingHorizontal: 13 },
  secondaryButtonText: { color: '#333', fontWeight: '600' },
  dangerText: { color: '#a43434', fontWeight: '600' },
  empty: { padding: 24, textAlign: 'center', color: '#666' },
  error: { marginHorizontal: 16, marginTop: 8, color: '#a43434' },
  loader: { marginTop: 40 },
  modal: { flex: 1, padding: 22, paddingTop: 48, backgroundColor: '#fff' },
  modalTitle: { fontSize: 23, fontWeight: '700', marginBottom: 20, color: '#222' },
  label: { marginTop: 12, marginBottom: 6, fontWeight: '600', color: '#333' },
  input: { width: '100%', borderWidth: 1, borderColor: '#d9d9d9', borderRadius: 9, padding: 12, fontSize: 16, color: '#222' },
  multilineInput: { minHeight: 100 },
  modalActions: { flexDirection: 'row', justifyContent: 'flex-end', gap: 10, marginTop: 24 },
  disabledButton: { opacity: 0.55 },
});
