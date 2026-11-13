import { useRouter } from 'expo-router';

import { ProviderCoverageScreen } from '@/components/ProviderCoverageScreen';

export default function CoverageRoute() {
  const router = useRouter();

  return <ProviderCoverageScreen onBack={() => router.replace('/(prestador)')} />;
}
