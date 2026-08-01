<template>
  <div class="portfolio-page">
    <header class="portfolio-header">
      <div class="portfolio-header__inner">
        <RouterLink class="portfolio-brand" to="/portfolio" aria-label="返回作品集顶部">
          <img :src="portfolioContent.project.logo" alt="" />
          <span><strong>食刻快取</strong><small>真实项目纪实</small></span>
        </RouterLink>

        <RouterLink class="back-link" to="/login">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回登录</span>
        </RouterLink>
      </div>
    </header>

    <aside class="portfolio-sidebar">
      <nav class="portfolio-nav" aria-label="作品集章节">
        <button
          v-for="(section, index) in portfolioContent.sections"
          :key="section.id"
          type="button"
          :class="{ 'is-active': activeSection === section.id }"
          :aria-current="activeSection === section.id ? 'true' : undefined"
          @click="scrollToSection(section.id)"
        >
          <span>{{ String(index + 1).padStart(2, '0') }}</span>
          <strong>{{ section.label }}</strong>
        </button>
      </nav>
    </aside>

    <main class="portfolio-main">
      <section class="portfolio-hero" aria-labelledby="portfolio-title">
        <div class="hero-brand">
          <img class="hero-brand__logo" :src="portfolioContent.project.logo" alt="食刻快取项目 Logo" />
          <span class="profit-badge"><i></i>稳定盈利</span>
          <h1 id="portfolio-title">{{ portfolioContent.project.name }}</h1>
          <p class="hero-brand__tagline">{{ portfolioContent.project.tagline }}</p>
          <p class="hero-brand__role">{{ portfolioContent.project.role }}</p>
          <div class="hero-status" aria-label="项目状态">
            <span><i></i>已完成工商登记</span>
            <span><i></i>小程序已认证</span>
            <span><i></i>校园真实运行</span>
            <span class="hero-status__profit"><i></i>稳定盈利</span>
          </div>
        </div>

        <figure class="license-figure">
          <button type="button" @click="openImage(portfolioContent.project.license)">
            <img :src="portfolioContent.project.license.src" :alt="portfolioContent.project.license.alt" />
            <span class="image-zoom"><el-icon><ZoomIn /></el-icon></span>
          </button>
          <figcaption>
            <strong>营业执照 · 公开脱敏版</strong>
            <span>{{ portfolioContent.project.license.caption }}</span>
          </figcaption>
        </figure>
      </section>

      <section id="origin" class="story-section" aria-labelledby="origin-title">
        <div class="section-heading">
          <span class="section-number">01</span>
          <h2 id="origin-title">先看见真实的排队现场</h2>
        </div>
        <p class="section-lead">午餐高峰期，学生集中排队、现场点单并等待制作。这些照片是项目开始前记录的食堂现场，也是食刻快取要解决的问题。</p>
        <div class="origin-gallery">
          <figure v-for="item in portfolioContent.origin" :key="item.src" class="photo-card">
            <button type="button" @click="openImage(item)">
              <img loading="lazy" :src="item.src" :alt="item.alt" />
              <span class="image-zoom"><el-icon><ZoomIn /></el-icon></span>
            </button>
            <figcaption>{{ item.caption }}</figcaption>
          </figure>
        </div>
      </section>

      <section id="foundation" class="story-section" aria-labelledby="foundation-title">
        <div class="section-heading">
          <span class="section-number">02</span>
          <h2 id="foundation-title">注册企业，也把小程序真正建起来</h2>
        </div>
        <div class="foundation-layout">
          <figure class="wide-evidence">
            <button type="button" @click="openImage(portfolioContent.foundation.image)">
              <img loading="lazy" :src="portfolioContent.foundation.image.src" :alt="portfolioContent.foundation.image.alt" />
              <span class="image-zoom"><el-icon><ZoomIn /></el-icon></span>
            </button>
            <figcaption>{{ portfolioContent.foundation.image.caption }}</figcaption>
          </figure>
          <ol class="milestone-list">
            <li v-for="item in portfolioContent.foundation.milestones" :key="item.date">
              <time>{{ item.date }}</time><strong>{{ item.title }}</strong>
            </li>
          </ol>
        </div>
      </section>

      <section id="campaign" class="story-section" aria-labelledby="campaign-title">
        <div class="section-heading">
          <span class="section-number">03</span>
          <h2 id="campaign-title">用免单活动完成第一轮引流</h2>
        </div>
        <p class="section-lead">活动在小程序内完成预约、定时开奖和结果展示。以下都是当时保留的真实页面截图。</p>
        <div class="phone-gallery">
          <figure v-for="item in portfolioContent.campaign" :key="item.src" class="phone-shot">
            <button type="button" @click="openImage(item)">
              <img loading="lazy" :src="item.src" :alt="item.alt" />
              <span class="image-zoom"><el-icon><ZoomIn /></el-icon></span>
            </button>
            <figcaption>{{ item.caption }}</figcaption>
          </figure>
        </div>
      </section>

      <section id="launch" class="story-section" aria-labelledby="launch-title">
        <div class="section-heading">
          <span class="section-number">04</span>
          <h2 id="launch-title">江西小炒接入后的初次成果</h2>
        </div>
        <p class="section-lead">商户端看板直接记录了接入初期的订单完成情况，不使用估算数据。</p>
        <div class="launch-gallery">
          <figure v-for="item in portfolioContent.launch" :key="item.src" class="phone-shot phone-shot--featured">
            <button type="button" @click="openImage(item)">
              <img loading="lazy" :src="item.src" :alt="item.alt" />
              <span class="image-zoom"><el-icon><ZoomIn /></el-icon></span>
            </button>
            <figcaption>{{ item.caption }}</figcaption>
          </figure>
        </div>
      </section>

      <section id="shops" class="story-section" aria-labelledby="shops-title">
        <div class="section-heading">
          <span class="section-number">05</span>
          <h2 id="shops-title">真实校园门店，后续陆续接入</h2>
        </div>
        <p class="section-lead">这些照片记录了校园食堂内常见的经营场景。当前作品集不把它们都描述成已合作商户，只作为后续持续接入的真实门店范围。</p>
        <div class="shop-gallery">
          <figure v-for="item in portfolioContent.shops" :key="item.src" class="shop-photo">
            <button type="button" @click="openImage(item)">
              <img loading="lazy" :src="item.src" :alt="item.alt" />
              <span class="image-zoom"><el-icon><ZoomIn /></el-icon></span>
            </button>
            <figcaption>{{ item.caption }}</figcaption>
          </figure>
        </div>
      </section>

      <section id="conference" class="story-section" aria-labelledby="conference-title">
        <div class="section-heading">
          <span class="section-number">06</span>
          <h2 id="conference-title">受老师邀约，参加校园企业入驻宣贯会</h2>
        </div>
        <p class="section-lead">2026 年 4 月 24 日，参加南昌航空大学科技园入驻企业宣贯会，现场了解大学科技园建设、学生创业实践与企业孵化路径。</p>
        <div class="conference-gallery">
          <figure v-for="item in portfolioContent.conference" :key="item.src" class="conference-photo">
            <button type="button" @click="openImage(item)">
              <img loading="lazy" :src="item.src" :alt="item.alt" />
              <span class="image-zoom"><el-icon><ZoomIn /></el-icon></span>
            </button>
            <figcaption>{{ item.caption }}</figcaption>
          </figure>
        </div>
      </section>

      <section id="evidence" class="story-section" aria-labelledby="evidence-title">
        <div class="section-heading">
          <span class="section-number">07</span>
          <h2 id="evidence-title">暑期人数减少，订单仍在持续产生</h2>
        </div>
        <figure class="database-evidence">
          <button type="button" @click="openImage(portfolioContent.evidence)">
            <img loading="lazy" :src="portfolioContent.evidence.src" :alt="portfolioContent.evidence.alt" />
            <span class="image-zoom"><el-icon><ZoomIn /></el-icon></span>
          </button>
          <figcaption>{{ portfolioContent.evidence.caption }}</figcaption>
        </figure>
      </section>

      <section id="redis" class="story-section story-section--last" aria-labelledby="redis-title">
        <div class="section-heading">
          <span class="section-number">08</span>
          <h2 id="redis-title">Redis 工程化能力验证</h2>
        </div>
        <div class="redis-intro">
          <span class="pending-badge">{{ portfolioContent.redis.status }}</span>
          <p>{{ portfolioContent.redis.description }}</p>
        </div>
        <div class="redis-test-list">
          <article v-for="test in portfolioContent.redis.scenarios" :key="test.id" class="redis-test-card">
            <div class="redis-test-card__header">
              <span class="redis-test-card__number">{{ test.number }}</span>
              <div>
                <h3>{{ test.title }}</h3>
                <p>{{ test.summary }}</p>
              </div>
              <span class="redis-test-card__value"><el-icon><Check /></el-icon>{{ test.value }}</span>
            </div>
            <div class="redis-test-card__gallery">
              <figure v-for="(item, imageIndex) in test.images" :key="item.src">
                <figcaption>
                  <span class="redis-image-index">{{ test.number }}-{{ String(imageIndex + 1).padStart(2, '0') }}</span>
                  {{ item.caption }}
                </figcaption>
                <button type="button" @click="openImage(item)">
                  <img loading="lazy" :src="item.src" :alt="item.alt" />
                  <span class="image-zoom"><el-icon><ZoomIn /></el-icon></span>
                </button>
              </figure>
            </div>
          </article>
        </div>

        <footer class="portfolio-footer">
          <div><img :src="portfolioContent.project.logo" alt="" /><span><strong>食刻快取</strong><small>{{ portfolioContent.project.period }}</small></span></div>
          <RouterLink to="/login">返回商户端登录 <el-icon><ArrowLeft /></el-icon></RouterLink>
        </footer>
      </section>
    </main>

    <Transition name="lightbox">
      <div
        v-if="openedImage"
        ref="lightboxElement"
        class="image-lightbox"
        role="dialog"
        aria-modal="true"
        :aria-label="openedImage.alt"
        tabindex="-1"
        @click.self="closeImage"
        @keydown.esc="closeImage"
      >
        <button class="image-lightbox__close" type="button" aria-label="关闭图片预览" @click="closeImage">
          <el-icon><Close /></el-icon>
        </button>
        <div class="image-lightbox__content">
          <img :src="openedImage.src" :alt="openedImage.alt" />
          <p>{{ openedImage.caption }}</p>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ArrowLeft, Check, Close, ZoomIn } from '@element-plus/icons-vue'
import { portfolioContent, type PortfolioImage } from '@/data/portfolio'

const activeSection = ref(portfolioContent.sections[0]?.id ?? 'origin')
const openedImage = ref<PortfolioImage | null>(null)
const lightboxElement = ref<HTMLElement | null>(null)
let sectionObserver: IntersectionObserver | undefined

const scrollToSection = (id: string) => {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  activeSection.value = id
}

const openImage = async (item: PortfolioImage) => {
  openedImage.value = item
  await nextTick()
  lightboxElement.value?.focus()
}

const closeImage = () => {
  openedImage.value = null
}

watch(openedImage, (item) => {
  document.body.style.overflow = item ? 'hidden' : ''
})

onMounted(() => {
  sectionObserver = new IntersectionObserver((entries) => {
    const visible = entries
      .filter((entry) => entry.isIntersecting)
      .sort((a, b) => b.intersectionRatio - a.intersectionRatio)[0]
    if (visible) activeSection.value = visible.target.id
  }, { rootMargin: '-24% 0px -62% 0px', threshold: [0.05, 0.2, 0.5] })

  portfolioContent.sections.forEach((section) => {
    const element = document.getElementById(section.id)
    if (element) sectionObserver?.observe(element)
  })
})

onBeforeUnmount(() => {
  sectionObserver?.disconnect()
  document.body.style.overflow = ''
})
</script>

<style scoped>
:global(html) { scroll-behavior: smooth; }
:global(body) { background: #f5f7fa; color: #252b36; }

.portfolio-page { min-height: 100vh; background: #f5f7fa; }
.portfolio-header { position: sticky; top: 0; z-index: 40; border-bottom: 1px solid #e7eaf0; background: rgba(255, 255, 255, .96); backdrop-filter: blur(14px); }
.portfolio-header__inner { width: min(1500px, calc(100% - 48px)); min-height: 64px; margin: 0 auto; display: grid; grid-template-columns: minmax(0, 1fr) auto; align-items: center; gap: 28px; }
.portfolio-brand { display: flex; align-items: center; gap: 9px; color: #252b36; text-decoration: none; }
.portfolio-brand img { width: 36px; height: 36px; border-radius: 8px; }
.portfolio-brand strong, .portfolio-brand small { display: block; }
.portfolio-brand strong { font-size: 14px; }
.portfolio-brand small { margin-top: 2px; color: #8b95a5; font-size: 10px; }
.portfolio-sidebar { position: fixed; top: 64px; bottom: 0; left: 0; z-index: 30; width: 208px; padding: 30px 16px; box-sizing: border-box; border-right: 1px solid #e2e7ed; background: rgba(255, 255, 255, .97); }
.portfolio-nav { min-width: 0; display: grid; gap: 5px; }
.portfolio-nav::-webkit-scrollbar { display: none; }
.portfolio-nav button { position: relative; width: 100%; min-height: 52px; padding: 0 12px; display: grid; grid-template-columns: 32px minmax(0, 1fr); align-items: center; gap: 8px; border: 0; border-radius: 4px; background: transparent; color: #697386; font: inherit; text-align: left; cursor: pointer; }
.portfolio-nav button::before { position: absolute; top: 10px; bottom: 10px; left: 0; width: 3px; border-radius: 2px; background: transparent; content: ''; }
.portfolio-nav button span { color: #a4adba; font-size: 13px; font-weight: 800; }
.portfolio-nav button strong { font-size: 13px; font-weight: 700; }
.portfolio-nav button:hover, .portfolio-nav button.is-active { color: #1989fa; background: #f1f7fd; }
.portfolio-nav button.is-active::before { background: #1989fa; }
.portfolio-nav button.is-active span { color: #ff7a1a; }
.back-link { display: inline-flex; align-items: center; gap: 5px; color: #5f6b7c; font-size: 12px; font-weight: 600; text-decoration: none; }
.back-link:hover { color: #1989fa; }

.portfolio-main { width: min(1280px, calc(100% - 256px)); margin: 0 24px 0 calc(208px + max(24px, (100vw - 208px - 1280px) / 2)); }
.portfolio-hero { min-height: calc(100vh - 64px); padding: 76px 0 70px; display: grid; grid-template-columns: minmax(320px, .62fr) minmax(620px, 1.38fr); gap: 56px; align-items: center; border-bottom: 1px solid #dfe4eb; }
.hero-brand { min-width: 0; }
.hero-brand__logo { width: 170px; height: 166px; display: block; border-radius: 32px; box-shadow: 0 18px 42px rgba(25, 137, 250, .18); }
.profit-badge { display: none; }
.hero-brand h1 { margin: 16px 0 0; color: #20252e; font-size: 60px; line-height: 1.08; letter-spacing: 0; }
.hero-brand__tagline { max-width: 520px; margin: 20px 0 0; color: #465366; font-size: 22px; font-weight: 700; line-height: 1.55; }
.hero-brand__role { margin: 18px 0 0; color: #8791a1; font-size: 14px; line-height: 1.7; }
.hero-status { margin-top: 28px; display: flex; flex-wrap: wrap; gap: 9px; }
.hero-status span { min-height: 30px; padding: 0 10px; display: inline-flex; align-items: center; gap: 7px; border: 1px solid #dce8f5; border-radius: 6px; background: #fff; color: #526175; font-size: 11px; }
.hero-status i { width: 6px; height: 6px; border-radius: 50%; background: #07c160; box-shadow: 0 0 0 3px rgba(7, 193, 96, .12); }
.hero-status__profit { border-color: #dce8e0 !important; background: #f7fbf8 !important; color: #50745d !important; }
.hero-status__profit i { background: #75a788; box-shadow: 0 0 0 3px rgba(117, 167, 136, .12); }

figure { margin: 0; }
figure button { position: relative; width: 100%; padding: 0; display: block; overflow: hidden; border: 0; background: #fff; cursor: zoom-in; }
figure button:focus-visible, .portfolio-nav button:focus-visible, .back-link:focus-visible, .image-lightbox__close:focus-visible { outline: 3px solid rgba(25, 137, 250, .35); outline-offset: 2px; }
.image-zoom { position: absolute; right: 10px; bottom: 10px; width: 34px; height: 34px; display: inline-flex; align-items: center; justify-content: center; border-radius: 50%; background: rgba(24, 31, 42, .72); color: #fff; opacity: 0; transition: opacity .2s ease; }
figure button:hover .image-zoom, figure button:focus-visible .image-zoom { opacity: 1; }
.license-figure { min-width: 0; border: 1px solid #e2e6ec; border-radius: 8px; overflow: hidden; background: #fff; box-shadow: 0 18px 44px rgba(50, 63, 82, .1); }
.license-figure button { background: #eef1f4; }
.license-figure img { width: 100%; min-height: 480px; height: auto; display: block; object-fit: contain; }
.license-figure figcaption { padding: 14px 16px 16px; }
.license-figure figcaption strong, .license-figure figcaption span { display: block; }
.license-figure figcaption strong { color: #303946; font-size: 13px; }
.license-figure figcaption span { margin-top: 5px; color: #8490a1; font-size: 11px; line-height: 1.65; }

.story-section { padding: 88px 0; border-bottom: 1px solid #dfe4eb; scroll-margin-top: 78px; }
.story-section--last { padding-bottom: 34px; border-bottom: 0; }
.section-heading { display: grid; grid-template-columns: 62px minmax(0, 1fr); align-items: start; gap: 20px; }
.section-number { color: #ff7a1a; font-size: 34px; font-weight: 800; line-height: 1.3; }
.section-heading h2 { margin: 0; color: #252b36; font-size: 34px; line-height: 1.3; letter-spacing: 0; }
.section-lead { max-width: 940px; margin: 24px 0 0; color: #6f7b8d; font-size: 15px; line-height: 1.9; }

.photo-card, .phone-shot, .shop-photo, .conference-photo, .wide-evidence, .database-evidence { overflow: hidden; border: 1px solid #e0e5ec; border-radius: 8px; background: #fff; }
.photo-card figcaption, .phone-shot figcaption, .shop-photo figcaption, .conference-photo figcaption, .wide-evidence figcaption, .database-evidence figcaption { padding: 12px 14px; color: #667386; font-size: 11px; line-height: 1.65; }
.origin-gallery { margin-top: 32px; display: grid; grid-template-columns: minmax(300px, .76fr) minmax(0, 1.24fr); grid-template-rows: repeat(2, minmax(0, 1fr)); gap: 18px; }
.origin-gallery .photo-card:first-child { grid-row: 1 / 3; }
.origin-gallery .photo-card { display: flex; flex-direction: column; }
.origin-gallery .photo-card button { flex: 1; min-height: 0; }
.origin-gallery .photo-card img { width: 100%; height: 100%; display: block; object-fit: cover; }
.origin-gallery .photo-card:not(:first-child) img { aspect-ratio: 4 / 3; }

.foundation-layout { margin-top: 32px; display: grid; grid-template-columns: minmax(0, 1.25fr) minmax(280px, .75fr); gap: 26px; align-items: start; }
.wide-evidence img, .database-evidence img { width: 100%; height: auto; display: block; }
.milestone-list { margin: 0; padding: 8px 0 0; display: grid; list-style: none; }
.milestone-list li { position: relative; min-height: 90px; padding: 3px 0 24px 42px; display: grid; gap: 7px; }
.milestone-list li::before { position: absolute; left: 8px; top: 7px; width: 10px; height: 10px; border: 3px solid #1989fa; border-radius: 50%; background: #f5f7fa; content: ''; }
.milestone-list li::after { position: absolute; left: 14px; top: 22px; bottom: -2px; width: 1px; background: #cddff2; content: ''; }
.milestone-list li:last-child::after { display: none; }
.milestone-list time { color: #1989fa; font-size: 12px; font-weight: 800; }
.milestone-list strong { color: #3f4a5a; font-size: 15px; }

.phone-gallery { margin-top: 32px; display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 14px; align-items: start; }
.phone-shot button { background: #f0f5fa; }
.phone-shot img { width: 100%; height: auto; display: block; }
.phone-shot figcaption { min-height: 66px; }
.launch-gallery { width: min(920px, 100%); margin: 32px auto 0; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 24px; align-items: start; }
.phone-shot--featured figcaption { min-height: 0; font-size: 12px; }

.shop-gallery { margin-top: 32px; display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 15px; }
.shop-photo img { width: 100%; aspect-ratio: 3 / 4; display: block; object-fit: cover; }
.shop-photo figcaption { color: #445164; font-size: 12px; font-weight: 700; }

.conference-gallery { margin-top: 32px; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 18px; align-items: start; }
.conference-photo button { background: #eef1f5; }
.conference-photo img { width: 100%; aspect-ratio: 4 / 3; display: block; object-fit: contain; }
.database-evidence { margin-top: 32px; }
.database-evidence figcaption { padding: 15px 17px; color: #48576a; font-size: 13px; }

.test-placeholder { margin-top: 32px; padding: 28px; display: grid; grid-template-columns: 64px minmax(0, 1fr) minmax(320px, .9fr); gap: 22px; border: 1px dashed #9fc7ee; border-radius: 8px; background: #f8fbff; }
.test-placeholder__mark { width: 54px; height: 54px; display: flex; align-items: center; justify-content: center; border-radius: 8px; background: #1989fa; color: #fff; font-size: 28px; }
.pending-badge { padding: 4px 8px; display: inline-flex; border-radius: 4px; background: #fff0e6; color: #d96a1a; font-size: 10px; font-weight: 800; }
.test-placeholder__copy h3 { margin: 12px 0 0; color: #2c3542; font-size: 19px; }
.test-placeholder__copy p { margin: 10px 0 0; color: #6c798b; font-size: 13px; line-height: 1.75; }
.test-placeholder__lists { grid-column: 2 / 3; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 22px; }
.test-placeholder__lists > div { display: grid; gap: 8px; }
.test-placeholder__lists strong { margin-bottom: 3px; color: #384657; font-size: 12px; }
.test-placeholder__lists span { display: flex; align-items: center; gap: 6px; color: #718094; font-size: 11px; }
.test-placeholder__lists .el-icon { color: #07c160; }
.test-placeholder__screen { grid-column: 3; grid-row: 1 / 3; min-height: 260px; padding: 24px; display: flex; flex-direction: column; align-items: center; justify-content: center; border: 1px solid #dbe8f5; border-radius: 6px; background: #fff; color: #8b98a9; text-align: center; }
.test-placeholder__screen .el-icon { color: #1989fa; font-size: 42px; }
.test-placeholder__screen strong { margin-top: 14px; color: #49576a; font-size: 13px; }
.test-placeholder__screen span { margin-top: 6px; font-size: 11px; }

.redis-intro { margin-top: 22px; display: flex; align-items: center; gap: 12px; }
.redis-intro p { margin: 0; color: #6c798b; font-size: 13px; line-height: 1.7; }
.redis-test-list { margin-top: 38px; display: grid; gap: 68px; }
.redis-test-card { min-width: 0; }
.redis-test-card__header { display: grid; grid-template-columns: 56px minmax(0, 1fr) auto; gap: 18px; align-items: start; }
.redis-test-card__number { color: #ff7a1a; font-size: 30px; font-weight: 800; line-height: 1.1; }
.redis-test-card h3 { margin: 0; color: #252b36; font-size: 25px; line-height: 1.25; }
.redis-test-card__header p { margin: 5px 0 0; color: #778497; font-size: 12px; }
.redis-test-card__value { display: inline-flex; align-items: center; gap: 5px; color: #0b9b59; font-size: 11px; font-weight: 700; white-space: nowrap; }
.redis-test-card__value .el-icon { font-size: 14px; }
.redis-test-card__gallery { margin-top: 20px; padding: 22px; display: grid; grid-template-columns: 1fr; gap: 18px; border: 1px solid #dfe8f2; border-radius: 8px; background: #f8fbff; }
.redis-test-card__gallery figure { overflow: hidden; border: 1px solid #e7ebf0; border-radius: 6px; background: #fff; }
.redis-test-card__gallery button { background: #f0f3f7; }
.redis-test-card__gallery img { width: 100%; height: auto; display: block; object-fit: contain; }
.redis-test-card__gallery figcaption { min-height: 48px; padding: 13px 16px; box-sizing: border-box; display: flex; align-items: center; gap: 10px; border-bottom: 1px solid #e7ebf0; color: #3f4b5c; font-size: 15px; font-weight: 700; line-height: 1.6; }
.redis-image-index { min-width: 48px; color: #ff7a1a; font-size: 13px; font-weight: 800; }

.portfolio-footer { margin-top: 70px; padding-top: 24px; display: flex; align-items: center; justify-content: space-between; border-top: 1px solid #dfe4eb; }
.portfolio-footer > div { display: flex; align-items: center; gap: 10px; }
.portfolio-footer img { width: 40px; height: 40px; border-radius: 8px; }
.portfolio-footer strong, .portfolio-footer small { display: block; }
.portfolio-footer strong { color: #303946; font-size: 13px; }
.portfolio-footer small { margin-top: 3px; color: #929cab; font-size: 10px; }
.portfolio-footer a { display: inline-flex; align-items: center; gap: 6px; color: #1989fa; font-size: 12px; font-weight: 700; text-decoration: none; }

.image-lightbox { position: fixed; inset: 0; z-index: 100; padding: 54px; display: flex; align-items: center; justify-content: center; background: rgba(16, 21, 29, .92); }
.image-lightbox__close { position: fixed; top: 18px; right: 20px; width: 40px; height: 40px; display: inline-flex; align-items: center; justify-content: center; border: 0; border-radius: 50%; background: rgba(255, 255, 255, .14); color: #fff; font-size: 21px; cursor: pointer; }
.image-lightbox__content { max-width: min(1500px, 100%); max-height: 100%; display: flex; flex-direction: column; align-items: center; }
.image-lightbox__content img { max-width: 100%; max-height: calc(100vh - 125px); display: block; object-fit: contain; }
.image-lightbox__content p { max-width: 900px; margin: 14px 0 0; color: #e8edf5; font-size: 12px; line-height: 1.7; text-align: center; }
.lightbox-enter-active, .lightbox-leave-active { transition: opacity .18s ease; }
.lightbox-enter-from, .lightbox-leave-to { opacity: 0; }

@media (max-width: 1080px) {
  .portfolio-header__inner { gap: 14px; }
  .portfolio-hero { grid-template-columns: 1fr; gap: 42px; }
  .phone-gallery { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .shop-gallery { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}

@media (max-width: 820px) {
  .portfolio-header__inner { width: 100%; min-height: 60px; padding: 0 14px; box-sizing: border-box; grid-template-columns: 1fr auto; gap: 12px; }
  .portfolio-brand img { width: 32px; height: 32px; }
  .back-link span { display: none; }
  .portfolio-sidebar { position: sticky; top: 60px; left: auto; z-index: 35; width: 100%; padding: 0 14px; box-sizing: border-box; border-bottom: 1px solid #e7eaf0; background: rgba(255, 255, 255, .96); }
  .portfolio-nav { margin: 0; padding: 0; display: flex; gap: 0; overflow-x: auto; border: 0; border-radius: 0; box-shadow: none; background: transparent; scrollbar-width: none; }
  .portfolio-nav button { width: auto; min-width: max-content; min-height: 44px; padding: 0 11px; display: inline-flex; gap: 6px; border-radius: 0; }
  .portfolio-nav button::before { top: auto; right: 0; bottom: 0; left: 0; width: auto; height: 2px; border-radius: 0; }
  .portfolio-nav button.is-active { background: transparent; }
  .portfolio-main { width: min(100% - 28px, 680px); margin: 0 auto; }
  .portfolio-hero { min-height: auto; padding: 42px 0 54px; grid-template-columns: 1fr; gap: 38px; }
  .hero-brand { text-align: center; }
  .hero-brand__logo { width: 124px; height: 121px; margin: 0 auto; border-radius: 24px; }
  .license-figure img { min-height: 0; }
  .profit-badge { margin-left: auto; margin-right: auto; }
  .hero-brand h1 { font-size: 38px; }
  .hero-brand__tagline { margin-left: auto; margin-right: auto; font-size: 18px; }
  .hero-status { justify-content: center; }
  .story-section { padding: 58px 0; scroll-margin-top: 118px; }
  .section-heading { grid-template-columns: 44px minmax(0, 1fr); gap: 12px; }
  .section-number, .section-heading h2 { font-size: 24px; }
  .origin-gallery, .foundation-layout { grid-template-columns: 1fr; grid-template-rows: auto; }
  .origin-gallery .photo-card:first-child { grid-row: auto; }
  .origin-gallery .photo-card:first-child img { aspect-ratio: 4 / 5; }
  .foundation-layout { gap: 22px; }
  .milestone-list { padding-left: 8px; }
  .phone-gallery { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
  .phone-shot figcaption { min-height: 74px; padding: 10px; }
  .launch-gallery { gap: 10px; }
  .shop-gallery { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
  .conference-gallery { grid-template-columns: 1fr; gap: 12px; }
  .test-placeholder { padding: 20px; grid-template-columns: 46px minmax(0, 1fr); gap: 16px; }
  .test-placeholder__mark { width: 44px; height: 44px; font-size: 22px; }
  .test-placeholder__lists { grid-column: 1 / -1; }
  .test-placeholder__screen { grid-column: 1 / -1; grid-row: auto; min-height: 210px; }
  .redis-intro { align-items: flex-start; flex-direction: column; gap: 8px; }
  .redis-test-list { gap: 48px; }
  .redis-test-card__header { grid-template-columns: 42px minmax(0, 1fr); gap: 12px; }
  .redis-test-card__number { font-size: 25px; }
  .redis-test-card h3 { font-size: 21px; }
  .redis-test-card__value { grid-column: 2; white-space: normal; }
  .redis-test-card__gallery { padding: 14px; gap: 12px; }
  .image-lightbox { padding: 58px 12px 24px; }
}

@media (max-width: 480px) {
  .hero-status { display: grid; grid-template-columns: 1fr; }
  .hero-status span { justify-content: center; }
  .section-heading { grid-template-columns: 40px minmax(0, 1fr); gap: 10px; }
  .section-number, .section-heading h2 { font-size: 22px; }
  .section-lead { font-size: 13px; }
  .phone-gallery { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .launch-gallery { grid-template-columns: 1fr; }
  .phone-shot--featured { width: min(100%, 390px); margin: 0 auto; }
  .test-placeholder__lists { grid-template-columns: 1fr; }
  .portfolio-footer { align-items: flex-start; gap: 18px; }
  .portfolio-footer a { max-width: 110px; justify-content: flex-end; text-align: right; }
}

@media (prefers-reduced-motion: reduce) {
  :global(html) { scroll-behavior: auto; }
  .image-zoom, .lightbox-enter-active, .lightbox-leave-active { transition: none; }
}
</style>
