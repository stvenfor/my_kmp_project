// Deep import: package Index does not re-export this (compose 1.9.2-0.3.0).
import { registerComposeInteropBuilder } from '@cpf-kmp-cmp/compose/src/main/ets/compose/ArkUIView';
import { webview } from '@kit.ArkWeb';

export interface AgreementWebArgs {
  url: string;
}

@Builder
export function agreementWebInlineBuilder() {
  AgreementWebInline()
}

@Component
struct AgreementWebInline {
  @Consume compose_args: AgreementWebArgs;
  private controller: webview.WebviewController = new webview.WebviewController();

  build() {
    Web({ src: this.compose_args.url, controller: this.controller })
      .width('100%')
      .height('100%')
      .javaScriptAccess(true)
      .domStorageAccess(true)
      .fileAccess(true)
  }
}

export function registerWebInteropBuilders(): void {
  registerComposeInteropBuilder('agreementWebInline', agreementWebInlineBuilder);
}
