export const jsonFilter = (value : string):string => {
        return `record.value.value.internalValue == ${value}`;
    }