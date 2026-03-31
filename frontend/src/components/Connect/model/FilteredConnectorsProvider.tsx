import { FullConnectorInfo } from 'generated-sources';
import React, {
  createContext,
  Dispatch,
  FC,
  PropsWithChildren,
  useContext,
  useReducer,
} from 'react';

const initialConnectors: FullConnectorInfo[] = [];
type Action = { type: 'updated'; connectors: FullConnectorInfo[] };

function reducer(connectors: FullConnectorInfo[], action: Action) {
  switch (action.type) {
    case 'updated': {
      return action.connectors;
    }
    default: {
      throw Error(`Unknown action: ${action.type}`);
    }
  }
}

const ConnectorsContext = createContext<FullConnectorInfo[] | null>(null);
const ConnectorsDispatchContext = createContext<Dispatch<Action> | null>(null);

export const FilteredConnectorsProvider: FC<
  PropsWithChildren<{ initialData?: FullConnectorInfo[] }>
> = ({ children, initialData }) => {
  const [connectors, dispatch] = useReducer(
    reducer,
    initialData ?? initialConnectors
  );
  return (
    <ConnectorsContext.Provider value={connectors}>
      <ConnectorsDispatchContext.Provider value={dispatch}>
        {children}
      </ConnectorsDispatchContext.Provider>
    </ConnectorsContext.Provider>
  );
};

export const useFilteredConnectors = () => {
  const context = useContext(ConnectorsContext);
  if (!context) {
    throw new Error('useCounter must be used within a CounterProvider');
  }
  return context;
};

export const useFilteredConnectorsDispatch = () => {
  const context = useContext(ConnectorsDispatchContext);
  if (!context) {
    throw new Error('useCounter must be used within a CounterProvider');
  }
  return context;
};
