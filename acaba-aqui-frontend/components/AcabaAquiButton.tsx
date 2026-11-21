import { Pressable, StyleSheet, Text, View } from 'react-native';

interface AcabaAquiButtonProps {
    text: string;
    onPress: () => void;
    disabled?: boolean;
    loading?: boolean;
    variant?: 'primary' | 'secondary' | 'danger';
    accessibilityLabel?: string;
}

export function AcabaAquiButton({
    text,
    onPress,
    disabled = false,
    loading = false,
    variant = 'primary',
    accessibilityLabel,
}: AcabaAquiButtonProps) {
    const unavailable = disabled || loading;
    return (
        <Pressable
            accessibilityRole="button"
            accessibilityLabel={accessibilityLabel ?? text}
            accessibilityState={{ disabled: unavailable, busy: loading }}
            disabled={unavailable}
            onPress={onPress}
            style={({ pressed }) => [
                styles.buttonContainer,
                variant === 'secondary' && styles.secondary,
                variant === 'danger' && styles.danger,
                unavailable && styles.disabled,
                pressed && !unavailable && styles.pressed,
            ]}
        >
            <View style={styles.labelContainer}>
                <Text style={[styles.buttonText, variant === 'secondary' && styles.secondaryText]}>
                    {loading ? 'Aguarde...' : text}
                </Text>
            </View>
        </Pressable>
    );
}

const styles = StyleSheet.create({
    buttonContainer: {
        backgroundColor: '#a43434',
        paddingVertical: 12,
        paddingHorizontal: 8,
        borderRadius: 16,
        minHeight: 48,
        justifyContent: 'center',
    },
    secondary: {
        backgroundColor: '#fff',
        borderWidth: 1,
        borderColor: '#a43434',
    },
    danger: {
        backgroundColor: '#7e2727',
    },
    disabled: {
        opacity: 0.55,
    },
    pressed: {
        opacity: 0.82,
    },
    labelContainer: {
        backgroundColor: 'transparent',
        alignItems: 'center',
    },
    buttonText: {
        color: '#FFFFFF',
        fontSize: 20,
        textAlign: 'center',
        fontWeight: '700',
    },
    secondaryText: {
        color: '#a43434',
    },
});