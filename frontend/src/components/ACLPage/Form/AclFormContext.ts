import { createContext } from 'react';

interface ACLFormContextProps {
  onClose: () => void;
}
const ACLFormContext = createContext<ACLFormContextProps | null>(null);

export default ACLFormContext;
