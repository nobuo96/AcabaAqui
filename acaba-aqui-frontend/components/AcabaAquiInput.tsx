import { StyleSheet, Text, TextInput, View } from 'react-native';

interface AcabaAquiInputProps {
    placeholder?: string;
    label?: string;
    error?: string;
    accessibilityHint?: string;
    accessibilityLabel?: string;
    value?: string;
    onChangeText?: (text: string) => void;
    editable?: boolean;
    maxLength?: number;
    multiline?: boolean;
    numberOfLines?: number;
    onBlur?: () => void;
    onSubmitEditing?: () => void;
    secureTextEntry?: boolean;
    keyboardType?: 'default' | 'email-address' | 'numeric' | 'phone-pad' | 'decimal-pad';
    autoCapitalize?: 'none' | 'sentences' | 'words' | 'characters';
}

export function AcabaAquiInput({
    placeholder,
    label,
    error,
    accessibilityHint,
    accessibilityLabel,
    value,
    onChangeText,
    editable = true,
    maxLength,
    multiline = false,
    numberOfLines,
    onBlur,
    onSubmitEditing,
    secureTextEntry = false,
    keyboardType = 'default',
    autoCapitalize = 'sentences',
}: AcabaAquiInputProps) {
    return (
        <View style={styles.field}>
            {label ? <Text nativeID={`${accessibilityLabel ?? label}-label`} style={styles.label}>{label}</Text> : null}
            <View style={[styles.inputContainer, error && styles.inputError, !editable && styles.disabled]}>
            <TextInput
                style={styles.input}
                placeholder={placeholder}
                placeholderTextColor="#8a8a8a"
                value={value}
                onChangeText={onChangeText}
                editable={editable}
                maxLength={maxLength}
                multiline={multiline}
                numberOfLines={numberOfLines}
                onBlur={onBlur}
                onSubmitEditing={onSubmitEditing}
                accessibilityLabel={accessibilityLabel ?? label ?? placeholder}
                accessibilityHint={accessibilityHint}
                accessibilityState={{ disabled: !editable }}
                accessibilityValue={value ? { text: value } : undefined}
                secureTextEntry={secureTextEntry}
                keyboardType={keyboardType}
                autoCapitalize={autoCapitalize}
            />
            </View>
            {error ? <Text nativeID={`${accessibilityLabel ?? label}-error`} style={styles.errorText}>{error}</Text> : null}
        </View>
    );
}

const styles = StyleSheet.create({
    field: { width: '100%', gap: 6 },
    label: { color: '#444', fontSize: 15, fontWeight: '600' },
    inputContainer: {
        width: '100%',
        backgroundColor: '#f5f5f5',
        borderWidth: 2,
        borderColor: '#d9d9d9',
        borderRadius: 12,
        paddingHorizontal: 12,
        paddingVertical: 10,

    },
    input: {
        fontSize: 16,
        color: '#1f1f1f',
    },
    inputError: { borderColor: '#a43434' },
    disabled: { opacity: 0.55 },
    errorText: { color: '#a43434', fontSize: 13 },
});