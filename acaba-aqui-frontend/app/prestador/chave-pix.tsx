import { useRouter } from 'expo-router';

import { ProviderPixKeyScreen } from '@/components/ProviderPixKeyScreen';

export default function ProviderPixKeyRoute() {
  const router = useRouter();

  return (
    <ProviderPixKeyScreen
      onBack={() => router.replace('/(prestador)/perfil')}
    />
  );
}
