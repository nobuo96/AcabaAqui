import { Pressable, StyleSheet, Text, View } from 'react-native';

interface AcabaAquiHeaderProps {
    title?: string;
    backButton?: boolean;
    onBackPress?: () => void;
}

export function AcabaAquiHeader({ title, backButton = false, onBackPress }: AcabaAquiHeaderProps) {
    return (
        <View style={styles.headerContainer}>
            {backButton ? (
                <Pressable onPress={onBackPress} style={styles.backButton}>
                    <Text style={styles.buttonText}>←</Text>
                </Pressable>
            ) : (
                <View style={styles.placeholder} />
            )}

            <Text style={styles.title}>{title ?? 'Título'}</Text>

            <View style={styles.placeholder} />
        </View>
    );
}

const styles = StyleSheet.create({
    headerContainer: {
        width: '100%',
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: 20,
        paddingVertical: 16,
        backgroundColor: '#fff',
        borderBottomWidth: 1,
        borderBottomColor: '#eaeaea',
    },
    backButton: {
        width: 32,
        height: 32,
        alignItems: 'center',
        justifyContent: 'center',
    },
    buttonText: {
        fontSize: 22,
        color: '#a43434',
        fontWeight: '700',
    },
    title: {
        fontSize: 18,
        fontWeight: '700',
        color: '#1f1f1f',
        textAlign: 'center',
        flex: 1,
    },
    placeholder: {
        width: 32,
        height: 32,
    },
});