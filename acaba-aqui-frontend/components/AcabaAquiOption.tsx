import { Pressable, Text } from 'react-native';
import { StyleSheet } from 'react-native';

export function AcabaAquiOption({ text, onPress }: { text: string; onPress: () => void }) {
    return (
        <Pressable style={styles.optionContainer} onPress={onPress}>
            <Text style={styles.optionText}>{text}</Text>
        </Pressable>
    );
}

const styles = StyleSheet.create({
    optionContainer: {
        padding: 10,
        marginVertical: 5,
        backgroundColor: '#FFF',
        borderRadius: 12,
        borderWidth: 2,
        borderColor: '#eaeaea',
    },
    optionText: {
        fontSize: 20,
    },
});