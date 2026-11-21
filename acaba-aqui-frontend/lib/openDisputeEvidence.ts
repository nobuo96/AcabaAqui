import { Linking, Platform } from 'react-native';
import * as FileSystem from 'expo-file-system/legacy';
import * as Sharing from 'expo-sharing';

import { serviceDisputeApi, type ServiceDisputeEvidence } from './communicationApi';

export async function openDisputeEvidence(disputeId: number, evidence: ServiceDisputeEvidence) {
  const blob = await serviceDisputeApi.downloadEvidence(disputeId, evidence.id);
  if (Platform.OS === 'web') {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = evidence.filename;
    link.rel = 'noopener';
    link.click();
    window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
    return;
  }
  const base64 = await new Promise<string>((resolve, reject) => {
    const reader = new FileReader();
    reader.onerror = () => reject(new Error('Não foi possível preparar o arquivo.'));
    reader.onload = () => {
      const result = String(reader.result);
      resolve(result.slice(result.indexOf(',') + 1));
    };
    reader.readAsDataURL(blob);
  });
  if (!FileSystem.cacheDirectory) {
    throw new Error('O armazenamento temporário não está disponível.');
  }
  const safeName = evidence.filename.replace(/[^\w.-]/g, '_');
  const uri = `${FileSystem.cacheDirectory}${Date.now()}-${safeName}`;
  await FileSystem.writeAsStringAsync(uri, base64, { encoding: FileSystem.EncodingType.Base64 });
  if (await Sharing.isAvailableAsync()) {
    await Sharing.shareAsync(uri, { mimeType: evidence.contentType, dialogTitle: evidence.filename });
  } else {
    await Linking.openURL(uri);
  }
}
