function isRegex(value: string): boolean {
  try {
    const PATTERN = /\*|\(|\)|\[|\]|\[|\]|\?|\+|\||\//gm;

    return PATTERN.test(value);
  } catch {
    return false;
  }
}

export default isRegex;
