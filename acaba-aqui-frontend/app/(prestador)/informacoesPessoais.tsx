import { useRouter } from 'expo-router';
import { InformacoesPessoaisForm } from '@/components/InformacoesPessoaisForm';

export default function InformacoesPessoaisScreen() {
  const router = useRouter();

  return <InformacoesPessoaisForm onBack={() => router.replace('/(prestador)/perfil')} />;
}
