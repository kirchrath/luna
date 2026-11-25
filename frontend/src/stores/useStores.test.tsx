import { renderHook } from '@testing-library/react';
import type { ReactNode } from 'react';
import { describe, expect, it } from 'vitest';

import { RootStore } from './RootStore';
import { StoresProvider } from './StoresProvider';
import { useStores } from './useStores';

describe('useStores', () => {
  it('that using the stores within a provider should result into the provided root store', () => {
    // given
    const rootStore = new RootStore();
    const wrapper = ({ children }: { children: ReactNode }) => (
      <StoresProvider rootStore={rootStore}>{children}</StoresProvider>
    );

    // when
    const { result } = renderHook(() => useStores(), { wrapper });

    // then
    expect(result.current).toBe(rootStore);
  });

  it('that using the stores without a provider should result into an error', () => {
    // given
    const renderOutsideProvider = () => renderHook(() => useStores());

    // when / then
    expect(renderOutsideProvider).toThrow('useStores must be used within a StoresProvider');
  });
});
