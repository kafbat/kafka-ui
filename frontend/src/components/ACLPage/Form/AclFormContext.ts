import { RefObject, createContext } from 'react';

interface ACLFormContextProps {
  onClose: () => void;
}
const ACLFormContext = createContext<ACLFormContextProps>({
  onClose: () => {},
});

export default ACLFormContext;
