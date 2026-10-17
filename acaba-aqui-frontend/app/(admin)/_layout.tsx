import { Tabs } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

export default function AdminLayout() {
  const insets = useSafeAreaInsets();

  return (
    <Tabs
      initialRouteName="index"
      screenOptions={{
        headerShown: false,
        tabBarShowLabel: true,
        tabBarIcon: () => null,
        tabBarLabelPosition: 'beside-icon',
        tabBarActiveTintColor: '#a43434',
        tabBarInactiveTintColor: '#7a7a7a',
        tabBarIconStyle: {
          display: 'none',
        },
        tabBarItemStyle: {
          justifyContent: 'center',
          alignItems: 'center',
        },
        tabBarLabelStyle: {
          fontSize: 20,
          fontWeight: '600',
        },
        tabBarStyle: {
          height: 64 + insets.bottom,
          paddingBottom: Math.max(insets.bottom, 8),
          paddingTop: 8,
          borderTopWidth: 1,
          borderTopColor: '#eaeaea',

        },
      }}
    >
      <Tabs.Screen name="index" options={{ title: 'Serviços' }} />
      <Tabs.Screen name="categorias" options={{ title: 'Categorias' }} />
      <Tabs.Screen name="perfil" options={{ title: 'Perfil' }} />
      <Tabs.Screen name="informacoesPessoais" options={{ href: null }} />
    </Tabs>
  );
}
