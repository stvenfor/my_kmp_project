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

  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(Color.White).onClick(() => this.onClose())
        Text('学习报告').fontSize(17).fontWeight(FontWeight.Medium).fontColor(Color.White).margin({ left: 8 })
      }.padding(16).width('100%').backgroundColor('#0B0C11')

      Scroll() {
        Column({ space: 12 }) {
          Text('今日高光').fontSize(16).fontWeight(FontWeight.Medium).fontColor('#F5F6F8').width('100%')
          ForEach([
            ['🎬', '《哈利波特》第3章', '视频配音 · 刚刚发布', '100'],
            ['🏆', '解锁「45天」打卡勋章', '里程碑达成 · 太棒了！', '🎉'],
            ['🎵', '"Wingardium Leviosa!"', '满分句子 · 可播放原声', '▶'],
          ], (h: string[]) => {
            Row({ space: 12 }) {
              Text(h[0]).fontSize(28)
              Column() {
                Text(h[1]).fontSize(15).fontWeight(FontWeight.Medium).fontColor(Color.White)
                Text(h[2]).fontSize(12).fontColor('#8B9099').margin({ top: 4 })
              }.alignItems(HorizontalAlign.Start).layoutWeight(1)
              Text(h[3]).fontSize(14).fontColor('#FF8A34')
            }
            .padding(14).width('100%')
            .backgroundColor('#121A1F').borderRadius(14)
          })

          Text('学习记录').fontSize(16).fontWeight(FontWeight.Medium).fontColor('#F5F6F8').width('100%').margin({ top: 8 })
          Column() {
            ForEach([
              ['🎬', '视频配音', '哈利波特 第3章', '18:32', '已发布'],
              ['⚔️', '配音闯关', 'Level 8 · 3关', '17:10', '15 min'],
              ['📚', '同步练', 'PEP 五年级上册 Unit 3', '16:45', '8 min'],
              ['🤖', 'AI 外教', '自由对话 · Tom老师', '15:20', '12 min'],
            ], (r: string[], index: number) => {
              Row({ space: 12 }) {
                Text(r[0]).fontSize(22)
                Column() {
                  Text(r[1]).fontSize(15).fontWeight(FontWeight.Medium).fontColor(Color.White)
                  Text(r[2]).fontSize(12).fontColor('#8B9099').margin({ top: 2 })
                }.alignItems(HorizontalAlign.Start).layoutWeight(1)
                Column() {
                  Text(r[3]).fontSize(11).fontColor('#6B7078')
                  Text(r[4]).fontSize(12).fontColor(index === 0 ? '#FF8A34' : '#8B9099')
                }.alignItems(HorizontalAlign.End)
              }.padding(14).width('100%')
              if (index < 3) {
                Divider().color('#2A2D35').strokeWidth(0.5)
              }
            })
          }.width('100%').backgroundColor('#17161F').borderRadius(14)
        }.padding(16).width('100%')
      }.layoutWeight(1).width('100%').backgroundColor('#0B0C11')
    }
    .width('100%')
    .height('100%')
    .backgroundColor('#0B0C11')
  }
}

@Component
export struct NativePurchaseCalculatorPane {
  onClose: () => void = () => {};
  @State priceText: string = '180000';
  @State downPercent: number = 30;
  @State months: number = 36;

  private price(): number {
    const n = Number(this.priceText);
    return isNaN(n) ? 180000 : n;
  }

  private down(): number {
    return this.price() * this.downPercent / 100;
  }

  private loan(): number {
    return this.price() - this.down();
  }

  private monthly(): number {
    const r = 0.045 / 12;
    const n = this.months;
    const factor = Math.pow(1 + r, n);
    return this.loan() * r * factor / (factor - 1);
  }

  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text('购车计算器').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink).margin({ left: 8 })
      }.padding(16).width('100%').backgroundColor(DesignTokens.canvas)

      Scroll() {
        Column({ space: 16 }) {
          Text('车价（元）').fontSize(14).fontColor(DesignTokens.body).width('100%')
          TextInput({ text: this.priceText })
            .height(44)
            .backgroundColor(DesignTokens.canvas)
            .borderRadius(8)
            .border({ width: 0.5, color: DesignTokens.hairline })
            .onChange((v: string) => { this.priceText = v; })

          Text(`首付 ${this.downPercent}%`).fontSize(14).fontColor(DesignTokens.ink).width('100%')
          Slider({ value: this.downPercent, min: 10, max: 60, step: 5 })
            .selectedColor(DesignTokens.link)
            .onChange((v: number) => { this.downPercent = v; })

          Text(`期数 ${this.months} 期`).fontSize(14).fontColor(DesignTokens.ink).width('100%')
          Slider({ value: this.months, min: 12, max: 60, step: 12 })
            .selectedColor(DesignTokens.link)
            .onChange((v: number) => { this.months = v; })

          this.ResultRow('首付', `¥ ${this.down().toFixed(0)}`)
          this.ResultRow('贷款', `¥ ${this.loan().toFixed(0)}`)
          this.ResultRow('月供估算', `¥ ${this.monthly().toFixed(2)} / ${this.months} 期`)
        }.padding(16).width('100%')
      }.layoutWeight(1).width('100%')
    }
    .width('100%')
    .height('100%')
    .backgroundColor(DesignTokens.canvasSoft2)
  }

  @Builder
  ResultRow(title: string, value: string) {
    Row() {
      Text(title).fontSize(15).fontColor(DesignTokens.body).layoutWeight(1)
      Text(value).fontSize(16).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
    }
    .padding(14).width('100%')
    .backgroundColor(DesignTokens.canvas).borderRadius(8)
  }
}

@Component
export struct NativeFriendPane {
  onClose: () => void = () => {};
  onOpenChat: () => void = () => {};

  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text('通讯录').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink).margin({ left: 8 })
      }.padding(16).width('100%').backgroundColor(DesignTokens.canvas)

      List() {
        ForEach([
          ['王同学', '请求添加你为好友', '新'],
          ['李老师', '请求添加你为好友', '新'],
          ['林林', '一周前', ''],
          ['客服小助手', '昨天', ''],
        ], (f: string[]) => {
          ListItem() {
            Row({ space: 12 }) {
              Text(f[0].substring(f[0].length - 1))
                .width(44).height(44).textAlign(TextAlign.Center)
                .fontColor(DesignTokens.link).backgroundColor('#260070F3').borderRadius(22)
              Column() {
                Text(f[0]).fontSize(16).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
                Text(f[1]).fontSize(13).fontColor(DesignTokens.body).margin({ top: 2 })
              }.alignItems(HorizontalAlign.Start).layoutWeight(1)
              if (f[2].length > 0) {
                Text(f[2]).fontSize(11).fontColor(DesignTokens.link)
                  .padding({ left: 8, right: 8, top: 3, bottom: 3 })
                  .backgroundColor('#1F0070F3').borderRadius(10)
              }
            }
            .padding(16).width('100%')
            .onClick(() => this.onOpenChat())
          }
        })
      }.layoutWeight(1).width('100%')
    }
    .width('100%')
    .height('100%')
    .backgroundColor(DesignTokens.canvasSoft2)
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
  private rooms: string[][] = [
    ['1', '沃德龙鼎直播间', '在线 326 · 讲解新车', '直播中'],
    ['2', '售后讲堂', '预约 88 · 明天 19:00', '预约'],
    ['3', '二手车清库', '回放 · 观看 8.6k', '回放'],
  ];

  build() {
    Stack() {
      if (this.roomId.length > 0) {
        Column() {
          Row() {
            Text('‹').fontSize(22).fontColor(Color.White).onClick(() => { this.roomId = ''; })
            Text(this.roomTitle())
              .fontSize(17).fontWeight(FontWeight.Medium).fontColor(Color.White)
            Blank().layoutWeight(1)
            Text(this.roomBadge())
              .fontSize(12).fontColor(Color.White)
              .padding({ left: 8, right: 8, top: 4, bottom: 4 })
              .backgroundColor('#CCE02020')
              .borderRadius(12)
          }.padding(16).width('100%')
          Blank().layoutWeight(1)
          Text(this.roomTitle())
            .fontSize(20).fontWeight(FontWeight.Medium).fontColor(Color.White)
          Text('直播画面（mock）').fontSize(14).fontColor('#B3FFFFFF').margin({ top: 8 })
          Blank().layoutWeight(1)
          Text('进入直播间 · 推流通道见 gap registry')
            .fontSize(12).fontColor('#80FFFFFF')
            .margin({ bottom: 32 })
        }
        .width('100%')
        .height('100%')
        .alignItems(HorizontalAlign.Center)
        .backgroundColor(Color.Black)
      } else {
        Column() {
          Row() {
            Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
            Text('直播').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
            Blank().layoutWeight(1)
          }.padding({ left: 16, right: 16, top: 12, bottom: 12 }).width('100%').backgroundColor(DesignTokens.canvas)

          Text('点选进入房间入口 · 推流/实时通道见 gap registry')
            .fontSize(13).fontColor(DesignTokens.body)
            .padding({ left: 16, right: 16, top: 12 })
            .width('100%')

          List() {
            ForEach(this.rooms, (room: string[]) => {
              ListItem() {
                Row() {
                  Column({ space: 4 }) {
                    Text(room[1]).fontSize(16).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
                    Text(room[2]).fontSize(13).fontColor(DesignTokens.body)
                  }.alignItems(HorizontalAlign.Start).layoutWeight(1)
                  Text(room[3])
                    .fontSize(11).fontColor(DesignTokens.link)
                    .padding({ left: 8, right: 8, top: 4, bottom: 4 })
                    .backgroundColor('#1A0070F3')
                    .borderRadius(12)
                }
                .padding(16)
                .width('100%')
                .backgroundColor(DesignTokens.canvas)
                .onClick(() => { this.roomId = room[0]; })
              }
            })
          }.layoutWeight(1).width('100%').padding(16)
        }
        .width('100%')
        .height('100%')
        .backgroundColor(DesignTokens.canvasSoft2)
      }
    }
    .width('100%')
    .height('100%')
  }

  private roomTitle(): string {
    const room = this.rooms.find((r: string[]) => r[0] === this.roomId);
    return room !== undefined ? room[1] : '直播间';
  }

  private roomBadge(): string {
    const room = this.rooms.find((r: string[]) => r[0] === this.roomId);
    return room !== undefined ? room[3] : '';
  }
}

@Component
export struct NativeAiStreamPane {
  onClose: () => void = () => {};
  @State input: string = '';
  @State streaming: boolean = false;
  @State bubbles: string[][] = [
    ['assistant', '你好，我是小石头。有什么想问的？'],
  ];
  private chips: string[] = ['今日学习建议', '语法纠错', '口语话题'];

  private send(prompt: string): void {
    const q = prompt.trim();
    if (q.length === 0 || this.streaming) {
      return;
    }
    this.bubbles = [...this.bubbles, ['user', q]];
    this.input = '';
    this.streaming = true;
    const reply = `关于「${q}」：建议每天跟读 15 分钟，并记录生词。（mock 流）`;
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
        TextInput({ placeholder: '问问小石头…', text: this.input })
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
  @State result: string = '';

  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text('扫一扫').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
        Blank().layoutWeight(1)
      }.padding({ left: 16, right: 16, top: 12, bottom: 12 }).width('100%').backgroundColor(DesignTokens.canvas)

      if (this.result.length > 0) {
        Column({ space: 12 }) {
          Text('扫码结果').fontSize(15).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
          Text(this.result).fontSize(15).fontColor(DesignTokens.link)
          Button('继续扫码')
            .backgroundColor(DesignTokens.link)
            .onClick(() => { this.result = ''; })
        }
        .alignItems(HorizontalAlign.Start)
        .padding(16)
        .width('100%')
        Blank().layoutWeight(1)
      } else {
        Column({ space: 24 }) {
          Blank().layoutWeight(1)
          Text('将二维码放入框内')
            .fontSize(13).fontColor('#CCFFFFFF')
            .width(220).height(220)
            .textAlign(TextAlign.Center)
            .border({ width: 2, color: DesignTokens.link })
            .borderRadius(12)
          Text('用于门店收款码 / 活动核销').fontSize(13).fontColor('#99FFFFFF')
          Button('模拟扫码成功')
            .backgroundColor(DesignTokens.link)
            .onClick(() => { this.result = 'myai://mall/orders?id=A1024'; })
          Blank().layoutWeight(1)
        }
        .width('100%')
        .layoutWeight(1)
        .alignItems(HorizontalAlign.Center)
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
  private scripts: string[][] = [
    ['周末到店礼', '适合朋友圈 · 已用 128 次', '推荐'],
    ['置换补贴海报', '适合群发 · 已用 56 次', ''],
    ['新车到店速递', '适合视频号 · 已用 34 次', ''],
  ];

  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text('投资策略').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
        Blank().layoutWeight(1)
      }.padding({ left: 16, right: 16, top: 12, bottom: 12 }).width('100%').backgroundColor(DesignTokens.canvas)

      List() {
        ForEach(this.scripts, (s: string[]) => {
          ListItem() {
            Column({ space: 8 }) {
              Row() {
                Text(s[0]).fontSize(16).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
                Blank().layoutWeight(1)
                if (s[2].length > 0) {
                  Text(s[2]).fontSize(11).fontColor(DesignTokens.link)
                    .padding({ left: 8, right: 8, top: 4, bottom: 4 })
                    .backgroundColor('#1A0070F3').borderRadius(12)
                }
              }.width('100%')
              Text(s[1]).fontSize(13).fontColor(DesignTokens.body)
              Text('周末到店看新车，置换补贴进行中，欢迎预约试驾～')
                .fontSize(14).fontColor(DesignTokens.ink)
                .padding(12).width('100%')
                .backgroundColor(DesignTokens.canvasSoft2).borderRadius(8)
              Button('一键发圈').backgroundColor(DesignTokens.link).width('100%')
            }
            .padding(16)
            .margin({ bottom: 12 })
            .backgroundColor(DesignTokens.canvas)
            .borderRadius(12)
          }
        })
      }.layoutWeight(1).width('100%').padding(16)
    }
    .width('100%').height('100%').backgroundColor(DesignTokens.canvasSoft2)
  }
}

@Component
export struct NativeCheckInMallPane {
  onClose: () => void = () => {};
  @State points: number = 0;
  @State checkedToday: boolean = false;
  @State streak: number = 1;
  private days: string[] = ['19', '20', '21', '22', '23', '24', '今天'];
  private gifts: string[][] = [
    ['洗车券 ×1', '200 积分'],
    ['香氛挂件', '500 积分'],
    ['定制马克杯', '800 积分'],
  ];

  build() {
    Column() {
      Column({ space: 12 }) {
        Row() {
          Text('‹').fontSize(22).fontColor(Color.White).onClick(() => this.onClose())
          Text('签到商城').fontSize(17).fontWeight(FontWeight.Medium).fontColor(Color.White)
          Blank().layoutWeight(1)
        }.width('100%')
        Row() {
          Column({ space: 4 }) {
            Text('我的积分').fontSize(13).fontColor('#D9FFFFFF')
            Text(`${this.points}`).fontSize(32).fontWeight(FontWeight.Bold).fontColor(Color.White)
          }.alignItems(HorizontalAlign.Start)
          Blank().layoutWeight(1)
          Text(this.checkedToday ? '已签到' : '立即签到')
            .fontSize(14).fontWeight(FontWeight.Medium).fontColor('#2F6BFF')
            .padding({ left: 16, right: 16, top: 10, bottom: 10 })
            .backgroundColor(Color.White).borderRadius(20)
            .onClick(() => {
              if (this.checkedToday) {
                return;
              }
              this.checkedToday = true;
              this.points += 10;
              this.streak += 1;
            })
        }.width('100%')
        Row() {
          ForEach(this.days, (d: string, i: number) => {
            Column({ space: 6 }) {
              Column()
                .width(28).height(28).borderRadius(14)
                .backgroundColor((i < this.streak || (i === this.days.length - 1 && this.checkedToday))
                  ? Color.White : '#40FFFFFF')
              Text(d).fontSize(11).fontColor('#E6FFFFFF')
            }.layoutWeight(1).alignItems(HorizontalAlign.Center)
          })
        }.width('100%')
      }
      .padding(16)
      .width('100%')
      .backgroundColor('#2F6BFF')

      List() {
        ListItem() {
          Text('积分换礼').fontSize(16).fontWeight(FontWeight.Bold).fontColor(DesignTokens.ink)
            .padding({ bottom: 8 })
        }
        ForEach(this.gifts, (g: string[]) => {
          ListItem() {
            Row({ space: 12 }) {
              Column().width(56).height(56).borderRadius(8).backgroundColor(DesignTokens.canvasSoft2)
              Column({ space: 4 }) {
                Text(g[0]).fontSize(15).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
                Text(g[1]).fontSize(13).fontColor(DesignTokens.body)
              }.alignItems(HorizontalAlign.Start).layoutWeight(1)
              Text('兑换').fontSize(13).fontWeight(FontWeight.Medium).fontColor(Color.White)
                .padding({ left: 12, right: 12, top: 8, bottom: 8 })
                .backgroundColor(DesignTokens.link).borderRadius(16)
            }
            .padding(12)
            .margin({ bottom: 12 })
            .backgroundColor(DesignTokens.canvas)
            .borderRadius(12)
          }
        })
      }.layoutWeight(1).width('100%').padding(16)
    }
    .width('100%').height('100%').backgroundColor(DesignTokens.canvasSoft2)
  }
}

@Component
export struct NativeMusicPane {
  onClose: () => void = () => {};
  @State playing: string = '';
  private tracks: string[][] = [
    ['Night Drive', '3:28'],
    ['Showroom BGM', '2:51'],
    ['Weekend Cruise', '4:02'],
  ];

  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text('音乐').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
        Blank().layoutWeight(1)
      }.padding({ left: 16, right: 16, top: 12, bottom: 12 }).width('100%').backgroundColor(DesignTokens.canvas)

      List() {
        ForEach(this.tracks, (t: string[]) => {
          ListItem() {
            Row({ space: 12 }) {
              Text(this.playing === t[0] ? '❚❚' : '▶')
                .fontSize(18).fontColor(DesignTokens.link)
                .width(28).height(28).textAlign(TextAlign.Center)
              Column({ space: 4 }) {
                Text(t[0]).fontSize(16).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
                Text(t[1]).fontSize(12).fontColor(DesignTokens.mute)
              }.alignItems(HorizontalAlign.Start).layoutWeight(1)
              if (this.playing === t[0]) {
                Text('播放中').fontSize(12).fontColor(DesignTokens.link)
              }
            }
            .padding(16)
            .width('100%')
            .backgroundColor(DesignTokens.canvas)
            .onClick(() => {
              this.playing = this.playing === t[0] ? '' : t[0];
            })
          }
        })
      }.layoutWeight(1).width('100%').padding(16)
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
        Text('Club').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
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
  private templates: string[][] = [
    ['到店提醒', '您好，您预约的试驾已确认，请准时到店。'],
    ['保养到期', '爱车即将到保养周期，回店可享工时折扣。'],
    ['交车祝福', '恭喜提车！如有用车问题随时联系专属顾问。'],
  ];
  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text('短信模板').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
        Blank().layoutWeight(1)
      }.padding({ left: 16, right: 16, top: 12, bottom: 12 }).width('100%').backgroundColor(DesignTokens.canvas)
      List() {
        ForEach(this.templates, (t: string[], i: number) => {
          ListItem() {
            Column({ space: 8 }) {
              Row() {
                Text(t[0]).fontSize(16).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
                Blank().layoutWeight(1)
                if (this.selected === i) {
                  Text('✓').fontSize(16).fontColor(DesignTokens.link)
                }
              }.width('100%')
              Text(t[1]).fontSize(14).fontColor(DesignTokens.body)
            }
            .padding(16).margin({ bottom: 12 })
            .backgroundColor(DesignTokens.canvas).borderRadius(12)
            .border({ width: 1, color: this.selected === i ? DesignTokens.link : DesignTokens.hairline })
            .onClick(() => { this.selected = i; })
          }
        }, (t: string[], i: number) => `${i}_${t[0]}`)
      }.layoutWeight(1).width('100%').padding(16)
      Button('发送').backgroundColor(DesignTokens.link).width('90%').margin({ bottom: 24 })
    }.width('100%').height('100%').backgroundColor(DesignTokens.canvasSoft2)
  }
}

@Component
export struct NativeStoreQrPane {
  onClose: () => void = () => {};
  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text('店铺收款码').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
        Blank().layoutWeight(1)
      }.padding({ left: 16, right: 16, top: 12, bottom: 12 }).width('100%').backgroundColor(DesignTokens.canvas)
      Column({ space: 16 }) {
        Blank().layoutWeight(1)
        Text('QR')
          .fontSize(48).fontColor(DesignTokens.ink)
          .width(220).height(220).textAlign(TextAlign.Center)
          .border({ width: 1, color: DesignTokens.hairline }).borderRadius(12)
        Text('沃德龙鼎收款码').fontSize(13).fontColor(DesignTokens.body)
        Text('展示给客户扫码支付').fontSize(14).fontColor(DesignTokens.body)
        Button('保存到相册').backgroundColor(DesignTokens.link)
        Blank().layoutWeight(1)
      }.width('100%').layoutWeight(1).alignItems(HorizontalAlign.Center)
    }.width('100%').height('100%').backgroundColor(DesignTokens.canvasSoft2)
  }
}

@Component
export struct NativeQaPane {
  onClose: () => void = () => {};
  private qs: string[][] = [
    ['双擎和汽油怎么选？', '待回复 · 3 人围观'],
    ['置换补贴怎么算？', '已回复 · 12 人围观'],
    ['保养套餐有哪些？', '待回复 · 1 人围观'],
  ];
  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text('选买问答').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
        Blank().layoutWeight(1)
      }.padding({ left: 16, right: 16, top: 12, bottom: 12 }).width('100%').backgroundColor(DesignTokens.canvas)
      List() {
        ForEach(this.qs, (q: string[]) => {
          ListItem() {
            Row() {
              Column({ space: 4 }) {
                Text(q[0]).fontSize(15).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
                Text(q[1]).fontSize(13).fontColor(DesignTokens.body)
              }.alignItems(HorizontalAlign.Start).layoutWeight(1)
              Text('去回答').fontSize(13).fontColor(DesignTokens.link)
            }.padding(16).width('100%').backgroundColor(DesignTokens.canvas)
          }
        })
      }.layoutWeight(1).width('100%').padding(16)
    }.width('100%').height('100%').backgroundColor(DesignTokens.canvasSoft2)
  }
}

@Component
export struct NativePosterPane {
  onClose: () => void = () => {};
  @State picked: number = 0;
  private templates: string[] = ['秋季置换季', '周末到店礼', '新车上市'];
  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text('商家海报').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
        Blank().layoutWeight(1)
      }.padding({ left: 16, right: 16, top: 12, bottom: 12 }).width('100%').backgroundColor(DesignTokens.canvas)
      List() {
        ForEach(this.templates, (name: string, i: number) => {
          ListItem() {
            Row({ space: 12 }) {
              Column().width(72).height(96).borderRadius(8).backgroundColor('#260070F3')
              Column({ space: 6 }) {
                Text(name).fontSize(16).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
                Text('模板 · 可编辑文案').fontSize(13).fontColor(DesignTokens.body)
              }.alignItems(HorizontalAlign.Start).layoutWeight(1)
              if (this.picked === i) {
                Text('✓').fontSize(16).fontColor(DesignTokens.link)
              }
            }
            .padding(12).margin({ bottom: 12 })
            .backgroundColor(DesignTokens.canvas).borderRadius(12)
            .onClick(() => { this.picked = i; })
          }
        }, (name: string, i: number) => `${i}_${name}`)
      }.layoutWeight(1).width('100%').padding(16)
      Button('生成海报').backgroundColor(DesignTokens.link).width('90%').margin({ bottom: 24 })
    }.width('100%').height('100%').backgroundColor(DesignTokens.canvasSoft2)
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
  @State role: string = '销售顾问';
  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text('个人资料').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
        Blank().layoutWeight(1)
      }.padding({ left: 16, right: 16, top: 12, bottom: 12 }).width('100%').backgroundColor(DesignTokens.canvas)
      Column({ space: 16 }) {
        Text('头').fontSize(28).fontColor(DesignTokens.link)
          .width(72).height(72).textAlign(TextAlign.Center)
          .borderRadius(36).backgroundColor('#260070F3')
        Text('昵称').fontSize(13).fontColor(DesignTokens.body)
        TextInput({ text: this.nickname }).height(44).padding({ left: 12 }).backgroundColor(DesignTokens.canvas).borderRadius(10)
          .onChange((v: string) => { this.nickname = v; })
        Text('职位').fontSize(13).fontColor(DesignTokens.body)
        TextInput({ text: this.role }).height(44).padding({ left: 12 }).backgroundColor(DesignTokens.canvas).borderRadius(10)
          .onChange((v: string) => { this.role = v; })
        Button('保存').backgroundColor(DesignTokens.link).width('100%').onClick(() => this.onClose())
      }.padding(16).width('100%').alignItems(HorizontalAlign.Start)
      Blank().layoutWeight(1)
    }.width('100%').height('100%').backgroundColor(DesignTokens.canvasSoft2)
  }
}

@Component
export struct NativeAddressesPane {
  onClose: () => void = () => {};
  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text('收货地址').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
        Blank().layoutWeight(1)
      }.padding({ left: 16, right: 16, top: 12, bottom: 12 }).width('100%').backgroundColor(DesignTokens.canvas)
      Column({ space: 12 }) {
        Column({ space: 6 }) {
          Row({ space: 8 }) {
            Text('默认地址').fontSize(15).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
            Text('默认').fontSize(11).fontColor(DesignTokens.link)
              .padding({ left: 6, right: 6, top: 2, bottom: 2 })
              .backgroundColor('#1A0070F3').borderRadius(8)
          }
          Text('北京市大兴区 · 兴荣丰田').fontSize(14).fontColor(DesignTokens.body)
          Text('qa_user  138****5172').fontSize(13).fontColor(DesignTokens.mute)
        }.alignItems(HorizontalAlign.Start).padding(16).width('100%')
        .backgroundColor(DesignTokens.canvas).borderRadius(12)
        Button('添加地址').backgroundColor(DesignTokens.link).width('100%')
      }.padding(16).width('100%')
      Blank().layoutWeight(1)
    }.width('100%').height('100%').backgroundColor(DesignTokens.canvasSoft2)
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
  private ranks: string[][] = [
    ['穿条纹睡衣的男孩', '热度 9821', '1'],
    ['蛮荒故事', '热度 8740', '2'],
    ['爱冒险的朵拉', '热度 7655', '3'],
  ];
  build() {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(DesignTokens.link).onClick(() => this.onClose())
        Text('热配榜').fontSize(17).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
        Blank().layoutWeight(1)
      }.padding({ left: 16, right: 16, top: 12, bottom: 12 }).width('100%').backgroundColor(DesignTokens.canvas)
      List() {
        ForEach(this.ranks, (r: string[]) => {
          ListItem() {
            Row({ space: 12 }) {
              Text(r[2]).fontSize(18).fontWeight(FontWeight.Bold).fontColor(DesignTokens.link).width(28)
              Column({ space: 4 }) {
                Text(r[0]).fontSize(15).fontWeight(FontWeight.Medium).fontColor(DesignTokens.ink)
                Text(r[1]).fontSize(13).fontColor(DesignTokens.body)
              }.alignItems(HorizontalAlign.Start).layoutWeight(1)
            }.padding(16).width('100%').backgroundColor(DesignTokens.canvas)
          }
        })
      }.layoutWeight(1).width('100%').padding(16)
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
