import { useRouter } from 'expo-router';

import { AlterarSenhaForm } from '@/components/AlterarSenhaForm';

export default function AlterarSenhaRoute() {
  const router = useRouter();

  return <AlterarSenhaForm onBack={() => router.replace('/(prestador)/perfil')} />;
}
