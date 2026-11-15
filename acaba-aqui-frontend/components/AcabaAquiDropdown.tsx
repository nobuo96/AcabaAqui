import React, { useState } from "react";
import { StyleSheet, View } from "react-native";
import { Dropdown } from "react-native-element-dropdown";

interface AcabaAquiDropdownProps {
  data: { label: string; value: string }[];
  value?: string | null;
  onChange?: (value: string) => void;
}

export function AcabaAquiDropdown({ data, value, onChange }: AcabaAquiDropdownProps) {
  const [uncontrolledValue, setUncontrolledValue] = useState<string | null>(null);
  const selectedValue = value === undefined ? uncontrolledValue : value;

  return (
    <View style={styles.dropdownContainer}>
      <Dropdown
        data={data}
        labelField="label"
        valueField="value"
        placeholder="Selecione um serviço"
        value={selectedValue}
        onChange={(item) => {
          setUncontrolledValue(item.value);
          onChange?.(item.value);
        }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
    dropdownContainer: {
        width: '100%',
        backgroundColor: '#f5f5f5',
        borderWidth: 2,
        borderColor: '#d9d9d9',
        borderRadius: 12,
        paddingHorizontal: 12,
        paddingVertical: 10,

    },
});