import { ArkUIViewController, Compose } from '@cpf-kmp-cmp/compose';
import { BusinessError } from '@kit.BasicServicesKit';
import { hilog } from '@kit.PerformanceAnalysisKit';
import { deviceInfo } from '@kit.BasicServicesKit';
// Default import = NAPI exports object (do NOT declare a separate `export default` in Index.d.ts).
import libentry from 'libentry.so';
import { registerVideoInteropBuilders } from '../ComposeVideoInterops';
import { registerWebInteropBuilders } from '../ComposeWebInterops';

const DOMAIN = 0x0000;

function formatThrown(error: Object): string {
  try {
    if (error instanceof Error) {
      return `${error.name}: ${error.message}`;
    }
    const be = error as BusinessError;
    if (be !== undefined && be !== null) {
      const codePart: string = be.code !== undefined ? `code=${be.code}` : '';
      const msgPart: string = be.message !== undefined ? String(be.message) : '';
      const namePart: string = be.name !== undefined ? String(be.name) : 'BusinessError';
      const stackPart: string = be.stack !== undefined ? String(be.stack) : '';
      const joined: string = [namePart, codePart, msgPart, stackPart]
        .filter((part: string) => part.length > 0)
        .join(' | ');
      if (joined.length > 0) {
        return joined;
      }
      if (namePart === 'BusinessError' || namePart === 'Error') {
        return `${namePart} (empty message — filter hilog DemoKN / DemoNapi)`;
      }
    }
    const asString: string = String(error);
    if (asString.length > 0 && asString !== '[object Object]') {
      return asString;
    }
    return JSON.stringify(error);
  } catch (_) {
    return 'unprintable error';
  }
}

@Entry
@Component
struct Index {
  private controller: ArkUIViewController | undefined = undefined;

  @State errorMessage: string =
    'Demo shell: waiting for libentry.so / libkn.so\n' +
    'If you see DevEco template "Hello World", you opened the wrong project.';

  aboutToAppear() {
    registerVideoInteropBuilders();
    registerWebInteropBuilders();
    const abi: string = deviceInfo.abiList;
    hilog.info(DOMAIN, 'Compose', 'device abiList=%{public}s', abi);

    if (libentry === undefined) {
      hilog.error(DOMAIN, 'Compose', 'libentry.so dlopen failed (default import undefined)');
      this.errorMessage =
        'libentry.so failed to load (dlopen).\n' +
        `abiList=${abi}\n` +
        'Check hilog for: Error loading shared library (often libruntime.so).\n' +
        'Run: ./gradlew :composeApp:publishDebugBinariesToHarmonyApp\n' +
        'Then DevEco Clean → Rebuild → uninstall + reinstall.';
      return;
    }

    const createController = libentry.MainArkUIViewController;
    if (createController === undefined) {
      hilog.error(DOMAIN, 'Compose', 'MainArkUIViewController missing on loaded libentry');
      this.errorMessage =
        'libentry.so loaded but MainArkUIViewController export missing.\n' +
        `abiList=${abi}\n` +
        'Rebuild native (napi_init Init must call napi_define_properties).';
      return;
    }

    try {
      this.controller = createController();
      const state: string = this.controller ? 'created' : 'not_created';
      hilog.info(DOMAIN, 'Compose', 'controller init state: %{public}s', state);
      if (!this.controller) {
        this.errorMessage =
          'MainArkUIViewController returned null (Kotlin threw; see hilog DemoKN).\n' +
          `abiList=${abi}`;
      }
    } catch (error) {
      const errText: string = formatThrown(error as Object);
      hilog.error(DOMAIN, 'Compose', 'Exception creating controller: %{public}s', errText);
      this.errorMessage =
        'Exception creating controller: ' + errText +
        `\nabiList=${abi}`;
    }
  }

  build() {
    Column() {
      if (this.controller) {
        Compose({
          controller: this.controller,
          libraryName: 'entry',
          onBackPressed: () => this.controller!.onBackPress()
        })
      } else {
        Text(this.errorMessage)
          .fontSize(14)
          .fontColor('#B00020')
          .padding(16)
          .width('100%')
      }
    }
    .width('100%')
    .height('100%')
  }

  onPageHide() {
    if (libentry !== undefined && libentry.AudioOnPageHide !== undefined) {
      libentry.AudioOnPageHide();
    }
    if (this.controller) {
      this.controller.onPageHide();
    }
  }

  onPageShow() {
    if (this.controller) {
      this.controller.onPageShow();
    }
  }

  onBackPressed(): boolean {
    return this.controller ? this.controller.onBackPress() : false;
  }
}
