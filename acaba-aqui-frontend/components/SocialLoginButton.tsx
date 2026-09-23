import { Pressable, StyleSheet, Text, View } from 'react-native';

interface SocialLoginButtonProps {
  provider: 'google' | 'facebook';
  text: string;
  onPress: () => void;
}

const providerConfig = {
  google: {
    backgroundColor: '#FFFFFF',
    borderColor: '#DADCE0',
    iconBackground: '#FFFFFF',
    iconText: 'G',
    iconTextColor: '#EA4335',
  },
  facebook: {
    backgroundColor: '#1877F2',
    borderColor: '#1877F2',
    iconBackground: '#FFFFFF',
    iconText: 'f',
    iconTextColor: '#1877F2',
  },
};

export function SocialLoginButton({ provider, text, onPress }: SocialLoginButtonProps) {
  const config = providerConfig[provider];

  return (
    <Pressable
      onPress={onPress}
      style={[styles.button, { backgroundColor: config.backgroundColor, borderColor: config.borderColor }]}
    >
      <View style={[styles.iconContainer, { backgroundColor: config.iconBackground }]}>
        <Text style={[styles.iconText, { color: config.iconTextColor }]}>{config.iconText}</Text>
      </View>
      <Text style={[styles.text, provider === 'facebook' ? styles.facebookText : styles.googleText]}>
        {text}
      </Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  button: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderRadius: 14,
    paddingVertical: 12,
    paddingHorizontal: 16,
    width: '100%',
    minHeight: 50,
  },
  iconContainer: {
    width: 28,
    height: 28,
    borderRadius: 14,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 12,
  },
  iconText: {
    fontSize: 18,
    fontWeight: 'bold',
  },
  text: {
    fontSize: 16,
    fontWeight: '600',
  },
  googleText: {
    color: '#1F1F1F',
  },
  facebookText: {
    color: '#FFFFFF',
  },
});
