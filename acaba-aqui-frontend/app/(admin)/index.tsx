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
import { catalogApi, CatalogService, ServiceCategory } from '@/lib/catalogApi';

type StatusFilter = 'todos' | 'ativos' | 'inativos';

const emptyForm = { nome: '', descricao: '', categoriaId: 0 };

export default function AdminServicesScreen() {
  const [services, setServices] = useState<CatalogService[]>([]);
  const [categories, setCategories] = useState<ServiceCategory[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState('');
  const [filter, setFilter] = useState<StatusFilter>('ativos');
  const [modalVisible, setModalVisible] = useState(false);
  const [editingService, setEditingService] = useState<CatalogService | null>(null);
  const [form, setForm] = useState(emptyForm);
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  const loadData = useCallback(async (refresh = false) => {
    if (refresh) setRefreshing(true);
    else setLoading(true);
    setError('');
    try {
      const [serviceData, categoryData] = await Promise.all([
        catalogApi.listServices(),
        catalogApi.listCategories(),
      ]);
      setServices(serviceData);
      setCategories(categoryData);
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : 'Não foi possível carregar o catálogo.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useFocusEffect(useCallback(() => {
    void loadData();
  }, [loadData]));

  const openForm = (item?: CatalogService) => {
    setEditingService(item ?? null);
    setForm(item
      ? { nome: item.nome, descricao: item.descricao, categoriaId: item.categoria.id }
      : { ...emptyForm, categoriaId: categories.find((category) => category.ativo)?.id ?? 0 });
    setFormError('');
    setModalVisible(true);
  };

  const saveService = async () => {
    if (!form.nome.trim() || !form.descricao.trim() || !form.categoriaId) {
      setFormError('Preencha o nome, a descrição e selecione uma categoria.');
      return;
    }
    setSaving(true);
    setFormError('');
    try {
      const input = {
        nome: form.nome.trim(),
        descricao: form.descricao.trim(),
        categoriaId: form.categoriaId,
        ...(editingService ? { ativo: editingService.ativo } : {}),
      };
      if (editingService) await catalogApi.updateService(editingService.id, input);
      else await catalogApi.createService(input);
      setModalVisible(false);
      await loadData(true);
    } catch (saveError) {
      setFormError(saveError instanceof Error ? saveError.message : 'Não foi possível salvar o serviço.');
    } finally {
      setSaving(false);
    }
  };

  const toggleService = (item: CatalogService) => {
    const action = item.ativo ? 'desativar' : 'reativar';
    Alert.alert(`${action[0].toUpperCase()}${action.slice(1)} serviço`, `Deseja ${action} "${item.nome}"?`, [
      { text: 'Cancelar', style: 'cancel' },
      {
        text: action[0].toUpperCase() + action.slice(1),
        style: item.ativo ? 'destructive' : 'default',
        onPress: async () => {
          try {
            if (item.ativo) await catalogApi.deactivateService(item.id);
            else await catalogApi.updateService(item.id, {
              nome: item.nome,
              descricao: item.descricao,
              categoriaId: item.categoria.id,
              ativo: true,
            });
            await loadData(true);
          } catch (actionError) {
            setError(actionError instanceof Error ? actionError.message : 'Não foi possível atualizar o serviço.');
          }
        },
      },
    ]);
  };

  const filteredServices = services.filter((item) => {
    if (filter === 'ativos') return item.ativo;
    if (filter === 'inativos') return !item.ativo;
    return true;
  });
  const formCategories = categories.filter((category) =>
    category.ativo || category.id === form.categoriaId);
  const hasAvailableCategory = formCategories.some((category) => category.ativo)
    || (editingService !== null && form.categoriaId === editingService.categoria.id);

  return (
    <View style={styles.screen}>
      <View style={styles.header}>
        <View>
          <Text style={styles.title}>Serviços</Text>
          <Text style={styles.subtitle}>Gerencie o catálogo da plataforma</Text>
        </View>
        <Pressable style={styles.primaryButton} onPress={() => openForm()}>
          <Text style={styles.primaryButtonText}>+ Novo</Text>
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
          data={filteredServices}
          keyExtractor={(item) => String(item.id)}
          contentContainerStyle={styles.list}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => void loadData(true)} />}
          ListEmptyComponent={<Text style={styles.empty}>Nenhum serviço encontrado.</Text>}
          renderItem={({ item }) => (
            <View style={styles.card}>
              <View style={styles.cardHeading}>
                <Text style={styles.cardTitle}>{item.nome}</Text>
                <Text style={[styles.badge, item.ativo ? styles.activeBadge : styles.inactiveBadge]}>
                  {item.ativo ? 'Ativo' : 'Inativo'}
                </Text>
              </View>
              <Text style={styles.category}>{item.categoria.nome}</Text>
              <Text style={styles.description}>{item.descricao}</Text>
              <View style={styles.actions}>
                <Pressable onPress={() => openForm(item)} style={styles.secondaryButton}>
                  <Text style={styles.secondaryButtonText}>Editar</Text>
                </Pressable>
                <Pressable onPress={() => toggleService(item)} style={styles.secondaryButton}>
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
          <Text style={styles.modalTitle}>{editingService ? 'Editar serviço' : 'Novo serviço'}</Text>
          <Text style={styles.label}>Nome</Text>
          <TextInput
            style={styles.input}
            value={form.nome}
            onChangeText={(nome) => setForm((current) => ({ ...current, nome }))}
            maxLength={100}
            placeholder="Ex.: Pintura de paredes"
          />
          <Text style={styles.label}>Descrição</Text>
          <TextInput
            style={[styles.input, styles.multilineInput]}
            value={form.descricao}
            onChangeText={(descricao) => setForm((current) => ({ ...current, descricao }))}
            maxLength={1000}
            multiline
            textAlignVertical="top"
            placeholder="Descreva o serviço"
          />
          <Text style={styles.label}>Categoria</Text>
          <View style={styles.categoryChoices}>
            {formCategories.map((category) => (
              <Pressable
                key={category.id}
                onPress={() => setForm((current) => ({ ...current, categoriaId: category.id }))}
                style={[styles.categoryChoice, form.categoriaId === category.id && styles.categoryChoiceSelected]}
              >
                <Text style={form.categoriaId === category.id ? styles.categoryChoiceTextSelected : styles.categoryChoiceText}>
                  {category.ativo ? category.nome : `${category.nome} (inativa)`}
                </Text>
              </Pressable>
            ))}
            {!hasAvailableCategory && (
              <Text style={styles.empty}>Cadastre uma categoria ativa primeiro.</Text>
            )}
          </View>
          {!!formError && <Text accessibilityRole="alert" style={styles.error}>{formError}</Text>}
          <View style={styles.modalActions}>
            <Pressable style={styles.secondaryButton} onPress={() => setModalVisible(false)}>
              <Text style={styles.secondaryButtonText}>Cancelar</Text>
            </Pressable>
            <Pressable
              style={[styles.primaryButton, (saving || !hasAvailableCategory) && styles.disabledButton]}
              onPress={() => void saveService()}
              disabled={saving || !hasAvailableCategory}
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
  category: { marginTop: 8, color: '#a43434', fontWeight: '600' },
  description: { marginTop: 6, color: '#555', lineHeight: 21 },
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
  categoryChoices: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginTop: 4 },
  categoryChoice: { borderWidth: 1, borderColor: '#d5d5d5', borderRadius: 18, paddingVertical: 8, paddingHorizontal: 12 },
  categoryChoiceSelected: { backgroundColor: '#a43434', borderColor: '#a43434' },
  categoryChoiceText: { color: '#444' },
  categoryChoiceTextSelected: { color: '#fff', fontWeight: '600' },
  modalActions: { flexDirection: 'row', justifyContent: 'flex-end', gap: 10, marginTop: 24 },
  disabledButton: { opacity: 0.55 },
});
