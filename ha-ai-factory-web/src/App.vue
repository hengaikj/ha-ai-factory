<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ApiError, addProjectMember, advanceProjectStage, beginLogin, createProject, createProjectDeliverable, createProjectIssue, createProjectTask, decideGate, decideGateCheck, decideProjectIssue, getCurrentSession, getProjects, getProjectMembers, getProjectTasks, getProjectActivity, getProjectDeliverables, getProjectGates, getProjectIssues, getProjectResources, getRuntimeConfigStatus, logout, removeProjectMember, replaceProjectMemberRoles, reviewProjectDeliverable, updateProject, updateProjectTask, type ActivityPage, type CurrentSession, type Deliverable, type Project, type ProjectGate, type ProjectMember, type ProjectResource, type ProjectTask, type OpenIssue, type RuntimeConfigStatus } from './api'

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
const memberProject = ref<Project | null>(null)
const members = ref<ProjectMember[]>([])
const memberLoading = ref(false)
const memberError = ref('')
const memberIssuer = ref(''); const memberSubject = ref(''); const memberRoles = ref('ENGINEER'); const memberSaving = ref(false)
const memberUpdating = ref(false)
const createError = ref('')
const taskProject = ref<Project | null>(null)
const tasks = ref<ProjectTask[]>([])
const taskLoading = ref(false)
const taskError = ref('')
const taskTitle = ref('')
const taskPhase = ref('DISCOVERY')
const taskDescription = ref('')
const taskCreating = ref(false)
const taskUpdating = ref(false)
const workspaceProject = ref<Project | null>(null)
const workspaceTab = ref<'overview' | 'deliverables' | 'issues' | 'gates' | 'activity' | 'resources' | 'runtime'>('overview')
const workspaceLoading = ref(false)
const workspaceError = ref('')
const workspaceDeliverables = ref<Deliverable[]>([])
const workspaceIssues = ref<OpenIssue[]>([])
const workspaceGates = ref<ProjectGate[]>([])
const workspaceActivity = ref<ActivityPage | null>(null)
const workspaceResources = ref<ProjectResource[]>([])
const workspaceRuntime = ref<RuntimeConfigStatus | null>(null)
const deliverableTitle = ref(''); const deliverablePhase = ref(''); const deliverableVersion = ref('v1.0'); const deliverableSourceRef = ref(''); const deliverableSaving = ref(false)
const deliverableReviewOutcome = ref<'APPROVED' | 'RETURNED' | 'CLARIFICATION_REQUIRED'>('APPROVED'); const deliverableReviewComment = ref(''); const deliverableReviewing = ref(false)
const issueTitle = ref(''); const issueDescription = ref(''); const issueImpact = ref(''); const issueDecisionRole = ref('OWNER'); const issueSaving = ref(false); const issueDecision = ref(''); const issueOutcome = ref<'OPEN' | 'HUMAN_DECISION_REQUIRED' | 'DECIDED' | 'TRACKING' | 'CLOSED'>('DECIDED'); const issueDeciding = ref(false)
const gateWorking = ref(false)
const stageWorking = ref(false)
const projectUpdating = ref(false)
const loggingOut = ref(false)
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

/** 使用服务器端注销端点销毁会话，再回到未登录状态。 */
async function signOut() {
  if (!session.value || loggingOut.value) return
  loggingOut.value = true
  try { await logout(session.value.csrfToken); session.value = null; projects.value = []; viewState.value = 'unauthenticated' }
  catch (error) { errorMessage.value = error instanceof Error ? error.message : '注销失败，请稍后重试。' }
  finally { loggingOut.value = false }
}

function formatDate(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '—' : new Intl.DateTimeFormat('zh-CN', { dateStyle: 'medium' }).format(date)
}

/** 打开项目成员只读面板，成员权限由后端按项目角色校验。 */
async function openMembers(project: Project) {
  memberProject.value = project
  members.value = []
  memberError.value = ''
  memberLoading.value = true
  try { members.value = await getProjectMembers(project.id) }
  catch (error) { memberError.value = error instanceof Error ? error.message : '成员加载失败，请稍后重试。' }
  finally { memberLoading.value = false }
}

async function submitMember() {
  if (!session.value || !memberProject.value || memberSaving.value) return
  if (!memberIssuer.value.trim() || !memberSubject.value.trim()) { memberError.value = '请填写身份提供方和主体标识。'; return }
  memberSaving.value = true; memberError.value = ''
  try { const member = await addProjectMember({ projectId: memberProject.value.id, issuer: memberIssuer.value.trim(), subject: memberSubject.value.trim(), roles: memberRoles.value.split(',').map(v => v.trim()).filter(Boolean), csrfToken: session.value.csrfToken }); members.value = [member, ...members.value]; memberIssuer.value = ''; memberSubject.value = '' }
  catch (error) { memberError.value = error instanceof Error ? error.message : '成员添加失败，请稍后重试。' }
  finally { memberSaving.value = false }
}

async function updateMemberRoles(member: ProjectMember) {
  if (!session.value || !memberProject.value || memberUpdating.value) return
  const roles = prompt('请输入角色，逗号分隔', member.roles.join(','))?.split(',').map(v => v.trim()).filter(Boolean)
  if (!roles?.length) return
  memberUpdating.value = true; memberError.value = ''
  try { const updated = await replaceProjectMemberRoles({ projectId: memberProject.value.id, principalRef: member.principalRef, roles, csrfToken: session.value.csrfToken }); members.value = members.value.map(v => v.principalRef === updated.principalRef ? updated : v) }
  catch (error) { memberError.value = error instanceof Error ? error.message : '角色更新失败，请稍后重试。' }
  finally { memberUpdating.value = false }
}

async function removeMember(member: ProjectMember) {
  if (!session.value || !memberProject.value || memberUpdating.value || !confirm(`确认移除 ${member.displayName}？`)) return
  memberUpdating.value = true; memberError.value = ''
  try { await removeProjectMember({ projectId: memberProject.value.id, principalRef: member.principalRef, csrfToken: session.value.csrfToken }); members.value = members.value.filter(v => v.principalRef !== member.principalRef) }
  catch (error) { memberError.value = error instanceof Error ? error.message : '成员移除失败，请稍后重试。' }
  finally { memberUpdating.value = false }
}

/** 打开项目任务面板，任务列表和项目访问权限由服务端统一控制。 */
async function openTasks(project: Project) {
  taskProject.value = project
  tasks.value = []
  taskError.value = ''
  taskLoading.value = true
  try { tasks.value = (await getProjectTasks(project.id)).items }
  catch (error) { taskError.value = error instanceof Error ? error.message : '任务加载失败，请稍后重试。' }
  finally { taskLoading.value = false }
}

/** 创建任务后刷新当前项目列表，默认状态由后端置为未开始。 */
async function submitTask() {
  if (!session.value || !taskProject.value || taskCreating.value) return
  if (!taskTitle.value.trim() || !taskPhase.value.trim()) { taskError.value = '请填写任务标题和阶段。'; return }
  taskCreating.value = true
  taskError.value = ''
  try {
    const task = await createProjectTask({ projectId: taskProject.value.id, title: taskTitle.value.trim(), phase: taskPhase.value.trim(), ...(taskDescription.value.trim() ? { description: taskDescription.value.trim() } : {}), csrfToken: session.value.csrfToken })
    tasks.value = [task, ...tasks.value]
    taskTitle.value = ''; taskDescription.value = ''
  } catch (error) { taskError.value = error instanceof Error ? error.message : '任务创建失败，请稍后重试。' }
  finally { taskCreating.value = false }
}

/** 仅通过后端状态机更新任务状态，失败时保留当前列表快照。 */
async function updateTaskStatus(task: ProjectTask, status: string) {
  if (!session.value || taskUpdating.value) return
  taskUpdating.value = true; taskError.value = ''
  try { const updated = await updateProjectTask({ taskId: task.id, status, csrfToken: session.value.csrfToken }); tasks.value = tasks.value.map(v => v.id === updated.id ? updated : v) }
  catch (error) { taskError.value = error instanceof Error ? error.message : '任务状态更新失败，请稍后重试。' }
  finally { taskUpdating.value = false }
}

/** 打开项目综合工作区，按选中的标签读取已批准的项目对象。 */
async function openWorkspace(project: Project, tab: typeof workspaceTab.value = 'overview') {
  workspaceProject.value = project; workspaceTab.value = tab; workspaceError.value = ''; workspaceLoading.value = true
  try {
    if (tab === 'deliverables') workspaceDeliverables.value = (await getProjectDeliverables(project.id)).items
    else if (tab === 'issues') workspaceIssues.value = await getProjectIssues(project.id)
    else if (tab === 'gates') workspaceGates.value = await getProjectGates(project.id)
    else if (tab === 'activity') workspaceActivity.value = await getProjectActivity(project.id)
    else if (tab === 'resources') workspaceResources.value = await getProjectResources(project.id)
    else if (tab === 'runtime') workspaceRuntime.value = await getRuntimeConfigStatus(project.id)
  } catch (error) { workspaceError.value = error instanceof Error ? error.message : '工作区数据加载失败，请稍后重试。' }
  finally { workspaceLoading.value = false }
}

async function submitDeliverable() {
  if (!session.value || !workspaceProject.value || deliverableSaving.value) return
  if (!deliverableTitle.value.trim() || !deliverablePhase.value.trim() || !deliverableSourceRef.value.trim()) { workspaceError.value = '请填写交付物标题、阶段和仓库引用。'; return }
  deliverableSaving.value = true; workspaceError.value = ''
  try { const item = await createProjectDeliverable({ projectId: workspaceProject.value.id, title: deliverableTitle.value.trim(), phase: deliverablePhase.value.trim(), version: deliverableVersion.value.trim() || 'v1.0', sourceRef: deliverableSourceRef.value.trim(), csrfToken: session.value.csrfToken }); workspaceDeliverables.value = [item, ...workspaceDeliverables.value]; deliverableTitle.value = ''; deliverableSourceRef.value = '' }
  catch (error) { workspaceError.value = error instanceof Error ? error.message : '交付物登记失败，请稍后重试。' }
  finally { deliverableSaving.value = false }
}

async function reviewDeliverable(item: Deliverable) {
  if (!session.value || deliverableReviewing.value || !deliverableReviewOutcome.value.trim() || !deliverableReviewComment.value.trim()) return
  deliverableReviewing.value = true; workspaceError.value = ''
  try { await reviewProjectDeliverable({ deliverableId: item.id, outcome: deliverableReviewOutcome.value, comment: deliverableReviewComment.value.trim(), csrfToken: session.value.csrfToken }); workspaceDeliverables.value = (await getProjectDeliverables(item.projectId)).items; deliverableReviewOutcome.value = 'APPROVED'; deliverableReviewComment.value = '' }
  catch (error) { workspaceError.value = error instanceof Error ? error.message : '交付物评审失败，请稍后重试。' }
  finally { deliverableReviewing.value = false }
}

async function advanceStage() {
  if (!session.value || !workspaceProject.value || stageWorking.value) return
  const target = prompt('请输入目标阶段（例如 PRODUCT、UX/UI、DEVELOPMENT）', 'PRODUCT')
  if (!target?.trim()) return
  stageWorking.value = true; workspaceError.value = ''
  try { const updated = await advanceProjectStage(workspaceProject.value.id, target.trim(), session.value.csrfToken); workspaceProject.value = updated; projects.value = projects.value.map(v => v.id === updated.id ? updated : v) }
  catch (error) { workspaceError.value = error instanceof Error ? error.message : '阶段推进失败，请稍后重试。' }
  finally { stageWorking.value = false }
}

async function editProjectMetadata() {
  if (!session.value || !workspaceProject.value || projectUpdating.value) return
  const name = prompt('项目名称', workspaceProject.value.name)
  if (!name?.trim()) return
  const description = prompt('项目描述', workspaceProject.value.description || '')
  projectUpdating.value = true; workspaceError.value = ''
  try { const updated = await updateProject({ projectId: workspaceProject.value.id, name: name.trim(), description: description ?? undefined, csrfToken: session.value.csrfToken }); workspaceProject.value = updated; projects.value = projects.value.map(v => v.id === updated.id ? updated : v) }
  catch (error) { workspaceError.value = error instanceof Error ? error.message : '项目更新失败，请稍后重试。' }
  finally { projectUpdating.value = false }
}

async function submitIssue() {
  if (!session.value || !workspaceProject.value || issueSaving.value) return
  if (!issueTitle.value.trim() || !issueDescription.value.trim() || !issueImpact.value.trim()) { workspaceError.value = '请填写Issue标题、描述和影响。'; return }
  issueSaving.value = true; workspaceError.value = ''
  try { const item = await createProjectIssue({ projectId: workspaceProject.value.id, title: issueTitle.value.trim(), description: issueDescription.value.trim(), impact: issueImpact.value.trim(), decisionRole: issueDecisionRole.value, csrfToken: session.value.csrfToken }); workspaceIssues.value = [item, ...workspaceIssues.value]; issueTitle.value = ''; issueDescription.value = ''; issueImpact.value = '' }
  catch (error) { workspaceError.value = error instanceof Error ? error.message : 'Issue创建失败，请稍后重试。' }
  finally { issueSaving.value = false }
}

async function decideIssue(item: OpenIssue) {
  if (!session.value || issueDeciding.value || !issueDecision.value.trim()) return
  issueDeciding.value = true; workspaceError.value = ''
  try { const updated = await decideProjectIssue({ issueId: item.id, decision: issueDecision.value.trim(), outcome: issueOutcome.value, csrfToken: session.value.csrfToken }); workspaceIssues.value = workspaceIssues.value.map(v => v.id === updated.id ? updated : v); issueDecision.value = ''; issueOutcome.value = 'DECIDED' }
  catch (error) { workspaceError.value = error instanceof Error ? error.message : 'Issue决策失败，请稍后重试。' }
  finally { issueDeciding.value = false }
}

async function decideCheck(gate: ProjectGate, checkId: number, status: 'PASSED' | 'FAILED') {
  if (!session.value || gateWorking.value) return
  gateWorking.value = true; workspaceError.value = ''
  try { await decideGateCheck({ gateId: gate.id, checkId, status, comment: status === 'PASSED' ? 'Reviewer通过检查' : 'Reviewer退回检查', csrfToken: session.value.csrfToken }); const refreshed = await getProjectGates(gate.projectId); workspaceGates.value = refreshed }
  catch (error) { workspaceError.value = error instanceof Error ? error.message : 'Gate检查决策失败，请稍后重试。' }
  finally { gateWorking.value = false }
}

async function decideFinalGate(gate: ProjectGate, decision: 'APPROVED' | 'RETURNED') {
  if (!session.value || gateWorking.value) return
  gateWorking.value = true; workspaceError.value = ''
  try { await decideGate({ gateId: gate.id, decision, comment: decision === 'APPROVED' ? 'Reviewer批准Gate' : 'Reviewer退回Gate', csrfToken: session.value.csrfToken }); workspaceGates.value = await getProjectGates(gate.projectId) }
  catch (error) { workspaceError.value = error instanceof Error ? error.message : 'Gate最终决策失败，请稍后重试。' }
  finally { gateWorking.value = false }
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
        <button class="nav-item" type="button" :disabled="viewState !== 'ready'" @click="projects[0] && openTasks(projects[0])"><span class="nav-icon" aria-hidden="true">⌘</span><span>任务</span></button>
        <button class="nav-item" type="button" :disabled="viewState !== 'ready' || !projects.length" @click="projects[0] && openWorkspace(projects[0], 'deliverables')"><span class="nav-icon" aria-hidden="true">▤</span><span>交付物</span></button>
        <button class="nav-item" type="button" :disabled="viewState !== 'ready' || !projects.length" @click="projects[0] && openWorkspace(projects[0], 'gates')"><span class="nav-icon" aria-hidden="true">✓</span><span>Gate</span></button>
        <button class="nav-item" type="button" :disabled="viewState !== 'ready' || !projects.length" @click="projects[0] && openWorkspace(projects[0], 'issues')"><span class="nav-icon" aria-hidden="true">◇</span><span>Open Issues</span></button>
        <button class="nav-item" type="button" :disabled="viewState !== 'ready' || !projects.length" @click="projects[0] && openWorkspace(projects[0], 'runtime')"><span class="nav-icon" aria-hidden="true">✳</span><span>Agent Runtime</span></button>
      </nav>
      <div class="sidebar-bottom">
        <div class="build-note"><span class="pulse"></span><span>项目工作台<br /><small>服务端授权数据</small></span></div>
        <div v-if="session" class="user-placeholder" aria-label="当前登录用户">
          <span class="avatar">{{ session.displayName.slice(0, 1) }}</span>
          <span><strong>{{ session.displayName }}</strong><small>已通过企业身份认证</small></span><button class="secondary-button" type="button" :disabled="loggingOut" @click="signOut">{{ loggingOut ? '注销中…' : '注销' }}</button>
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
                  <td><button class="project-link" type="button" @click="openMembers(project)">{{ project.name }}</button><small>{{ project.description || '暂无项目描述' }}</small><span class="project-id">项目 #{{ project.id }} · <button class="inline-link" type="button" @click="openTasks(project)">任务</button> · <button class="inline-link" type="button" @click="openWorkspace(project)">工作区</button></span></td>
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

    <div v-if="memberProject" class="dialog-backdrop" @click.self="memberProject = null">
      <section class="create-dialog" role="dialog" aria-modal="true" aria-labelledby="members-title">
        <div class="dialog-heading"><div><h2 id="members-title">{{ memberProject.name }} · 项目成员</h2><p>成员角色由服务端项目权限控制。</p></div><button class="dialog-close" type="button" aria-label="关闭" @click="memberProject = null">×</button></div>
        <div v-if="memberLoading" class="state-panel" role="status">正在加载成员…</div>
        <p v-else-if="memberError" class="form-error" role="alert">{{ memberError }}</p>
        <div v-else class="member-list"><form class="task-create-form" @submit.prevent="submitMember"><input v-model="memberIssuer" required placeholder="Issuer" aria-label="Issuer"><input v-model="memberSubject" required placeholder="Subject" aria-label="Subject"><input v-model="memberRoles" required placeholder="角色，逗号分隔" aria-label="角色"><button class="primary-button" type="submit" :disabled="memberSaving">{{ memberSaving ? '添加中…' : '添加成员' }}</button></form><div v-for="member in members" :key="member.principalRef" class="member-row"><span class="avatar">{{ member.displayName.slice(0, 1) }}</span><span><strong>{{ member.displayName }}</strong><small>{{ member.roles.join(' · ') }}</small></span><span class="task-create-form"><button class="secondary-button" type="button" :disabled="memberUpdating" @click="updateMemberRoles(member)">改角色</button><button class="secondary-button" type="button" :disabled="memberUpdating" @click="removeMember(member)">移除</button></span></div><p v-if="!members.length" class="inline-empty">暂无可见成员</p></div>
      </section>
    </div>

    <div v-if="taskProject" class="dialog-backdrop" @click.self="taskProject = null">
      <section class="create-dialog task-dialog" role="dialog" aria-modal="true" aria-labelledby="tasks-title">
        <div class="dialog-heading"><div><h2 id="tasks-title">{{ taskProject.name }} · 任务</h2><p>任务创建需要 Owner、Project Admin 或 Orchestrator 角色。</p></div><button class="dialog-close" type="button" aria-label="关闭" @click="taskProject = null">×</button></div>
        <div v-if="taskLoading" class="state-panel" role="status">正在加载任务…</div>
        <p v-else-if="taskError && !session" class="form-error" role="alert">{{ taskError }}</p>
        <div v-else class="task-content">
          <form class="task-create-form" @submit.prevent="submitTask">
            <input v-model="taskTitle" maxlength="200" required placeholder="任务标题" aria-label="任务标题">
            <input v-model="taskPhase" maxlength="64" required placeholder="阶段" aria-label="任务阶段">
            <input v-model="taskDescription" maxlength="2000" placeholder="任务说明（选填）" aria-label="任务说明">
            <button class="primary-button" type="submit" :disabled="taskCreating">{{ taskCreating ? '创建中…' : '新建任务' }}</button>
          </form>
          <p v-if="taskError" class="form-error" role="alert">{{ taskError }}</p>
          <div v-if="tasks.length" class="task-list"><div v-for="task in tasks" :key="task.id" class="task-row"><div><strong>{{ task.title }}</strong><small>{{ task.phase }} · {{ task.status === 'NOT_STARTED' ? '未开始' : task.status }}</small></div><span class="task-create-form"><select :value="task.status" :disabled="taskUpdating" aria-label="任务状态" @change="updateTaskStatus(task, ($event.target as HTMLSelectElement).value)"><option value="NOT_STARTED">未开始</option><option value="IN_PROGRESS">进行中</option><option value="READY_FOR_REVIEW">待评审</option><option value="COMPLETED">已完成</option><option value="RETURNED">已退回</option><option value="BLOCKED">已阻塞</option><option value="HUMAN_DECISION_REQUIRED">待人工决策</option></select><span class="phase-tag">#{{ task.id }}</span></span></div></div>
          <p v-else-if="!taskLoading" class="inline-empty">暂无任务，可在上方创建。</p>
        </div>
      </section>
    </div>

    <div v-if="workspaceProject" class="dialog-backdrop" @click.self="workspaceProject = null">
      <section class="create-dialog workspace-dialog" role="dialog" aria-modal="true" aria-labelledby="workspace-title">
        <div class="dialog-heading"><div><h2 id="workspace-title">{{ workspaceProject.name }} · 工作区</h2><p>项目对象按当前账号权限读取，文件内容仍通过仓库引用管理。</p></div><button class="dialog-close" type="button" aria-label="关闭" @click="workspaceProject = null">×</button></div>
        <div class="workspace-tabs" role="tablist" aria-label="项目工作区标签">
          <button v-for="tab in (['overview','deliverables','issues','gates','activity','resources','runtime'] as const)" :key="tab" type="button" :class="['workspace-tab', { active: workspaceTab === tab }]" @click="openWorkspace(workspaceProject!, tab)">{{ ({ overview: '概览', deliverables: '交付物', issues: 'Issues', gates: 'Gate', activity: 'Activity', resources: '资源', runtime: 'Runtime' } as Record<string,string>)[tab] }}</button>
        </div>
        <div v-if="workspaceLoading" class="state-panel" role="status">正在加载工作区…</div>
        <p v-else-if="workspaceError" class="form-error" role="alert">{{ workspaceError }}</p>
        <div v-else class="workspace-body">
          <div v-if="workspaceTab === 'overview'" class="workspace-overview"><div><small>当前阶段</small><strong>{{ workspaceProject.currentPhase }}</strong></div><div><small>Gate</small><strong>{{ gateLabels[workspaceProject.gateStatus] }}</strong></div><div><small>Open Issues</small><strong>{{ workspaceProject.openIssueCount }}</strong></div><button class="secondary-button" type="button" :disabled="projectUpdating" @click="editProjectMetadata">编辑项目</button><button class="secondary-button" type="button" :disabled="stageWorking" @click="advanceStage">推进阶段</button></div>
          <div v-else-if="workspaceTab === 'deliverables'" class="workspace-list"><form class="task-create-form" @submit.prevent="submitDeliverable"><input v-model="deliverableTitle" required placeholder="交付物标题"><input v-model="deliverablePhase" required placeholder="阶段"><input v-model="deliverableVersion" required placeholder="版本"><input v-model="deliverableSourceRef" required placeholder="仓库引用"><button class="primary-button" type="submit" :disabled="deliverableSaving">{{ deliverableSaving ? '登记中…' : '登记交付物' }}</button></form><div class="task-create-form"><select v-model="deliverableReviewOutcome" aria-label="交付物评审结论"><option value="APPROVED">通过</option><option value="RETURNED">退回</option><option value="CLARIFICATION_REQUIRED">需澄清</option></select><input v-model="deliverableReviewComment" placeholder="评审意见"><span>填写后点击对应交付物的“评审”</span></div><div v-for="item in workspaceDeliverables" :key="item.id" class="workspace-row"><span><strong>{{ item.title }}</strong><small>{{ item.phase }} · {{ item.version }} · {{ item.sourceRef }}</small><button class="secondary-button" type="button" :disabled="deliverableReviewing" @click="reviewDeliverable(item)">评审</button></span><span class="phase-tag">{{ item.reviewStatus }}</span></div><p v-if="!workspaceDeliverables.length" class="inline-empty">暂无交付物登记</p></div>
          <div v-else-if="workspaceTab === 'issues'" class="workspace-list"><form class="task-create-form" @submit.prevent="submitIssue"><input v-model="issueTitle" required placeholder="Issue标题"><input v-model="issueDescription" required placeholder="Issue描述"><input v-model="issueImpact" required placeholder="影响"><input v-model="issueDecisionRole" required placeholder="决策角色"><button class="primary-button" type="submit" :disabled="issueSaving">{{ issueSaving ? '创建中…' : '创建Issue' }}</button></form><div v-for="item in workspaceIssues" :key="item.id" class="workspace-row"><span><strong>{{ item.code || `OI-${item.id}` }} · {{ item.title }}</strong><small>{{ item.impact }} · 决策角色：{{ item.decisionRole }}</small><span v-if="item.status !== 'CLOSED'" class="task-create-form"><input v-model="issueDecision" placeholder="决策"><select v-model="issueOutcome" aria-label="Issue决策状态"><option value="DECIDED">已决策</option><option value="TRACKING">跟踪中</option><option value="CLOSED">已关闭</option><option value="HUMAN_DECISION_REQUIRED">待人工决策</option></select><button class="secondary-button" type="button" :disabled="issueDeciding" @click="decideIssue(item)">记录决策</button></span></span><span class="gate-tag gate-pending">{{ item.status }}</span></div><p v-if="!workspaceIssues.length" class="inline-empty">暂无 Open Issue</p></div>
          <div v-else-if="workspaceTab === 'gates'" class="workspace-list"><div v-for="item in workspaceGates" :key="item.id" class="workspace-row"><span><strong>{{ item.phase }} Gate</strong><small>{{ item.taskIds.length }} 个任务 · {{ item.deliverableIds.length }} 个交付物 · {{ item.checks.length }} 个检查项</small><span v-for="check in item.checks" :key="check.id" class="task-create-form"><small>{{ check.code }} · {{ check.title }} · {{ check.status }}</small><button class="secondary-button" type="button" :disabled="gateWorking" @click="decideCheck(item, check.id, 'PASSED')">通过检查</button><button class="secondary-button" type="button" :disabled="gateWorking" @click="decideCheck(item, check.id, 'FAILED')">退回检查</button></span><span class="task-create-form"><button class="primary-button" type="button" :disabled="gateWorking" @click="decideFinalGate(item, 'APPROVED')">批准Gate</button><button class="secondary-button" type="button" :disabled="gateWorking" @click="decideFinalGate(item, 'RETURNED')">退回Gate</button></span></span><span class="gate-tag" :class="`gate-${item.status.toLowerCase()}`">{{ item.status }}</span></div><p v-if="!workspaceGates.length" class="inline-empty">暂无 Gate</p></div>
          <div v-else-if="workspaceTab === 'activity'" class="workspace-list"><div v-for="item in workspaceActivity?.items" :key="item.id" class="workspace-row"><span><strong>{{ item.action }}</strong><small>{{ item.objectType }} #{{ item.objectId }} · {{ formatDate(item.occurredAt) }}</small></span></div><p v-if="!workspaceActivity?.items.length" class="inline-empty">暂无 Activity</p></div>
          <div v-else-if="workspaceTab === 'resources'" class="workspace-list"><div v-for="item in workspaceResources" :key="item.id" class="workspace-row"><span><strong>{{ item.title }}</strong><small>{{ item.kind }} · {{ item.phase }} · {{ item.sourceRef }}</small></span><span class="phase-tag">{{ item.version }}</span></div><p v-if="!workspaceResources.length" class="inline-empty">暂无资源索引</p></div>
          <div v-else class="workspace-overview runtime-overview"><div><small>授权状态</small><strong>{{ workspaceRuntime?.status || 'UNCONFIGURED' }}</strong></div><div><small>模型引用</small><strong>{{ workspaceRuntime?.modelRef || '未配置' }}</strong></div><p>真实模型、工具和外部副作用执行按 HD-002 保持失败关闭。</p></div>
        </div>
      </section>
    </div>
  </div>
</template>
