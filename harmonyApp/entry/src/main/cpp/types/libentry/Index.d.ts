import { ArkUIViewController } from "compose/src/main/cpp/types/libcompose_arkui_utils";

export const MainArkUIViewController: () => ArkUIViewController
export const SetOhosHost: (kind: number, routeCode: number) => void
export const SetOhosSecondaryRoute: (route: string) => void
export const ApplyAuthSession: (token: string, userId: string, displayName: string, phone: string) => void
export const AudioOnPageHide: () => void
