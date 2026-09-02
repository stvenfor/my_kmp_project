import { webview } from '@kit.ArkWeb';
import { router } from '@kit.ArkUI';

interface WebPageParams {
  url?: string;
  transparent?: boolean;
}

@Entry
@Component
struct WebPage {
  @State url: string = '';
  @State transparent: boolean = false;
  private controller: webview.WebviewController = new webview.WebviewController();

  aboutToAppear(): void {
    const params = router.getParams() as WebPageParams;
    this.url = params?.url ?? '';
    this.transparent = params?.transparent ?? false;
  }

  build() {
    Column() {
      if (!this.transparent) {
        Row() {
          Text('返回')
            .fontSize(16)
            .padding(12)
            .onClick(() => router.back());
          Text('网页')
            .fontSize(16)
            .layoutWeight(1)
            .textAlign(TextAlign.Center);
        }
        .width('100%')
        .backgroundColor('#FFFFFF');
      }
      Web({ src: this.url, controller: this.controller })
        .width('100%')
        .layoutWeight(1)
        .javaScriptAccess(true)
        .domStorageAccess(true)
        .fileAccess(true)
        .backgroundColor(this.transparent ? Color.Transparent : '#FFFFFF');
    }
    .width('100%')
    .height('100%')
    .backgroundColor(this.transparent ? Color.Transparent : '#FFFFFF');
  }
}
