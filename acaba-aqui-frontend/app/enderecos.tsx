import { useRouter } from 'expo-router';
import { AddressManagement } from '@/components/AddressManagement';

export default function EnderecosScreen() {
  const router = useRouter();
  return <AddressManagement onBack={() => router.back()} />;
}
