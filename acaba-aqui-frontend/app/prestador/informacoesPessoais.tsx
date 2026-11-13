import { useRouter } from 'expo-router';

import { InformacoesPessoaisForm } from '@/components/InformacoesPessoaisForm';

export default function InformacoesPessoaisRoute() {
  const router = useRouter();

  return (
    <InformacoesPessoaisForm
      onBack={() => router.replace('/(prestador)/perfil')}
      onManageAddresses={() => router.push('/enderecos')}
    />
  );
}
