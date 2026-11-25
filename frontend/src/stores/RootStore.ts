/**
 * Creates and holds all MobX stores. Every communication with a backend (except OIDC) runs through a store
 * created here; stores for backend data are added with the features that need them.
 */
// eslint-disable-next-line @typescript-eslint/no-extraneous-class -- stays empty until the first backend store (step 6c)
export class RootStore {}
