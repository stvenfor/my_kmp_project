// Preview/mock only — real device loads libentry.so NAPI.
import { ArkUIViewController } from 'compose/src/main/cpp/types/libcompose_arkui_utils';

const NativeMock = {
  MainArkUIViewController: (): ArkUIViewController | undefined => undefined,
  SetOhosHost: (_kind: number, _routeCode: number): void => {},
  AudioOnPageHide: (): void => {},
};

export default NativeMock;
export const MainArkUIViewController = NativeMock.MainArkUIViewController as () => ArkUIViewController;
export const AudioOnPageHide = NativeMock.AudioOnPageHide as () => void;
