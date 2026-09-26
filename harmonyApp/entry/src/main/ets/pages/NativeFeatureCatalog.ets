/**
 * ADR 0002 — ArkTS native feature catalog + host.
 * Mirrors iosApp NativeFeatureCatalog.swift
 */

export interface FeatureRow {
  id: string;
  title: string;
  detail: string;
  badge: string;
}

export interface FeaturePage {
  id: string;
  title: string;
  subtitle: string;
  rows: FeatureRow[];
  primaryAction: string;
}

const LABEL_TO_PATH: Map<string, string> = new Map([
  ['搜索', '/home/search'],
  ['全部服务', '/home/all_services'],
  ['更多', '/home/all_services'],
  ['投资策略', '/home/strategy'],
  ['策略', '/home/strategy'],
  ['学习报告', '/home/learning_report'],
  ['签到商城', '/home/check_in_mall'],
  ['积分商城', '/home/check_in_mall'],
  ['配音', '/home/dubbing_feed'],
  ['配音首页', '/home/dubbing_feed'],
  ['热榜', '/home/hot_rank_detail'],
  ['热配榜', '/home/hot_rank_detail'],
  ['生活服务', '/home/life_service'],
  ['直播带货', '/home/live_commerce'],
  ['直播', '/home/live_commerce'],
  ['Club', '/home/club'],
  ['二手车', '/home/used_car'],
  ['台账', '/home/ledger'],
  ['公司数据', '/home/ledger'],
  ['收支', '/home/ledger'],
  ['数据分析', '/home/data_analytics'],
  ['竞品分析', '/home/data_analytics'],
  ['新伙伴待确认', '/home/todo/partner-pending'],
  ['待跟进客户', '/home/todo/follow-up-customers'],
  ['售后预约', '/home/todo/after-sales-appointments'],
  ['订单待审核', '/home/todo/order-pending-review'],
  ['售后', '/home/after_sales'],
  ['售后专区', '/home/after_sales'],
  ['新车跟进', '/home/new_car_follow'],
  ['新车成交', '/home/new_car_follow'],
  ['新车关注', '/home/new_car_follow'],
  ['销售顾问', '/home/todo/follow-up-customers'],
  ['AI小石头', '/ai/stream'],
  ['AI 小石', '/ai/stream'],
  ['AI小石', '/ai/stream'],
  ['订单中心', '/mall/orders'],
  ['营销活动', '/home/check_in_mall'],
  ['朋友圈营销', '/home/strategy'],
  ['朋友圈', '/home/strategy'],
  ['话术库', '/home/strategy'],
  ['视频号', '/video'],
  ['素材库', '/video'],
  ['培训', '/classroom/my_class'],
  ['扫一扫', '/scan'],
  ['消息', '/chat'],
  ['发布动态', '/community/publish'],
  ['社区发布', '/community/publish'],
  ['社区搜索', '/community/search'],
  ['评论', '/community/comment'],
  ['社区评论', '/community/comment'],
  ['商城', '/mall'],
  ['我的钱包', '/wallet'],
  ['我的课程', '/classroom/my_class'],
  ['我的订单', '/mall/orders'],
  ['短信模板', '/mine/sms_template'],
  ['购车计算器', '/mine/purchase_calculator'],
  ['小视频', '/video/short'],
  ['短视频', '/video/short'],
  ['店铺收款码', '/mine/store_qr'],
  ['选买问答', '/mine/qa'],
  ['商家海报', '/mine/poster'],
  ['商务合作', '/mine/business'],
  ['提醒事项', '/mine/reminders'],
  ['邀请好友', '/friend'],
  ['粉丝群', '/mine/fan_group'],
  ['意见反馈', '/mine/feedback'],
  ['帮助中心', '/mine/feedback'],
  ['收货地址', '/mine/addresses'],
  ['地址管理', '/mine/addresses'],
  ['地址', '/mine/addresses'],
  ['个人资料', '/mine/profile'],
  ['资料', '/mine/profile'],
  ['签到日历', '/home/check_in_mall'],
  ['签到', '/home/check_in_mall'],
  ['H5 调试', '/web'],
  ['内嵌网页', '/web'],
  ['音乐', '/music/list'],
  ['好友', '/friend'],
  ['通讯录', '/friend'],
  ['朋友', '/friend'],
  ['切换门店', '/friend'],
  ['首页', '/home'],
  ['聊天', '/chat'],
  ['社区', '/community'],
  ['我的', '/mine'],
  ['设置', '/settings'],
  ['个性化', '/mine/personalized_settings'],
  ['会员', '/pay/membership'],
  ['会员续费', '/pay/membership'],
  ['电子名片', '/mine/business_card'],
  ['门店', '/home/data_analytics'],
  ['订单中心', '/mall/orders'],
  ['视频列表', '/video'],
  ['作品列表', '/video/dubbing/works'],
  ['关于', '/mine/about'],
]);

export function resolveNativePath(raw: string): string {
  const key = raw.trim();
  if (key.length === 0) {
    return key;
  }
  if (key.startsWith('/')) {
    if (key.startsWith('/video/short/') || key === '/video/short') {
      return key === '/video/short' || key.startsWith('/video/short?') ? '/video/short' : key;
    }
    if (key.startsWith('/live')) {
      return '/live';
    }
    if (key.startsWith('/friend')) {
      return '/friend';
    }
    if (key.startsWith('/mall/orders')) {
      return '/mall/orders';
    }
    if (key.startsWith('/mall/detail')) {
      return '/mall/detail';
    }
    if (key.startsWith('/mall')) {
      return '/mall';
    }
    if (key.startsWith('/wallet')) {
      return '/wallet';
    }
    if (key.startsWith('/ai')) {
      return '/ai/stream';
    }
    if (key.startsWith('/music')) {
      return '/music/list';
    }
    if (key.startsWith('/classroom')) {
      return '/classroom/my_class';
    }
    if (key.startsWith('/community/publish')) {
      return '/community/publish';
    }
    if (key.startsWith('/community/search')) {
      return '/community/search';
    }
    if (key.startsWith('/community/comment')) {
      return '/community/comment';
    }
    if (key.startsWith('/community/image_preview')) {
      return '/community/image_preview';
    }
    if (key.startsWith('/web') || key.startsWith('http')) {
      return '/web';
    }
    if (key.startsWith('/scan')) {
      return '/scan';
    }
    return key;
  }
  const mapped = LABEL_TO_PATH.get(key);
  return mapped !== undefined ? mapped : key;
}

function page(id: string, title: string, subtitle: string, rows: FeatureRow[], primaryAction: string): FeaturePage {
  return { id, title, subtitle, rows, primaryAction };
}

function row(id: string, title: string, detail: string, badge: string = ''): FeatureRow {
  return { id, title, detail, badge };
}

export function featurePageFor(pathOrLabel: string): FeaturePage {
  const path = resolveNativePath(pathOrLabel);
  switch (path) {
    case '/home/used_car':
      return page(path, '二手车', '置换 / 专卖 / 估价', [
        row('1', '2019 凯美瑞 2.0G', '里程 6.2 万 · 北京大兴', '在售'),
        row('2', '2021 卡罗拉 双擎', '里程 3.1 万 · 天津', '已定'),
        row('3', '2018 RAV4 荣放', '里程 9.8 万 · 廊坊', '在售'),
      ], '发布车源');
    case '/home/life_service':
      return page(path, '生活服务', '洗车 · 保养 · 代驾', [
        row('1', '精洗套餐', '外观+内饰 · ¥99', '热门'),
        row('2', '小保养预约', '机油机滤 · 明日可约'),
        row('3', '代驾上门', '起步 5km · ¥35'),
      ], '立即预约');
    case '/home/new_car_follow':
      return page(path, '新车关注', '跟进中的意向客户', [
        row('1', '王先生 · 凯美瑞', '跟进中 · 预计本周到店', 'A'),
        row('2', '李女士 · 汉兰达', '报价已发 · 待回访', 'B'),
      ], '新建跟进');
    case '/home/data_analytics':
      return page(path, '数据分析', '本周门店经营概览', [
        row('1', '进店客流', '328 · 环比 +12%'),
        row('2', '成交台数', '17 · 环比 +2'),
        row('3', '线索转化', '6.8% · 持平'),
      ], '');
    case '/home/live_commerce':
      return page(path, '直播带货', '本场预告与回放', [
        row('1', '周末新车专场', '今晚 20:00 · 预约 1.2k', '预告'),
        row('2', '二手车清库直播', '回放 · 观看 8.6k', '回放'),
      ], '开播');
    case '/home/club':
      return page(path, 'Club', '车友活动与圈子', [
        row('1', '周末自驾·密云水库', '报名 36 · 周六出发', '报名中'),
        row('2', '店庆抽奖夜', '本周五 · 门店大厅'),
      ], '发布活动');
    case '/home/strategy':
      return page(path, '投资策略', '朋友圈营销话术', [
        row('1', '周末到店礼', '适合朋友圈 · 已用 128 次', '推荐'),
        row('2', '置换补贴海报', '适合群发 · 已用 56 次'),
      ], '一键发圈');
    case '/home/learning_report':
      return page(path, '学习报告', '本周学习时长', [
        row('1', '产品知识', '完成 4/5 · 86 分'),
        row('2', '话术演练', '完成 2/3 · 78 分'),
      ], '');
    case '/home/check_in_mall':
      return page(path, '签到商城', '积分兑换好物', [
        row('1', '洗车券 ×1', '200 积分', '可兑'),
        row('2', '香氛挂件', '500 积分'),
      ], '去签到');
    case '/home/dubbing_feed':
      return page(path, '配音', '热门配音素材', [
        row('1', '小王子 · 片段 3', '难度初级 · 女声', '热'),
        row('2', '飞屋环游记 · OP', '难度中级 · 男声'),
      ], '开始配音');
    case '/home/hot_rank_detail':
      return page(path, '热配榜', '今日热门', [
        row('1', '穿条纹睡衣的男孩', '热度 9821', '1'),
        row('2', '蛮荒故事', '热度 8740', '2'),
        row('3', '爱冒险的朵拉', '热度 7655', '3'),
      ], '');
    case '/home/ledger':
      return page(path, '台账', '公司经营数据', [
        row('1', '9 月销售额', '¥ 2,860,000'),
        row('2', '毛利率', '18.6%'),
      ], '');
    case '/home/todo/partner-pending':
      return page(path, '新伙伴待确认', '3 位新成员', [
        row('1', '赵倩 · 销售顾问', '待审核 · 昨天申请', '待审'),
        row('2', '孙浩 · 售后技师', '待审核 · 今天申请', '待审'),
      ], '全部通过');
    case '/home/todo/follow-up-customers':
      return page(path, '待跟进客户', '今日 5 位意向', [
        row('1', '陈先生', '意向凯美瑞 · 未回访 2 天', '紧急'),
        row('2', '周女士', '询价汉兰达 · 今早留言'),
      ], '开始跟进');
    case '/home/todo/after-sales-appointments':
      return page(path, '售后预约', '今日工位', [
        row('1', '10:30 · 保养', '京 A·88888 · 技师小刘'),
        row('2', '14:00 · 钣喷', '京 B·66666 · 技师老王'),
      ], '新建预约');
    case '/home/todo/order-pending-review':
      return page(path, '订单待审核', '门店订单', [
        row('1', '订单 #90821', '定金 ¥5000 · 待店长审', '待审'),
      ], '');
    case '/home/after_sales':
      return page(path, '售后专区', '维修保养记录', [
        row('1', '京 A·12345 · 小保养', '完成 · 2026-09-20', '完成'),
        row('2', '京 C·54321 · 四轮定位', '进行中', '进行中'),
      ], '新建工单');
    case '/mall':
      return page(path, '商城', '车品精选', [
        row('1', '行车记录仪 Pro', '¥ 399', '热销'),
        row('2', '脚垫全包围', '¥ 268'),
        row('3', '玻璃水 2L×4', '¥ 39'),
      ], '去结算');
    case '/mall/orders':
      return page(path, '我的订单', '', [
        row('1', '订单 #A1024', '待发货 · ¥399', '待发货'),
        row('2', '订单 #A0988', '已完成 · ¥268', '完成'),
      ], '');
    case '/mall/detail':
      return page(path, '商品详情', '积分兑换', [
        row('1', '店庆纪念马克杯', '39.90元 · 已兑2391', '实物'),
        row('2', '规格', '默认款 · 1 件'),
      ], '立即兑换');
    case '/wallet':
      return page(path, '我的钱包', '可用余额', [
        row('1', '余额', '¥ 1,280.50'),
        row('2', '积分', '3,560'),
        row('3', '优惠券', '4 张可用'),
      ], '充值');
    case '/video/short':
      return page(path, '小视频', '用小视频秀车秀店', [
        row('1', '新车到店 · 15s', '播放 2.1k · 赞 186'),
        row('2', '保养小贴士', '播放 980 · 赞 64'),
      ], '拍一个');
    case '/video/short/play':
      return page(path, '播放', '全屏预览', [
        row('1', '新车到店 · 15s', '点赞 · 评论 · 分享'),
      ], '');
    case '/video/short/publish':
      return page(path, '发布小视频', '选择素材', [
        row('1', '封面', '从相册选择'),
        row('2', '标题', '说说这一刻…'),
      ], '发布');
    case '/video/short/help':
      return page(path, '如何拍摄小视频', '拍摄技巧', [
        row('1', '竖屏构图', '主体居中，光线充足'),
        row('2', '时长建议', '15–30 秒最佳'),
      ], '');
    case '/live':
      return page(path, '直播', '直播间列表', [
        row('1', '沃德龙鼎直播间', '在线 326 · 讲解新车', '直播中'),
        row('2', '售后讲堂', '预约 88 · 明天 19:00', '预约'),
      ], '进入直播间');
    case '/friend':
      return page(path, '通讯录', '好友与申请', [
        row('1', '王同学', '请求添加你为好友', '新'),
        row('2', '李老师', '请求添加你为好友', '新'),
        row('3', '林林', '一周前'),
        row('4', '客服小助手', '昨天'),
      ], '建群');
    case '/ai/stream':
      return page(path, 'AI 小石头', '智能问答（mock）', [
        row('1', '你', '帮我写一条凯美瑞朋友圈'),
        row('2', '小石头', '周末到店看凯美瑞双擎，油耗惊喜，置换补贴进行中～', 'AI'),
      ], '继续对话');
    case '/classroom/my_class':
      return page(path, '我的课程', '班级与作业', [
        row('1', '产品知识班', '未交作业 2'),
        row('2', '配音作业', '待批改 1'),
      ], '进入课堂');
    case '/classroom/homework':
      return page(path, '作业详情', '待提交', [
        row('1', '产品知识测验', '截止今晚 22:00', '待交'),
      ], '去完成');
    case '/music/list':
      return page(path, '音乐', '播放列表', [
        row('1', 'Night Drive', '3:28'),
        row('2', 'Showroom BGM', '2:51'),
      ], '播放');
    case '/community/publish':
      return page(path, '发布动态', '分享新鲜事', [
        row('1', '正文', '说说今天的见闻…'),
        row('2', '话题', '#Flutter开发 #户外'),
      ], '发布');
    case '/community/search':
      return page(path, '社区搜索', '动态 · 话题 · 用户', [
        row('1', '热门话题', '#换车季 #保养日记'),
      ], '');
    case '/community/comment':
      return page(path, '评论', '说说你的看法', [
        row('1', '李四', '说得对！'),
        row('2', '赵六', '同感 +1'),
      ], '发送评论');
    case '/community/image_preview':
      return page(path, '图片预览', '', [
        row('1', '预览图', '点击返回'),
      ], '');
    case '/scan':
      return page(path, '扫一扫', '对准二维码完成核销 / 收款', [
        row('1', '对准二维码', '用于门店收款码 / 活动核销'),
      ], '打开相机');
    case '/web':
      return page(path, '网页', '离线 fixture', [
        row('1', '离线网页', '无网络也可显示的固定页面'),
      ], '');
    case '/mine/purchase_calculator':
      return page(path, '购车计算器', '全款 / 贷款 / 保险', [
        row('1', '车价', '¥ 180,000'),
        row('2', '首付 30%', '¥ 54,000'),
        row('3', '月供估算', '¥ 5,830.00 / 36 期', '结果'),
      ], '重新计算');
    case '/mine/sms_template':
      return page(path, '短信模板', '一键发送', [
        row('1', '到店提醒', '您好，您预约的试驾已确认…'),
        row('2', '保养到期', '爱车即将到保养周期…'),
      ], '发送');
    case '/mine/store_qr':
      return page(path, '店铺收款码', '常见问题 · 功能介绍', [
        row('1', '收款码', '展示给客户扫码支付'),
      ], '保存到相册');
    case '/mine/qa':
      return page(path, '选买问答', '在线解答客户问题', [
        row('1', '双擎和汽油怎么选？', '待回复 · 3 人围观', '待回'),
      ], '去回答');
    case '/mine/poster':
      return page(path, '商家海报', '置换 / 专卖 / 估价', [
        row('1', '秋季置换季', '模板 · 可编辑文案'),
      ], '生成海报');
    case '/mine/business':
      return page(path, '商务合作', '', [
        row('1', '渠道合作', '提交合作意向'),
      ], '提交');
    case '/mine/reminders':
      return page(path, '提醒事项', '', [
        row('1', '回访陈先生', '今天 16:00'),
        row('2', '提交周报', '周五 18:00'),
      ], '新建提醒');
    case '/mine/feedback':
      return page(path, '意见反馈', '', [
        row('1', '反馈类型', '功能建议 / 体验问题 / 其它'),
      ], '提交反馈');
    case '/mine/profile':
      return page(path, '个人资料', '', [
        row('1', '昵称', 'qa_user'),
        row('2', '职位', '销售顾问'),
      ], '保存');
    case '/mine/addresses':
      return page(path, '收货地址', '', [
        row('1', '默认地址', '北京市大兴区 · 兴荣丰田', '默认'),
      ], '添加地址');
    case '/home/search':
      return page(path, '搜索', '', [], '');
    case '/home/all_services':
      return page(path, '全部服务', '', [], '');
    default: {
      const parts = path.split('/');
      const title = parts.length > 0 ? parts[parts.length - 1] : pathOrLabel;
      return page(path, title.length > 0 ? title : '功能', path, [
        row('1', '功能已接入原生壳', 'ArkTS 实现，业务 mock 对齐 Flutter'),
      ], '');
    }
  }
}
