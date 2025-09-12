import React, { createContext, useContext, ReactNode } from 'react';
import { TopicMessage } from 'generated-sources';

interface TopicActionsContextValue {
  openSidebarWithMessage: (message: TopicMessage) => void;
}

const TopicActionsContext = createContext<TopicActionsContextValue | null>(
  null
);

interface TopicActionsProviderProps {
  children: ReactNode;
  openSidebarWithMessage: (message: TopicMessage) => void;
}

export const TopicActionsProvider: React.FC<TopicActionsProviderProps> = ({
  children,
  openSidebarWithMessage,
}) => {
  const value = React.useMemo(
    () => ({
      openSidebarWithMessage,
    }),
    [openSidebarWithMessage]
  );

  return (
    <TopicActionsContext.Provider value={value}>
      {children}
    </TopicActionsContext.Provider>
  );
};

export const useTopicActions = (): TopicActionsContextValue => {
  const context = useContext(TopicActionsContext);
  if (!context) {
    throw new Error(
      'useTopicActions must be used within a TopicActionsProvider'
    );
  }
  return context;
};
