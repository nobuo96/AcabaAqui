import { Tabs } from 'expo-router';

export default function ClienteLayout() {
  return (
    <Tabs
      initialRouteName="index"
      screenOptions={{
        headerShown: false,
        tabBarShowLabel: true,
        tabBarActiveTintColor: '#a43434',
        tabBarInactiveTintColor: '#7a7a7a',
        tabBarLabelStyle: {
          fontSize: 24,
          fontWeight: '600',
        },
        tabBarStyle: {
          height: 64,
          paddingBottom: 8,
          paddingTop: 8,
          borderTopWidth: 1,
          borderTopColor: '#eaeaea',
        },
      }}
    >
      <Tabs.Screen
        name="index"
        options={{
          title: 'Home',
        }}
      />
      <Tabs.Screen
        name="perfil"
        options={{
          title: 'Perfil',
        }}
      />
            <Tabs.Screen
        name="informacoesPessoais"
        options={{
          href:null,
        }}
      />
    </Tabs>
  );
}
