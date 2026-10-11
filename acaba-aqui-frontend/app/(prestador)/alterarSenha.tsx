import { useRouter } from 'expo-router';
import { AlterarSenhaForm } from '@/components/AlterarSenhaForm';

export default function AlterarSenhaScreen() {
  const router = useRouter();

  return <AlterarSenhaForm onBack={() => router.replace('/(prestador)/perfil')} />;
}