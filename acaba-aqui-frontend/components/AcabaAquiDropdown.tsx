import React, { useState } from "react";
import { StyleSheet, TextInput, View } from "react-native";
import { Dropdown } from "react-native-element-dropdown";

export function AcabaAquiDropdown({data}: {data: {label: string, value: string}[]}) {
  const [value, setValue] = useState(null);

  return (
    <View style={styles.dropdownContainer}>
      <Dropdown
        data={data}
        labelField="label"
        valueField="value"
        placeholder="Selecione um serviço"
        value={value}
        onChange={(item) => setValue(item.value)}
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