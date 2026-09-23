<script setup lang="ts">
import { ref } from 'vue'

const activeSection = ref('总览')
const currentDate = new Intl.DateTimeFormat('en-US', {
  weekday: 'long',
  month: 'long',
  day: '2-digit',
  year: 'numeric',
}).format(new Date()).toUpperCase()
const navigation = [
  { label: '总览', icon: '◫' },
  { label: '项目空间', icon: '▦' },
  { label: '工程流程', icon: '⌘' },
  { label: '交付与证据', icon: '▤' },
  { label: '评审记录', icon: '✓' },
]
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar" aria-label="主导航">
      <a class="brand" href="#overview" aria-label="HA AI Factory 首页">
        <span class="brand-mark">H</span>
        <span class="brand-copy"><strong>HA AI Factory</strong><small>SOFTWARE FACTORY</small></span>
      </a>

      <div class="workspace-label">工作台</div>
      <nav class="navigation">
        <button
          v-for="item in navigation"
          :key="item.label"
          class="nav-item"
          :class="{ selected: activeSection === item.label }"
          type="button"
          @click="activeSection = item.label"
        >
          <span class="nav-icon" aria-hidden="true">{{ item.icon }}</span>
          <span>{{ item.label }}</span>
          <span v-if="item.label === '总览'" class="nav-dot" aria-hidden="true"></span>
        </button>
      </nav>

      <div class="sidebar-bottom">
        <div class="build-note"><span class="pulse"></span>内部骨架<br /><small>功能建设中</small></div>
        <div class="user-placeholder" aria-label="身份认证尚未接入">
          <span class="avatar">?</span>
          <span><strong>身份未接入</strong><small>认证能力待后续契约</small></span>
          <span class="more">···</span>
        </div>
      </div>
    </aside>

    <main id="overview" class="main-content">
      <header class="topbar">
        <div class="breadcrumbs"><span>工作台</span><span class="crumb-slash">/</span><strong>{{ activeSection }}</strong></div>
        <div class="topbar-actions">
          <span class="environment"><span class="environment-dot"></span>开发环境</span>
          <button class="icon-button" type="button" aria-label="帮助">?</button>
          <span class="top-avatar" aria-hidden="true">HA</span>
        </div>
      </header>

      <section class="page-wrap">
        <div class="page-heading">
          <div>
            <div class="eyebrow">{{ currentDate }} <span class="eyebrow-line"></span> FOUNDATION</div>
            <h1>工程总览<span class="heading-period">.</span></h1>
            <p class="heading-copy">在一个清晰的工作台中，跟进项目阶段、交付进展与评审状态。</p>
          </div>
          <button class="primary-button" type="button" disabled title="项目管理能力尚未实现">
            <span>＋</span> 新建项目
          </button>
        </div>

        <div class="skeleton-banner" role="status">
          <span class="banner-symbol">i</span>
          <div><strong>当前为内部骨架</strong><span>登录、持久化权限与项目数据尚未接入；本页面不执行认证或访问控制。</span></div>
          <span class="banner-tag">M01 FOUNDATION</span>
        </div>

        <section class="summary-grid" aria-label="项目指标占位">
          <article class="summary-card">
            <div class="card-top"><span>活跃项目</span><span class="metric-icon indigo">▦</span></div>
            <div class="metric-value">—</div>
            <div class="metric-foot"><span class="neutral-pill">等待项目模块</span><span>暂未连接数据</span></div>
          </article>
          <article class="summary-card">
            <div class="card-top"><span>进行中任务</span><span class="metric-icon cyan">⌘</span></div>
            <div class="metric-value">—</div>
            <div class="metric-foot"><span class="neutral-pill">等待流程模块</span><span>暂未连接数据</span></div>
          </article>
          <article class="summary-card">
            <div class="card-top"><span>待处理评审</span><span class="metric-icon amber">◷</span></div>
            <div class="metric-value">—</div>
            <div class="metric-foot"><span class="neutral-pill">等待评审模块</span><span>暂未连接数据</span></div>
          </article>
          <article class="summary-card">
            <div class="card-top"><span>Agent Runtime</span><span class="metric-icon violet">✳</span></div>
            <div class="metric-value state-value"><span class="state-ring"></span>未启用</div>
            <div class="metric-foot"><span class="neutral-pill">策略审批后开放</span><span>当前安全关闭</span></div>
          </article>
        </section>

        <div class="content-grid">
          <section class="panel projects-panel">
            <div class="panel-heading">
              <div><h2>项目空间</h2><p>查看你参与的项目与最近进展</p></div>
              <button class="text-button" type="button" disabled>查看全部 <span>→</span></button>
            </div>
            <div class="empty-state">
              <div class="empty-art" aria-hidden="true">
                <span class="art-orbit orbit-one"></span><span class="art-orbit orbit-two"></span>
                <span class="art-tile tile-back"></span><span class="art-tile tile-front"><i>＋</i></span>
                <span class="art-spark spark-one">✦</span><span class="art-spark spark-two">✧</span>
              </div>
              <h3>项目空间即将就绪</h3>
              <p>项目管理能力将在后续模块接入。<br />当前暂无可展示的项目数据。</p>
              <button class="secondary-button" type="button" disabled>项目管理待接入</button>
            </div>
          </section>

          <section class="panel activity-panel">
            <div class="panel-heading">
              <div><h2>最近动态</h2><p>追踪项目中的关键变化</p></div>
              <button class="dots-button" type="button" aria-label="更多动态选项">···</button>
            </div>
            <div class="activity-empty">
              <div class="activity-icon">◷</div>
              <strong>还没有活动记录</strong>
              <span>新动态将在这里显示</span>
            </div>
            <div class="activity-footer"><span class="footer-check">✓</span>活动记录将保留完整审计线索</div>
          </section>
        </div>

        <footer class="page-footer"><span>HA AI SOFTWARE FACTORY <i>·</i> FOUNDATION SKELETON</span><span>构建中 <b></b></span></footer>
      </section>
    </main>
  </div>
</template>
