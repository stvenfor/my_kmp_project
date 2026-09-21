import { DesignTokens } from './NativeShellShared';

@Component
export struct NativeHomeContent {
  onDeferred: (title: string) => void = (_t: string) => {};

  private homeFeatures: string[] = [
    '销售顾问', '生活服务', '二手车', '新车关注', 'AI小石头',
    '订单中心', '数据分析', '直播带货', '营销活动', '更多',
  ];

  @Builder
  MetricCell(value: string, label: string) {
    Column({ space: 4 }) {
      Text(value).fontSize(16).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
      Text(label).fontSize(11).fontColor(DesignTokens.mute)
    }.layoutWeight(1)
  }

  build() {
    Scroll() {
      Column({ space: 16 }) {
        Text('首页').fontSize(18).fontWeight(FontWeight.Bold).fontColor(DesignTokens.ink).width('100%')
        Row() {
          Text('早上好，沃德龙鼎').fontSize(18).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink).layoutWeight(1)
          Text('3条新消息')
            .fontSize(12)
            .fontColor(DesignTokens.link)
            .padding({ left: 10, right: 10, top: 6, bottom: 6 })
            .backgroundColor('#1A0070F3')
            .borderRadius(20)
        }.width('100%')

        Row({ space: 12 }) {
          Row() {
            Text('搜索客户、订单、资讯').fontSize(15).fontColor(DesignTokens.mute)
          }
          .layoutWeight(1)
          .padding(12)
          .backgroundColor(DesignTokens.canvas)
          .borderRadius(12)
          .border({ width: 0.5, color: DesignTokens.hairline })
          .onClick(() => this.onDeferred('搜索'))

          Text('▦')
            .fontSize(18)
            .fontColor(DesignTokens.link)
            .width(44)
            .height(44)
            .textAlign(TextAlign.Center)
            .backgroundColor(DesignTokens.canvas)
            .borderRadius(12)
            .border({ width: 0.5, color: DesignTokens.hairline })
            .onClick(() => this.onDeferred('扫一扫'))
        }.width('100%')

        Column({ space: 8 }) {
          Text('朋友圈营销').fontSize(18).fontWeight(FontWeight.Medium).fontColor(Color.White)
          Text('一键分享，高效触达客户').fontSize(14).fontColor('#E6FFFFFF')
          Text('立即体验')
            .fontSize(13)
            .fontColor(DesignTokens.link)
            .padding({ left: 12, right: 12, top: 8, bottom: 8 })
            .backgroundColor(Color.White)
            .borderRadius(20)
        }
        .width('100%')
        .padding(20)
        .alignItems(HorizontalAlign.Start)
        .borderRadius(12)
        .linearGradient({ angle: 90, colors: [[DesignTokens.link, 0], ['#8C0070F3', 1]] })

        Flex({ wrap: FlexWrap.Wrap }) {
          ForEach(this.homeFeatures, (label: string) => {
            Column({ space: 6 }) {
              Text(label.substring(0, 1))
                .fontSize(16)
                .fontWeight(FontWeight.Medium)
                .fontColor(DesignTokens.link)
                .width(48)
                .height(48)
                .textAlign(TextAlign.Center)
                .backgroundColor('#1A0070F3')
                .borderRadius(12)
              Text(label).fontSize(11).fontColor(DesignTokens.ink).maxLines(1)
            }
            .width('20%')
            .padding({ top: 8, bottom: 8 })
            .onClick(() => {
              if (label === '更多' || label === '直播带货') {
                this.onDeferred(label);
              }
            })
          })
        }.width('100%')

        Column({ space: 8 }) {
          Text('[4S]北京沃德龙鼎吉利').fontSize(14).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
          Row({ space: 8 }) {
            ForEach(['今日', '昨日', '本月'], (t: string) => {
              Text(t)
                .fontSize(12)
                .fontColor(t === '今日' ? DesignTokens.link : DesignTokens.mute)
                .padding({ left: 10, right: 10, top: 6, bottom: 6 })
                .backgroundColor(t === '今日' ? '#1A0070F3' : Color.Transparent)
                .borderRadius(20)
            })
          }
          Row() {
            this.MetricCell('99', '意向客户')
            this.MetricCell('2', '新车订单')
            this.MetricCell('999.8', '成交额(万)')
            this.MetricCell('15', '试驾预约')
          }.width('100%')
        }
        .width('100%')
        .padding(16)
        .backgroundColor(DesignTokens.canvas)
        .borderRadius(12)
        .border({ width: 0.5, color: DesignTokens.hairline })

        Row() {
          Text('投')
            .fontSize(12)
            .fontColor(Color.White)
            .width(28)
            .height(28)
            .textAlign(TextAlign.Center)
            .backgroundColor(DesignTokens.link)
            .borderRadius(14)
          Column() {
            Text('投资策略').fontSize(15).fontColor(DesignTokens.ink)
            Text('资产九宫格 · 恐贪定投 · 趋势策略').fontSize(12).fontColor(DesignTokens.body)
          }.alignItems(HorizontalAlign.Start).margin({ left: 12 }).layoutWeight(1)
          Text('›').fontColor(DesignTokens.mute)
        }
        .width('100%')
        .padding(16)
        .backgroundColor(DesignTokens.canvas)
        .borderRadius(12)
        .border({ width: 0.5, color: DesignTokens.hairline })
        .onClick(() => this.onDeferred('投资策略'))
      }
      .padding(16)
      .width('100%')
    }
    .width('100%')
    .height('100%')
    .backgroundColor(DesignTokens.canvasSoft2)
  }
}
