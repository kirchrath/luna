import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

import {
  BASE_URL,
  CLIENT_ID,
  FRONTEND_IMAGE,
  KEYCLOAK_ADMIN,
  LUNA_CORE_IMAGE,
  LUNA_ALERT_IMAGE,
  LUNA_APPOINTMENT_IMAGE,
  LUNA_NOTE_IMAGE,
  LUNA_TASK_IMAGE,
  REALM,
  TEST_USER,
} from './settings.ts';

const E2E_DIR = fileURLToPath(new URL('..', import.meta.url));
const FRONTEND_DIR = fileURLToPath(new URL('../../frontend', import.meta.url));
const LUNA_CORE_DIR = fileURLToPath(new URL('../../luna-core', import.meta.url));
const BACKENDS = [
  { dir: fileURLToPath(new URL('../../luna-task', import.meta.url)), image: LUNA_TASK_IMAGE },
  { dir: fileURLToPath(new URL('../../luna-alert', import.meta.url)), image: LUNA_ALERT_IMAGE },
  { dir: fileURLToPath(new URL('../../luna-note', import.meta.url)), image: LUNA_NOTE_IMAGE },
  { dir: fileURLToPath(new URL('../../luna-appointment', import.meta.url)), image: LUNA_APPOINTMENT_IMAGE },
];
const READY_TIMEOUT_MS = 5 * 60 * 1000;
const POLL_INTERVAL_MS = 2000;

function run(command: string, args: string[], cwd: string): void {
  execFileSync(command, args, { cwd, stdio: 'inherit' });
}

/** Builds the images under test; skipped with E2E_SKIP_BUILD=true when they were built before. */
export function buildImages(): void {
  if (process.env['E2E_SKIP_BUILD'] === 'true') {
    return;
  }
  run(
    'docker',
    ['build', '--platform', 'linux/amd64', '--build-arg', 'MODE=e2e', '-t', FRONTEND_IMAGE, '.'],
    FRONTEND_DIR,
  );
  run(
    './mvnw',
    ['-B', '-q', 'package', 'jib:dockerBuild', '-DskipTests', `-Djib.to.image=${LUNA_CORE_IMAGE}`],
    LUNA_CORE_DIR,
  );
  for (const { dir, image } of BACKENDS) {
    run('./mvnw', ['-B', '-q', 'package', 'jib:dockerBuild', '-DskipTests', `-Djib.to.image=${image}`], dir);
  }
}

export function startEnvironment(): void {
  run('docker', ['compose', 'up', '-d'], E2E_DIR);
}

export function stopEnvironment(): void {
  run('docker', ['compose', 'down', '--volumes'], E2E_DIR);
}

async function isReady(url: string, expectedText?: string): Promise<boolean> {
  try {
    const response = await fetch(url);
    return response.ok && (expectedText === undefined || (await response.text()).includes(expectedText));
  } catch {
    return false;
  }
}

export async function waitUntilReady(): Promise<void> {
  const checks = [
    () => isReady(`${BASE_URL}/auth/realms/${REALM}/.well-known/openid-configuration`),
    () => isReady(`${BASE_URL}/api/luna-core/actuator/health`, '"UP"'),
    () => isReady(`${BASE_URL}/api/luna-task/actuator/health`, '"UP"'),
    () => isReady(`${BASE_URL}/api/luna-alert/actuator/health`, '"UP"'),
    () => isReady(`${BASE_URL}/api/luna-note/actuator/health`, '"UP"'),
    () => isReady(`${BASE_URL}/api/luna-appointment/actuator/health`, '"UP"'),
    () => isReady(`${BASE_URL}/`),
  ];
  const deadline = Date.now() + READY_TIMEOUT_MS;
  for (const check of checks) {
    while (!(await check())) {
      if (Date.now() > deadline) {
        throw new Error('The end-to-end environment did not become ready in time');
      }
      await new Promise((resolve) => setTimeout(resolve, POLL_INTERVAL_MS));
    }
  }
}

async function adminRequest(token: string, path: string, init: RequestInit = {}): Promise<Response> {
  const response = await fetch(`${BASE_URL}/auth/admin/realms/${REALM}${path}`, {
    ...init,
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
  });
  if (!response.ok) {
    throw new Error(`Keycloak admin request ${path} failed with ${String(response.status)}`);
  }
  return response;
}

async function adminToken(): Promise<string> {
  const response = await fetch(`${BASE_URL}/auth/realms/master/protocol/openid-connect/token`, {
    method: 'POST',
    body: new URLSearchParams({ grant_type: 'password', client_id: 'admin-cli', ...KEYCLOAK_ADMIN }),
  });
  const { access_token: token } = (await response.json()) as { access_token: string };
  return token;
}

interface ClientRepresentation {
  id: string;
  redirectUris: string[];
  webOrigins: string[];
  attributes: Record<string, string>;
}

/** Allows the client to redirect to the test environment and creates the test user (idempotent). */
export async function prepareKeycloak(): Promise<void> {
  const token = await adminToken();
  const clients = (await (
    await adminRequest(token, `/clients?clientId=${CLIENT_ID}`)
  ).json()) as ClientRepresentation[];
  const client = clients[0];
  if (client === undefined) {
    throw new Error(`Client ${CLIENT_ID} missing in realm ${REALM}`);
  }
  const redirect = `${BASE_URL}/*`;
  if (!client.redirectUris.includes(redirect)) {
    const logoutKey = 'post.logout.redirect.uris';
    await adminRequest(token, `/clients/${client.id}`, {
      method: 'PUT',
      body: JSON.stringify({
        ...client,
        redirectUris: [...client.redirectUris, redirect],
        webOrigins: [...client.webOrigins, BASE_URL],
        attributes: { ...client.attributes, [logoutKey]: `${client.attributes[logoutKey] ?? ''}##${redirect}` },
      }),
    });
  }

  const users = (await (
    await adminRequest(token, `/users?exact=true&username=${TEST_USER.username}`)
  ).json()) as unknown[];
  if (users.length === 0) {
    await adminRequest(token, '/users', {
      method: 'POST',
      body: JSON.stringify({
        username: TEST_USER.username,
        firstName: TEST_USER.firstName,
        lastName: TEST_USER.lastName,
        email: `${TEST_USER.username}@example.test`,
        emailVerified: true,
        enabled: true,
        credentials: [{ type: 'password', value: TEST_USER.password, temporary: false }],
      }),
    });
  }
}

export async function setUpEnvironment(): Promise<void> {
  buildImages();
  startEnvironment();
  await waitUntilReady();
  await prepareKeycloak();
}
