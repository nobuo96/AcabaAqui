import { PropsWithChildren } from 'react';
import {
  KeyboardAvoidingView,
  Platform,
  ScrollView,
  type ScrollViewProps,
  type StyleProp,
  StyleSheet,
  type ViewStyle,
} from 'react-native';

type ScrollAreaViewProps = PropsWithChildren<
  Omit<ScrollViewProps, 'style'> & {
    style?: StyleProp<ViewStyle>;
    keyboardVerticalOffset?: number;
  }
>;

export function ScrollAreaView({
  children,
  keyboardVerticalOffset = 0,
  style,
  ...scrollViewProps
}: ScrollAreaViewProps) {
  return (
    <KeyboardAvoidingView
      style={[styles.container, style]}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      keyboardVerticalOffset={keyboardVerticalOffset}
    >
      <ScrollView
        {...scrollViewProps}
        style={styles.scrollView}
        keyboardShouldPersistTaps={scrollViewProps.keyboardShouldPersistTaps ?? 'handled'}
      >
        {children}
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    width: '100%',
  },
  scrollView: {
    flex: 1,
  },
});
