import { DesignTokens } from './NativeShellShared';

@Component
export struct NativeHomeContent {
  onDeferred: (title: string) => void = (_t: string) => {};
  @State topTab: number = 0;
  @State metricTab: number = 0;

  private homeFeatures: string[] = [
    '销售顾问', '生活服务', '二手车', '新车关注', 'AI小石头',
    '订单中心', '数据分析', '直播带货', '营销活动', '更多',
  ];

  private featureRes(label: string): Resource {
    if (label === '销售顾问') return $r('app.media.home_feature_sales');
    if (label === '生活服务') return $r('app.media.home_feature_life');
    if (label === '二手车') return $r('app.media.home_feature_usedcar');
    if (label === '新车关注') return $r('app.media.home_feature_newcar');
    if (label === 'AI小石头') return $r('app.media.home_feature_ai_stone');
    if (label === '订单中心') return $r('app.media.home_feature_order');
    if (label === '数据分析') return $r('app.media.home_feature_data');
    if (label === '直播带货') return $r('app.media.home_feature_live');
    if (label === '营销活动') return $r('app.media.home_feature_market');
    return $r('app.media.home_feature_more');
  }

  private greeting(): string {
    const hour = new Date().getHours();
    const prefix = hour < 12 ? '早上好' : (hour < 18 ? '下午好' : '晚上好');
    return `${prefix}，沃德龙鼎`;
  }

  @Builder
  FeatureRow(start: number) {
    Row() {
      ForEach(this.homeFeatures.slice(start, start + 5), (label: string) => {
        Column({ space: 4 }) {
          Image(this.featureRes(label)).width(70).height(70).objectFit(ImageFit.Contain)
          Text(label).fontSize(11).fontColor(DesignTokens.ink).maxLines(1)
        }
        .layoutWeight(1)
        .onClick(() => {
          if (label === '更多') this.onDeferred('全部服务');
          else if (label === '直播带货') this.onDeferred('直播');
        })
      })
    }.width('100%')
  }

  @Builder
  MetricCell(value: string, label: string) {
    Column({ space: 2 }) {
      Text(value).fontSize(16).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
      Text(label).fontSize(11).fontColor(DesignTokens.body)
    }.layoutWeight(1)
  }

  build() {
    Scroll() {
      Column() {
        Row() {
          Text(this.greeting())
            .fontSize(28)
            .fontWeight(FontWeight.Medium)
            .fontColor(DesignTokens.ink)
            .layoutWeight(1)
          Row({ space: 4 }) {
            Text('🔔').fontSize(12)
            Text('3条新消息').fontSize(12).fontColor(DesignTokens.link)
          }
          .padding({ left: 12, right: 12, top: 6, bottom: 6 })
          .backgroundColor('#1A0070F3')
          .borderRadius(20)
          .onClick(() => this.onDeferred('消息'))
        }
        .width('100%')
        .padding({ left: 16, right: 16, top: 48 })

        Row({ space: 12 }) {
          Row({ space: 8 }) {
            Text('⌕').fontSize(18).fontColor(DesignTokens.body)
            Text('搜索客户、订单、资讯').fontSize(15).fontColor(DesignTokens.body)
          }
          .layoutWeight(1)
          .height(44)
          .padding({ left: 14, right: 14 })
          .backgroundColor(DesignTokens.canvas)
          .borderRadius(8)
          .border({ width: 0.5, color: DesignTokens.hairline })
          .onClick(() => this.onDeferred('搜索'))

          Text('▣')
            .fontSize(18)
            .fontColor(DesignTokens.link)
            .width(44)
            .height(44)
            .textAlign(TextAlign.Center)
            .backgroundColor(DesignTokens.canvas)
            .borderRadius(8)
            .border({ width: 0.5, color: DesignTokens.hairline })
            .onClick(() => this.onDeferred('扫一扫'))
        }
        .width('100%')
        .padding({ left: 16, right: 16, top: 32 })

        Row({ space: 24 }) {
          ForEach(['首页', '视频', 'Club'], (label: string, index: number) => {
            Column({ space: 6 }) {
              Text(label)
                .fontSize(this.topTab === index ? 16 : 15)
                .fontWeight(this.topTab === index ? FontWeight.Medium : FontWeight.Regular)
                .fontColor(this.topTab === index ? DesignTokens.ink : DesignTokens.body)
              Row()
                .width(20)
                .height(3)
                .borderRadius(2)
                .backgroundColor(this.topTab === index ? DesignTokens.link : Color.Transparent)
            }.onClick(() => { this.topTab = index; })
          })
        }
        .width('100%')
        .padding({ left: 16, right: 16, top: 20 })

        if (this.topTab === 0) {
          Image($r('app.media.home_banner'))
            .width('100%')
            .height(193)
            .borderRadius(12)
            .objectFit(ImageFit.Cover)
            .margin({ left: 24, right: 24, top: 31 })
            .onClick(() => this.onDeferred('朋友圈营销'))

          Column({ space: 56 }) {
            this.FeatureRow(0)
            this.FeatureRow(5)
          }
          .width('100%')
          .padding({ left: 8, right: 8, top: 16, bottom: 16 })
          .backgroundColor(DesignTokens.canvas)
          .borderRadius(8)
          .border({ width: 0.5, color: DesignTokens.hairline })
          .margin({ left: 26, right: 26, top: 86 })

          Row({ space: 12 }) {
            this.QuickCard('新伙伴待确认', '3 位新成员等待审核')
            this.QuickCard('待跟进客户', '今日 5 位意向客户')
          }
          .width('100%')
          .padding({ left: 16, right: 16, top: 48 })

          Column({ space: 8 }) {
            Text('[4S]北京沃德龙鼎吉利').fontSize(16).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
            Row({ space: 8 }) {
              ForEach(['今日', '昨日', '本月'], (t: string, index: number) => {
                Text(t)
                  .fontSize(12)
                  .fontColor(this.metricTab === index ? DesignTokens.link : DesignTokens.body)
                  .padding({ left: 10, right: 10, top: 6, bottom: 6 })
                  .backgroundColor(this.metricTab === index ? '#1F0070F3' : Color.Transparent)
                  .borderRadius(20)
                  .onClick(() => { this.metricTab = index; })
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
          .margin({ left: 16, right: 16, top: 16 })
          .backgroundColor(DesignTokens.canvas)
          .borderRadius(8)
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
            Text('›').fontColor(DesignTokens.body)
          }
          .width('100%')
          .padding(16)
          .margin(16)
          .backgroundColor(DesignTokens.canvas)
          .borderRadius(8)
          .border({ width: 0.5, color: DesignTokens.hairline })
          .onClick(() => this.onDeferred('投资策略'))
        } else {
          Text(this.topTab === 1 ? '视频 Tab · 一期后置' : 'Club Tab · 一期后置')
            .fontColor(DesignTokens.body)
            .width('100%')
            .textAlign(TextAlign.Center)
            .margin({ top: 48 })
        }
      }
      .width('100%')
      .padding({ bottom: 24 })
    }
    .width('100%')
    .height('100%')
    .backgroundColor(DesignTokens.canvasSoft2)
  }

  @Builder
  QuickCard(title: string, subtitle: string) {
    Row({ space: 10 }) {
      Text(title.substring(0, 1))
        .fontSize(16)
        .fontWeight(FontWeight.Bold)
        .fontColor(DesignTokens.link)
        .width(40)
        .height(40)
        .textAlign(TextAlign.Center)
        .backgroundColor(DesignTokens.canvasSoft2)
        .borderRadius(10)
      Column() {
        Text(title).fontSize(13).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink).maxLines(1)
        Text(subtitle).fontSize(11).fontColor(DesignTokens.body).maxLines(1)
      }.alignItems(HorizontalAlign.Start).layoutWeight(1)
    }
    .layoutWeight(1)
    .padding(14)
    .backgroundColor(DesignTokens.canvas)
    .borderRadius(8)
    .border({ width: 0.5, color: DesignTokens.hairline })
  }
}
