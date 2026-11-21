import { Tabs } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { SymbolView } from 'expo-symbols';
import { StatusBar } from 'expo-status-bar';

export default function ClienteLayout() {
  const insets = useSafeAreaInsets();

  return (
    <>
      <StatusBar style="dark" />
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
          fontSize: 11,
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
        sceneStyle: {
          paddingTop: insets.top,
        },
      }}
      >
      <Tabs.Screen
        name="index"
        options={{
          title: 'Home',
          tabBarIcon: ({ color }) => <SymbolView name="house.fill" tintColor={color} size={21} />,
        }}
      />
      <Tabs.Screen
        name="solicitacoes"
        options={{
          title: 'Pedidos',
          tabBarIcon: ({ color }) => <SymbolView name="list.bullet" tintColor={color} size={21} />,
        }}
      />
      <Tabs.Screen
        name="servicos"
        options={{
          title: 'Serviços',
          tabBarIcon: ({ color }) => <SymbolView name="wrench.and.screwdriver" tintColor={color} size={21} />,
        }}
      />
      <Tabs.Screen
        name="conversas"
        options={{
          title: 'Conversas',
          tabBarIcon: ({ color }) => <SymbolView name="bubble.left.and.bubble.right" tintColor={color} size={21} />,
        }}
      />
      <Tabs.Screen
        name="perfil"
        options={{
          title: 'Perfil',
          tabBarIcon: ({ color }) => <SymbolView name="person.fill" tintColor={color} size={21} />,
        }}
      />
      </Tabs>
    </>
  );
}
