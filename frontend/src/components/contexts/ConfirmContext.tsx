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
}

export const ConfirmContext = createContext<ConfirmContextType | null>(null);

export const ConfirmContextProvider: FC<PropsWithChildren> = ({ children }) => {
  const [content, setContent] = useState<ReactNode>(null);
  const [confirm, setConfirm] = useState<(() => void) | undefined>(undefined);
  const [dangerButton, setDangerButton] = useState(false);
  const [isConfirming, setIsConfirming] = useState(false);

  const cancel = () => {
    setContent(null);
    setConfirm(undefined);
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
      }}
    >
      {children}
    </ConfirmContext.Provider>
  );
};
