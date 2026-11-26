import { stopEnvironment } from './environment.ts';

/** E2E_KEEP_ENVIRONMENT=true keeps the environment running, e.g. to inspect it after a failure. */
export default function globalTeardown(): void {
  if (process.env['E2E_KEEP_ENVIRONMENT'] !== 'true') {
    stopEnvironment();
  }
}
