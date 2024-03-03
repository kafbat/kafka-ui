function isRegex(value: string): boolean {
  try {
    const PATTERN = /\*|\(|\)|\[|\]|\[|\]|\?|\+|\||\//gm;

    return PATTERN.test(value);
  } catch (e) {
    return false;
  }
}

export default isRegex;
