export const hexToRgba = (hexInput: string, opacity: number): string => {
  const hex = hexInput.replace('#', '');
  let r: number;
  let g: number;
  let b: number;

  if (hex.length === 3) {
    r = parseInt(hex.charAt(0) + hex.charAt(0), 16);
    g = parseInt(hex.charAt(1) + hex.charAt(1), 16);
    b = parseInt(hex.charAt(2) + hex.charAt(2), 16);
  } else if (hex.length === 6) {
    r = parseInt(hex.substring(0, 2), 16);
    g = parseInt(hex.substring(2, 4), 16);
    b = parseInt(hex.substring(4, 6), 16);
  } else {
    throw new Error('Invalid HEX color.');
  }

  return `rgba(${r}, ${g}, ${b}, ${opacity})`;
};
