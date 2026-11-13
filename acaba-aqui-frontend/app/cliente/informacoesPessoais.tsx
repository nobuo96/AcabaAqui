import { useRouter } from 'expo-router';

import { InformacoesPessoaisForm } from '@/components/InformacoesPessoaisForm';

export default function InformacoesPessoaisRoute() {
  const router = useRouter();

  return (
    <InformacoesPessoaisForm
      onBack={() => router.replace('/(cliente)/perfil')}
      onManageAddresses={() => router.push('/enderecos')}
    />
  );
}
