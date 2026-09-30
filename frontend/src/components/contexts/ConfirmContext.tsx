import React, {
  createContext,
  type Dispatch,
  type FC,
  type PropsWithChildren,
  type ReactNode,
  type SetStateAction,
  useState,
} from 'react';

interface ConfirmContextType {
  content: ReactNode;
  confirm?: () => void;
  setContent: Dispatch<SetStateAction<ReactNode>>;
  setConfirm: Dispatch<SetStateAction<(() => void) | undefined>>;
  cancel: () => void;
  dangerButton: boolean;
  setDangerButton: Dispatch<SetStateAction<boolean>>;
  isConfirming: boolean;
  setIsConfirming: Dispatch<SetStateAction<boolean>>;
  title: string;
  setTitle: Dispatch<SetStateAction<string>>;
  confirmLabel: string;
  setConfirmLabel: Dispatch<SetStateAction<string>>;
}

export const ConfirmContext = createContext<ConfirmContextType | null>(null);

export const ConfirmContextProvider: FC<PropsWithChildren> = ({ children }) => {
  const [content, setContent] = useState<ReactNode>(null);
  const [confirm, setConfirm] = useState<(() => void) | undefined>(undefined);
  const [dangerButton, setDangerButton] = useState(false);
  const [isConfirming, setIsConfirming] = useState(false);
  const [title, setTitle] = useState('Confirm the action');
  const [confirmLabel, setConfirmLabel] = useState('Confirm');

  const cancel = () => {
    setContent(null);
    setConfirm(undefined);
    setTitle('Confirm the action');
    setConfirmLabel('Confirm');
  };

  return (
    <ConfirmContext.Provider
      value={{
        content,
        setContent,
        confirm,
        setConfirm,
        cancel,
        dangerButton,
        setDangerButton,
        isConfirming,
        setIsConfirming,
        title,
        setTitle,
        confirmLabel,
        setConfirmLabel,
      }}
    >
      {children}
    </ConfirmContext.Provider>
  );
};
