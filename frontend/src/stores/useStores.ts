import { useContext } from 'react';

import type { RootStore } from './RootStore';
import { StoresContext } from './StoresContext';

export function useStores(): RootStore {
  const rootStore = useContext(StoresContext);
  if (rootStore === null) {
    throw new Error('useStores must be used within a StoresProvider');
  }
  return rootStore;
}
