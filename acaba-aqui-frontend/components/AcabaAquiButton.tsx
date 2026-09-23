import { Pressable, StyleSheet, Text, View } from 'react-native';

interface AcabaAquiButtonProps {
    text: string;
    onPress: () => void;
}

export function AcabaAquiButton( {text, onPress}: AcabaAquiButtonProps ) {
    return (
        <View style={styles.buttonContainer}>
        <Pressable onPress={onPress}>
            <View>
                <Text style={styles.buttonText}>
                    {text}
                </Text>
            </View>
        </Pressable>   
        </View>         
    );
}

const styles = StyleSheet.create({
    buttonContainer: {
        backgroundColor: '#a43434',
        paddingVertical: 12,
        paddingHorizontal: 8,
        borderRadius: 16,
        // width: '100%',
        // alignItems: 'center',
        // justifyContent: 'center',
    },
    buttonText: {
        color: '#FFFFFF',
        fontSize: 20,
        textAlign: 'center',
    },
});