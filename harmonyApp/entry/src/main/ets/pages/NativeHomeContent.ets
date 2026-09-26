import { DesignTokens } from './NativeShellShared';

/**
 * SoT: my_ai_project HomePage —
 * greeting → search → banner → feature(9) → todo → store metrics →
 * strategy → services → contacts → news → learning report.
 * NO 首页/视频/Club segment (removed from Flutter).
 */
@Component
export struct NativeHomeContent {
  onDeferred: (title: string) => void = (_t: string) => {};
  displayName: string = '访客';
  isLoggedIn: boolean = false;
  @State metricTab: number = 0;
  @State showTodos: boolean = true;
  @State storeName: string = '[4S]北京沃德龙鼎吉利';
  @State showSwitchStore: boolean = false;
  @State selectedStoreId: string = '1';
  @State showCheckIn: boolean = false;
  private readonly stores: string[][] = [
    ['1', '[4S]北京沃德龙鼎吉利'],
    ['2', '[4S]北京腾远吉利'],
  ];

  private homeFeatures: string[] = [
    'H5 调试', '生活服务', '二手车', '新车成交', '新车跟进',
    'AI小石头', 'Club', '直播带货', '更多',
  ];

  private quickActions: string[][] = [
    ['新伙伴待确认', '3 位新成员等待审核', '去处理'],
    ['待跟进客户', '今日 5 位意向客户', '去查看'],
    ['订单待审核', '2 笔新车订单', '去处理'],
    ['售后预约', '4 位客户今日到店', '去查看'],
  ];

  private services: string[][] = [
    ['朋友圈', '热门'], ['视频号', ''], ['直播', '新品'], ['素材库', ''],
    ['话术库', ''], ['培训', ''], ['竞品分析', ''], ['更多', ''],
  ];

  private metricSets: string[][][] = [
    [['99', '意向客户'], ['2', '新车订单'], ['999.8', '成交额(万)'], ['15', '试驾预约']],
    [['86', '意向客户'], ['1', '新车订单'], ['520.0', '成交额(万)'], ['12', '试驾预约']],
    [['1280', '意向客户'], ['45', '新车订单'], ['8600.5', '成交额(万)'], ['320', '试驾预约']],
  ];

  private metricDetails: string[][] = [
    ['8', '待交车'], ['3', '待回访'], ['12', '待跟进'],
  ];

  private featureRes(label: string): Resource {
    if (label === 'H5 调试' || label === '销售顾问') return $r('app.media.home_feature_sales');
    if (label === '生活服务') return $r('app.media.home_feature_life');
    if (label === '二手车') return $r('app.media.home_feature_usedcar');
    if (label === '新车成交' || label === '新车关注') return $r('app.media.home_feature_newcar');
    if (label === '新车跟进' || label === '数据分析') return $r('app.media.home_feature_data');
    if (label === 'AI小石头') return $r('app.media.home_feature_ai_stone');
    if (label === 'Club' || label === '订单中心') return $r('app.media.home_feature_order');
    if (label === '直播带货') return $r('app.media.home_feature_live');
    if (label === '营销活动') return $r('app.media.home_feature_market');
    return $r('app.media.home_feature_more');
  }

  private greeting(): string {
    const hour = new Date().getHours();
    const prefix = hour < 12 ? '早上好' : (hour < 18 ? '下午好' : '晚上好');
    const name = (this.displayName || '').trim();
    return `${prefix}，${name.length > 0 ? name : '访客'}`;
  }

  private todayLocal(): string {
    const n = new Date();
    const y = n.getFullYear().toString();
    const m = (n.getMonth() + 1).toString().padStart(2, '0');
    const d = n.getDate().toString().padStart(2, '0');
    return `${y}-${m}-${d}`;
  }

  aboutToAppear(): void {
    if (!this.isLoggedIn || this.showCheckIn) {
      return;
    }
    // Flutter DailyCheckInDialog.maybeShow — mock once per Home enter when logged in.
    this.showCheckIn = true;
  }

  private onFeatureTap(label: string) {
    if (label === '更多') this.onDeferred('全部服务');
    else if (label === '直播带货') this.onDeferred('直播带货');
    else if (label === 'H5 调试') this.onDeferred('/web');
    else this.onDeferred(label);
  }

  @Builder
  FeatureRow(start: number) {
    Row() {
      ForEach(this.homeFeatures.slice(start, Math.min(start + 5, this.homeFeatures.length)),
        (label: string) => {
          Column({ space: 4 }) {
            Image(this.featureRes(label))
              .width(44)
              .height(44)
              .borderRadius(12)
              .objectFit(ImageFit.Cover)
            Text(label)
              .fontSize(11)
              .fontColor(DesignTokens.ink)
              .maxLines(1)
              .textAlign(TextAlign.Center)
              .width('100%')
          }
          .layoutWeight(1)
          .alignItems(HorizontalAlign.Center)
          .padding({ top: 8, bottom: 8 })
          .onClick(() => this.onFeatureTap(label))
        })
      // pad to 5 columns when last row has fewer items
      if (start + 5 > this.homeFeatures.length) {
        Column().layoutWeight(1)
      }
    }.width('100%')
  }

  @Builder
  HubEntry(title: string, subtitle: string, route: string) {
    Row({ space: 12 }) {
      Text(title.substring(0, 1))
        .width(44).height(44).textAlign(TextAlign.Center)
        .fontColor(DesignTokens.link).fontSize(16).fontWeight(FontWeight.Medium)
        .backgroundColor('#1A0070F3').borderRadius(12)
      Column() {
        Text(title).fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
        Text(subtitle).fontSize(13).fontColor(DesignTokens.body).margin({ top: 2 })
      }.alignItems(HorizontalAlign.Start).layoutWeight(1)
      Text('›').fontColor(DesignTokens.mute)
    }
    .width('100%')
    .padding(16)
    .margin({ left: 16, right: 16, top: 16 })
    .backgroundColor(DesignTokens.canvas)
    .borderRadius(8)
    .border({ width: 0.5, color: DesignTokens.hairline })
    .onClick(() => this.onDeferred(route))
  }

  @Builder
  QuickCard(title: string, subtitle: string, action: string) {
    Column({ space: 4 }) {
      Text(title).fontSize(15).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink).maxLines(1)
      Text(subtitle).fontSize(12).fontColor(DesignTokens.body).maxLines(1)
      Text(action).fontSize(13).fontWeight(FontWeight.Medium).fontColor(DesignTokens.link).margin({ top: 4 })
    }
    .alignItems(HorizontalAlign.Start)
    .layoutWeight(1)
    .padding(14)
    .backgroundColor(DesignTokens.canvas)
    .borderRadius(8)
    .border({ width: 0.5, color: DesignTokens.hairline })
    .onClick(() => this.onDeferred(title))
  }

  build() {
    Stack() {
      Scroll() {
      Column() {
        // Greeting
        Row() {
          Text(this.greeting())
            .fontSize(28)
            .fontWeight(FontWeight.Bold)
            .fontColor(DesignTokens.ink)
            .maxLines(1)
            .layoutWeight(1)
          Row({ space: 4 }) {
            Text('🔔').fontSize(14).fontColor(DesignTokens.link)
            Text('3条新消息').fontSize(12).fontWeight(FontWeight.Medium).fontColor(DesignTokens.link)
          }
          .padding({ left: 12, right: 12, top: 6, bottom: 6 })
          .backgroundColor('#1A0070F3')
          .borderRadius(20)
        }
        .width('100%')
        .padding({ left: 16, right: 16, top: 16 })

        // Search
        Row({ space: 12 }) {
          Row({ space: 8 }) {
            Text('⌕').fontSize(16).fontColor(DesignTokens.body)
            Text('搜索客户、订单、资讯').fontSize(15).fontColor(DesignTokens.mute)
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
        .padding({ left: 16, right: 16, top: 16 })

        // Flutter HomeBannerSection: display-only (no onTap). Asset has title/CTA paint.
        Image($r('app.media.home_banner'))
          .width('100%')
          .height(132)
          .objectFit(ImageFit.Cover)
          .borderRadius(8)
          .margin({ left: 16, right: 16, top: 16 })

        // Feature grid (9 items, 5-col)
        Column() {
          this.FeatureRow(0)
          this.FeatureRow(5)
        }
        .width('100%')
        .padding({ left: 16, right: 16, top: 12 })

        // Todo strip (2x2)
        if (this.showTodos) {
          Column({ space: 10 }) {
            Row({ space: 12 }) {
              this.QuickCard(this.quickActions[0][0], this.quickActions[0][1], this.quickActions[0][2])
              this.QuickCard(this.quickActions[1][0], this.quickActions[1][1], this.quickActions[1][2])
            }.width('100%')
            Row({ space: 12 }) {
              this.QuickCard(this.quickActions[2][0], this.quickActions[2][1], this.quickActions[2][2])
              this.QuickCard(this.quickActions[3][0], this.quickActions[3][1], this.quickActions[3][2])
            }.width('100%')
          }
          .width('100%')
          .padding({ left: 16, right: 16, top: 12 })
        }

        // Store metrics
        Column({ space: 12 }) {
          Row() {
            Text('公司数据').fontSize(18).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink).layoutWeight(1)
            Text('查看更多').fontSize(13).fontColor(DesignTokens.link)
          }.width('100%')

          Column({ space: 14 }) {
            Row({ space: 8 }) {
              Text('店')
                .width(28).height(28).textAlign(TextAlign.Center)
                .fontSize(12).fontColor(DesignTokens.link)
                .backgroundColor('#1F0070F3').borderRadius(8)
              Text(this.storeName)
                .fontSize(14).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
                .maxLines(1).layoutWeight(1)
              Text('∨').fontSize(12).fontColor(DesignTokens.body)
            }
            .width('100%')
            .padding({ left: 10, right: 10, top: 8, bottom: 8 })
            .backgroundColor(DesignTokens.canvasSoft2)
            .borderRadius(10)
            .onClick(() => {
              if (this.isLoggedIn) {
                this.showSwitchStore = true;
              } else {
                this.onDeferred('请先登录');
              }
            })

            Row({ space: 16 }) {
              ForEach(['今日', '昨日', '近30天'], (t: string, index: number) => {
                Text(t)
                  .fontSize(14)
                  .fontWeight(this.metricTab === index ? FontWeight.Medium : FontWeight.Regular)
                  .fontColor(this.metricTab === index ? DesignTokens.link : DesignTokens.body)
                  .onClick(() => { this.metricTab = index; })
              })
            }

            Grid() {
              ForEach(this.metricSets[this.metricTab], (m: string[]) => {
                Column({ space: 4 }) {
                  Text(m[0]).fontSize(18).fontWeight(FontWeight.Bold).fontColor(DesignTokens.ink)
                  Text(m[1]).fontSize(11).fontColor(DesignTokens.body)
                }
                .alignItems(HorizontalAlign.Start)
                .padding(12)
                .backgroundColor(DesignTokens.canvasSoft2)
                .borderRadius(8)
              })
            }
            .columnsTemplate('1fr 1fr')
            .rowsGap(10)
            .columnsGap(10)
            .width('100%')

            Divider().color(DesignTokens.hairline).strokeWidth(0.5)

            Row({ space: 8 }) {
              ForEach(this.metricDetails, (d: string[]) => {
                Column({ space: 4 }) {
                  Text(d[0]).fontSize(16).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
                  Text(d[1]).fontSize(11).fontColor(DesignTokens.body)
                  Text('详情 >').fontSize(11).fontColor(DesignTokens.link)
                }.layoutWeight(1)
              })
            }.width('100%')
          }
          .width('100%')
          .padding(14)
          .backgroundColor(DesignTokens.canvas)
          .borderRadius(8)
          .border({ width: 0.5, color: DesignTokens.hairline })
        }
        .width('100%')
        .padding({ left: 16, right: 16, top: 16 })

        this.HubEntry('投资策略', '资产九宫格 · 恐贪定投 · 趋势策略', '投资策略')

        // Services
        Column({ space: 12 }) {
          Text('营销服务').fontSize(18).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
          Grid() {
            ForEach(this.services, (item: string[]) => {
              Column({ space: 6 }) {
                Stack({ alignContent: Alignment.TopEnd }) {
                  Text(item[0].substring(0, 1))
                    .width(48).height(48).textAlign(TextAlign.Center)
                    .fontSize(16).fontWeight(FontWeight.Medium).fontColor(DesignTokens.link)
                    .backgroundColor('#1A0070F3').borderRadius(12)
                  if (item[1].length > 0) {
                    Text(item[1])
                      .fontSize(9).fontColor(Color.White)
                      .padding({ left: 4, right: 4, top: 1, bottom: 1 })
                      .backgroundColor(Color.Red)
                      .borderRadius(8)
                      .offset({ x: 6, y: -4 })
                  }
                }
                Text(item[0]).fontSize(11).fontColor(DesignTokens.ink).maxLines(1)
              }
              .onClick(() => {
                // Flutter HomeServiceGrid: only「更多」→ AllServices.
                if (item[0] === '更多') {
                  this.onDeferred('全部服务');
                }
              })
            })
          }
          .columnsTemplate('1fr 1fr 1fr 1fr')
          .rowsGap(12)
          .columnsGap(8)
          .width('100%')
          .padding(12)
          .backgroundColor(DesignTokens.canvas)
          .borderRadius(8)
          .border({ width: 0.5, color: DesignTokens.hairline })
        }
        .width('100%')
        .alignItems(HorizontalAlign.Start)
        .padding({ left: 16, right: 16, top: 16 })

        // Contacts
        Column({ space: 12 }) {
          Text('联系汽车之家').fontSize(18).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
          Column() {
            this.ContactRow('销售顾问小王', '在线 · 专属顾问', '聊')
            Divider().color(DesignTokens.hairline).strokeWidth(0.5)
            this.ContactRow('售后服务热线', '400-800-8888', '拨')
          }
          .width('100%')
          .backgroundColor(DesignTokens.canvas)
          .borderRadius(8)
          .border({ width: 0.5, color: DesignTokens.hairline })
        }
        .width('100%')
        .alignItems(HorizontalAlign.Start)
        .padding({ left: 16, right: 16, top: 16 })

        // News
        Column({ space: 12 }) {
          Text('行业动态').fontSize(18).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
          Row({ space: 12 }) {
            Row().width(72).height(72).backgroundColor(DesignTokens.canvasSoft2).borderRadius(8)
            Column({ space: 6 }) {
              Text('吉利银河 E8 获年度车型奖')
                .fontSize(15).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink).maxLines(2)
              Text('汽车之家 · 09-20').fontSize(12).fontColor(DesignTokens.body)
            }.alignItems(HorizontalAlign.Start).layoutWeight(1)
          }
          .width('100%')
          .padding(14)
          .backgroundColor(DesignTokens.canvas)
          .borderRadius(8)
          .border({ width: 0.5, color: DesignTokens.hairline })
        }
        .width('100%')
        .alignItems(HorizontalAlign.Start)
        .padding({ left: 16, right: 16, top: 16 })

        this.HubEntry('学习报告', '今日高光 · 学习记录', '学习报告')
      }
      .width('100%')
      .padding({ bottom: 24 })
    }
    .width('100%')
    .height('100%')
    .backgroundColor(DesignTokens.canvasSoft2)

      if (this.showSwitchStore) {
        Column() {
          Column()
            .width('100%')
            .layoutWeight(1)
            .backgroundColor('#8C000000')
            .onClick(() => { this.showSwitchStore = false; })
          Column({ space: 12 }) {
            Text('切换店铺')
              .fontSize(18).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
              .margin({ top: 24 })
            Text('可切换多个店铺查看数据')
              .fontSize(13).fontColor(DesignTokens.body)
            ForEach(this.stores, (s: string[]) => {
              Text(s[1])
                .fontSize(15).fontWeight(FontWeight.Medium)
                .fontColor(s[0] === this.selectedStoreId ? '#1B82D2' : DesignTokens.ink)
                .width('100%')
                .textAlign(TextAlign.Center)
                .padding({ top: 14, bottom: 14 })
                .border({
                  width: 1,
                  color: s[0] === this.selectedStoreId ? '#1B82D2' : DesignTokens.hairline,
                  radius: 24,
                })
                .onClick(() => {
                  this.selectedStoreId = s[0];
                  this.storeName = s[1];
                  this.showSwitchStore = false;
                })
            }, (s: string[]) => s[0])
            Text('取消')
              .fontSize(15).fontColor(DesignTokens.body)
              .margin({ top: 8, bottom: 24 })
              .onClick(() => { this.showSwitchStore = false; })
          }
          .width('100%')
          .padding({ left: 20, right: 20 })
          .backgroundColor(Color.White)
          .borderRadius({ topLeft: 16, topRight: 16 })
        }
        .width('100%')
        .height('100%')
        .justifyContent(FlexAlign.End)
      }

      if (this.showCheckIn && this.isLoggedIn) {
        Column() {
          Column()
            .width('100%')
            .height('100%')
            .backgroundColor('#8C000000')
            .onClick(() => { this.showCheckIn = false; })
          Column({ space: 12 }) {
            Text('每日签到')
              .fontSize(18).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
            Text('连续签到 3 天 · 今日可领 +10 积分')
              .fontSize(13).fontColor(DesignTokens.body)
            Button('立即签到')
              .backgroundColor(DesignTokens.link)
              .onClick(() => { this.showCheckIn = false; })
            Text('稍后再说')
              .fontSize(14).fontColor(DesignTokens.body)
              .onClick(() => { this.showCheckIn = false; })
          }
          .width('80%')
          .padding(22)
          .backgroundColor(Color.White)
          .borderRadius(16)
        }
        .width('100%')
        .height('100%')
        .justifyContent(FlexAlign.Center)
        .alignItems(HorizontalAlign.Center)
      }
    }
    .width('100%')
    .height('100%')
  }

  @Builder
  ContactRow(title: string, subtitle: string, trailing: string) {
    Row({ space: 12 }) {
      Text(title.substring(title.length - 1))
        .width(44).height(44).textAlign(TextAlign.Center)
        .fontColor(DesignTokens.link).backgroundColor('#260070F3').borderRadius(22)
      Column() {
        Text(title).fontSize(15).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
        Text(subtitle).fontSize(12).fontColor(DesignTokens.body).margin({ top: 2 })
      }.alignItems(HorizontalAlign.Start).layoutWeight(1)
      Text(trailing)
        .fontSize(13).fontColor(DesignTokens.link)
        .padding({ left: 12, right: 12, top: 6, bottom: 6 })
        .backgroundColor('#1A0070F3')
        .borderRadius(14)
    }
    .width('100%')
    .padding(14)
  }
}
