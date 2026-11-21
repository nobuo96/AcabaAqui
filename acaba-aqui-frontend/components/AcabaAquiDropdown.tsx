import React, { useState } from "react";
import { StyleSheet, Text, View } from "react-native";
import { Dropdown } from "react-native-element-dropdown";

interface AcabaAquiDropdownProps {
  data: { label: string; value: string }[];
  value?: string | null;
  onChange?: (value: string) => void;
  placeholder?: string;
  label?: string;
  error?: string;
  disabled?: boolean;
  accessibilityLabel?: string;
}

export function AcabaAquiDropdown({
  data,
  value,
  onChange,
  placeholder = 'Selecione uma opção',
  label,
  error,
  disabled = false,
  accessibilityLabel,
}: AcabaAquiDropdownProps) {
  const [uncontrolledValue, setUncontrolledValue] = useState<string | null>(null);
  const selectedValue = value === undefined ? uncontrolledValue : value;

  return (
    <View style={styles.field}>
      {label ? <View style={styles.labelContainer}><Text style={styles.label}>{label}</Text></View> : null}
    <View style={[styles.dropdownContainer, error && styles.errorBorder, disabled && styles.disabled]}>
      <Dropdown
        data={data}
        labelField="label"
        valueField="value"
        placeholder={placeholder}
        value={selectedValue}
        disable={disabled}
        accessibilityLabel={accessibilityLabel ?? label ?? placeholder}
        onChange={(item) => {
          setUncontrolledValue(item.value);
          onChange?.(item.value);
        }}
      />
    </View>
    {error ? <Text style={styles.errorText}>{error}</Text> : null}
    </View>
  );
}

const styles = StyleSheet.create({
    field: { width: '100%', gap: 6 },
    labelContainer: { backgroundColor: 'transparent' },
    label: { color: '#444', fontSize: 15, fontWeight: '600' },
    dropdownContainer: {
        width: '100%',
        backgroundColor: '#f5f5f5',
        borderWidth: 2,
        borderColor: '#d9d9d9',
        borderRadius: 12,
        paddingHorizontal: 12,
        paddingVertical: 10,

    },
    errorBorder: { borderColor: '#a43434' },
    disabled: { opacity: 0.55 },
    errorText: { color: '#a43434', fontSize: 13 },
});