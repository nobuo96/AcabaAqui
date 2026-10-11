import type { ConfigContext, ExpoConfig } from 'expo/config';

export default ({ config }: ConfigContext): ExpoConfig => {
  const facebookAppId = process.env.EXPO_PUBLIC_FACEBOOK_APP_ID?.trim();
  const existingSchemes = Array.isArray(config.scheme)
    ? config.scheme
    : config.scheme
      ? [config.scheme]
      : [];
  const schemes = new Set(existingSchemes);
  if (facebookAppId) {
    schemes.add(`fb${facebookAppId}`);
  }

  return {
    ...config,
    name: config.name ?? 'acaba-aqui-frontend',
    slug: config.slug ?? 'acaba-aqui-frontend',
    scheme: [...schemes],
    ios: {
      ...config.ios,
      bundleIdentifier: config.ios?.bundleIdentifier ?? 'com.acabaaqui.app.dev',
    },
    android: {
      ...config.android,
      package: config.android?.package ?? 'com.acabaaqui.app.dev',
    },
  };
};
