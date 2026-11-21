import { Tabs } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { SymbolView } from 'expo-symbols';

export const unstable_settings = {
  anchor: 'index',
};

export default function AdminLayout() {
  const insets = useSafeAreaInsets();

  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarShowLabel: true,
        tabBarLabelPosition: 'below-icon',
        tabBarActiveTintColor: '#a43434',
        tabBarInactiveTintColor: '#7a7a7a',
        tabBarItemStyle: {
          justifyContent: 'center',
          alignItems: 'center',
        },
        tabBarLabelStyle: {
          fontSize: 10,
          fontWeight: '600',
          marginBottom: 2,
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
      <Tabs.Screen
        name="home"
        options={{
          title: 'Painel administrativo',
          tabBarLabel: 'Painel',
          tabBarIcon: ({ color }) => <SymbolView name="house.fill" tintColor={color} size={20} />,
        }}
      />
      <Tabs.Screen
        name="index"
        options={{
          title: 'Serviços',
          tabBarIcon: ({ color }) => <SymbolView name="wrench.and.screwdriver" tintColor={color} size={20} />,
        }}
      />
      <Tabs.Screen
        name="repasses"
        options={{
          title: 'Repasses Pix',
          tabBarLabel: 'Repasses',
          tabBarIcon: ({ color }) => <SymbolView name="arrow.left.arrow.right" tintColor={color} size={20} />,
        }}
      />
      <Tabs.Screen
        name="disputas"
        options={{
          title: 'Disputas',
          tabBarIcon: ({ color }) => <SymbolView name="exclamationmark.bubble" tintColor={color} size={20} />,
        }}
      />
      <Tabs.Screen
        name="categorias"
        options={{
          title: 'Categorias',
          tabBarIcon: ({ color }) => <SymbolView name="tag" tintColor={color} size={20} />,
        }}
      />
      <Tabs.Screen
        name="perfil"
        options={{
          title: 'Perfil',
          tabBarIcon: ({ color }) => <SymbolView name="person.fill" tintColor={color} size={20} />,
        }}
      />
    </Tabs>
  );
}
