/** Source of the current access token, e.g. the UserManager of oidc-client-ts. */
export interface TokenSource {
  getUser(): Promise<{ access_token: string } | null>;
}
