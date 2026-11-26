import { setUpEnvironment } from './environment.ts';

export default async function globalSetup(): Promise<void> {
  await setUpEnvironment();
}
