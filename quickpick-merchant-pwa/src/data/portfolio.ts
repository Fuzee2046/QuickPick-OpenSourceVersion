import brandLogo from '@/assets/portfolio/story/brand-logo.webp'
import businessLicense from '@/assets/portfolio/story/business-license-public.webp'
import miniProgram from '@/assets/portfolio/story/mini-program.webp'
import pain01 from '@/assets/portfolio/story/pain-01.webp'
import pain02 from '@/assets/portfolio/story/pain-02.webp'
import pain03 from '@/assets/portfolio/story/pain-03.webp'
import campaignStart01 from '@/assets/portfolio/story/campaign-start-01.webp'
import campaignStart02 from '@/assets/portfolio/story/campaign-start-02.webp'
import campaignReservations from '@/assets/portfolio/story/campaign-reservations.webp'
import campaignWinner01 from '@/assets/portfolio/story/campaign-winner-01.webp'
import campaignWinner02 from '@/assets/portfolio/story/campaign-winner-02.webp'
import launchDay02 from '@/assets/portfolio/story/launch-day-02.webp'
import launchDay03 from '@/assets/portfolio/story/launch-day-03.webp'
import shop01 from '@/assets/portfolio/story/shop-01.webp'
import shop02 from '@/assets/portfolio/story/shop-02.webp'
import shop03 from '@/assets/portfolio/story/shop-03.webp'
import shop04 from '@/assets/portfolio/story/shop-04.webp'
import shop05 from '@/assets/portfolio/story/shop-05.webp'
import shop06 from '@/assets/portfolio/story/shop-06.webp'
import shop07 from '@/assets/portfolio/story/shop-07.webp'
import shop08 from '@/assets/portfolio/story/shop-08.webp'
import shop09 from '@/assets/portfolio/story/shop-09.webp'
import shop10 from '@/assets/portfolio/story/shop-10.webp'
import shop11 from '@/assets/portfolio/story/shop-11.webp'
import shop12 from '@/assets/portfolio/story/shop-12.webp'
import shop13 from '@/assets/portfolio/story/shop-13.webp'
import conference01 from '@/assets/portfolio/story/conference-01.webp'
import conference02 from '@/assets/portfolio/story/conference-02.webp'
import conference03 from '@/assets/portfolio/story/conference-03.webp'
import conference04 from '@/assets/portfolio/story/conference-04.webp'
import summerOrders from '@/assets/portfolio/story/orders-summer.webp'
import redis11 from '@/assets/portfolio/redis/测试1/1-清楚缓存数据.png'
import redis12 from '@/assets/portfolio/redis/测试1/2-测试数据.png'
import redis13 from '@/assets/portfolio/redis/测试1/3-Redis结果.png'
import redis21 from '@/assets/portfolio/redis/测试2/1-删除江西小炒热点key.png'
import redis22 from '@/assets/portfolio/redis/测试2/2-测试前回源累计次数.png'
import redis23 from '@/assets/portfolio/redis/测试2/3-回源时间.png'
import redis24 from '@/assets/portfolio/redis/测试2/4-测试后回源累计次数.png'
import redis25 from '@/assets/portfolio/redis/测试2/5-回源结果.png'
import redis31 from '@/assets/portfolio/redis/测试3/1-测试前缓存情况.png'
import redis32 from '@/assets/portfolio/redis/测试3/2-测试数据.png'
import redis33 from '@/assets/portfolio/redis/测试3/3-测试后缓存情况.png'
import redis34 from '@/assets/portfolio/redis/测试3/4-空缓存消失.png'
import redis41 from '@/assets/portfolio/redis/测试4/1-填入创建请求体数据.png'
import redis42 from '@/assets/portfolio/redis/测试4/2-Redis上锁.png'
import redis43 from '@/assets/portfolio/redis/测试4/3-订单创建情况.png'
import redis51 from '@/assets/portfolio/redis/测试5/1-后端启动多实例分布式.png'
import redis52 from '@/assets/portfolio/redis/测试5/2-Redis数据.png'

export interface PortfolioSection {
  id: string
  label: string
}

export interface PortfolioImage {
  src: string
  alt: string
  caption: string
}

const image = (src: string, alt: string, caption: string): PortfolioImage => ({ src, alt, caption })

export const portfolioContent = {
  project: {
    name: '食刻快取',
    tagline: '一款已经在校园食堂真实运行的提前点餐小程序',
    period: '2026.03 - 至今',
    role: '个人创业项目 · 独立设计、开发与运营',
    logo: brandLogo,
    license: image(
      businessLicense,
      '食刻快取信息技术服务部营业执照公开脱敏版',
      '2026 年 3 月 17 日完成工商登记。公开展示版仅遮挡左上角统一社会信用代码。',
    ),
  },
  sections: [
    { id: 'origin', label: '现场痛点' },
    { id: 'foundation', label: '企业与小程序' },
    { id: 'campaign', label: '引流活动' },
    { id: 'launch', label: '初次成果' },
    { id: 'shops', label: '门店实景' },
    { id: 'conference', label: '校园宣贯会' },
    { id: 'evidence', label: '真实数据' },
    { id: 'redis', label: '后续测试' },
  ] satisfies PortfolioSection[],
  origin: [
    image(pain01, '午餐高峰期五谷渔粉门店前排队的学生', '午餐高峰期，学生集中在档口前等待。'),
    image(pain02, '午餐高峰期楚山牛蛋炒饭门店前的队伍', '点单、制作和取餐集中在同一窗口。'),
    image(pain03, '楚山牛蛋炒饭门店前等待取餐的学生', '有限的课间时间被排队和等待占用。'),
  ],
  foundation: {
    image: image(miniProgram, '微信公众平台中的食刻快取小程序基本信息', '食刻快取小程序完成主体认证，页面记录认证主体和认证时间。'),
    milestones: [
      { date: '2026.03.17', title: '完成工商登记' },
      { date: '2026.03.25', title: '完成小程序微信认证' },
      { date: '2026.04', title: '进入校园食堂真实运行' },
    ],
  },
  campaign: [
    image(campaignStart01, '食刻快取免单活动已有 24 人预约', '活动上线后，首轮预约人数达到 24 人。'),
    image(campaignStart02, '食刻快取免单活动已有 28 人预约', '预约人数继续增长至 28 人。'),
    image(campaignReservations, '食刻快取免单活动已有 40 人预约', '后续单轮活动预约人数达到 40 人。'),
    image(campaignWinner01, '食刻快取免单活动中奖页面', '系统按设定时间开奖并展示中奖结果。'),
    image(campaignWinner02, '食刻快取免单活动 41 人参与后的中奖页面', '另一轮活动记录了 41 名预约用户和开奖结果。'),
  ],
  launch: [
    image(launchDay02, '江西小炒接入食刻快取第二天的商户订单看板', '接入第二天：商户端记录 46 单完成、当日营业额 751 元。'),
    image(launchDay03, '江西小炒接入食刻快取第三天的商户订单看板', '接入第三天：商户端记录 50 单完成、当日营业额 767 元。'),
  ],
  shops: [
    image(shop01, '校园食堂蜀记麻辣烫档口实景', '蜀记麻辣烫'),
    image(shop02, '校园食堂水饺和广东石磨肠粉档口实景', '水饺 · 广东石磨肠粉'),
    image(shop03, '校园食堂五谷渔粉档口实景', '五谷渔粉'),
    image(shop04, '校园食堂川香粉面世家档口实景', '川香粉面世家'),
    image(shop05, '校园食堂餐饮档口实景', '校园餐饮档口'),
    image(shop06, '校园食堂拌粉瓦罐汤档口实景', '拌粉瓦罐汤'),
    image(shop07, '校园食堂福建馄饨粥铺档口实景', '福建馄饨 · 粥铺'),
    image(shop08, '校园食堂沙县小吃档口实景', '沙县小吃'),
    image(shop09, '校园食堂饺多多档口实景', '饺多多'),
    image(shop10, '校园食堂牛肉牛杂面酸辣粉档口实景', '牛肉牛杂面 · 酸辣粉'),
    image(shop11, '校园食堂好口味粉面档口实景', '好口味粉面'),
    image(shop12, '校园食堂煲仔饭辣椒炒肉档口实景', '煲仔饭 · 辣椒炒肉'),
    image(shop13, '校园食堂江西小炒档口实景', '江西小炒'),
  ],
  conference: [
    image(conference01, '南昌航空大学科技园入驻企业宣贯会现场', '2026 年 4 月 24 日，南昌航空大学科技园入驻企业宣贯会。'),
    image(conference02, '校园企业宣贯会建设大学科技园主题分享', '宣贯会现场的大学科技园建设主题分享。'),
    image(conference03, '校园企业宣贯会学生创业实践路径分享', '现场介绍学生创业实践与企业孵化路径。'),
    image(conference04, '校园企业宣贯会入驻交流环节', '宣贯会现场的入驻交流环节。'),
  ],
  evidence: image(
    summerOrders,
    'shop_id 已脱敏的江西小炒 2026 年 7 月 21 日至 7 月 30 日每日订单计数',
    '暑期校园人数较少，江西小炒仍保持每日订单产出。截图已遮挡 shop_id，可见 7 月 21 日至 30 日每日记录为 50 至 73 单。',
  ),
  redis: {
    status: '真实测试记录',
    title: 'Redis 工程化能力验证',
    description: '基于真实运行环境完成缓存、热点 Key、缓存穿透、订单幂等和多实例任务锁验证。',
    scenarios: [
      {
        id: 'redis-cache', number: '01', title: '目录缓存命中',
        summary: '冷读回源，热读命中 Redis', value: '降低店铺与菜单的重复查询',
        images: [redis11, redis12, redis13].map((src, index) => image(src, `Redis 测试一第 ${index + 1} 张`, ['清理指定缓存键', 'JMeter 顺序读取数据', 'Redis 写入结果'][index] ?? '测试证据')),
      },
      {
        id: 'redis-hot-key', number: '02', title: '热点 Key 互斥重建',
        summary: '30 并发请求验证热点菜单回源', value: '互斥锁避免缓存击穿',
        images: [redis21, redis22, redis23, redis24, redis25].map((src, index) => image(src, `Redis 测试二第 ${index + 1} 张`, ['删除热点 Key', '记录回源基线', '查看回源耗时', '记录测试后回源次数', '并发响应结果'][index] ?? '测试证据')),
      },
      {
        id: 'redis-penetration', number: '03', title: '缓存穿透防护',
        summary: '不存在店铺写入短期空值缓存', value: '拦截重复无效查询，保护 MySQL',
        images: [redis31, redis32, redis33, redis34].map((src, index) => image(src, `Redis 测试三第 ${index + 1} 张`, ['测试前缓存状态', '请求不存在数据', '测试后空值缓存', 'TTL 到期自动消失'][index] ?? '测试证据')),
      },
      {
        id: 'redis-idempotency', number: '04', title: '订单幂等防重',
        summary: '相同请求 ID 并发提交订单', value: 'Lua 原子状态机确保只创建一笔订单',
        images: [redis41, redis42, redis43].map((src, index) => image(src, `Redis 测试四第 ${index + 1} 张`, ['填写相同请求体', 'Redis 写入处理中状态', '最终订单结果'][index] ?? '测试证据')),
      },
      {
        id: 'redis-scheduler-lock', number: '05', title: '定时任务分布式锁',
        summary: '两个后端实例连接同一 Redis', value: '避免抽奖、提醒任务多实例重复执行',
        images: [redis51, redis52].map((src, index) => image(src, `Redis 测试五第 ${index + 1} 张`, ['双实例启动配置', 'Redis 任务锁记录'][index] ?? '测试证据')),
      },
    ],
  },
}
