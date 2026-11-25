import type { ReactNode } from 'react';

import type { RootStore } from './RootStore';
import { StoresContext } from './StoresContext';

interface StoresProviderProps {
  rootStore: RootStore;
  children: ReactNode;
}

export function StoresProvider({ rootStore, children }: StoresProviderProps) {
  return <StoresContext value={rootStore}>{children}</StoresContext>;
}
