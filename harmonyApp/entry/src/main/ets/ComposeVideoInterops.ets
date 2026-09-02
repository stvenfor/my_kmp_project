// Deep import: package Index does not re-export this (compose 1.9.2-0.3.0).
import { registerComposeInteropBuilder } from '@cpf-kmp-cmp/compose/src/main/ets/compose/ArkUIView';

export interface VodVideoArgs {
  url: string;
  autoPlay: boolean;
  muted: boolean;
  showControls: boolean;
  startPositionMs: number;
  playing: boolean;
  seekToken: number;
  seekPositionMs: number;
}

@Builder
export function vodVideoInlineBuilder() {
  VodVideoInline()
}

@Component
struct VodVideoInline {
  @Consume @Watch('onComposeArgsChanged') compose_args: VodVideoArgs;
  private controller: VideoController = new VideoController();
  private lastSeekToken: number = -1;
  private lastPlaying: boolean = false;

  aboutToAppear(): void {
    this.syncPlayback();
  }

  onComposeArgsChanged(): void {
    this.syncPlayback();
  }

  private syncPlayback(): void {
    if (this.compose_args.seekToken > 0 &&
      this.compose_args.seekToken !== this.lastSeekToken &&
      this.compose_args.seekPositionMs >= 0) {
      this.lastSeekToken = this.compose_args.seekToken;
      this.controller.setCurrentTime(this.compose_args.seekPositionMs / 1000);
    }
    if (this.compose_args.playing !== this.lastPlaying) {
      this.lastPlaying = this.compose_args.playing;
      if (this.compose_args.playing) {
        this.controller.start();
      } else {
        this.controller.pause();
      }
    }
  }

  build() {
    Video({
      src: this.compose_args.url,
      controller: this.controller,
    })
      .width('100%')
      .height('100%')
      .controls(this.compose_args.showControls)
      .muted(this.compose_args.muted)
      .autoPlay(this.compose_args.autoPlay)
      .onPrepared(() => {
        this.syncPlayback();
        if (this.compose_args.startPositionMs > 0) {
          this.controller.setCurrentTime(this.compose_args.startPositionMs / 1000);
        }
      })
  }
}

@Builder
export function vodVideoPreviewBuilder() {
  VodVideoPreview()
}

@Component
struct VodVideoPreview {
  @Consume compose_args: VodVideoArgs;
  private controller: VideoController = new VideoController();

  aboutToAppear(): void {
    if (this.compose_args.startPositionMs > 0) {
      this.controller.setCurrentTime(this.compose_args.startPositionMs / 1000);
    }
  }

  build() {
    Video({
      src: this.compose_args.url,
      controller: this.controller,
    })
      .width('100%')
      .height('100%')
      .controls(true)
      .muted(this.compose_args.muted)
      .autoPlay(this.compose_args.autoPlay)
      .onPrepared(() => {
        if (this.compose_args.startPositionMs > 0) {
          this.controller.setCurrentTime(this.compose_args.startPositionMs / 1000);
        }
      })
  }
}

export function registerVideoInteropBuilders(): void {
  registerComposeInteropBuilder('vodVideoInline', vodVideoInlineBuilder);
  registerComposeInteropBuilder('vodVideoPreview', vodVideoPreviewBuilder);
}
