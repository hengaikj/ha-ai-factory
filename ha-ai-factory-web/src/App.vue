<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ApiError, beginLogin, createProject, getCurrentSession, getProjects, type CurrentSession, type Project } from './api'

type ViewState = 'loading' | 'unauthenticated' | 'ready' | 'error'
const session = ref<CurrentSession | null>(null)
const projects = ref<Project[]>([])
const projectTotal = ref(0)
const currentPage = ref(1)
const pageSize = 20
const viewState = ref<ViewState>('loading')
const errorMessage = ref('')
const query = ref('')
const createOpen = ref(false)
const creating = ref(false)
const loginStarting = ref(false)
const loginErrorMessage = ref('')
const createError = ref('')
const projectName = ref('')
const projectDescription = ref('')
const loginError = new URLSearchParams(window.location.search).get('authError')

const gateLabels: Record<Project['gateStatus'], string> = {
  PENDING: '待评审', APPROVED: '已通过', RETURNED: '已退回', BLOCKED: '已阻塞',
  HUMAN_DECISION_REQUIRED: '待人工决策',
}

function isUnauthorized(error: unknown): boolean {
  return error instanceof ApiError && error.status === 401
}

async function loadProjects() {
  errorMessage.value = ''
  viewState.value = 'loading'
  try {
    // Session check gates the project request: never call the protected list anonymously.
    session.value = await getCurrentSession()
    const result = await getProjects(currentPage.value, pageSize, query.value)
    projects.value = result.items
    projectTotal.value = result.total
    viewState.value = 'ready'
  } catch (error) {
    if (isUnauthorized(error)) {
      session.value = null
      viewState.value = 'unauthenticated'
      return
    }
    errorMessage.value = error instanceof Error ? error.message : '加载失败，请稍后重试。'
    viewState.value = 'error'
  }
}

/** 使用后端搜索并从第一页重查，避免仅筛选当前页造成总数遗漏。 */
function searchProjects() {
  currentPage.value = 1
  void loadProjects()
}

/** 切换服务端分页并重新加载当前会话有权访问的项目。 */
function changePage(page: number) {
  if (page < 1 || page > Math.ceil(projectTotal.value / pageSize)) return
  currentPage.value = page
  void loadProjects()
}

/** 创建项目后回到第一页并从服务端刷新项目及Gate/Issue摘要。 */
async function submitProject() {
  if (!session.value || creating.value) return
  const name = projectName.value.trim()
  if (!name) {
    createError.value = '请填写项目名称。'
    return
  }
  creating.value = true
  createError.value = ''
  try {
    const created = await createProject({
      name,
      ...(projectDescription.value.trim() ? { description: projectDescription.value.trim() } : {}),
      csrfToken: session.value.csrfToken,
    })
    createOpen.value = false
    projectName.value = ''
    projectDescription.value = ''
    query.value = ''
    currentPage.value = 1
    await loadProjects()
  } catch (error) {
    if (isUnauthorized(error)) {
      session.value = null
      viewState.value = 'unauthenticated'
      createOpen.value = false
    } else {
      createError.value = error instanceof Error ? error.message : '创建失败，请稍后重试。'
    }
  } finally {
    creating.value = false
  }
}

/** 先检查OIDC入口响应，再将浏览器导航到服务端给出的同源授权地址。 */
async function startLogin() {
  if (loginStarting.value) return
  loginStarting.value = true
  loginErrorMessage.value = ''
  try {
    window.location.assign(await beginLogin())
  } catch (error) {
    loginErrorMessage.value = error instanceof Error ? error.message : '登录暂不可用，请联系管理员检查配置。'
    loginStarting.value = false
  }
}

function formatDate(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '—' : new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium' }).format(date)
}

onMounted(loadProjects)
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar" aria-label="主导航">
      <a class="brand" href="#projects" aria-label="HA AI Factory 首页">
        <span class="brand-mark">H</span>
        <span class="brand-copy"><strong>HA AI Factory</strong><small>SOFTWARE FACTORY</small></span>
      </a>
      <div class="workspace-label">工作台</div>
      <nav class="navigation">
        <button class="nav-item selected" type="button"><span class="nav-icon" aria-hidden="true">▦</span><span>项目</span></button>
        <button class="nav-item" type="button" disabled><span class="nav-icon" aria-hidden="true">⌘</span><span>任务</span></button>
        <button class="nav-item" type="button" disabled><span class="nav-icon" aria-hidden="true">▤</span><span>交付物</span></button>
        <button class="nav-item" type="button" disabled><span class="nav-icon" aria-hidden="true">✓</span><span>Gate</span></button>
        <button class="nav-item" type="button" disabled><span class="nav-icon" aria-hidden="true">◇</span><span>Open Issues</span></button>
        <button class="nav-item" type="button" disabled><span class="nav-icon" aria-hidden="true">✳</span><span>Agent Runtime</span></button>
      </nav>
      <div class="sidebar-bottom">
        <div class="build-note"><span class="pulse"></span><span>项目工作台<br /><small>服务端授权数据</small></span></div>
        <div v-if="session" class="user-placeholder" aria-label="当前登录用户">
          <span class="avatar">{{ session.displayName.slice(0, 1) }}</span>
          <span><strong>{{ session.displayName }}</strong><small>已通过企业身份认证</small></span>
        </div>
        <div v-else class="user-placeholder"><span class="avatar">?</span><span><strong>未登录</strong><small>请使用企业账号登录</small></span></div>
      </div>
    </aside>

    <main id="projects" class="main-content">
      <header class="topbar">
        <div class="breadcrumbs"><span>工作台</span><span class="crumb-slash">/</span><strong>项目</strong></div>
        <div class="topbar-actions">
          <span class="environment"><span class="environment-dot"></span>开发环境</span>
          <span v-if="session" class="top-user">{{ session.displayName }}</span>
        </div>
      </header>

      <section class="page-wrap">
        <div class="page-heading">
          <div><div class="eyebrow">HA AI SOFTWARE FACTORY <span class="eyebrow-line"></span> PROJECTS</div>
            <h1>项目<span class="heading-period">.</span></h1>
            <p class="heading-copy">查看当前账号有权访问的项目，跟进项目阶段与 Gate 状态。</p>
          </div>
          <button v-if="viewState === 'ready'" class="primary-button" type="button" @click="createOpen = true"><span>＋</span> 创建项目</button>
        </div>

        <div v-if="viewState === 'loading'" class="state-panel" role="status" aria-live="polite">
          <span class="loading-indicator" aria-hidden="true"></span><strong>正在加载项目</strong><p>正在验证会话并读取有权访问的项目。</p>
        </div>

        <div v-else-if="viewState === 'unauthenticated'" class="state-panel auth-panel" role="status">
          <div class="state-icon">↗</div><h2>{{ loginError ? '企业账号登录未完成' : '请先登录' }}</h2><p>{{ loginError ? '身份提供方未能完成认证。请重试；若问题持续，请联系管理员检查企业身份配置。' : '使用企业 OIDC 账号登录后，才能查看你有权访问的项目。' }}</p>
          <button class="primary-button login-button" type="button" :disabled="loginStarting" @click="startLogin">{{ loginStarting ? '正在连接企业身份服务…' : '使用企业账号登录' }}</button>
          <p v-if="loginErrorMessage" class="form-error" role="alert">{{ loginErrorMessage }}</p>
        </div>

        <div v-else-if="viewState === 'error'" class="state-panel error-panel" role="alert">
          <div class="state-icon">!</div><h2>项目加载失败</h2><p>{{ errorMessage }}</p>
          <button class="secondary-button" type="button" @click="loadProjects">重试</button>
        </div>

        <section v-else class="panel projects-panel">
          <div class="panel-heading project-toolbar">
            <div><h2>项目空间</h2><p>共 {{ projectTotal }} 个项目 · 仅显示当前账号有权访问的项目</p></div>
            <label class="search-box"><span aria-hidden="true">⌕</span><input v-model="query" type="search" maxlength="128" placeholder="搜索项目名称或描述" aria-label="搜索项目名称或描述" @keyup.enter="searchProjects"><button type="button" @click="searchProjects">搜索</button></label>
          </div>

          <div v-if="projects.length" class="project-table-wrap">
            <table class="project-table">
              <thead><tr><th>项目</th><th>负责人</th><th>当前阶段</th><th>Gate 状态</th><th>Open Issues</th><th>更新时间</th></tr></thead>
              <tbody>
                <tr v-for="project in projects" :key="project.id">
                  <td><strong>{{ project.name }}</strong><small>{{ project.description || '暂无项目描述' }}</small><span class="project-id">项目 #{{ project.id }}</span></td>
                  <td><span class="owner-chip">{{ project.ownerRef === session?.principalRef ? `我（${session.displayName}）` : `成员 #${project.ownerRef.slice(0, 8)}` }}</span></td>
                  <td><span class="phase-tag">{{ project.currentPhase }}</span></td>
                  <td><span class="gate-tag" :class="`gate-${project.gateStatus.toLowerCase()}`">{{ gateLabels[project.gateStatus] }}</span></td>
                  <td>{{ project.openIssueCount }}</td>
                  <td>{{ formatDate(project.updatedAt) }}</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div v-else-if="query.trim()" class="inline-empty">
            <h3>没有匹配的项目</h3><p>试试其他关键词，或清除搜索内容。</p>
            <button class="secondary-button" type="button" @click="query = ''; searchProjects()">清除搜索</button>
          </div>
          <div v-else class="inline-empty">
            <div class="empty-art" aria-hidden="true"><span class="art-tile tile-back"></span><span class="art-tile tile-front"><i>＋</i></span></div>
            <h3>暂无项目</h3><p>创建项目后，这里会显示你有权访问的项目和阶段状态。</p>
            <button class="secondary-button" type="button" @click="createOpen = true">创建第一个项目</button>
          </div>
          <div v-if="projectTotal > pageSize" class="pagination" aria-label="项目列表分页">
            <button class="secondary-button" type="button" :disabled="currentPage === 1" @click="changePage(currentPage - 1)">上一页</button>
            <span>第 {{ currentPage }} / {{ Math.ceil(projectTotal / pageSize) }} 页</span>
            <button class="secondary-button" type="button" :disabled="currentPage >= Math.ceil(projectTotal / pageSize)" @click="changePage(currentPage + 1)">下一页</button>
          </div>
        </section>

        <footer class="page-footer"><span>HA AI SOFTWARE FACTORY <i>·</i> PROJECT WORKSPACE</span><button v-if="viewState === 'ready'" type="button" @click="loadProjects">刷新项目</button></footer>
      </section>
    </main>

    <div v-if="createOpen" class="dialog-backdrop" @click.self="createOpen = false">
      <section class="create-dialog" role="dialog" aria-modal="true" aria-labelledby="create-title">
        <div class="dialog-heading"><div><h2 id="create-title">创建项目</h2><p>创建后，当前账号将成为项目 Owner。</p></div><button class="dialog-close" type="button" aria-label="关闭" @click="createOpen = false">×</button></div>
        <form @submit.prevent="submitProject">
          <label class="form-field">项目名称 <span>*</span><input v-model="projectName" maxlength="128" required autofocus placeholder="输入项目名称"></label>
          <label class="form-field">项目描述<textarea v-model="projectDescription" maxlength="4000" rows="4" placeholder="说明项目目标或背景（选填）"></textarea></label>
          <p v-if="createError" class="form-error" role="alert">{{ createError }}</p>
          <div class="dialog-actions"><button class="secondary-button" type="button" :disabled="creating" @click="createOpen = false">取消</button><button class="primary-button" type="submit" :disabled="creating">{{ creating ? '正在创建…' : '创建项目' }}</button></div>
        </form>
      </section>
    </div>
  </div>
</template>
