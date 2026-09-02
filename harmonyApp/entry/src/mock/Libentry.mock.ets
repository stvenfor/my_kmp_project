// Preview/mock only — real device loads libentry.so NAPI (named exports).
import { ArkUIViewController } from 'compose/src/main/cpp/types/libcompose_arkui_utils';

const NativeMock: Record<string, Object> = {
  MainArkUIViewController: (): ArkUIViewController | undefined => undefined,
  AudioOnPageHide: (): void => {},
};

export default NativeMock;
export const MainArkUIViewController = NativeMock.MainArkUIViewController as () => ArkUIViewController;
export const AudioOnPageHide = NativeMock.AudioOnPageHide as () => void;
