import { ArkUIViewController } from "compose/src/main/cpp/types/libcompose_arkui_utils";

export const MainArkUIViewController: () => ArkUIViewController
export const SetOhosHost: (kind: number, routeCode: number) => void
export const SetOhosSecondaryRoute: (route: string) => void
export const ApplyAuthSession: (token: string, userId: string, displayName: string, phone: string) => void

/** BFF auth via Kotlin AuthBridge (kotlinx-serialization). Resolves on success. */
export const AuthLoginWithPassword: (account: string, password: string) => Promise<void>
export const AuthLoginWithOtp: (phone: string, code: string) => Promise<void>
export const AuthSendPhoneOtp: (phone: string) => Promise<void>
export const AuthRegister: (email: string, password: string, displayName: string) => Promise<void>
export const AuthLoginWithHuawei: (code: string, deviceId: string) => Promise<void>
export const AuthToken: () => string
export const AuthDisplayName: () => string
export const AuthUserId: () => string
export const AuthPhone: () => string
export const AuthIsLoggedIn: () => boolean
export const AuthLogout: () => void

export const AudioOnPageHide: () => void
