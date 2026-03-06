export interface MessageFormData {
  key: string;
  content: string;
  headers: string;
  partition: number;
  keySerde: string;
  valueSerde: string;
  keySubject?: string;
  valueSubject?: string;
  keepContents: boolean;
}
