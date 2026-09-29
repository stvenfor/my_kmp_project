// Preview/mock only — real device loads libentry.so NAPI.
export default {
  MainArkUIViewController: (): Object => ({}),
  SetOhosHost: (_kind: number, _routeCode: number): void => {},
  SetOhosSecondaryRoute: (_route: string): void => {},
  ApplyAuthSession: (_token: string, _userId: string, _displayName: string, _phone: string): void => {},
  AuthLoginWithPassword: async (_account: string, _password: string): Promise<void> => {},
  AuthLoginWithOtp: async (_phone: string, _code: string): Promise<void> => {},
  AuthSendPhoneOtp: async (_phone: string): Promise<void> => {},
  AuthRegister: async (_email: string, _password: string, _displayName: string): Promise<void> => {},
  AuthLoginWithHuawei: async (_code: string, _deviceId: string): Promise<void> => {},
  AuthToken: (): string => '',
  AuthDisplayName: (): string => '用户',
  AuthUserId: (): string => '',
  AuthPhone: (): string => '',
  AuthIsLoggedIn: (): boolean => false,
  AuthLogout: (): void => {},
  AudioOnPageHide: (): void => {},
}
