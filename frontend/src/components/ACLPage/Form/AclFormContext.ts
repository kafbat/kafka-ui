import { createContext } from 'react';

interface ACLFormContextProps {
  close: () => void;
}
const ACLFormContext = createContext<ACLFormContextProps | null>(null);

export default ACLFormContext;
