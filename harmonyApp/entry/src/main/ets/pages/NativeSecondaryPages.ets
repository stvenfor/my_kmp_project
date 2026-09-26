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
