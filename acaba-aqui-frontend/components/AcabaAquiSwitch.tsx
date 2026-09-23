import { Pressable, StyleSheet, Text, View } from 'react-native';

interface AcabaAquiSwitchProps {
  options: string[];
  value: string;
  onChange: (value: string) => void;
}

export function AcabaAquiSwitch({ options, value, onChange }: AcabaAquiSwitchProps) {
  return (
    <View style={styles.container}>
      {options.map((option) => {
        const isSelected = option === value;

        return (
          <Pressable
            key={option}
            onPress={() => onChange(option)}
            style={[
              styles.option,
              isSelected && styles.selectedOption,
            ]}
          >
            <Text
              style={[
                styles.optionText,
                isSelected && styles.selectedOptionText,
              ]}
            >
              {option}
            </Text>
          </Pressable>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    width: '100%',
    backgroundColor: '#f3f3f3',
    borderRadius: 18,
    padding: 6,
    borderWidth: 1,
    borderColor: '#e7e7e7',
    gap: 6,
  },
  option: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 10,
    borderRadius: 12,
  },
  selectedOption: {
    backgroundColor: '#ffffff',
    shadowColor: '#000000',
    shadowOffset: { width: 0, height: 3 },
    shadowOpacity: 0.08,
    shadowRadius: 8,
    elevation: 3,
  },
  optionText: {
    color: '#6a6a6a',
    fontSize: 14,
    fontWeight: '600',
  },
  selectedOptionText: {
    color: '#a43434',
  },
});
