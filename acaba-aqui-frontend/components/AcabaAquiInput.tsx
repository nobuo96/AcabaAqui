import { StyleSheet, TextInput, View } from 'react-native';

interface AcabaAquiInputProps {
    placeholder?: string;
    value?: string;
    onChangeText?: (text: string) => void;
    secureTextEntry?: boolean;
    keyboardType?: 'default' | 'email-address' | 'numeric' | 'phone-pad';
    autoCapitalize?: 'none' | 'sentences' | 'words' | 'characters';
}

export function AcabaAquiInput({
    placeholder,
    value,
    onChangeText,
    secureTextEntry = false,
    keyboardType = 'default',
    autoCapitalize = 'sentences',
}: AcabaAquiInputProps) {
    return (
        <View style={styles.inputContainer}>
            <TextInput
                style={styles.input}
                placeholder={placeholder}
                placeholderTextColor="#8a8a8a"
                value={value}
                onChangeText={onChangeText}
                secureTextEntry={secureTextEntry}
                keyboardType={keyboardType}
                autoCapitalize={autoCapitalize}
            />
        </View>
    );
}

const styles = StyleSheet.create({
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
});