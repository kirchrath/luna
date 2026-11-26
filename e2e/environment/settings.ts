/** Test values of the end-to-end environment (docker-compose.yml); none of them is a secret. */
export const BASE_URL = 'http://localhost:7180';
export const REALM = 'luna';
export const CLIENT_ID = 'luna-frontend';
export const KEYCLOAK_ADMIN = { username: 'admin', password: 'admin-e2e' } as const;
export const TEST_USER = { username: 'e2e', password: 'e2e-password', firstName: 'Erika', lastName: 'Test' } as const;
export const FRONTEND_IMAGE = 'luna-frontend:e2e';
export const LUNA_CORE_IMAGE = 'luna-core:e2e';
export const LUNA_TASK_IMAGE = 'luna-task:e2e';
