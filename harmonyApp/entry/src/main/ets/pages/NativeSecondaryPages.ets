import { promptAction } from '@kit.ArkUI';
import { DesignTokens } from './NativeShellShared';

/** Flutter/Compose-aligned secondary pages for OHOS shell (ADR 0002). */

export interface ServiceItem {
  id: string;
  label: string;
  asset: Resource;
}

@Component
export struct NativeSearchPane {
  onClose: () => void = () => {};
  @State query: string = '';
  @State history: string[] = ['极限文字一排两个显示', '极限文字超出九个字...', '宫崎骏宫漫作品', '龙猫'];
  @State discovery: string[] = ['罗振宇2026跨年演讲', '小猪佩奇全系列', '百家讲坛全集', '百家讲坛明朝'];
  @State rankTab: number = 0;
  private rankTabs: string[] = ['热配榜', '诵读榜', '剧集榜', '记录榜', '合作榜'];
  private filters: string[] = ['3-5 个句子的配音', '较慢的语速', '初级难度', '女声', '1 分钟以内的视频'];

  private rankItems(): string[][] {
    if (this.rankTab === 1) {
      return [['静夜思', '床前明月光…'], ['春晓', '春眠不觉晓…'], ['登鹳雀楼', '白日依山尽…'], ['望庐山瀑布', '日照香炉…'], ['悯农', '锄禾日当午…']];
    }
    if (this.rankTab === 2) {
      return [['小猪佩奇', '佩奇和乔治…'], ['汪汪队立大功', '莱德队长…'], ['超级飞侠', '乐迪环游…'], ['熊出没', '熊大熊二…'], ['喜羊羊与灰太狼', '羊村与狼堡…']];
    }
    if (this.rankTab === 3) {
      return [['我的第一次配音', '完成度 98%…'], ['英语朗读打卡', '连续打卡 30 天…'], ['亲子共读记录', '温馨朗读…'], ['班级作业精选', '老师推荐…'], ['周末练习成果', '成果汇总…']];
    }
    if (this.rankTab === 4) {
      return [['BBC 合作专区', '纪录片配音…'], ['迪士尼经典合作', '动画片段…'], ['国家地理探索', '自然与科学…'], ['牛津阅读树', '分级阅读…'], ['剑桥少儿英语', '标准发音…']];
    }
    return [
      ['穿条纹睡衣的男孩', '某日布鲁诺决定…'], ['蛮荒故事', '六个独立故事…'], ['爱冒险的朵拉', '奇妙冒险…'],
      ['小王子', 'B612 小行星…'], ['寻梦环游记', '亡灵节…'], ['飞屋环游记', '气球房子…'],
      ['头脑特工队', '情绪小人…'], ['疯狂动物城', '兔子警官…'],
    ];
  }

  build() {
    Column() {
      Row({ space: 12 }) {
        Text('返回').fontSize(16).fontColor(DesignTokens.link).onClick(() => this.onClose())
        TextInput({ placeholder: '搜索客户、订单、资讯', text: this.query })
          .layoutWeight(1)
          .height(40)
          .backgroundColor(DesignTokens.canvasSoft2)
          .borderRadius(10)
          .onChange((v: string) => { this.query = v; })
        Text('搜索')
          .fontSize(15)
          .fontWeight(FontWeight.Medium)
          .fontColor(DesignTokens.link)
          .onClick(() => {
            const t = this.query.trim();
            if (t.length === 0) {
              return;
            }
            this.history = [t].concat(this.history.filter((h: string) => h !== t)).slice(0, 10);
          })
      }.padding(12).width('100%').backgroundColor(DesignTokens.canvas)

      Scroll() {
        Column({ space: 16 }) {
          if (this.history.length > 0) {
            Row() {
              Text('搜索历史').fontSize(15).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink).layoutWeight(1)
              Text('清除').fontSize(13).fontColor(DesignTokens.link).onClick(() => { this.history = []; })
            }.width('100%')
            Flex({ wrap: FlexWrap.Wrap }) {
              ForEach(this.history, (t: string) => {
                Text(t).fontSize(13).fontColor(DesignTokens.ink)
                  .padding({ left: 12, right: 12, top: 6, bottom: 6 })
                  .margin({ right: 8, bottom: 8 })
                  .backgroundColor(DesignTokens.hairline)
                  .borderRadius(16)
                  .onClick(() => { this.query = t; })
              })
            }.width('100%')
          }

          Row() {
            Text('搜索发现').fontSize(15).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink).layoutWeight(1)
            Text('换一批').fontSize(13).fontColor(DesignTokens.link).onClick(() => {
              this.discovery = this.discovery.slice().reverse();
            })
          }.width('100%')
          Flex({ wrap: FlexWrap.Wrap }) {
            ForEach(this.discovery, (t: string) => {
              Text(t).fontSize(13).fontColor(DesignTokens.ink)
                .padding({ left: 12, right: 12, top: 6, bottom: 6 })
                .margin({ right: 8, bottom: 8 })
                .backgroundColor(DesignTokens.hairline)
                .borderRadius(16)
                .onClick(() => { this.query = t; })
            })
          }.width('100%')

          Text('筛选标签').fontSize(15).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink).width('100%')
          Scroll() {
            Row({ space: 8 }) {
              ForEach(this.filters, (t: string) => {
                Text(t).fontSize(13).fontColor(DesignTokens.link)
                  .padding({ left: 12, right: 12, top: 6, bottom: 6 })
                  .border({ width: 1, color: '#800070F3' })
                  .borderRadius(16)
                  .onClick(() => { this.query = t; })
              })
            }
          }.scrollable(ScrollDirection.Horizontal).width('100%')

          Scroll() {
            Row({ space: 20 }) {
              ForEach(this.rankTabs, (label: string, index: number) => {
                Column({ space: 6 }) {
                  Text(label)
                    .fontSize(15)
                    .fontWeight(this.rankTab === index ? FontWeight.Medium : FontWeight.Regular)
                    .fontColor(this.rankTab === index ? DesignTokens.ink : DesignTokens.body)
                  Row().width(20).height(3).borderRadius(2)
                    .backgroundColor(this.rankTab === index ? DesignTokens.link : Color.Transparent)
                }.onClick(() => { this.rankTab = index; })
              })
            }
          }.scrollable(ScrollDirection.Horizontal).width('100%')

          Column() {
            ForEach(this.rankItems(), (item: string[], index: number) => {
              Row({ space: 12 }) {
                Text(`${index + 1}`).fontSize(16).fontWeight(FontWeight.Bold)
                  .fontColor(index < 3 ? '#EE0000' : DesignTokens.body).width(24)
                Text(item[0].substring(0, 1)).width(40).height(40).textAlign(TextAlign.Center)
                  .fontColor(DesignTokens.link).backgroundColor('#1A0070F3').borderRadius(6)
                Column() {
                  Text(item[0]).fontSize(15).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
                  Text(item[1]).fontSize(12).fontColor(DesignTokens.body).maxLines(1)
                }.alignItems(HorizontalAlign.Start).layoutWeight(1)
              }
              .padding(12).width('100%')
              .onClick(() => { this.query = item[0]; })
              if (index < this.rankItems().length - 1) {
                Divider().strokeWidth(0.5).color(DesignTokens.hairline)
              }
            })
          }
          .width('100%')
          .backgroundColor(DesignTokens.canvas)
          .borderRadius(12)
          .border({ width: 0.5, color: DesignTokens.hairline })
        }.padding(16).width('100%')
      }.layoutWeight(1).width('100%')
    }
    .width('100%')
    .height('100%')
    .backgroundColor(DesignTokens.canvasSoft2)
  }
}

@Component
export struct NativeAllServicesPane {
  onClose: () => void = () => {};
  onOpen: (label: string) => void = (_l: string) => {};
  @State isEditing: boolean = false;
  @State favoriteIds: string[] = ['intro', 'glass', 'diet', 'drawer', 'diary', 'training', 'running', 'wave'];

  private favorites(): ServiceItem[] {
    return [
      { id: 'intro', label: '引导动画', asset: $r('app.media.home_all_services_smart_online_marketing') },
      { id: 'glass', label: '玻璃卡片', asset: $r('app.media.home_all_services_online_customer_acquisition') },
      { id: 'diet', label: '地中海饮食', asset: $r('app.media.home_all_services_small_video') },
      { id: 'drawer', label: '侧滑导航', asset: $r('app.media.home_all_services_service_management') },
      { id: 'diary', label: '我的日记', asset: $r('app.media.home_all_services_exhibition_hall_shooting') },
      { id: 'training', label: '训练计划', asset: $r('app.media.home_all_services_intelligence_task') },
      { id: 'running', label: '跑步数据', asset: $r('app.media.home_all_services_new_car_in_store') },
      { id: 'wave', label: '波浪动画', asset: $r('app.media.home_all_services_smart_number') },
    ];
  }

  private tools(): ServiceItem[] {
    return [
      { id: 'calc', label: '购车计算器', asset: $r('app.media.home_all_services_calculator') },
      { id: 'used', label: '二手车', asset: $r('app.media.home_all_services_used_car') },
      { id: 'after', label: '售后专区', asset: $r('app.media.home_all_services_after_sales_area') },
      { id: 'all', label: '全部功能', asset: $r('app.media.home_all_services_all_functions') },
    ];
  }

  private visibleFavorites(): ServiceItem[] {
    return this.favorites().filter((i: ServiceItem) => this.favoriteIds.indexOf(i.id) >= 0);
  }

  private toggle(id: string) {
    const idx = this.favoriteIds.indexOf(id);
    if (idx >= 0) {
      if (this.favoriteIds.length > 3) {
        this.favoriteIds = this.favoriteIds.filter((x: string) => x !== id);
      }
    } else if (this.favoriteIds.length < 8) {
      this.favoriteIds = this.favoriteIds.concat([id]);
    }
  }

  @Builder
  ServiceGrid(items: ServiceItem[], favoriteSection: boolean) {
    Flex({ wrap: FlexWrap.Wrap }) {
      ForEach(items, (item: ServiceItem) => {
        Stack({ alignContent: Alignment.TopEnd }) {
          Column({ space: 6 }) {
            Image(item.asset).width(48).height(48).borderRadius(12).objectFit(ImageFit.Contain)
            Text(item.label).fontSize(12).fontColor(DesignTokens.ink).maxLines(1)
          }
          .width('100%')
          .onClick(() => {
            if (this.isEditing) {
              this.toggle(item.id);
            } else {
              this.onOpen(item.label);
            }
          })
          if (this.isEditing) {
            Text(favoriteSection || this.favoriteIds.indexOf(item.id) >= 0 ? '−' : '+')
              .fontSize(12).fontColor(Color.White)
              .width(18).height(18).textAlign(TextAlign.Center)
              .backgroundColor(favoriteSection || this.favoriteIds.indexOf(item.id) >= 0 ? '#EE0000' : DesignTokens.link)
              .borderRadius(9)
              .offset({ x: 4, y: -4 })
              .onClick(() => this.toggle(item.id))
          }
        }.width('25%').padding({ top: 8, bottom: 8 })
      })
    }.width('100%')
  }

  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text('全部服务').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink).margin({ left: 8 }).layoutWeight(1)
      }.padding(16).width('100%').backgroundColor(DesignTokens.canvas)

      Scroll() {
        Column({ space: 16 }) {
          Row() {
            Text('常用服务').fontSize(16).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink).layoutWeight(1)
            Text(this.isEditing ? '完成' : '编辑')
              .fontSize(13).fontColor(DesignTokens.link)
              .onClick(() => { this.isEditing = !this.isEditing; })
          }.width('100%')
          Text('将按自定义顺序出现在首页').fontSize(12).fontColor(DesignTokens.body).width('100%')
          Column() {
            this.ServiceGrid(this.visibleFavorites().length > 0 ? this.visibleFavorites() : this.favorites().slice(0, 3), true)
          }.width('100%').padding(12).backgroundColor(DesignTokens.canvas).borderRadius(12)

          Text('车商工具').fontSize(16).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink).width('100%')
          Column() {
            this.ServiceGrid(this.tools(), false)
          }.width('100%').padding(12).backgroundColor(DesignTokens.canvas).borderRadius(12)
        }.padding(16).width('100%')
      }.layoutWeight(1).width('100%')
    }
    .width('100%')
    .height('100%')
    .backgroundColor(DesignTokens.canvasSoft2)
  }
}

@Component
export struct NativeLearningReportPane {
  onClose: () => void = () => {};
  private highlights: string[][] = [
    ['🎬', '《哈利波特》第3章', '视频配音 · 刚刚发布', '100'],
    ['🏆', '解锁「45天」打卡勋章', '里程碑达成 · 太棒了！', '🎉'],
    ['🎵', '"Wingardium Leviosa!"', '满分句子 · 可播放原声', '▶'],
  ];
  private records: string[][] = [
    ['🎬', '视频配音', '哈利波特 第3章', '18:32', '已发布'],
    ['⚔️', '配音闯关', 'Level 8 · 3关', '17:10', '15 min'],
    ['📚', '同步练', 'PEP 五年级上册 Unit 3', '16:45', '8 min'],
    ['🤖', 'AI 外教', '自由对话 · Tom老师', '15:20', '12 min'],
    ['🎧', '听力练习', '英美绕口令 · 5题', '14:00', '5 min'],
  ];

  build() {
    Stack({ alignContent: Alignment.Bottom }) {
      Column() {
        Row() {
          Text('‹').fontSize(22).fontColor(Color.White).onClick(() => this.onClose())
          Text('学习报告').fontSize(17).fontWeight(FontWeight.Medium).fontColor(Color.White).margin({ left: 8 })
          Blank().layoutWeight(1)
        }.padding(16).width('100%').backgroundColor('#0B0C11')

        Scroll() {
          Column({ space: 16 }) {
            Text('今日高光').fontSize(16).fontWeight(FontWeight.Medium).fontColor('#F5F6F8').width('100%')
            Column() {
              ForEach(this.highlights, (h: string[]) => {
                Row({ space: 12 }) {
                  Text(h[0]).fontSize(28)
                  Column({ space: 4 }) {
                    Text(h[1]).fontSize(15).fontWeight(FontWeight.Medium).fontColor(Color.White)
                    Text(h[2]).fontSize(12).fontColor('#8B9099')
                  }.alignItems(HorizontalAlign.Start).layoutWeight(1)
                  Text(h[3]).fontSize(14).fontColor('#FFFF8A34')
                }.padding(14).width('100%')
              })
            }
            .width('100%')
            .backgroundColor('#121A1F')
            .borderRadius(16)
            .border({ width: 1, color: '#1E3330' })

            Text('今日学习记录').fontSize(16).fontWeight(FontWeight.Medium).fontColor('#F5F6F8').width('100%')
            Column({ space: 10 }) {
              ForEach(this.records, (r: string[]) => {
                Row({ space: 12 }) {
                  Text(r[0]).fontSize(22)
                  Column({ space: 4 }) {
                    Text(r[1]).fontSize(15).fontWeight(FontWeight.Medium).fontColor(Color.White)
                    Text(r[2]).fontSize(12).fontColor('#8B9099')
                  }.alignItems(HorizontalAlign.Start).layoutWeight(1)
                  Column({ space: 4 }) {
                    Text(r[3]).fontSize(11).fontColor('#6B7078')
                    Text(r[4]).fontSize(12).fontColor(r[4] === '已发布' ? '#FFFF8A34' : '#8B9099')
                  }.alignItems(HorizontalAlign.End)
                }
                .padding(12)
                .width('100%')
                .backgroundColor('#242630')
                .borderRadius(12)
              })
            }
            .padding(12)
            .width('100%')
            .backgroundColor('#17161F')
            .borderRadius(16)
          }.padding({ left: 16, right: 16, top: 12, bottom: 120 })
        }.layoutWeight(1).width('100%')
      }.width('100%').height('100%')

      Column({ space: 10 }) {
        Row() {
          Text('👨‍👩‍👧 家长助手').fontSize(12).fontColor('#8B9099')
        }
        .padding({ left: 12, right: 12, top: 8, bottom: 8 })
        .backgroundColor('#242630')
        .borderRadius(20)
        .alignSelf(ItemAlign.End)
        .margin({ right: 16 })

        Row({ space: 10 }) {
          Text('👑').fontSize(28)
          Column({ space: 4 }) {
            Text('开通会员，解锁全部内容')
              .fontSize(14).fontWeight(FontWeight.Medium).fontColor(Color.White)
            Text('全量剧集 · AI外教不限时 · 专属勋章')
              .fontSize(11).fontColor('#8B9099').maxLines(1)
          }.alignItems(HorizontalAlign.Start).layoutWeight(1)
          Text('立即开通')
            .fontSize(13).fontWeight(FontWeight.Medium).fontColor(Color.White)
            .padding({ left: 14, right: 14, top: 8, bottom: 8 })
            .backgroundColor('#FFE65100')
            .borderRadius(20)
        }
        .padding(14)
        .width('100%')
        .borderRadius(16)
        .border({ width: 1, color: '#3D2A20' })
        .linearGradient({
          angle: 90,
          colors: [['#2A1810', 0], ['#1A1210', 1]],
        })
      }
      .width('100%')
      .padding({ left: 16, right: 16, bottom: 12 })
    }
    .width('100%').height('100%')
    .backgroundColor('#0B0C11')
  }
}


@Component
export struct NativePurchaseCalculatorPane {
  onClose: () => void = () => {};
  @State mode: string = 'cash';
  @State barePrice: string = '100000';
  @State taxable: string = '';
  @State includeCommercial: boolean = false;
  @State selectedProduct: number = 1;
  @State quoteKeys: string[] = [];
  @State quoteVals: string[] = [];
  private products: string[][] = [
    ['1', '示例银行车贷', '年利率 4.5% · 最低首付 20.0%'],
    ['2', '厂商金融贴息', '年利率 4.5% · 最低首付 20.0% · 贴息减 0.5%'],
    ['3', '低息精品贷', '年利率 3.98% · 最低首付 15.0% · 减本金 ¥2000'],
  ];

  private calc(): void {
    const bare = Number(this.barePrice);
    const b = isNaN(bare) ? 0 : bare;
    const taxN = Number(this.taxable);
    const tax = isNaN(taxN) || this.taxable.length === 0 ? b / 1.13 : taxN;
    const product = this.products.find((p: string[]) => Number(p[0]) === this.selectedProduct);
    const name = product !== undefined ? product[1] : '';
    this.quoteKeys = ['付款方式', '金融产品', '裸车价', '计税价格', '商业险', '合计参考'];
    this.quoteVals = [
      this.mode === 'cash' ? '全款' : '贷款',
      name,
      `¥${Math.floor(b)}`,
      `¥${Math.floor(tax)}`,
      this.includeCommercial ? '已计入粗算' : '未计入',
      `¥${Math.floor(b * 1.08)}`,
    ];
  }

  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text('购车计算器').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink).margin({ left: 8 })
        Blank().layoutWeight(1)
      }.padding({ left: 16, right: 16, top: 12, bottom: 12 }).width('100%').backgroundColor(DesignTokens.canvas)

      Scroll() {
        Column({ space: 12 }) {
          Column({ space: 12 }) {
            Text('付款方式').fontSize(15).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
            Row({ space: 8 }) {
              Text(this.mode === 'cash' ? '✓ 全款' : '全款')
                .fontSize(14).fontColor(this.mode === 'cash' ? Color.White : DesignTokens.ink)
                .padding({ left: 14, right: 14, top: 8, bottom: 8 })
                .backgroundColor(this.mode === 'cash' ? DesignTokens.link : DesignTokens.canvas)
                .borderRadius(20)
                .border({ width: 1, color: this.mode === 'cash' ? DesignTokens.link : DesignTokens.hairline })
                .onClick(() => { this.mode = 'cash'; })
              Text(this.mode === 'loan' ? '✓ 贷款' : '贷款')
                .fontSize(14).fontColor(this.mode === 'loan' ? Color.White : DesignTokens.ink)
                .padding({ left: 14, right: 14, top: 8, bottom: 8 })
                .backgroundColor(this.mode === 'loan' ? DesignTokens.link : DesignTokens.canvas)
                .borderRadius(20)
                .border({ width: 1, color: this.mode === 'loan' ? DesignTokens.link : DesignTokens.hairline })
                .onClick(() => { this.mode = 'loan'; })
            }
            Text('裸车价（元）').fontSize(12).fontColor(DesignTokens.body)
            TextInput({ text: this.barePrice })
              .onChange((v: string) => { this.barePrice = v; })
              .height(44).padding({ left: 12, right: 12 })
              .backgroundColor('#FFF5F6F8').borderRadius(8)
            Text('计税价格（可选，默认裸车价/1.13）').fontSize(12).fontColor(DesignTokens.body)
            TextInput({ text: this.taxable })
              .onChange((v: string) => { this.taxable = v; })
              .height(44).padding({ left: 12, right: 12 })
              .backgroundColor('#FFF5F6F8').borderRadius(8)
            Row() {
              Text('计入商业险粗算').fontSize(15).fontColor(DesignTokens.ink).layoutWeight(1)
              Toggle({ type: ToggleType.Switch, isOn: this.includeCommercial })
                .selectedColor(DesignTokens.link)
                .onChange((v: boolean) => { this.includeCommercial = v; })
            }.width('100%')
          }
          .padding(14).width('100%')
          .backgroundColor(DesignTokens.canvas).borderRadius(12)

          Column({ space: 8 }) {
            Row() {
              Text('金融产品').fontSize(15).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink).layoutWeight(1)
              Text('刷新').fontSize(14).fontColor(DesignTokens.link)
            }.width('100%')
            ForEach(this.products, (p: string[]) => {
              Row({ space: 10 }) {
                Text(this.selectedProduct === Number(p[0]) ? '◉' : '○')
                  .fontSize(18)
                  .fontColor(this.selectedProduct === Number(p[0]) ? DesignTokens.link : '#888888')
                Column({ space: 4 }) {
                  Text(p[1]).fontSize(15).fontWeight(FontWeight.Medium)
                    .fontColor(this.selectedProduct === Number(p[0]) ? DesignTokens.link : DesignTokens.ink)
                  Text(p[2]).fontSize(12).fontColor(DesignTokens.body)
                }.alignItems(HorizontalAlign.Start).layoutWeight(1)
              }
              .width('100%').padding({ top: 8, bottom: 8 })
              .onClick(() => { this.selectedProduct = Number(p[0]); })
            })
          }
          .padding(14).width('100%')
          .backgroundColor(DesignTokens.canvas).borderRadius(12)

          Text('计算报价')
            .fontSize(16).fontWeight(FontWeight.Medium).fontColor(Color.White)
            .width('100%').textAlign(TextAlign.Center)
            .height(48).borderRadius(12)
            .backgroundColor(DesignTokens.ink)
            .onClick(() => this.calc())

          if (this.quoteKeys.length > 0) {
            Column({ space: 8 }) {
              Text('报价结果').fontSize(15).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
              ForEach(this.quoteKeys, (k: string, i: number) => {
                Row() {
                  Text(k).fontSize(14).fontColor(DesignTokens.body).layoutWeight(1)
                  Text(this.quoteVals[i]).fontSize(14).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
                }.width('100%')
              }, (k: string, i: number) => `${k}-${i}`)
            }
            .alignItems(HorizontalAlign.Start)
            .padding(14).width('100%')
            .backgroundColor(DesignTokens.canvas).borderRadius(12)
          }
        }.padding(16)
      }.layoutWeight(1).width('100%')
    }
    .width('100%').height('100%')
    .backgroundColor('#FFF5F6F8')
  }
}


@Component
export struct NativeMallDetailPane {
  onClose: () => void = () => {};
  @State qty: number = 1;
  @State skuSel: number = 0;
  private skus: string[] = ['经典白', '店庆红'];

  private flash(msg: string) {
    promptAction.showToast({ message: msg });
  }

  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text('商品详情').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink).margin({ left: 8 })
        Blank().layoutWeight(1)
      }.padding(16).width('100%').backgroundColor(DesignTokens.canvas)

      Scroll() {
        Column({ space: 8 }) {
          Text('店庆纪念马克杯')
            .fontSize(14).fontColor(DesignTokens.mute)
            .width('100%').height(280).textAlign(TextAlign.Center)
            .backgroundColor('#FFF0F0F0')

          Column({ space: 8 }) {
            Text('店庆纪念马克杯').fontSize(18).fontWeight(FontWeight.Bold).fontColor(DesignTokens.ink)
            Text('39.90元').fontSize(22).fontWeight(FontWeight.Medium).fontColor('#FFEE0000')
            Text('实体商品 · 需填写收货信息').fontSize(13).fontColor(DesignTokens.body)
          }
          .width('100%').padding(16).alignItems(HorizontalAlign.Start)
          .backgroundColor(Color.White)

          Row({ space: 8 }) {
            Text('📍').fontSize(14)
            Text('选择收货地址').fontSize(14).fontColor(DesignTokens.mute).layoutWeight(1)
            Text('›').fontSize(18).fontColor(DesignTokens.mute)
          }
          .width('100%')
          .padding({ left: 16, right: 16, top: 14, bottom: 14 })
          .backgroundColor(Color.White)
          .onClick(() => this.flash('选择收货地址'))

          Column({ space: 12 }) {
            Text('规格').fontSize(14).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
            Row({ space: 8 }) {
              ForEach(this.skus, (label: string, i: number) => {
                Text(label)
                  .fontSize(13)
                  .fontWeight(this.skuSel === i ? FontWeight.Medium : FontWeight.Normal)
                  .fontColor(this.skuSel === i ? DesignTokens.link : DesignTokens.body)
                  .padding({ left: 12, right: 12, top: 8, bottom: 8 })
                  .backgroundColor(this.skuSel === i ? '#1A0070F3' : DesignTokens.canvasSoft2)
                  .border({ width: 1, color: this.skuSel === i ? DesignTokens.link : DesignTokens.hairline })
                  .borderRadius(8)
                  .onClick(() => { this.skuSel = i; })
              })
            }
            Row() {
              Text('数量').fontSize(14).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
              Blank().layoutWeight(1)
              Text('库存 128').fontSize(12).fontColor(DesignTokens.mute).margin({ right: 8 })
              Text('−').fontSize(16)
                .padding({ left: 10, right: 10, top: 4, bottom: 4 })
                .border({ width: 1, color: DesignTokens.hairline }).borderRadius(4)
                .onClick(() => { if (this.qty > 1) { this.qty -= 1; } })
              Text(`${this.qty}`).fontSize(15).fontWeight(FontWeight.Medium)
                .padding({ left: 12, right: 12 })
              Text('+').fontSize(16)
                .padding({ left: 10, right: 10, top: 4, bottom: 4 })
                .border({ width: 1, color: DesignTokens.hairline }).borderRadius(4)
                .onClick(() => { this.qty += 1; })
            }.width('100%').alignItems(VerticalAlign.Center)
          }
          .width('100%').padding(16).alignItems(HorizontalAlign.Start)
          .backgroundColor(Color.White)

          Blank().height(24)
        }.width('100%')
      }.layoutWeight(1).width('100%').scrollBar(BarState.Off)

      Row({ space: 12 }) {
        Text('加入购物车')
          .fontSize(15).fontColor(DesignTokens.ink)
          .layoutWeight(1).textAlign(TextAlign.Center)
          .padding({ top: 12, bottom: 12 })
          .border({ width: 1, color: DesignTokens.hairline }).borderRadius(8)
          .onClick(() => this.flash('已加入购物车'))
        Text('立即购买')
          .fontSize(15).fontWeight(FontWeight.Medium).fontColor(Color.White)
          .layoutWeight(1).textAlign(TextAlign.Center)
          .padding({ top: 12, bottom: 12 })
          .backgroundColor(DesignTokens.link).borderRadius(8)
          .onClick(() => this.flash('立即购买'))
      }
      .width('100%')
      .padding({ left: 16, right: 16, top: 10, bottom: 10 })
      .backgroundColor(Color.White)
      .border({ width: { top: 1 }, color: DesignTokens.hairline })
    }
    .width('100%').height('100%').backgroundColor(DesignTokens.canvasSoft2)
  }
}

@Component
export struct NativeMallOrdersPane {
  onClose: () => void = () => {};
  onOpen: (path: string) => void = (_path: string) => {};
  @State tab: string = '全部';
  private tabs: string[] = ['全部', '待支付', '已支付', '已取消'];
  private all: string[][] = [
    ['MO-1001', '店庆纪念马克杯', '2', '79.80', '待支付'],
    ['MO-0998', '电子礼品卡 50 元', '1', '50.00', '已支付'],
    ['MO-0992', '品牌帆布袋', '1', '29.00', '已取消'],
  ];

  private filtered(): string[][] {
    if (this.tab === '待支付') {
      return this.all.filter((o: string[]) => o[4] === '待支付');
    }
    if (this.tab === '已支付') {
      return this.all.filter((o: string[]) => o[4] === '已支付' || o[4] === '待发货');
    }
    if (this.tab === '已取消') {
      return this.all.filter((o: string[]) => o[4] === '已取消');
    }
    return this.all;
  }

  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text('我的订单').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink).margin({ left: 8 })
        Blank().layoutWeight(1)
      }.padding(16).width('100%').backgroundColor(DesignTokens.canvas)

      Row() {
        ForEach(this.tabs, (t: string) => {
          Column({ space: 6 }) {
            Text(t)
              .fontSize(14)
              .fontWeight(this.tab === t ? FontWeight.Medium : FontWeight.Normal)
              .fontColor(this.tab === t ? DesignTokens.link : DesignTokens.body)
            Row()
              .width(28).height(2)
              .backgroundColor(this.tab === t ? DesignTokens.link : Color.Transparent)
          }
          .layoutWeight(1)
          .padding({ top: 12, bottom: 12 })
          .alignItems(HorizontalAlign.Center)
          .onClick(() => { this.tab = t; })
        })
      }.width('100%').backgroundColor(DesignTokens.canvas)

      if (this.filtered().length === 0) {
        Column() {
          Blank()
          Text('暂无订单').fontSize(15).fontColor(DesignTokens.body)
          Blank()
        }.width('100%').layoutWeight(1).alignItems(HorizontalAlign.Center)
      } else {
        List() {
          ForEach(this.filtered(), (o: string[]) => {
            ListItem() {
              Row({ space: 12 }) {
                Column().width(64).height(64).borderRadius(8).backgroundColor(DesignTokens.canvasSoft2)
                Column({ space: 4 }) {
                  Text(o[1]).fontSize(15).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
                  Text(`×${o[2]} · ¥${o[3]}`).fontSize(13).fontColor(DesignTokens.body)
                  Text(o[0]).fontSize(12).fontColor(DesignTokens.mute)
                }.alignItems(HorizontalAlign.Start).layoutWeight(1)
                Text(o[4])
                  .fontSize(13).fontWeight(FontWeight.Medium)
                  .fontColor(o[4] === '待支付' ? DesignTokens.link : DesignTokens.body)
              }
              .padding(12)
              .margin({ bottom: 10 })
              .backgroundColor(DesignTokens.canvas)
              .borderRadius(10)
              .onClick(() => this.onOpen('/mall/detail'))
            }
          })
        }.layoutWeight(1).width('100%').padding(16)
      }
    }
    .width('100%').height('100%').backgroundColor(DesignTokens.canvasSoft2)
  }
}

@Component
export struct NativeFriendPane {
  onClose: () => void = () => {};
  onOpenChat: () => void = () => {};
  @State query: string = '';
  @State searchHits: string[][] = [];
  @State incoming: string[][] = [
    ['i1', '王同学'],
    ['i2', '李老师'],
  ];
  @State friends: string[][] = [
    ['1', '小明', '刚刚在线'],
    ['2', '阿哲', '三天前'],
    ['3', '林林', '一周前'],
    ['4', '客服小助手', '昨天'],
  ];
  private directory: string[][] = [
    ['新同学小周', '同校'],
    ['外教 Anna', '口语'],
  ];

  private flash(msg: string) {
    promptAction.showToast({ message: msg });
  }

  private runSearch() {
    const q = this.query.trim();
    if (q.length === 0) {
      this.searchHits = [];
      return;
    }
    this.searchHits = this.directory.filter((d: string[]) => d[0].indexOf(q) >= 0);
    if (this.searchHits.length === 0) {
      this.flash('未找到用户');
    }
  }

  private accept(req: string[]) {
    this.friends = this.friends.concat([[req[0], req[1], '刚刚']]);
    this.incoming = this.incoming.filter((r: string[]) => r[0] !== req[0]);
    this.flash('已添加');
  }

  private reject(req: string[]) {
    this.incoming = this.incoming.filter((r: string[]) => r[0] !== req[0]);
  }

  @Builder
  SectionLabel(text: string) {
    Text(text).fontSize(14).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
  }

  build() {
    Column() {
      Row() {
        Text('‹').fontSize(28).fontColor(DesignTokens.link)
          .width(44).onClick(() => this.onClose())
        Text('通讯录')
          .fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
          .layoutWeight(1).textAlign(TextAlign.Center)
        Text('建群').fontSize(15).fontColor(DesignTokens.link)
          .width(44).textAlign(TextAlign.End)
          .onClick(() => this.flash('建群成功（mock）· 请到聊天 Tab'))
      }
      .width('100%').height(44).padding({ left: 8, right: 8 })
      .backgroundColor(DesignTokens.canvas)

      Scroll() {
        Column({ space: 16 }) {
          Row() {
            TextInput({ placeholder: '搜索好友', text: this.query })
              .layoutWeight(1).height(40).backgroundColor(Color.Transparent)
              .onChange((v: string) => { this.query = v; })
            Text('搜索').fontSize(14).fontWeight(FontWeight.Medium).fontColor(DesignTokens.link)
              .onClick(() => this.runSearch())
          }
          .width('100%')
          .padding({ left: 14, right: 14, top: 8, bottom: 8 })
          .backgroundColor(Color.White)
          .borderRadius(12)

          if (this.searchHits.length > 0) {
            this.SectionLabel('搜索结果')
            Column() {
              ForEach(this.searchHits, (hit: string[], i: number) => {
                if (i > 0) {
                  Row().width('100%').height(0.5).backgroundColor(DesignTokens.hairline).margin({ left: 70 })
                }
                Row({ space: 12 }) {
                  Text(hit[0].substring(hit[0].length - 1))
                    .width(44).height(44).textAlign(TextAlign.Center)
                    .fontColor(DesignTokens.link).backgroundColor('#260070F3').borderRadius(22)
                  Column({ space: 2 }) {
                    Text(hit[0]).fontSize(16).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
                    Text(hit[1]).fontSize(13).fontColor(DesignTokens.body)
                  }.alignItems(HorizontalAlign.Start).layoutWeight(1)
                  Text('加好友')
                    .fontSize(13).fontWeight(FontWeight.Medium).fontColor(Color.White)
                    .padding({ left: 12, right: 12, top: 6, bottom: 6 })
                    .backgroundColor(DesignTokens.link).borderRadius(14)
                    .onClick(() => this.flash('已发送好友申请'))
                }.width('100%').padding({ left: 14, right: 14, top: 12, bottom: 12 })
              })
            }.width('100%').backgroundColor(Color.White).borderRadius(12)
          }

          this.SectionLabel(`新的朋友（${this.incoming.length}）`)
          Column() {
            ForEach(this.incoming, (req: string[], i: number) => {
              if (i > 0) {
                Row().width('100%').height(0.5).backgroundColor(DesignTokens.hairline).margin({ left: 70 })
              }
              Row({ space: 12 }) {
                Text(req[1].substring(req[1].length - 1))
                  .width(44).height(44).textAlign(TextAlign.Center)
                  .fontColor(DesignTokens.link).backgroundColor('#260070F3').borderRadius(22)
                Column({ space: 2 }) {
                  Text(req[1]).fontSize(16).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
                  Text('请求添加你为好友').fontSize(13).fontColor(DesignTokens.body)
                }.alignItems(HorizontalAlign.Start).layoutWeight(1)
                Text('接受')
                  .fontSize(13).fontWeight(FontWeight.Medium).fontColor(Color.White)
                  .padding({ left: 12, right: 12, top: 6, bottom: 6 })
                  .backgroundColor(DesignTokens.link).borderRadius(14)
                  .onClick(() => this.accept(req))
                Text('拒绝').fontSize(13).fontColor(DesignTokens.mute)
                  .margin({ left: 8 })
                  .onClick(() => this.reject(req))
              }.width('100%').padding({ left: 14, right: 14, top: 12, bottom: 12 })
            })
          }.width('100%').backgroundColor(Color.White).borderRadius(12)

          this.SectionLabel(`好友（${this.friends.length}）`)
          Column() {
            if (this.friends.length === 0) {
              Text('暂无好友').fontSize(14).fontColor(DesignTokens.mute).padding(16)
            } else {
              ForEach(this.friends, (row: string[], i: number) => {
                if (i > 0) {
                  Row().width('100%').height(0.5).backgroundColor(DesignTokens.hairline).margin({ left: 70 })
                }
                Row({ space: 12 }) {
                  Text(row[1].substring(row[1].length - 1))
                    .width(44).height(44).textAlign(TextAlign.Center)
                    .fontColor(DesignTokens.link).backgroundColor('#260070F3').borderRadius(22)
                  Column({ space: 2 }) {
                    Text(row[1]).fontSize(16).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
                    Text(row[2]).fontSize(13).fontColor(DesignTokens.body)
                  }.alignItems(HorizontalAlign.Start).layoutWeight(1)
                }
                .width('100%')
                .padding({ left: 14, right: 14, top: 12, bottom: 12 })
                .onClick(() => this.onOpenChat())
              })
            }
          }.width('100%').backgroundColor(Color.White).borderRadius(12)
        }
        .width('100%')
        .padding(16)
        .padding({ bottom: 28 })
      }
      .layoutWeight(1).width('100%').scrollBar(BarState.Off)
    }
    .width('100%').height('100%').backgroundColor(DesignTokens.canvasSoft2)
  }
}

@Component
export struct NativeCommunitySearchPane {
  onClose: () => void = () => {};
  @State query: string = '';
  @State tab: number = 0;

  build() {
    Column() {
      Row({ space: 12 }) {
        Text('取消').fontSize(16).fontColor(DesignTokens.link).onClick(() => this.onClose())
        TextInput({ placeholder: '搜索动态、话题、用户', text: this.query })
          .layoutWeight(1).height(40)
          .backgroundColor(DesignTokens.canvasSoft2).borderRadius(10)
          .onChange((v: string) => { this.query = v; })
      }.padding(12).width('100%').backgroundColor(DesignTokens.canvas)

      Row() {
        ForEach(['动态', '话题', '用户'], (label: string, index: number) => {
          Text(label)
            .fontSize(15)
            .fontWeight(this.tab === index ? FontWeight.Medium : FontWeight.Regular)
            .fontColor(this.tab === index ? DesignTokens.link : DesignTokens.body)
            .layoutWeight(1)
            .textAlign(TextAlign.Center)
            .padding({ top: 10, bottom: 10 })
            .border({ width: { bottom: this.tab === index ? 2 : 0 }, color: DesignTokens.link })
            .onClick(() => { this.tab = index; })
        })
      }.width('100%').backgroundColor(DesignTokens.canvas)

      List() {
        if (this.tab === 0) {
          ListItem() { Text('张三：周末 hiking #户外').padding(16) }
          ListItem() { Text('李四：新车到店试驾').padding(16) }
        } else if (this.tab === 1) {
          ListItem() { Text('#换车季').padding(16) }
          ListItem() { Text('#保养日记').padding(16) }
          ListItem() { Text('#Flutter开发').padding(16) }
        } else {
          ListItem() { Text('张三 · 粉丝 128').padding(16) }
          ListItem() { Text('李四 · 粉丝 86').padding(16) }
        }
      }.layoutWeight(1).width('100%')
    }
    .width('100%')
    .height('100%')
    .backgroundColor(DesignTokens.canvasSoft2)
  }
}

@Component
export struct NativeCommunityPublishPane {
  onClose: () => void = () => {};
  @State content: string = '';
  @State mediaType: string = 'none';
  @State topic: string = '';
  @State showConvention: boolean = true;
  @State publishing: boolean = false;

  build() {
    Stack() {
      Column() {
        Row() {
          Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
          Blank().layoutWeight(1)
          Text('发布')
            .fontSize(14).fontWeight(FontWeight.Medium).fontColor(Color.White)
            .padding({ left: 16, right: 16, top: 8, bottom: 8 })
            .backgroundColor(this.content.trim().length > 0 ? '#1677FF' : '#991677FF')
            .borderRadius(20)
            .onClick(() => {
              if (this.content.trim().length === 0 || this.publishing) {
                return;
              }
              this.publishing = true;
              setTimeout(() => {
                this.publishing = false;
                this.onClose();
              }, 400);
            })
        }.padding(16).width('100%').backgroundColor(DesignTokens.canvas)

        TextArea({ placeholder: '记录一下吧', text: this.content })
          .height(160)
          .margin({ left: 16, right: 16, top: 8 })
          .backgroundColor(Color.Transparent)
          .onChange((v: string) => { this.content = v; })

        Row({ space: 8 }) {
          ForEach([['无媒体', 'none'], ['图片', 'image'], ['视频', 'video']], (pair: string[]) => {
            Text(pair[0])
              .fontSize(13)
              .fontColor(this.mediaType === pair[1] ? DesignTokens.link : DesignTokens.body)
              .padding({ left: 12, right: 12, top: 6, bottom: 6 })
              .backgroundColor(this.mediaType === pair[1] ? '#1A0070F3' : DesignTokens.canvasSoft2)
              .borderRadius(16)
              .onClick(() => { this.mediaType = pair[1]; })
          })
        }.padding({ left: 16, right: 16, top: 12 }).width('100%')

        if (this.mediaType === 'image') {
          Row() {
            Text('+ 添加图片').fontSize(14).fontColor(DesignTokens.link)
              .width(96).height(96).textAlign(TextAlign.Center)
              .backgroundColor(DesignTokens.canvasSoft2).borderRadius(8)
          }.padding({ left: 16, top: 12 })
        }
        if (this.mediaType === 'video') {
          Row() {
            Text('+ 添加视频').fontSize(14).fontColor(DesignTokens.link)
              .width(160).height(96).textAlign(TextAlign.Center)
              .backgroundColor(DesignTokens.canvasSoft2).borderRadius(8)
          }.padding({ left: 16, top: 12 })
        }

        Row() {
          Text(this.topic.length > 0 ? `#${this.topic}` : '+ 添加话题')
            .fontSize(14)
            .fontColor(DesignTokens.link)
            .padding({ left: 12, right: 12, top: 8, bottom: 8 })
            .backgroundColor('#1A0070F3')
            .borderRadius(16)
            .onClick(() => {
              this.topic = this.topic.length > 0 ? '' : '换车季';
            })
        }.padding({ left: 16, top: 16 }).width('100%')

        Blank().layoutWeight(1)
      }
      .width('100%')
      .height('100%')
      .backgroundColor(DesignTokens.canvas)

      if (this.showConvention) {
        Column() {
          Column({ space: 12 }) {
            Text('社区公约').fontSize(18).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
            Text('请文明发言，禁止发布违法违规、广告引流等内容。')
              .fontSize(14).fontColor(DesignTokens.body)
            Button('我知道了')
              .backgroundColor(DesignTokens.link)
              .onClick(() => { this.showConvention = false; })
          }
          .padding(24)
          .width('80%')
          .backgroundColor(DesignTokens.canvas)
          .borderRadius(12)
        }
        .width('100%')
        .height('100%')
        .justifyContent(FlexAlign.Center)
        .backgroundColor('#80000000')
      }
    }
    .width('100%')
    .height('100%')
  }
}

@Component
export struct NativeCommunityCommentPane {
  onClose: () => void = () => {};
  @State draft: string = '';
  @State comments: string[][] = [
    ['李四', '说得对！周末一起去门店看看'],
    ['赵六', '同感 +1，双擎确实省油'],
    ['客服小助手', '欢迎到店试驾，预约通道已开放'],
  ];

  build() {
    Column() {
      Row() {
        Column()
          .width(36).height(4).borderRadius(2)
          .backgroundColor(DesignTokens.hairline)
      }.width('100%').justifyContent(FlexAlign.Center).padding({ top: 8 })

      Row() {
        Text(`评论 ${this.comments.length}`)
          .fontSize(16).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
        Blank().layoutWeight(1)
        Text('关闭').fontSize(15).fontColor(DesignTokens.link).onClick(() => this.onClose())
      }.padding(16).width('100%')

      List() {
        ForEach(this.comments, (item: string[], index: number) => {
          ListItem() {
            Row({ space: 12 }) {
              Text(item[0].substring(0, 1))
                .fontSize(14).fontColor(DesignTokens.link)
                .width(36).height(36).textAlign(TextAlign.Center)
                .borderRadius(18).backgroundColor('#260070F3')
              Column({ space: 4 }) {
                Text(item[0]).fontSize(14).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
                Text(item[1]).fontSize(14).fontColor(DesignTokens.body)
              }.alignItems(HorizontalAlign.Start).layoutWeight(1)
            }.padding({ left: 16, right: 16, top: 12, bottom: 12 }).width('100%')
          }
        }, (item: string[], index: number) => `${index}_${item[0]}`)
      }.layoutWeight(1).width('100%')

      Row({ space: 10 }) {
        TextInput({ placeholder: '说说你的看法…', text: this.draft })
          .layoutWeight(1).height(40)
          .backgroundColor(DesignTokens.canvasSoft2).borderRadius(20)
          .padding({ left: 12, right: 12 })
          .onChange((v: string) => { this.draft = v; })
        Text('发送')
          .fontSize(14).fontWeight(FontWeight.Medium).fontColor(Color.White)
          .padding({ left: 14, right: 14, top: 10, bottom: 10 })
          .backgroundColor(DesignTokens.link).borderRadius(20)
          .onClick(() => {
            const text = this.draft.trim();
            if (text.length === 0) {
              return;
            }
            this.comments = [['我', text], ...this.comments];
            this.draft = '';
          })
      }.padding(12).width('100%').backgroundColor(DesignTokens.canvas)
    }
    .width('100%')
    .height('100%')
    .backgroundColor(DesignTokens.canvas)
  }
}

@Component
export struct NativeCommunityImagePreviewPane {
  onClose: () => void = () => {};
  @State index: number = 0;
  private labels: string[] = ['社区配图 1', '社区配图 2', '社区配图 3'];

  build() {
    Stack() {
      Column() {
        Blank().layoutWeight(1)
        Text(this.labels[this.index])
          .fontSize(18).fontWeight(FontWeight.Medium).fontColor(Color.White)
          .width('100%').height(360)
          .textAlign(TextAlign.Center)
          .backgroundColor('#1FFFFFFF')
          .borderRadius(8)
          .margin({ left: 24, right: 24 })
        Blank().layoutWeight(1)
      }.width('100%').height('100%')

      Column() {
        Row() {
          Text('关闭').fontSize(15).fontColor(Color.White).onClick(() => this.onClose())
          Blank().layoutWeight(1)
          Text(`${this.index + 1}/${this.labels.length}`).fontSize(15).fontColor(Color.White)
        }.padding(16).width('100%')
        Blank().layoutWeight(1)
        Row() {
          Text('上一张').fontSize(15).fontColor(Color.White)
            .onClick(() => {
              if (this.index > 0) {
                this.index -= 1;
              }
            })
          Blank().layoutWeight(1)
          Text('下一张').fontSize(15).fontColor(Color.White)
            .onClick(() => {
              if (this.index < this.labels.length - 1) {
                this.index += 1;
              }
            })
        }.padding(24).width('100%')
      }.width('100%').height('100%')
    }
    .width('100%')
    .height('100%')
    .backgroundColor(Color.Black)
  }
}

@Component
export struct NativeMallPane {
  onClose: () => void = () => {};
  onOpen: (path: string) => void = (_p: string) => {};
  @State category: number = 0;
  @State filter: number = 1;
  private categories: string[] = ['推荐', '0元起兑', '国庆季', '钻铂专享', '数码家电', '生活好物'];
  private filters: string[] = ['积分', '热兑', '上新', '筛选'];
  private products: string[][] = [
    ['店庆纪念马克杯', '39.90元', '已兑2391', '0'],
    ['电子礼品卡 50 元', '50.00元', '已兑2877', '1'],
    ['会员壁纸包', '6.00元', '已兑6566', '1'],
    ['线上精品课兑换', '99.00元', '已兑9492', '1'],
    ['品牌帆布袋', '29.00元', '已兑5613', '0'],
    ['冬季保暖围巾', '128.00元', '已兑8555', '0'],
  ];

  build() {
    Stack({ alignContent: Alignment.Bottom }) {
      Column() {
        Row({ space: 8 }) {
          Text('‹').fontSize(28).fontColor(DesignTokens.ink).onClick(() => this.onClose())
          Row({ space: 6 }) {
            Text('⌕').fontSize(14).fontColor(DesignTokens.mute)
            Text('视频会员卡').fontSize(14).fontColor(DesignTokens.mute).layoutWeight(1)
            Text('搜索')
              .fontSize(12).fontWeight(FontWeight.Medium).fontColor(Color.White)
              .padding({ left: 12, right: 12, top: 6, bottom: 6 })
              .backgroundColor(DesignTokens.link).borderRadius(6)
          }
          .layoutWeight(1).height(36)
          .padding({ left: 10, right: 4 })
          .backgroundColor(DesignTokens.canvasSoft2).borderRadius(8)
        }.padding({ left: 12, right: 12, top: 8, bottom: 8 }).width('100%').backgroundColor(DesignTokens.canvas)

        Scroll().scrollable(ScrollDirection.Horizontal).scrollBar(BarState.Off) {
          Row({ space: 18 }) {
            ForEach(this.categories, (label: string, i: number) => {
              Column({ space: 4 }) {
                Text(label)
                  .fontSize(i === this.category ? 15 : 14)
                  .fontWeight(i === this.category ? FontWeight.Medium : FontWeight.Regular)
                  .fontColor(i === this.category ? DesignTokens.ink : DesignTokens.mute)
                Column()
                  .width(16).height(2)
                  .backgroundColor(i === this.category ? DesignTokens.link : Color.Transparent)
              }.onClick(() => { this.category = i; })
            })
          }.padding({ left: 12, right: 12, top: 8, bottom: 8 })
        }.width('100%').backgroundColor(DesignTokens.canvas)

        Row({ space: 6 }) {
          ForEach(this.filters, (label: string, i: number) => {
            Row({ space: 2 }) {
              if (i === this.filter) {
                Text('✓').fontSize(11).fontColor(DesignTokens.link)
              }
              Text(label)
                .fontSize(12)
                .fontWeight(i === this.filter ? FontWeight.Medium : FontWeight.Regular)
                .fontColor(i === this.filter ? DesignTokens.link : DesignTokens.body)
              if (label === '积分' || label === '筛选') {
                Text('▾').fontSize(11)
                  .fontColor(i === this.filter ? DesignTokens.link : DesignTokens.mute)
              }
            }
            .layoutWeight(1).height(30)
            .justifyContent(FlexAlign.Center)
            .backgroundColor(i === this.filter ? '#1A0070F3' : DesignTokens.canvas)
            .border({
              width: 1,
              color: i === this.filter ? DesignTokens.link : DesignTokens.hairline,
            })
            .borderRadius(8)
            .onClick(() => { this.filter = i; })
          })
        }.padding({ left: 12, right: 12, top: 8, bottom: 8 }).width('100%')

        Grid() {
          ForEach(this.products, (p: string[]) => {
            GridItem() {
              Column({ space: 8 }) {
                Text(p[3] === '1' ? '虚拟' : '实物')
                  .fontSize(12).fontColor(DesignTokens.mute)
                  .width('100%').height(110)
                  .textAlign(TextAlign.Center)
                  .backgroundColor(DesignTokens.canvasSoft2).borderRadius(8)
                Text(p[0]).fontSize(14).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
                  .maxLines(2)
                Text(p[1]).fontSize(15).fontWeight(FontWeight.Medium).fontColor('#E64D33')
                Text(p[2]).fontSize(11).fontColor(DesignTokens.mute)
              }
              .padding(8)
              .backgroundColor(DesignTokens.canvas)
              .borderRadius(10)
              .onClick(() => this.onOpen('/mall/detail'))
            }
          })
        }
        .columnsTemplate('1fr 1fr')
        .columnsGap(8)
        .rowsGap(8)
        .padding({ left: 10, right: 10, bottom: 88 })
        .layoutWeight(1)
        .width('100%')
      }
      .width('100%')
      .height('100%')
      .backgroundColor(DesignTokens.canvasSoft2)

      Row() {
        Text('我的订单')
          .fontSize(14).fontWeight(FontWeight.Medium).fontColor(Color.White)
          .textAlign(TextAlign.Center)
          .layoutWeight(1)
          .padding({ top: 14, bottom: 14 })
          .backgroundColor(DesignTokens.link)
          .borderRadius(24)
          .onClick(() => this.onOpen('/mall/orders'))
      }.padding({ left: 24, right: 24, bottom: 16 }).width('100%')
    }
    .width('100%')
    .height('100%')
  }
}

@Component
export struct NativeWalletPane {
  onClose: () => void = () => {};
  @State amount: string = '';
  @State channel: number = 1;
  private flows: string[][] = [
    ['membership_pay', 'ref m2', '-30.00'],
    ['充值', 'ref alipay', '+100.00'],
    ['充值', 'ref alipay', '+1.00'],
  ];

  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text('我的钱包').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
        Blank().layoutWeight(1)
      }.padding({ left: 16, right: 16, top: 12, bottom: 12 }).width('100%').backgroundColor(DesignTokens.canvas)

      Scroll() {
        Column({ space: 12 }) {
          Column({ space: 8 }) {
            Text('余额（元）').fontSize(14).fontColor(DesignTokens.body)
            Text('71.00').fontSize(32).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
          }
          .alignItems(HorizontalAlign.Start)
          .padding(20)
          .width('100%')
          .backgroundColor(DesignTokens.canvas)
          .borderRadius(12)

          Text('充值').fontSize(16).fontWeight(FontWeight.Bold).fontColor(DesignTokens.ink)
          TextInput({ placeholder: '金额 0.01-50000', text: this.amount })
            .height(44)
            .backgroundColor(DesignTokens.canvas)
            .border({ width: 1, color: DesignTokens.hairline })
            .borderRadius(8)
            .padding({ left: 12, right: 12 })
            .onChange((v: string) => { this.amount = v; })

          Flex({ wrap: FlexWrap.Wrap }) {
            Text(`${this.channel === 1 ? '✓ ' : ''}支付宝`)
              .fontSize(13)
              .fontColor(this.channel === 1 ? Color.White : DesignTokens.ink)
              .padding({ left: 12, right: 12, top: 8, bottom: 8 })
              .margin({ right: 8, bottom: 8 })
              .backgroundColor(this.channel === 1 ? DesignTokens.link : DesignTokens.canvas)
              .border({ width: 1, color: this.channel === 1 ? DesignTokens.link : DesignTokens.hairline })
              .borderRadius(8)
              .onClick(() => { this.channel = 1; })
            Text(`${this.channel === 2 ? '✓ ' : ''}微信`)
              .fontSize(13)
              .fontColor(this.channel === 2 ? Color.White : DesignTokens.ink)
              .padding({ left: 12, right: 12, top: 8, bottom: 8 })
              .margin({ right: 8, bottom: 8 })
              .backgroundColor(this.channel === 2 ? DesignTokens.link : DesignTokens.canvas)
              .border({ width: 1, color: this.channel === 2 ? DesignTokens.link : DesignTokens.hairline })
              .borderRadius(8)
              .onClick(() => { this.channel = 2; })
            Text(`${this.channel === 3 ? '✓ ' : ''}银行卡`)
              .fontSize(13)
              .fontColor(this.channel === 3 ? Color.White : DesignTokens.ink)
              .padding({ left: 12, right: 12, top: 8, bottom: 8 })
              .margin({ right: 8, bottom: 8 })
              .backgroundColor(this.channel === 3 ? DesignTokens.link : DesignTokens.canvas)
              .border({ width: 1, color: this.channel === 3 ? DesignTokens.link : DesignTokens.hairline })
              .borderRadius(8)
              .onClick(() => { this.channel = 3; })
            ForEach(['10', '50', '100'], (a: string) => {
              Text(a)
                .fontSize(13).fontColor(DesignTokens.ink)
                .padding({ left: 16, right: 16, top: 8, bottom: 8 })
                .margin({ right: 8, bottom: 8 })
                .backgroundColor(DesignTokens.canvas)
                .border({ width: 1, color: DesignTokens.hairline })
                .borderRadius(8)
                .onClick(() => { this.amount = a; })
            })
          }.width('100%')

          Button('立即充值').backgroundColor(DesignTokens.link).width('100%')

          Text('流水').fontSize(16).fontWeight(FontWeight.Bold).fontColor(DesignTokens.ink)
          ForEach(this.flows, (f: string[]) => {
            Row() {
              Column({ space: 4 }) {
                Text(f[0]).fontSize(15).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
                Text(f[1]).fontSize(12).fontColor(DesignTokens.mute)
              }.alignItems(HorizontalAlign.Start).layoutWeight(1)
              Text(f[2])
                .fontSize(15).fontWeight(FontWeight.Medium)
                .fontColor(f[2].startsWith('+') ? '#1A9933' : DesignTokens.ink)
            }
            .padding(14)
            .width('100%')
            .backgroundColor(DesignTokens.canvas)
            .borderRadius(10)
          })
        }.padding(16).width('100%')
      }.layoutWeight(1).width('100%')
    }
    .width('100%')
    .height('100%')
    .backgroundColor(DesignTokens.canvasSoft2)
  }
}

@Component
export struct NativeShortVideoPlayPane {
  onClose: () => void = () => {};
  @State playing: boolean = true;
  @State liked: boolean = false;

  private flash(msg: string) {
    promptAction.showToast({ message: msg });
  }

  build() {
    Column() {
      Row() {
        Text('‹').fontSize(28).fontColor(Color.White).width(44).onClick(() => this.onClose())
        Text('播放').fontSize(17).fontWeight(FontWeight.Medium).fontColor(Color.White)
          .layoutWeight(1).textAlign(TextAlign.Center)
        Blank().width(44)
      }.width('100%').height(44).padding({ left: 8, right: 8 })

      Column() {
        Blank()
        Text(this.playing ? '播放中 · 单击暂停' : '已暂停 · 单击继续')
          .fontSize(16).fontColor(Color.White)
          .onClick(() => { this.playing = !this.playing; })
        Blank()
      }.layoutWeight(1).width('100%').alignItems(HorizontalAlign.Center)

      Row() {
        Text(this.liked ? '已赞' : '赞').fontSize(15).fontColor(Color.White).layoutWeight(1)
          .textAlign(TextAlign.Center).onClick(() => { this.liked = !this.liked; })
        Text('评').fontSize(15).fontColor(Color.White).layoutWeight(1)
          .textAlign(TextAlign.Center).onClick(() => this.flash('评论（mock）'))
        Text('分享').fontSize(15).fontColor(Color.White).layoutWeight(1)
          .textAlign(TextAlign.Center).onClick(() => this.flash('分享（mock）'))
      }.width('100%').padding(16)

      Text('口语跟读 · 第一课  #口语')
        .fontSize(14).fontColor(Color.White)
        .width('100%').padding(16)
    }
    .width('100%').height('100%').backgroundColor(Color.Black)
  }
}

@Component
export struct NativeShortVideoPublishPane {
  onClose: () => void = () => {};
  @State titleText: string = '';
  @State topic: string = '#口语';

  private flash(msg: string) {
    promptAction.showToast({ message: msg });
  }

  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text('发布短视频').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink).margin({ left: 8 })
        Blank().layoutWeight(1)
      }.padding(16).width('100%').backgroundColor(DesignTokens.canvas)

      Scroll() {
        Column({ space: 12 }) {
          Text('本地预览 · 选视频/拍摄见 platform-gap')
            .fontSize(13).fontColor(DesignTokens.body)
            .width('100%').height(180).textAlign(TextAlign.Center)
            .backgroundColor(DesignTokens.canvasSoft2).borderRadius(12)

          Text('标题').fontSize(15).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
          TextInput({ placeholder: '输入标题（≤40）', text: this.titleText })
            .height(44).padding({ left: 12 }).backgroundColor(DesignTokens.canvas).borderRadius(8)
            .onChange((v: string) => { this.titleText = v.length > 40 ? v.substring(0, 40) : v; })

          Text('话题').fontSize(15).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
          TextInput({ text: this.topic })
            .height(44).padding({ left: 12 }).backgroundColor(DesignTokens.canvas).borderRadius(8)
            .onChange((v: string) => { this.topic = v; })

          Text('提交')
            .fontSize(16).fontWeight(FontWeight.Medium).fontColor(Color.White)
            .width('100%').textAlign(TextAlign.Center)
            .padding({ top: 14, bottom: 14 })
            .margin({ top: 8 })
            .backgroundColor(DesignTokens.link).borderRadius(10)
            .onClick(() => {
              if (this.titleText.trim().length === 0) {
                this.flash('请填写标题');
              } else {
                this.flash('已提交（mock）');
                this.onClose();
              }
            })
        }.width('100%').padding(16)
      }.layoutWeight(1).width('100%').scrollBar(BarState.Off)
    }
    .width('100%').height('100%').backgroundColor(DesignTokens.canvasSoft2)
  }
}

@Component
export struct NativeShortVideoHelpPane {
  onClose: () => void = () => {};
  private steps: string[] = ['选择或拍摄视频', '填写标题与话题', '提交后等待审核', '在「我的作品」查看'];

  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text('短视频帮助').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink).margin({ left: 8 })
        Blank().layoutWeight(1)
      }.padding(16).width('100%').backgroundColor(DesignTokens.canvas)

      List() {
        ForEach(this.steps, (step: string, i: number) => {
          ListItem() {
            Column({ space: 4 }) {
              Text(`步骤 ${i + 1}`).fontSize(15).fontWeight(FontWeight.Medium).fontColor(DesignTokens.link)
              Text(step).fontSize(14).fontColor(DesignTokens.ink)
            }
            .width('100%')
            .padding(14)
            .margin({ bottom: 10 })
            .backgroundColor(DesignTokens.canvas)
            .borderRadius(10)
            .alignItems(HorizontalAlign.Start)
          }
        })
      }.layoutWeight(1).width('100%').padding(16)
    }
    .width('100%').height('100%').backgroundColor(DesignTokens.canvasSoft2)
  }
}

@Component
export struct NativeShortVideoPane {
  onClose: () => void = () => {};
  onOpen: (path: string) => void = (_p: string) => {};
  private clips: string[][] = [
    ['新车到店 · 15s', '播放 2.1k · 赞 186'],
    ['保养小贴士', '播放 980 · 赞 64'],
    ['交车仪式', '播放 3.4k · 赞 412'],
    ['展厅速览', '播放 1.1k · 赞 88'],
  ];

  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text('小视频').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
        Blank().layoutWeight(1)
      }.padding({ left: 16, right: 16, top: 12, bottom: 12 }).width('100%').backgroundColor(DesignTokens.canvas)

      Scroll() {
        Column({ space: 12 }) {
          Row({ space: 12 }) {
            Column()
              .width(56).height(56).borderRadius(28)
              .backgroundColor('#330070F3')
            Column({ space: 4 }) {
              Text('沃德龙鼎').fontSize(16).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
              Text('粉丝 12.6万 · 作品 48').fontSize(13).fontColor(DesignTokens.body)
            }.alignItems(HorizontalAlign.Start)
            Blank().layoutWeight(1)
          }
          .padding(16)
          .width('100%')
          .linearGradient({
            angle: 180,
            colors: [['#DCEEF9', 0], [DesignTokens.canvasSoft2, 1]],
          })

          Row() {
            Text('我发布的小视频').fontSize(16).fontWeight(FontWeight.Bold).fontColor(DesignTokens.ink)
            Blank().layoutWeight(1)
            Text('如何拍摄小视频')
              .fontSize(13).fontColor(DesignTokens.link)
              .onClick(() => this.onOpen('/video/short/help'))
          }.padding({ left: 16, right: 16 }).width('100%')

          Grid() {
            GridItem() {
              Column({ space: 8 }) {
                Text('+').fontSize(28).fontColor(DesignTokens.link)
                Text('拍一个').fontSize(13).fontColor(DesignTokens.link)
              }
              .justifyContent(FlexAlign.Center)
              .width('100%').height(160)
              .border({ width: 1, color: DesignTokens.link, style: BorderStyle.Dashed })
              .borderRadius(10)
              .onClick(() => this.onOpen('/video/short/publish'))
            }
            ForEach(this.clips, (c: string[]) => {
              GridItem() {
                Column({ space: 6 }) {
                  Text('▶')
                    .fontSize(28).fontColor(Color.White)
                    .width('100%').height(120)
                    .textAlign(TextAlign.Center)
                    .backgroundColor('#1A1F29')
                    .borderRadius(10)
                  Text(c[0]).fontSize(13).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
                    .maxLines(1)
                  Text(c[1]).fontSize(11).fontColor(DesignTokens.mute)
                }.onClick(() => this.onOpen('/video/short/play'))
              }
            })
          }
          .columnsTemplate('1fr 1fr')
          .columnsGap(8)
          .rowsGap(8)
          .padding({ left: 16, right: 16 })
          .width('100%')

          Text('没有更多了')
            .fontSize(12).fontColor(DesignTokens.mute)
            .width('100%').textAlign(TextAlign.Center)
            .padding({ top: 16, bottom: 16 })
        }.width('100%')
      }.layoutWeight(1).width('100%')
    }
    .width('100%')
    .height('100%')
    .backgroundColor(DesignTokens.canvasSoft2)
  }
}

@Component
export struct NativeLivePane {
  onClose: () => void = () => {};
  @State roomId: string = '';
  @State joined: boolean = false;
  @State signals: string[] = ['state: idle'];
  /** Align Compose LiveMockData */
  private rooms: string[][] = [
    ['mock_room_001', '晚间答疑直播', '主播 · 小智'],
    ['mock_room_002', '口语陪练公开课', '主播 · 阿语'],
    ['mock_room_003', '周末分享会', '主播 · Demo'],
  ];

  build() {
    if (this.roomId.length > 0) {
      Column() {
        Row() {
          Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => {
            this.roomId = '';
            this.joined = false;
            this.signals = ['state: idle'];
          })
          Text(`直播 ${this.roomId}`).fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink).margin({ left: 8 })
          Blank().layoutWeight(1)
        }.padding({ left: 16, right: 16, top: 12, bottom: 12 }).width('100%').backgroundColor(DesignTokens.canvas)

        Column() {
          Text(this.joined ? 'WS: connected · paused 保持连接' : 'WS: disconnected')
            .fontSize(15).fontColor(DesignTokens.ink)
        }
        .width('100%').height(180)
        .justifyContent(FlexAlign.Center)
        .backgroundColor(DesignTokens.canvasSoft2)
        .borderRadius(12)
        .margin({ left: 16, right: 16, top: 8 })

        Column({ space: 12 }) {
          Text(this.roomHost()).fontSize(14).fontColor(DesignTokens.body)
          Text('信令（上限 30）').fontSize(15).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
          ForEach(this.signals.slice(-30), (line: string) => {
            Text(line).fontSize(12).fontColor('#888888')
          })
          Button('发送 Mock 信令 live.join')
            .backgroundColor(DesignTokens.link)
            .width('100%')
            .onClick(() => {
              this.joined = true;
              const next = this.signals.concat([`signal: live.join payload={room=${this.roomId}}`]);
              this.signals = next.slice(-30);
            })
          Button('退订 liveSignal')
            .width('100%')
            .onClick(() => {
              this.joined = false;
              this.signals = this.signals.concat(['state: left']);
            })
          Text('Realtime SDK 未接入；本页 mock 信令列表。')
            .fontSize(12).fontColor('#888888')
        }
        .alignItems(HorizontalAlign.Start)
        .padding(16)
        .width('100%')
        Blank().layoutWeight(1)
      }
      .width('100%').height('100%')
      .backgroundColor(DesignTokens.canvasSoft2)
    } else {
      Column() {
        Row() {
          Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
          Text('直播').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink).margin({ left: 8 })
          Blank().layoutWeight(1)
        }.padding({ left: 16, right: 16, top: 12, bottom: 12 }).width('100%').backgroundColor(DesignTokens.canvas)

        Text('点选进入房间入口 · 推流/实时通道见 gap registry')
          .fontSize(13).fontColor(DesignTokens.body)
          .padding({ left: 16, right: 16, top: 12 })
          .width('100%')

        List() {
          ForEach(this.rooms, (room: string[]) => {
            ListItem() {
              Column({ space: 4 }) {
                Text(room[1]).fontSize(15).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
                Text(room[2]).fontSize(12).fontColor('#888888')
              }
              .alignItems(HorizontalAlign.Start)
              .padding({ left: 16, right: 16, top: 12, bottom: 12 })
              .width('100%')
              .onClick(() => { this.roomId = room[0]; })
            }
          })
        }.layoutWeight(1).width('100%')
      }
      .width('100%').height('100%')
      .backgroundColor(DesignTokens.canvasSoft2)
    }
  }

  private roomHost(): string {
    const room = this.rooms.find((r: string[]) => r[0] === this.roomId);
    return room !== undefined ? room[2] : '';
  }
}

@Component
export struct NativeAiStreamPane {
  onClose: () => void = () => {};
  @State input: string = '';
  @State streaming: boolean = false;
  @State bubbles: string[][] = [
    ['welcome', '你好，我是 AI 小石头——本 App / 4S 店的业务向导。你可以问「二手车入口在哪」「如何登录」或点下方快捷问。'],
  ];
  private chips: string[] = ['二手车入口在哪里？', '怎么登录账号？', '数据分析怎么看？'];

  private send(prompt: string): void {
    const q = prompt.trim();
    if (q.length === 0 || this.streaming) {
      return;
    }
    this.bubbles = [...this.bubbles, ['user', q]];
    this.input = '';
    this.streaming = true;
    let reply = `关于「${q}」：可指路到二手车、登录、数据分析等入口。（mock 流）`;
    if (q.indexOf('二手车') >= 0) {
      reply = '二手车入口：首页「二手车」或全部服务 → 二手车（/home/used_car）。（mock）';
    } else if (q.indexOf('登录') >= 0) {
      reply = '登录：我的 Tab 点头像/登录，或打开 /auth/login。（mock）';
    } else if (q.indexOf('数据') >= 0) {
      reply = '数据分析：全部服务 →「数据分析」（/home/data_analytics）。（mock）';
    }
    setTimeout(() => {
      this.bubbles = [...this.bubbles, ['assistant', reply]];
      this.streaming = false;
    }, 350);
  }

  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text('AI 小石头').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
        Blank().layoutWeight(1)
      }.padding({ left: 16, right: 16, top: 12, bottom: 12 }).width('100%').backgroundColor(DesignTokens.canvas)

      List() {
        ForEach(this.bubbles, (b: string[], i: number) => {
          ListItem() {
            Row() {
              if (b[0] === 'user') {
                Blank().layoutWeight(1)
              }
              Text(b[1])
                .fontSize(15)
                .fontColor(b[0] === 'user' ? Color.White : DesignTokens.ink)
                .padding(12)
                .backgroundColor(b[0] === 'user' ? DesignTokens.link : DesignTokens.canvas)
                .borderRadius(12)
                .constraintSize({ maxWidth: '75%' })
              if (b[0] === 'assistant') {
                Blank().layoutWeight(1)
              }
            }.padding({ left: 16, right: 16, top: 6, bottom: 6 }).width('100%')
          }
        }, (b: string[], i: number) => `${i}_${b[0]}`)
      }.layoutWeight(1).width('100%').backgroundColor(DesignTokens.canvasSoft2)

      Scroll().scrollable(ScrollDirection.Horizontal).scrollBar(BarState.Off) {
        Row({ space: 8 }) {
          ForEach(this.chips, (chip: string) => {
            Text(chip)
              .fontSize(13).fontColor(DesignTokens.link)
              .padding({ left: 12, right: 12, top: 8, bottom: 8 })
              .backgroundColor('#1A0070F3')
              .borderRadius(16)
              .onClick(() => this.send(chip))
          })
        }.padding({ left: 16, right: 16, top: 8, bottom: 8 })
      }.width('100%')

      Row({ space: 10 }) {
        TextInput({ placeholder: '输入问题…', text: this.input })
          .layoutWeight(1).height(40)
          .backgroundColor(DesignTokens.canvas).borderRadius(20)
          .padding({ left: 12, right: 12 })
          .onChange((v: string) => { this.input = v; })
        if (this.streaming) {
          Text('停止').fontSize(14).fontColor(DesignTokens.link)
            .onClick(() => { this.streaming = false; })
        } else {
          Text('发送')
            .fontSize(14).fontWeight(FontWeight.Medium).fontColor(Color.White)
            .padding({ left: 14, right: 14, top: 10, bottom: 10 })
            .backgroundColor(DesignTokens.link).borderRadius(20)
            .onClick(() => this.send(this.input))
        }
      }.padding(12).width('100%').backgroundColor(DesignTokens.canvas)
    }
    .width('100%')
    .height('100%')
  }
}

@Component
export struct NativeScanPane {
  onClose: () => void = () => {};
  onOpen: (path: string) => void = (_p: string) => {};
  @State result: string = '';
  @State torchOn: boolean = false;
  private pink: string = '#FFFF1493';

  build() {
    Column() {
      if (this.result.length > 0) {
        Row() {
          Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
          Text('扫一扫').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink).margin({ left: 8 })
          Blank().layoutWeight(1)
        }.padding({ left: 16, right: 16, top: 12, bottom: 12 }).width('100%').backgroundColor(DesignTokens.canvas)

        Column({ space: 12 }) {
          Text('扫码结果').fontSize(15).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
          Text(this.result).fontSize(15).fontColor(DesignTokens.link)
          Text('打开结果')
            .fontSize(15).fontColor(Color.White)
            .width('100%').textAlign(TextAlign.Center)
            .padding({ top: 12, bottom: 12 })
            .backgroundColor(DesignTokens.link).borderRadius(8)
            .onClick(() => {
              const payload = this.result;
              this.onOpen(payload);
              this.onClose();
            })
          Text('继续扫码')
            .fontSize(15).fontColor(DesignTokens.link)
            .onClick(() => { this.result = ''; })
        }
        .alignItems(HorizontalAlign.Start)
        .padding(16)
        .width('100%')
        Blank().layoutWeight(1)
      } else {
        Column() {
          Row() {
            Text('‹').fontSize(28).fontColor(Color.White).width(44).onClick(() => this.onClose())
            Text('扫一扫').fontSize(15).fontColor(Color.White)
              .layoutWeight(1).textAlign(TextAlign.Center)
            Text(this.torchOn ? '🔦' : '💡')
              .fontSize(20).fontColor(Color.White).width(44).textAlign(TextAlign.Center)
              .onClick(() => { this.torchOn = !this.torchOn; })
          }.width('100%').height(44).padding({ left: 8, right: 8 })

          Blank().layoutWeight(1)
          Column({ space: 20 }) {
            Stack({ alignContent: Alignment.Center }) {
              Column()
                .width(280).height(280)
                .border({ width: 3, color: this.pink })
                .borderRadius(10)
              Row().width('90%').height(2).backgroundColor(this.pink)
            }
            Text('将二维码放入框内，即可自动扫码')
              .fontSize(14).fontColor('#E6FFFFFF')
          }.alignItems(HorizontalAlign.Center)
          Blank().layoutWeight(1)
          Text('模拟扫码成功')
            .fontSize(15).fontColor(Color.White)
            .padding({ left: 24, right: 24, top: 12, bottom: 12 })
            .margin({ bottom: 40 })
            .backgroundColor(this.pink).borderRadius(8)
            .onClick(() => { this.result = 'myai://mall/orders?id=A1024'; })
        }
        .width('100%')
        .layoutWeight(1)
        .backgroundColor(Color.Black)
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor(DesignTokens.canvasSoft2)
  }
}

@Component
export struct NativeStrategyPane {
  onClose: () => void = () => {};
  @State tab: number = 0;
  @State period: number = 4;
  private tabs: string[] = ['推荐', '逆向', '趋势'];
  private periods: string[] = ['今年来', '近1周', '近1月', '近3月', '近1年'];
  private assets: string[][] = [
    ['A股', '+19.22%', '1'], ['中债', '+3.15%', '1'], ['黄金', '+8.76%', '1'],
    ['港股', '+12.40%', '1'], ['美股', '+15.88%', '1'], ['原油', '-2.34%', '0'],
    ['美元债', '-1.80%', '0'], ['商品', '+4.56%', '1'], ['现金', '+1.20%', '1'],
  ];

  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text('策略').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink).margin({ left: 8 })
        Blank().layoutWeight(1)
      }.padding({ left: 16, right: 16, top: 12, bottom: 12 }).width('100%').backgroundColor(DesignTokens.canvas)

      Scroll() {
        Column({ space: 16 }) {
          Row({ space: 32 }) {
            ForEach(this.tabs, (label: string, i: number) => {
              Column({ space: 8 }) {
                Text(label)
                  .fontSize(16)
                  .fontWeight(this.tab === i ? FontWeight.Medium : FontWeight.Regular)
                  .fontColor(this.tab === i ? DesignTokens.ink : DesignTokens.body)
                Row().width(20).height(3)
                  .backgroundColor(this.tab === i ? DesignTokens.link : Color.Transparent)
                  .borderRadius(2)
              }.onClick(() => { this.tab = i; })
            }, (label: string, i: number) => `${label}-${i}`)
          }.justifyContent(FlexAlign.Center).width('100%')

          Column({ space: 12 }) {
            Row({ space: 8 }) {
              ForEach(this.periods, (p: string, i: number) => {
                Text(p)
                  .fontSize(12)
                  .fontColor(this.period === i ? Color.White : DesignTokens.body)
                  .padding({ left: 10, right: 10, top: 6, bottom: 6 })
                  .backgroundColor(this.period === i ? DesignTokens.link : DesignTokens.canvasSoft2)
                  .borderRadius(12)
                  .onClick(() => { this.period = i; })
              }, (p: string, i: number) => `${p}-${i}`)
            }

            Flex({ wrap: FlexWrap.Wrap, justifyContent: FlexAlign.SpaceBetween }) {
              ForEach(this.assets, (a: string[]) => {
                Column({ space: 6 }) {
                  Text(a[0]).fontSize(13).fontColor(DesignTokens.body)
                  Text(a[1]).fontSize(15).fontWeight(FontWeight.Medium)
                    .fontColor(a[2] === '1' ? '#FFFF3B30' : '#FF34C759')
                }
                .width('30%')
                .padding({ top: 12, bottom: 12 })
                .backgroundColor(DesignTokens.canvasSoft2)
                .borderRadius(8)
                .margin({ bottom: 8 })
              })
            }.width('100%')
          }
          .padding(16)
          .backgroundColor(DesignTokens.canvas)
          .borderRadius(12)
          .width('100%')

          Column({ space: 8 }) {
            Text(`${this.tabs[this.tab]}策略计划`)
              .fontSize(16).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
            Text('基于当前行情的 mock 配置，切换 Tab 查看不同风格。')
              .fontSize(13).fontColor(DesignTokens.body)
            Text('仓位 62%').fontSize(12).fontColor('#888888')
          }
          .alignItems(HorizontalAlign.Start)
          .padding(16)
          .width('100%')
          .backgroundColor(DesignTokens.canvas)
          .borderRadius(12)
        }.padding(16)
      }.layoutWeight(1).width('100%')
    }
    .width('100%').height('100%')
    .backgroundColor(DesignTokens.canvasSoft2)
  }
}


@Component
export struct NativeCheckInMallPane {
  onClose: () => void = () => {};
  @State points: number = 1280;
  @State streak: number = 3;
  @State checkedToday: boolean = false;
  @State remind: boolean = false;
  @State checkingIn: boolean = false;

  private headerBlue: string = DesignTokens.link;
  private coinGold: string = '#FFF5A623';
  private dayGray: string = '#FFF5F6F8';
  private calendar: CheckInDayCell[] = [
    { label: '19', reward: 5, signed: true, isToday: false },
    { label: '20', reward: 5, signed: true, isToday: false },
    { label: '21', reward: 5, signed: true, isToday: false },
    { label: '22', reward: 5, signed: false, isToday: false },
    { label: '23', reward: 5, signed: false, isToday: false },
    { label: '24', reward: 5, signed: false, isToday: false },
    { label: '今天', reward: 10, signed: false, isToday: true },
  ];
  private tasks: CheckInTaskRow[] = [
    { title: '每日登录', points: 5, action: '领取' },
    { title: '发一条动态', points: 10, action: '去完成' },
    { title: '商城下单', points: 20, action: '去完成' },
  ];

  private dayBg(day: CheckInDayCell): string {
    const signed = day.signed || (day.isToday && this.checkedToday);
    if (signed) {
      return this.headerBlue;
    }
    if (day.isToday) {
      return this.coinGold;
    }
    return this.dayGray;
  }

  private dayFg(day: CheckInDayCell): ResourceColor {
    const signed = day.signed || (day.isToday && this.checkedToday);
    if (signed || day.isToday) {
      return Color.White;
    }
    return DesignTokens.body;
  }

  private dayLabel(day: CheckInDayCell): string {
    const signed = day.signed || (day.isToday && this.checkedToday);
    return signed ? '已签' : day.label;
  }

  private flash(msg: string) {
    promptAction.showToast({ message: msg });
  }

  build() {
    Column() {
      // Flutter header chrome: nav + notice + stats
      Column() {
        Row() {
          Text('‹').fontSize(28).fontColor(Color.White)
            .width(44).onClick(() => this.onClose())
          Text('签到商城')
            .fontSize(18).fontWeight(FontWeight.Medium).fontColor(Color.White)
            .layoutWeight(1).textAlign(TextAlign.Center)
          Blank().width(44)
        }.width('100%').height(44).padding({ left: 4, right: 4 })

        Row({ space: 8 }) {
          Text('🔊').fontSize(14)
          Text('温馨提示：本页面只保留近3个月内的积分记录')
            .fontSize(12).fontColor(Color.White).layoutWeight(1).maxLines(1)
        }
        .width('100%')
        .padding({ left: 12, right: 12, top: 8, bottom: 8 })
        .margin({ left: 16, right: 16 })
        .backgroundColor('#FF3A8EE6')
        .borderRadius(4)

        Row() {
          Column({ space: 4 }) {
            Text('我的积分').fontSize(13).fontColor('#CCFFFFFF')
            Text(`${this.points}`).fontSize(32).fontWeight(FontWeight.Bold).fontColor(Color.White)
          }.alignItems(HorizontalAlign.Start).layoutWeight(1)
          Column({ space: 4 }) {
            Text('连续签到').fontSize(13).fontColor('#CCFFFFFF')
            Row({ space: 4 }) {
              Text(`${this.streak}`).fontSize(32).fontWeight(FontWeight.Bold).fontColor(Color.White)
              Text('天').fontSize(13).fontColor('#CCFFFFFF').margin({ bottom: 6 })
            }.alignItems(VerticalAlign.Bottom)
          }.alignItems(HorizontalAlign.Start).layoutWeight(1)
        }
        .width('100%')
        .padding({ left: 24, right: 24, top: 16, bottom: 16 })
      }
      .width('100%')
      .backgroundColor(this.headerBlue)

      Scroll() {
        Column({ space: 16 }) {
          // Check-in card
          Column({ space: 16 }) {
            Row() {
              Column({ space: 4 }) {
                Text('连签可得更多积分')
                  .fontSize(15).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
                Row() {
                  Text('已连续签到 ').fontSize(12).fontColor(DesignTokens.body)
                  Text(`${this.streak}`).fontSize(12).fontWeight(FontWeight.Medium).fontColor(this.headerBlue)
                  Text(' 天').fontSize(12).fontColor(DesignTokens.body)
                }
              }.alignItems(HorizontalAlign.Start).layoutWeight(1)
              Text(this.checkedToday ? '已签到' : (this.checkingIn ? '签到中…' : '立即签到'))
                .fontSize(14).fontWeight(FontWeight.Medium)
                .fontColor(this.checkedToday ? DesignTokens.mute : Color.White)
                .padding({ left: 16, right: 16, top: 8, bottom: 8 })
                .backgroundColor(this.checkedToday ? '#FFF5F6F8' : this.headerBlue)
                .borderRadius(20)
                .onClick(() => {
                  if (this.checkedToday || this.checkingIn) {
                    return;
                  }
                  this.checkingIn = true;
                  this.checkedToday = true;
                  this.points += 10;
                  this.streak += 1;
                  this.checkingIn = false;
                  this.flash('签到成功，+10积分');
                })
            }.width('100%')

            Row() {
              ForEach(this.calendar, (day: CheckInDayCell) => {
                Column({ space: 4 }) {
                  Column({ space: 2 }) {
                    Text(`+${day.reward}`)
                      .fontSize(11).fontWeight(FontWeight.Medium).fontColor(this.dayFg(day))
                    Text('›').fontSize(10).fontColor(this.dayFg(day))
                  }
                  .width('100%')
                  .aspectRatio(1)
                  .justifyContent(FlexAlign.Center)
                  .alignItems(HorizontalAlign.Center)
                  .backgroundColor(this.dayBg(day))
                  .borderRadius(8)
                  Text(this.dayLabel(day))
                    .fontSize(11)
                    .fontColor((day.signed || (day.isToday && this.checkedToday)) ? this.headerBlue : DesignTokens.body)
                    .maxLines(1)
                }
                .layoutWeight(1)
                .padding({ left: 2, right: 2 })
                .alignItems(HorizontalAlign.Center)
              })
            }.width('100%')

            Row() {
              Text('断签或者签完需重新开始').fontSize(12).fontColor(DesignTokens.mute).layoutWeight(1)
              Text('签到提醒').fontSize(12).fontColor(DesignTokens.body).margin({ right: 8 })
              Toggle({ type: ToggleType.Switch, isOn: this.remind })
                .selectedColor(this.headerBlue)
                .width(44).height(24)
                .onChange((isOn: boolean) => { this.remind = isOn; })
            }.width('100%').alignItems(VerticalAlign.Center)
          }
          .width('100%')
          .padding(16)
          .margin({ left: 16, right: 16, top: 16 })
          .backgroundColor(Color.White)
          .borderRadius(12)

          Text('成长任务')
            .fontSize(16).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
            .margin({ left: 16 })

          Column() {
            ForEach(this.tasks, (task: CheckInTaskRow, idx: number) => {
              if (idx > 0) {
                Row().width('100%').height(0.5).backgroundColor(DesignTokens.hairline)
              }
              Row() {
                Column({ space: 2 }) {
                  Text(task.title).fontSize(15).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
                  Text(`+${task.points}积分`).fontSize(12).fontColor(DesignTokens.body)
                }.alignItems(HorizontalAlign.Start).layoutWeight(1)
                Text(task.action)
                  .fontSize(13).fontWeight(FontWeight.Medium).fontColor(this.headerBlue)
                  .padding({ left: 12, right: 12, top: 6, bottom: 6 })
                  .backgroundColor('#1A0070F3')
                  .borderRadius(14)
                  .onClick(() => {
                    if (task.action === '领取') {
                      this.points += task.points;
                      this.flash(`领取成功，+${task.points}积分`);
                    } else {
                      this.flash(`去完成：${task.title}`);
                    }
                  })
              }
              .width('100%')
              .padding({ left: 14, right: 14, top: 12, bottom: 12 })
            })
          }
          .width('100%')
          .margin({ left: 16, right: 16 })
          .backgroundColor(Color.White)
          .borderRadius(12)

          Text('积分换礼')
            .fontSize(16).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
            .margin({ left: 16 })

          Column({ space: 16 }) {
            Text('🎁').fontSize(64).fontColor('#4D888888')
            Text('暂无积分商品').fontSize(14).fontColor(DesignTokens.mute)
          }
          .width('100%')
          .padding({ top: 40, bottom: 40 })
          .alignItems(HorizontalAlign.Center)
        }
        .width('100%')
        .padding({ bottom: 24 })
      }
      .layoutWeight(1)
      .width('100%')
      .scrollBar(BarState.Off)
      .align(Alignment.Top)
    }
    .width('100%').height('100%').backgroundColor(DesignTokens.canvasSoft2)
  }
}

interface CheckInDayCell {
  label: string;
  reward: number;
  signed: boolean;
  isToday: boolean;
}

interface CheckInTaskRow {
  title: string;
  points: number;
  action: string;
}

@Component
export struct NativeMusicPane {
  onClose: () => void = () => {};
  @State playingTitle: string = '';
  @State playingArtist: string = '';
  private tracks: string[][] = [
    ['Ya Ali - DJMaza.Com', 'DJMaza'],
    ['Ek Do Teen - DJMaza.Info', 'DJMaza'],
    ['16 yeh dil diwana hai', 'Classic'],
    ['Shape of You', 'Ed Sheeran'],
    ['Blinding Lights', 'The Weeknd'],
    ['Levitating', 'Dua Lipa'],
  ];

  build() {
    Stack({ alignContent: Alignment.Bottom }) {
      Column() {
        Row() {
          Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
          Text('音频列表').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
            .layoutWeight(1).textAlign(TextAlign.Center)
          if (this.playingTitle.length > 0) {
            Text('Now Playing').fontSize(14).fontColor(DesignTokens.link)
          } else {
            Text('').width(80)
          }
        }.padding({ left: 8, right: 8, top: 12, bottom: 12 }).width('100%').backgroundColor(DesignTokens.canvas)

        List() {
          ForEach(this.tracks, (t: string[]) => {
            ListItem() {
              Column({ space: 4 }) {
                Text(t[0]).fontSize(15).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
                Text(`By ${t[1]}`).fontSize(12).fontColor('#888888')
              }
              .alignItems(HorizontalAlign.Start)
              .padding(16)
              .width('100%')
              .backgroundColor(DesignTokens.canvas)
              .borderRadius(8)
              .margin({ bottom: 8 })
              .onClick(() => {
                this.playingTitle = t[0];
                this.playingArtist = t[1];
              })
            }
          })
        }
        .layoutWeight(1)
        .width('100%')
        .padding({ left: 16, right: 16, top: 12, bottom: this.playingTitle.length > 0 ? 72 : 16 })
      }
      .width('100%').height('100%')
      .backgroundColor(DesignTokens.canvasSoft2)

      if (this.playingTitle.length > 0) {
        Row() {
          Column({ space: 2 }) {
            Text(this.playingTitle).fontSize(14).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
              .maxLines(1)
            Text(this.playingArtist).fontSize(12).fontColor('#888888')
          }.alignItems(HorizontalAlign.Start).layoutWeight(1)
          Text('关闭').fontSize(13).fontColor(DesignTokens.body)
            .onClick(() => {
              this.playingTitle = '';
              this.playingArtist = '';
            })
        }
        .padding({ left: 16, right: 16, top: 10, bottom: 10 })
        .width('100%')
        .backgroundColor(DesignTokens.canvas)
      }
    }
    .width('100%').height('100%')
  }
}


@Component
export struct NativeHomeworkStatsPane {
  onClose: () => void = () => {};
  onOpen: (path: string) => void = (_path: string) => {};
  private rows: string[][] = [
    ['语法练习 3', '书面', '今日 23:59'],
    ['配音作业 · 致橡树', '配音', '明日 18:00'],
    ['听力精听', '听力', '本周六'],
  ];

  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text('作业统计').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink).margin({ left: 8 })
        Blank().layoutWeight(1)
      }.padding(16).width('100%').backgroundColor(DesignTokens.canvas)

      List() {
        ListItem() {
          Text('班级作业概览（mock）').fontSize(13).fontColor(DesignTokens.body).padding({ bottom: 8 })
        }
        ForEach(this.rows, (row: string[]) => {
          ListItem() {
            Column({ space: 4 }) {
              Text(row[0]).fontSize(15).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
              Text(`${row[1]} · 截止 ${row[2]}`).fontSize(12).fontColor(DesignTokens.mute)
            }
            .width('100%')
            .padding(14)
            .margin({ bottom: 10 })
            .backgroundColor(DesignTokens.canvas)
            .borderRadius(10)
            .alignItems(HorizontalAlign.Start)
            .onClick(() => {
              if (row[1] === '配音') {
                this.onOpen('/classroom/homework/dubbing');
              } else {
                this.onOpen('/classroom/homework/detail_teacher');
              }
            })
          }
        })
        ListItem() {
          Text('学生视角详情 →').fontSize(15).fontColor(DesignTokens.link)
            .padding({ top: 8 }).onClick(() => this.onOpen('/classroom/homework/detail_student'))
        }
        ListItem() {
          Text('作业点评 →').fontSize(15).fontColor(DesignTokens.link)
            .onClick(() => this.onOpen('/classroom/homework/review'))
        }
        ListItem() {
          Text('课堂视频 →').fontSize(15).fontColor(DesignTokens.link)
            .onClick(() => this.onOpen('/classroom/video'))
        }
        ListItem() {
          Text('领取礼品卡 →').fontSize(15).fontColor(DesignTokens.link)
            .onClick(() => this.onOpen('/classroom/gift/claim'))
        }
      }.layoutWeight(1).width('100%').padding(16)
    }
    .width('100%').height('100%').backgroundColor(DesignTokens.canvasSoft2)
  }
}

@Component
export struct NativeGiftClaimPane {
  onClose: () => void = () => {};
  @State claimed: boolean = false;

  private flash(msg: string) {
    promptAction.showToast({ message: msg });
  }

  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text('领取礼品卡').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink).margin({ left: 8 })
        Blank().layoutWeight(1)
      }.padding(16).width('100%').backgroundColor(DesignTokens.canvas)

      Column({ space: 12 }) {
        Text(this.claimed ? '已领取 · 卡号 GC-2026-001' : '完成课堂任务后可领取礼品卡')
          .fontSize(15).fontColor(DesignTokens.ink)
        Text(this.claimed ? '已领取' : '立即领取')
          .fontSize(15).fontColor(Color.White)
          .width('100%').textAlign(TextAlign.Center)
          .padding({ top: 12, bottom: 12 })
          .backgroundColor(this.claimed ? DesignTokens.mute : DesignTokens.link)
          .borderRadius(8)
          .onClick(() => {
            if (this.claimed) {
              return;
            }
            this.claimed = true;
            this.flash('领取成功（mock）');
          })
      }.width('100%').padding(16).alignItems(HorizontalAlign.Start)
      Blank().layoutWeight(1)
    }
    .width('100%').height('100%').backgroundColor(DesignTokens.canvasSoft2)
  }
}

@Component
export struct NativeHomeworkDetailPane {
  titleText: string = '作业详情';
  detail: string = '';
  action: string = '';
  onClose: () => void = () => {};

  private flash(msg: string) {
    promptAction.showToast({ message: msg });
  }

  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text(this.titleText).fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink).margin({ left: 8 })
        Blank().layoutWeight(1)
      }.padding(16).width('100%').backgroundColor(DesignTokens.canvas)

      Column({ space: 16 }) {
        Text(this.detail).fontSize(15).fontColor(DesignTokens.body)
        if (this.action.length > 0) {
          Text(this.action)
            .fontSize(15).fontColor(Color.White)
            .padding({ left: 16, right: 16, top: 10, bottom: 10 })
            .backgroundColor(DesignTokens.link).borderRadius(8)
            .onClick(() => this.flash(`${this.action}（mock）`))
        }
      }.width('100%').padding(16).alignItems(HorizontalAlign.Start)
      Blank().layoutWeight(1)
    }
    .width('100%').height('100%').backgroundColor(DesignTokens.canvasSoft2)
  }
}

@Component
export struct NativeClassroomPane {
  onClose: () => void = () => {};
  onOpen: (path: string) => void = (_p: string) => {};
  private classes: string[][] = [
    ['产品知识班', '未交作业 2'],
    ['配音作业', '待批改 1'],
    ['销售话术营', '进行中'],
  ];

  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text('我的课程').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
        Blank().layoutWeight(1)
      }.padding({ left: 16, right: 16, top: 12, bottom: 12 }).width('100%').backgroundColor(DesignTokens.canvas)

      List() {
        ForEach(this.classes, (c: string[]) => {
          ListItem() {
            Row({ space: 12 }) {
              Text('书')
                .fontSize(14).fontColor(DesignTokens.link)
                .width(48).height(48).textAlign(TextAlign.Center)
                .backgroundColor('#1A0070F3').borderRadius(10)
              Column({ space: 4 }) {
                Text(c[0]).fontSize(16).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
                Text(c[1]).fontSize(13).fontColor(DesignTokens.body)
              }.alignItems(HorizontalAlign.Start).layoutWeight(1)
              Text('›').fontSize(20).fontColor(DesignTokens.mute)
            }
            .padding(16)
            .margin({ bottom: 12 })
            .backgroundColor(DesignTokens.canvas)
            .borderRadius(12)
            .onClick(() => this.onOpen('/classroom/homework'))
          }
        })
      }.layoutWeight(1).width('100%').padding(16)
    }
    .width('100%').height('100%').backgroundColor(DesignTokens.canvasSoft2)
  }
}


@Component
export struct NativeUsedCarPane {
  onClose: () => void = () => {};
  onOpen: (path: string) => void = (_p: string) => {};
  private cars: string[][] = [
    ['2019 凯美瑞 双擎', '12.8 万 · 4.2 万公里', '急售'],
    ['2021 汉兰达', '22.5 万 · 3.1 万公里', ''],
    ['2018 雷凌', '7.9 万 · 6.8 万公里', '新上'],
  ];
  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text('二手车').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
        Blank().layoutWeight(1)
        Text('发布').fontSize(14).fontColor(DesignTokens.link).onClick(() => this.onOpen('/home/used_car/create'))
      }.padding({ left: 16, right: 16, top: 12, bottom: 12 }).width('100%').backgroundColor(DesignTokens.canvas)
      List() {
        ForEach(this.cars, (c: string[]) => {
          ListItem() {
            Row({ space: 12 }) {
              Column().width(96).height(72).borderRadius(8).backgroundColor(DesignTokens.canvasSoft2)
              Column({ space: 4 }) {
                Text(c[0]).fontSize(15).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
                Text(c[1]).fontSize(13).fontColor(DesignTokens.body)
                if (c[2].length > 0) {
                  Text(c[2]).fontSize(11).fontColor(DesignTokens.link)
                }
              }.alignItems(HorizontalAlign.Start).layoutWeight(1)
            }
            .padding(12).margin({ bottom: 12 }).backgroundColor(DesignTokens.canvas).borderRadius(12)
            .onClick(() => this.onOpen('/home/used_car/detail'))
          }
        })
      }.layoutWeight(1).width('100%').padding(16)
    }.width('100%').height('100%').backgroundColor(DesignTokens.canvasSoft2)
  }
}

@Component
export struct NativeNewCarFollowPane {
  onClose: () => void = () => {};
  onOpen: (path: string) => void = (_p: string) => {};
  private rows: string[][] = [
    ['陈先生 · 凯美瑞', '意向强 · 未回访 2 天', '紧急'],
    ['周女士 · 汉兰达', '询价 · 今早留言', '跟进'],
    ['刘总 · 塞那', '试驾预约 · 周六', '预约'],
  ];
  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text('新车跟进').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
        Blank().layoutWeight(1)
        Text('新建').fontSize(14).fontColor(DesignTokens.link).onClick(() => this.onOpen('/home/new_car_follow/create'))
      }.padding({ left: 16, right: 16, top: 12, bottom: 12 }).width('100%').backgroundColor(DesignTokens.canvas)
      List() {
        ForEach(this.rows, (r: string[]) => {
          ListItem() {
            Row() {
              Column({ space: 4 }) {
                Text(r[0]).fontSize(16).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
                Text(r[1]).fontSize(13).fontColor(DesignTokens.body)
              }.alignItems(HorizontalAlign.Start).layoutWeight(1)
              Text(r[2]).fontSize(11).fontColor(DesignTokens.link)
                .padding({ left: 8, right: 8, top: 4, bottom: 4 })
                .backgroundColor('#1A0070F3').borderRadius(12)
            }.padding(16).width('100%').backgroundColor(DesignTokens.canvas)
            .onClick(() => this.onOpen('/home/new_car_follow/detail'))
          }
        })
      }.layoutWeight(1).width('100%').padding(16)
    }.width('100%').height('100%').backgroundColor(DesignTokens.canvasSoft2)
  }
}

@Component
export struct NativeClubPane {
  onClose: () => void = () => {};
  @Prop title: string = 'Club';
  @State filter: number = 0;
  private filters: string[] = ['最新', '嘉宾分享', '资料'];
  private posts: string[][] = [
    ['莫听官方', '06-24', '【官方纪要】本期聚焦 AI 算力与产业趋势，内容仅供合格投资者参考。', '【莫听Club第78期】聊聊AI最靓的仔.pdf'],
    ['策略研究员', '06-20', '当星舰遇到算力：嘉宾分享回顾与延伸阅读。', ''],
  ];
  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text(this.title).fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
        Blank().layoutWeight(1)
      }.padding({ left: 16, right: 16, top: 12, bottom: 12 }).width('100%').backgroundColor(DesignTokens.canvas)
      Scroll() {
        Column({ space: 12 }) {
          Row({ space: 12 }) {
            Text('Club').fontSize(13).fontWeight(FontWeight.Bold).fontColor(Color.White)
              .width(52).height(52).textAlign(TextAlign.Center).borderRadius(12).backgroundColor('#1C1C3A')
            Column({ space: 4 }) {
              Text('莫听Club').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
              Text('动态 127 | 成员 1040').fontSize(12).fontColor(DesignTokens.body)
            }.alignItems(HorizontalAlign.Start).layoutWeight(1)
            Text('+ 加入').fontSize(14).fontColor(Color.White)
              .padding({ left: 16, right: 16, top: 8, bottom: 8 })
              .backgroundColor(DesignTokens.link).borderRadius(20)
          }.padding(16).width('100%').backgroundColor(DesignTokens.canvas).borderRadius(12)
          Row() {
            ForEach(this.filters, (f: string, i: number) => {
              Text(f)
                .fontSize(14)
                .fontWeight(i === this.filter ? FontWeight.Medium : FontWeight.Regular)
                .fontColor(i === this.filter ? DesignTokens.link : DesignTokens.body)
                .layoutWeight(1).textAlign(TextAlign.Center)
                .padding({ top: 10, bottom: 10 })
                .border({ width: { bottom: i === this.filter ? 2 : 0 }, color: DesignTokens.link })
                .onClick(() => { this.filter = i; })
            })
          }.width('100%').backgroundColor(DesignTokens.canvas).borderRadius(12)
          ForEach(this.posts, (p: string[]) => {
            Column({ space: 10 }) {
              Row({ space: 10 }) {
                Column().width(40).height(40).borderRadius(20).backgroundColor(DesignTokens.canvasSoft2)
                Column({ space: 2 }) {
                  Text(p[0]).fontSize(15).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
                  Text(p[1]).fontSize(12).fontColor(DesignTokens.mute)
                }.alignItems(HorizontalAlign.Start)
              }
              Text(p[2]).fontSize(15).fontColor(DesignTokens.ink)
              if (p[3].length > 0) {
                Text(`📄 ${p[3]}`).fontSize(13).fontColor(DesignTokens.body)
                  .padding(12).width('100%').backgroundColor(DesignTokens.canvasSoft2).borderRadius(10)
              }
              Row({ space: 20 }) {
                Text('分享').fontSize(13).fontColor(DesignTokens.body)
                Text('评论').fontSize(13).fontColor(DesignTokens.body)
                Text('点赞').fontSize(13).fontColor(DesignTokens.body)
              }
            }.padding(16).width('100%').backgroundColor(DesignTokens.canvas).borderRadius(12)
          })
        }.padding(16).width('100%')
      }.layoutWeight(1).width('100%')
    }.width('100%').height('100%').backgroundColor(DesignTokens.canvasSoft2)
  }
}

@Component
export struct NativeLifeServicePane {
  onClose: () => void = () => {};
  private items: string[] = ['代驾', '洗车', '道路救援', '充电桩', '年检代办', '保险续保'];
  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text('生活服务').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
        Blank().layoutWeight(1)
      }.padding({ left: 16, right: 16, top: 12, bottom: 12 }).width('100%').backgroundColor(DesignTokens.canvas)
      Grid() {
        ForEach(this.items, (item: string) => {
          GridItem() {
            Column({ space: 8 }) {
              Text(item.substring(0, 1)).fontSize(16).fontColor(DesignTokens.link)
                .width(48).height(48).textAlign(TextAlign.Center).borderRadius(24).backgroundColor('#1A0070F3')
              Text(item).fontSize(13).fontColor(DesignTokens.ink)
            }.padding({ top: 16, bottom: 16 }).width('100%').backgroundColor(DesignTokens.canvas).borderRadius(12)
          }
        })
      }.columnsTemplate('1fr 1fr 1fr').columnsGap(12).rowsGap(12).padding(16).width('100%')
      Blank().layoutWeight(1)
    }.width('100%').height('100%').backgroundColor(DesignTokens.canvasSoft2)
  }
}

@Component
export struct NativeTodoListPane {
  titleText: string = '待办';
  rows: string[][] = [];
  action: string = '';
  onClose: () => void = () => {};
  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text(this.titleText).fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
        Blank().layoutWeight(1)
      }.padding({ left: 16, right: 16, top: 12, bottom: 12 }).width('100%').backgroundColor(DesignTokens.canvas)
      List() {
        ForEach(this.rows, (r: string[]) => {
          ListItem() {
            Row() {
              Column({ space: 4 }) {
                Text(r[0]).fontSize(16).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
                Text(r[1]).fontSize(13).fontColor(DesignTokens.body)
              }.alignItems(HorizontalAlign.Start).layoutWeight(1)
              if (r[2].length > 0) {
                Text(r[2]).fontSize(11).fontColor(DesignTokens.link)
                  .padding({ left: 8, right: 8, top: 4, bottom: 4 })
                  .backgroundColor('#1A0070F3').borderRadius(12)
              }
            }.padding(16).width('100%').backgroundColor(DesignTokens.canvas)
          }
        })
      }.layoutWeight(1).width('100%').padding(16)
      if (this.action.length > 0) {
        Button(this.action).backgroundColor(DesignTokens.link).width('90%').margin({ bottom: 24 })
      }
    }.width('100%').height('100%').backgroundColor(DesignTokens.canvasSoft2)
  }
}


@Component
export struct NativeWebPane {
  onClose: () => void = () => {};
  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text('网页').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
        Blank().layoutWeight(1)
      }.padding({ left: 16, right: 16, top: 12, bottom: 12 }).width('100%').backgroundColor(DesignTokens.canvas)
      Column({ space: 12 }) {
        Text('离线 fixture').fontSize(20).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
        Text('无网络也可显示的固定页面，对齐 Flutter InAppWeb 离线兜底。')
          .fontSize(15).fontColor(DesignTokens.body)
        Text('myai://web?fixture=offline').fontSize(13).fontColor(DesignTokens.link)
          .padding(12).width('100%').backgroundColor(DesignTokens.canvasSoft2).borderRadius(8)
      }.padding(16).width('100%')
      Blank().layoutWeight(1)
    }.width('100%').height('100%').backgroundColor(DesignTokens.canvas)
  }
}

@Component
export struct NativeAfterSalesPane {
  onClose: () => void = () => {};
  onOpen: (path: string) => void = (_p: string) => {};
  private rows: string[][] = [
    ['京 A·12345 · 小保养', '完成 · 2026-09-20', '完成'],
    ['京 C·54321 · 四轮定位', '进行中', '进行中'],
    ['京 B·99887 · 钣喷', '待进厂 · 明天 09:00', '预约'],
  ];
  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text('售后专区').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
        Blank().layoutWeight(1)
        Text('新建工单').fontSize(14).fontColor(DesignTokens.link).onClick(() => this.onOpen('/home/after_sales/create'))
      }.padding({ left: 16, right: 16, top: 12, bottom: 12 }).width('100%').backgroundColor(DesignTokens.canvas)
      List() {
        ForEach(this.rows, (r: string[]) => {
          ListItem() {
            Row() {
              Column({ space: 4 }) {
                Text(r[0]).fontSize(16).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
                Text(r[1]).fontSize(13).fontColor(DesignTokens.body)
              }.alignItems(HorizontalAlign.Start).layoutWeight(1)
              Text(r[2]).fontSize(11).fontColor(DesignTokens.link)
                .padding({ left: 8, right: 8, top: 4, bottom: 4 })
                .backgroundColor('#1A0070F3').borderRadius(12)
            }.padding(16).width('100%').backgroundColor(DesignTokens.canvas)
            .onClick(() => this.onOpen('/home/after_sales/detail'))
          }
        })
      }.layoutWeight(1).width('100%').padding(16)
    }.width('100%').height('100%').backgroundColor(DesignTokens.canvasSoft2)
  }
}

@Component
export struct NativeLedgerPane {
  onClose: () => void = () => {};
  private metrics: string[][] = [
    ['9 月销售额', '¥ 2,860,000'],
    ['毛利率', '18.6%'],
    ['库存周转', '42 天'],
    ['售后产值', '¥ 486,000'],
  ];
  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text('台账').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
        Blank().layoutWeight(1)
      }.padding({ left: 16, right: 16, top: 12, bottom: 12 }).width('100%').backgroundColor(DesignTokens.canvas)
      Grid() {
        ForEach(this.metrics, (m: string[]) => {
          GridItem() {
            Column({ space: 8 }) {
              Text(m[0]).fontSize(13).fontColor(DesignTokens.body)
              Text(m[1]).fontSize(20).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
            }.alignItems(HorizontalAlign.Start).padding(16).width('100%')
            .backgroundColor(DesignTokens.canvas).borderRadius(12)
          }
        })
      }.columnsTemplate('1fr 1fr').columnsGap(12).rowsGap(12).padding(16).width('100%')
      Blank().layoutWeight(1)
    }.width('100%').height('100%').backgroundColor(DesignTokens.canvasSoft2)
  }
}


@Component
export struct NativeSmsTemplatePane {
  onClose: () => void = () => {};
  @State selected: number = 0;
  /** Align Compose SmsTemplateScreen */
  private templates: string[][] = [
    ['到店提醒', '尊敬的客户，预约保养已排至今日 14:00，请准时到店。'],
    ['试驾确认', '您好，试驾预约已确认，顾问将提前电话联系您。'],
    ['活动邀约', '本周末门店试驾会，到店即送礼品，欢迎莅临。'],
    ['回访关怀', '购车已满一周，如有用车问题请随时联系您的顾问。'],
  ];

  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text('短信模板').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink).margin({ left: 8 })
        Blank().layoutWeight(1)
      }.padding({ left: 16, right: 16, top: 12, bottom: 12 }).width('100%').backgroundColor(DesignTokens.canvas)

      List() {
        ForEach(this.templates, (item: string[], i: number) => {
          ListItem() {
            Column({ space: 8 }) {
              Row() {
                Text(item[0]).fontSize(16).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
                Blank().layoutWeight(1)
                if (this.selected === i) {
                  Text('已选').fontSize(12).fontColor(DesignTokens.link)
                }
              }.width('100%')
              Text(item[1]).fontSize(14).fontColor(DesignTokens.body)
              Text('复制文案')
                .fontSize(14).fontColor(Color.White)
                .padding({ left: 14, right: 14, top: 8, bottom: 8 })
                .backgroundColor(DesignTokens.link).borderRadius(8)
                .onClick(() => { this.selected = i; })
            }
            .alignItems(HorizontalAlign.Start)
            .padding(16)
            .width('100%')
            .backgroundColor(DesignTokens.canvas)
            .borderRadius(12)
            .border({
              width: this.selected === i ? 1.5 : 0.5,
              color: this.selected === i ? DesignTokens.link : DesignTokens.hairline,
            })
            .margin({ bottom: 12 })
            .onClick(() => { this.selected = i; })
          }
        }, (item: string[], i: number) => `${item[0]}-${i}`)
      }
      .layoutWeight(1).width('100%')
      .padding(16)
    }
    .width('100%').height('100%')
    .backgroundColor(DesignTokens.canvasSoft2)
  }
}


@Component
export struct NativeStoreQrPane {
  onClose: () => void = () => {};

  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text('店铺收款码').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink).margin({ left: 8 })
        Blank().layoutWeight(1)
      }.padding({ left: 16, right: 16, top: 12, bottom: 12 }).width('100%').backgroundColor(DesignTokens.canvas)

      Scroll() {
        Column({ space: 16 }) {
          Column({ space: 8 }) {
            Text('演示门店').fontSize(18).fontWeight(FontWeight.Bold).fontColor(DesignTokens.ink)
            Text('扫码向本店付款').fontSize(13).fontColor(DesignTokens.body)
            Stack({ alignContent: Alignment.Center }) {
              Column().width(200).height(200).backgroundColor('#111111').borderRadius(12)
              Column().width(160).height(160).backgroundColor(Color.White).borderRadius(8)
              Text('QR').fontSize(28).fontWeight(FontWeight.Bold).fontColor('#111111')
            }.margin({ top: 12, bottom: 12 })
            Text('支持微信 / 支付宝').fontSize(12).fontColor('#888888')
          }
          .padding(24)
          .width('100%')
          .alignItems(HorizontalAlign.Center)
          .backgroundColor(DesignTokens.canvas)
          .borderRadius(16)

          Text('保存到相册')
            .fontSize(15).fontColor(Color.White)
            .width('100%').textAlign(TextAlign.Center)
            .padding({ top: 12, bottom: 12 })
            .backgroundColor(DesignTokens.link).borderRadius(8)
        }.padding({ left: 24, right: 24, top: 24, bottom: 32 })
      }.layoutWeight(1).width('100%')
    }
    .width('100%').height('100%')
    .backgroundColor(DesignTokens.canvasSoft2)
  }
}


@Component
export struct NativeQaPane {
  onClose: () => void = () => {};
  private qs: string[][] = [
    ['全款和贷款怎么选？', '已解答 · 顾问回复'],
    ['置换能抵多少？', '待回复 · 客户追问'],
    ['保养周期多久一次？', '已解答 · 知识库'],
  ];

  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text('选买问答').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink).margin({ left: 8 })
        Blank().layoutWeight(1)
      }.padding({ left: 16, right: 16, top: 12, bottom: 12 }).width('100%').backgroundColor(DesignTokens.canvas)

      List() {
        ForEach(this.qs, (q: string[]) => {
          ListItem() {
            Column({ space: 6 }) {
              Text(q[0]).fontSize(15).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
              Text(q[1]).fontSize(13).fontColor(DesignTokens.body)
            }
            .alignItems(HorizontalAlign.Start)
            .padding(14)
            .width('100%')
            .backgroundColor(DesignTokens.canvas)
            .borderRadius(10)
            .margin({ bottom: 10 })
          }
        })
      }.layoutWeight(1).width('100%').padding(16)
    }
    .width('100%').height('100%')
    .backgroundColor(DesignTokens.canvasSoft2)
  }
}


@Component
export struct NativePosterPane {
  onClose: () => void = () => {};
  @State picked: number = 0;
  private templates: string[] = ['置换专场', '专卖精选', '估价引流', '到店礼'];

  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text('商家海报').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink).margin({ left: 8 })
        Blank().layoutWeight(1)
      }.padding({ left: 16, right: 16, top: 12, bottom: 12 }).width('100%').backgroundColor(DesignTokens.canvas)

      Flex({ wrap: FlexWrap.Wrap, justifyContent: FlexAlign.SpaceBetween }) {
        ForEach(this.templates, (name: string, i: number) => {
          Column({ space: 8 }) {
            Column() {
              Text(name).fontSize(16).fontWeight(FontWeight.Medium).fontColor(DesignTokens.link)
            }
            .width('100%').height(120)
            .justifyContent(FlexAlign.Center)
            .backgroundColor('#1A0070F3')
            .borderRadius(10)
            Text(this.picked === i ? '已选用' : '选用模板')
              .fontSize(13)
              .fontColor(this.picked === i ? DesignTokens.link : DesignTokens.body)
          }
          .width('48%')
          .padding(10)
          .margin({ bottom: 12 })
          .backgroundColor(DesignTokens.canvas)
          .borderRadius(12)
          .border({
            width: this.picked === i ? 1.5 : 0.5,
            color: this.picked === i ? DesignTokens.link : DesignTokens.hairline,
          })
          .onClick(() => { this.picked = i; })
        }, (name: string, i: number) => `${name}-${i}`)
      }
      .padding(16)
      .layoutWeight(1)
      .width('100%')
    }
    .width('100%').height('100%')
    .backgroundColor(DesignTokens.canvasSoft2)
  }
}


@Component
export struct NativeBusinessPane {
  onClose: () => void = () => {};
  @State note: string = '';
  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text('商务合作').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
        Blank().layoutWeight(1)
      }.padding({ left: 16, right: 16, top: 12, bottom: 12 }).width('100%').backgroundColor(DesignTokens.canvas)
      Column({ space: 16 }) {
        Text('渠道合作').fontSize(16).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
        Text('提交合作意向后，商务同学会在 1–2 个工作日内联系您。').fontSize(14).fontColor(DesignTokens.body)
        TextArea({ placeholder: '请简述合作意向…', text: this.note })
          .height(120).padding(12).backgroundColor(DesignTokens.canvas).borderRadius(10)
          .onChange((v: string) => { this.note = v; })
        Button('提交').backgroundColor(DesignTokens.link).width('100%')
      }.padding(16).width('100%')
      Blank().layoutWeight(1)
    }.width('100%').height('100%').backgroundColor(DesignTokens.canvasSoft2)
  }
}

@Component
export struct NativeRemindersPane {
  onClose: () => void = () => {};
  private items: string[][] = [
    ['回访陈先生', '今天 16:00'],
    ['提交周报', '周五 18:00'],
    ['试驾接待 · 周女士', '周六 10:30'],
  ];
  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text('提醒事项').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
        Blank().layoutWeight(1)
      }.padding({ left: 16, right: 16, top: 12, bottom: 12 }).width('100%').backgroundColor(DesignTokens.canvas)
      List() {
        ForEach(this.items, (it: string[]) => {
          ListItem() {
            Column({ space: 4 }) {
              Text(it[0]).fontSize(16).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
              Text(it[1]).fontSize(13).fontColor(DesignTokens.body)
            }.alignItems(HorizontalAlign.Start).padding(16).width('100%').backgroundColor(DesignTokens.canvas)
          }
        })
      }.layoutWeight(1).width('100%').padding(16)
      Button('新建提醒').backgroundColor(DesignTokens.link).width('90%').margin({ bottom: 24 })
    }.width('100%').height('100%').backgroundColor(DesignTokens.canvasSoft2)
  }
}

@Component
export struct NativeFeedbackPane {
  onClose: () => void = () => {};
  @State kind: number = 0;
  @State content: string = '';
  private kinds: string[] = ['功能建议', '体验问题', '其它'];
  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text('意见反馈').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
        Blank().layoutWeight(1)
      }.padding({ left: 16, right: 16, top: 12, bottom: 12 }).width('100%').backgroundColor(DesignTokens.canvas)
      Column({ space: 16 }) {
        Text('反馈类型').fontSize(14).fontColor(DesignTokens.body)
        Row({ space: 8 }) {
          ForEach(this.kinds, (k: string, i: number) => {
            Text(k).fontSize(13)
              .fontColor(this.kind === i ? Color.White : DesignTokens.ink)
              .padding({ left: 12, right: 12, top: 8, bottom: 8 })
              .backgroundColor(this.kind === i ? DesignTokens.link : DesignTokens.canvas)
              .borderRadius(16)
              .onClick(() => { this.kind = i; })
          })
        }
        TextArea({ placeholder: '请描述问题或建议…', text: this.content })
          .height(140).padding(12).backgroundColor(DesignTokens.canvas).borderRadius(10)
          .onChange((v: string) => { this.content = v; })
        Button('提交反馈').backgroundColor(DesignTokens.link).width('100%')
      }.padding(16).width('100%')
      Blank().layoutWeight(1)
    }.width('100%').height('100%').backgroundColor(DesignTokens.canvasSoft2)
  }
}

@Component
export struct NativeProfilePane {
  onClose: () => void = () => {};
  @State nickname: string = 'qa_user';
  @State dirty: boolean = false;
  private phone: string = '138****5172';

  private flash(msg: string) {
    promptAction.showToast({ message: msg });
  }

  build() {
    Column() {
      Row() {
        Text('‹').fontSize(28).fontColor(DesignTokens.link)
          .width(44).onClick(() => this.onClose())
        Text('个人资料')
          .fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
          .layoutWeight(1).textAlign(TextAlign.Center)
        Text('保存')
          .fontSize(16).fontWeight(FontWeight.Medium)
          .fontColor(this.dirty ? DesignTokens.link : DesignTokens.mute)
          .width(52).textAlign(TextAlign.End)
          .onClick(() => {
            if (!this.dirty) {
              return;
            }
            this.flash('已保存');
            this.dirty = false;
            this.onClose();
          })
      }
      .width('100%').height(44).padding({ left: 8, right: 8 })
      .backgroundColor(DesignTokens.canvas)

      Scroll() {
        Column() {
          Column({ space: 10 }) {
            Stack({ alignContent: Alignment.BottomEnd }) {
              Text('Q')
                .fontSize(40).fontWeight(FontWeight.Bold).fontColor(DesignTokens.mute)
                .width(104).height(104).textAlign(TextAlign.Center)
                .borderRadius(52).backgroundColor('#FFE0E0E0')
                .onClick(() => this.flash('更换头像（开发中）'))
              Text('📷')
                .fontSize(12)
                .width(32).height(32).textAlign(TextAlign.Center)
                .borderRadius(16).backgroundColor(DesignTokens.link)
                .border({ width: 2.5, color: Color.White })
            }
            Text('轻触更换头像').fontSize(13).fontColor(DesignTokens.mute)
          }
          .width('100%')
          .alignItems(HorizontalAlign.Center)
          .margin({ top: 24, bottom: 32 })

          Text('基本信息')
            .fontSize(12).fontWeight(FontWeight.Medium).fontColor(DesignTokens.mute)
            .margin({ left: 20 })

          Column() {
            Row() {
              Text('昵称').fontSize(15).fontColor(DesignTokens.ink).width(72)
              TextInput({ placeholder: '请输入昵称', text: this.nickname })
                .layoutWeight(1).height(40).textAlign(TextAlign.End)
                .backgroundColor(Color.Transparent)
                .onChange((v: string) => {
                  this.nickname = v;
                  this.dirty = true;
                })
            }
            .width('100%')
            .padding({ left: 16, right: 16, top: 14, bottom: 14 })

            Row().width('100%').height(1).backgroundColor(DesignTokens.hairline)
              .margin({ left: 16, right: 16 })

            Row() {
              Text('手机号').fontSize(15).fontColor(DesignTokens.ink).width(72)
              Blank().layoutWeight(1)
              Text(this.phone).fontSize(15).fontColor(DesignTokens.body)
            }
            .width('100%')
            .padding({ left: 16, right: 16, top: 14, bottom: 14 })
          }
          .width('100%')
          .margin({ left: 16, right: 16, top: 8 })
          .backgroundColor(Color.White)
          .borderRadius(12)

          Text('退出登录')
            .fontSize(16).fontWeight(FontWeight.Medium).fontColor('#FFE53935')
            .width('100%')
            .textAlign(TextAlign.Center)
            .padding({ top: 16, bottom: 16 })
            .margin({ left: 16, right: 16, top: 32, bottom: 40 })
            .backgroundColor(Color.White)
            .borderRadius(12)
            .onClick(() => this.flash('退出登录（开发中）'))
        }
        .width('100%')
      }
      .layoutWeight(1).width('100%').scrollBar(BarState.Off)
    }
    .width('100%').height('100%').backgroundColor(DesignTokens.canvasSoft2)
  }
}

@Component
export struct NativeAddressesPane {
  onClose: () => void = () => {};
  @State list: string[][] = [
    ['qa_user', '138****5172', '北京市朝阳区演示路 1 号', '1'],
    ['测试乙', '139****0000', '上海市浦东新区世纪大道 100 号', '0'],
  ];

  private flash(msg: string) {
    promptAction.showToast({ message: msg });
  }

  private setDefault(idx: number) {
    this.list = this.list.map((row: string[], i: number) => [row[0], row[1], row[2], i === idx ? '1' : '0']);
    this.flash('已设为默认');
  }

  private removeAt(idx: number) {
    this.list = this.list.filter((_: string[], i: number) => i !== idx);
    this.flash('已删除');
  }

  build() {
    Column() {
      Row() {
        Text('‹').fontSize(28).fontColor(DesignTokens.link)
          .width(44).onClick(() => this.onClose())
        Text('收货地址')
          .fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
          .layoutWeight(1).textAlign(TextAlign.Center)
        Text('新增').fontSize(15).fontWeight(FontWeight.Medium).fontColor(DesignTokens.link)
          .width(52).textAlign(TextAlign.End)
          .onClick(() => this.flash('编辑地址（开发中）'))
      }
      .width('100%').height(44).padding({ left: 8, right: 8 })
      .backgroundColor(DesignTokens.canvas)

      if (this.list.length === 0) {
        Column({ space: 12 }) {
          Blank()
          Text('暂无地址').fontSize(15).fontColor(DesignTokens.mute)
          Text('添加收货地址').fontSize(15).fontWeight(FontWeight.Medium).fontColor(DesignTokens.link)
            .onClick(() => this.flash('编辑地址（开发中）'))
          Blank()
        }.width('100%').layoutWeight(1).alignItems(HorizontalAlign.Center)
      } else {
        List() {
          ForEach(this.list, (a: string[], idx: number) => {
            ListItem() {
              Column({ space: 6 }) {
                Row({ space: 8 }) {
                  Text(`${a[0]}  ${a[1]}`)
                    .fontSize(15).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
                  if (a[3] === '1') {
                    Text('默认').fontSize(11).fontColor(Color.White)
                      .padding({ left: 6, right: 6, top: 2, bottom: 2 })
                      .backgroundColor(DesignTokens.link).borderRadius(4)
                  }
                  Blank().layoutWeight(1)
                }.width('100%')
                Text(a[2]).fontSize(13).fontColor(DesignTokens.body)
                Row({ space: 16 }) {
                  Text('设为默认').fontSize(13).fontColor(DesignTokens.link)
                    .onClick(() => this.setDefault(idx))
                  Text('删除').fontSize(13).fontColor('#FFE53935')
                    .onClick(() => this.removeAt(idx))
                }.width('100%').margin({ top: 4 })
              }
              .width('100%')
              .padding(14)
              .margin({ bottom: 10 })
              .backgroundColor(Color.White)
              .borderRadius(12)
              .onClick(() => this.flash('编辑地址（开发中）'))
            }
          }, (a: string[], idx: number) => `${a[0]}_${idx}`)
        }.layoutWeight(1).width('100%').padding(16)
      }
    }
    .width('100%').height('100%').backgroundColor(DesignTokens.canvasSoft2)
  }
}

@Component
export struct NativeDubbingFeedPane {
  onClose: () => void = () => {};
  private items: string[][] = [
    ['小王子 · 片段 3', '难度初级 · 女声', '热'],
    ['飞屋环游记 · OP', '难度中级 · 男声', ''],
    ['穿条纹睡衣的男孩', '难度高级 · 男声', '新'],
  ];
  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text('配音').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
        Blank().layoutWeight(1)
      }.padding({ left: 16, right: 16, top: 12, bottom: 12 }).width('100%').backgroundColor(DesignTokens.canvas)
      List() {
        ForEach(this.items, (it: string[]) => {
          ListItem() {
            Row({ space: 12 }) {
              Column().width(56).height(56).borderRadius(8).backgroundColor(DesignTokens.canvasSoft2)
              Column({ space: 4 }) {
                Text(it[0]).fontSize(15).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
                Text(it[1]).fontSize(13).fontColor(DesignTokens.body)
              }.alignItems(HorizontalAlign.Start).layoutWeight(1)
              if (it[2].length > 0) {
                Text(it[2]).fontSize(11).fontColor(DesignTokens.link)
                  .padding({ left: 8, right: 8, top: 4, bottom: 4 })
                  .backgroundColor('#1A0070F3').borderRadius(12)
              }
            }.padding(16).width('100%').backgroundColor(DesignTokens.canvas)
          }
        })
      }.layoutWeight(1).width('100%').padding(16)
      Button('开始配音').backgroundColor(DesignTokens.link).width('90%').margin({ bottom: 24 })
    }.width('100%').height('100%').backgroundColor(DesignTokens.canvasSoft2)
  }
}

@Component
export struct NativeHotRankPane {
  onClose: () => void = () => {};
  @State category: number = 3;
  @State age: number = 2;
  @State showAgeMenu: boolean = false;
  private categories: string[] = ['热读榜', '新书榜', '童话榜', '热搜榜', '科普榜', '高分榜'];
  private ages: string[] = ['1-2岁', '3岁到大班', '1-3年级', '4年级以上'];
  private titles: string[] = ['穿条纹睡衣的...', '蛮荒故事', '爱冒险的朵拉', '道奇小狗', '你好，小朋友', '完美的世界', '萌宠部落', '穿梭在迷宫的勇士'];
  private subs: string[] = ['某日布鲁诺决定...', '一种近似父子的不寻常感情', '开启你的奇幻冒险之旅', '跟佩奇一起快乐学英语', '经典动画配音练习', '趣味英语启蒙课堂'];
  private heats: number[] = [39274, 28390, 22007, 19874, 18560, 16230, 14890, 13540];
  private ranks: number[] = [1, 2, 3, 88, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20];
  build() {
    Column() {
      Column({ space: 6 }) {
        Row() {
          Text('‹').fontSize(28).fontColor(DesignTokens.ink).onClick(() => this.onClose())
          Blank().layoutWeight(1)
          Text('分享').fontSize(14).fontColor(DesignTokens.ink)
        }.width('100%').padding({ left: 16, right: 16 })
        Row({ space: 8 }) {
          Text('🌾')
          Text(this.categories[this.category]).fontSize(22).fontWeight(FontWeight.Bold).fontColor(DesignTokens.ink)
          Text('🌾')
        }
        Text('iHome用户近期热搜内容').fontSize(12).fontColor(DesignTokens.mute)
      }.width('100%').padding({ top: 12, bottom: 12 }).backgroundColor(DesignTokens.canvasSoft2)
      Row() {
        Scroll() {
          Column({ space: 2 }) {
            ForEach(this.categories, (label: string, i: number) => {
              Text(label)
                .fontSize(13)
                .fontWeight(i === this.category ? FontWeight.Medium : FontWeight.Regular)
                .fontColor(i === this.category ? DesignTokens.ink : DesignTokens.mute)
                .width('100%').textAlign(TextAlign.Center)
                .padding({ top: 14, bottom: 14 })
                .backgroundColor(i === this.category ? DesignTokens.canvas : Color.Transparent)
                .onClick(() => { this.category = i; })
            }, (label: string, i: number) => `${i}_${label}`)
          }.padding({ top: 8 })
        }.width(88).height('100%').backgroundColor(DesignTokens.canvasSoft2)
        Column() {
          Row() {
            Blank().layoutWeight(1)
            Text(`${this.ages[this.age]} ▾`).fontSize(13).fontColor(DesignTokens.ink)
              .padding({ left: 10, right: 10, top: 6, bottom: 6 })
              .backgroundColor(DesignTokens.canvas).borderRadius(14)
              .onClick(() => { this.showAgeMenu = !this.showAgeMenu; })
          }.width('100%').padding({ left: 12, right: 12, top: 10 })
          if (this.showAgeMenu) {
            Column() {
              ForEach(this.ages, (label: string, i: number) => {
                Text(label)
                  .fontSize(13)
                  .fontWeight(i === this.age ? FontWeight.Medium : FontWeight.Regular)
                  .fontColor(i === this.age ? DesignTokens.link : DesignTokens.ink)
                  .padding({ left: 14, right: 14, top: 10, bottom: 10 })
                  .onClick(() => { this.age = i; this.showAgeMenu = false; })
              }, (label: string, i: number) => `age_${i}`)
            }.backgroundColor(DesignTokens.canvas).borderRadius(10).margin({ right: 12 })
          }
          List() {
            ForEach(this.ranks, (rank: number, i: number) => {
              ListItem() {
                Row({ space: 8 }) {
                  Text('').width(56).height(56).borderRadius(8).backgroundColor('#E8F0FE')
                  Text(`${rank}`).fontSize(11).fontWeight(FontWeight.Bold)
                    .fontColor(rank <= 3 ? Color.White : DesignTokens.mute)
                    .width(18).height(18).textAlign(TextAlign.Center).borderRadius(4)
                    .backgroundColor(rank === 1 ? '#FF9500' : rank === 2 ? '#B0B0B8' : rank === 3 ? '#C9783A' : '#B0B0B855')
                  Column({ space: 4 }) {
                    Text(this.titles[i % this.titles.length]).fontSize(14).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink).maxLines(1)
                    Text(this.subs[i % this.subs.length]).fontSize(12).fontColor(DesignTokens.mute).maxLines(1)
                    Text(`热度${this.heats[i % this.heats.length]}`).fontSize(11).fontColor(DesignTokens.mute)
                  }.alignItems(HorizontalAlign.Start).layoutWeight(1)
                }.padding({ left: 12, right: 12, top: 10, bottom: 10 }).width('100%')
              }
            }, (rank: number, i: number) => `r_${i}_${rank}`)
          }.layoutWeight(1).width('100%')
        }.layoutWeight(1).height('100%')
      }.layoutWeight(1).width('100%')
    }.width('100%').height('100%').backgroundColor(DesignTokens.canvasSoft2)
  }
}

@Component
export struct NativeDataAnalyticsPane {
  onClose: () => void = () => {};
  private rows: string[][] = [
    ['本周成交', '12 台'],
    ['进店客流', '286'],
    ['试驾转化', '18.4%'],
    ['售后进厂', '64'],
  ];
  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text('数据分析').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
        Blank().layoutWeight(1)
      }.padding({ left: 16, right: 16, top: 12, bottom: 12 }).width('100%').backgroundColor(DesignTokens.canvas)
      Grid() {
        ForEach(this.rows, (r: string[]) => {
          GridItem() {
            Column({ space: 8 }) {
              Text(r[0]).fontSize(13).fontColor(DesignTokens.body)
              Text(r[1]).fontSize(22).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
            }.alignItems(HorizontalAlign.Start).padding(16).width('100%')
            .backgroundColor(DesignTokens.canvas).borderRadius(12)
          }
        })
      }.columnsTemplate('1fr 1fr').columnsGap(12).rowsGap(12).padding(16).width('100%')
      Blank().layoutWeight(1)
    }.width('100%').height('100%').backgroundColor(DesignTokens.canvasSoft2)
  }
}

@Component
export struct NativeBusinessCardPane {
  onClose: () => void = () => {};
  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text('电子名片').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
        Blank().layoutWeight(1)
      }.padding({ left: 16, right: 16, top: 12, bottom: 12 }).width('100%').backgroundColor(DesignTokens.canvas)
      Column({ space: 16 }) {
        Column({ space: 8 }) {
          Text('qa_user').fontSize(22).fontWeight(FontWeight.Bold).fontColor(Color.White)
          Text('销售顾问 · 沃德龙鼎').fontSize(14).fontColor('#E6FFFFFF')
          Text('138****5172').fontSize(14).fontColor('#D9FFFFFF')
          Blank().height(40)
          Text('扫码加好友 / 预约到店').fontSize(12).fontColor('#BFFFFFFF')
        }
        .alignItems(HorizontalAlign.Start)
        .padding(20).width('100%').height(180).borderRadius(16)
        .linearGradient({ angle: 135, colors: [['#0070F3', 0], ['#3359D9', 1]] })
        Button('分享名片').backgroundColor(DesignTokens.link).width('100%')
      }.padding(16).width('100%')
      Blank().layoutWeight(1)
    }.width('100%').height('100%').backgroundColor(DesignTokens.canvasSoft2)
  }
}
