export interface CurrentSession {
  principalRef: string
  displayName: string
  csrfToken: string
}

export interface Project {
  id: number
  name: string
  description?: string | null
  techStack?: Record<string, string>
  ownerRef: string
  currentPhase: string
  gateStatus: 'PENDING' | 'APPROVED' | 'RETURNED' | 'BLOCKED' | 'HUMAN_DECISION_REQUIRED'
  openIssueCount: number
  createdAt: string
  updatedAt: string
}

export interface ProjectPage {
  items: Project[]
  page: number
  pageSize: number
  total: number
}

export interface ProjectMember {
  principalRef: string
  displayName: string
  roles: string[]
  joinedAt: string
}

export interface ProjectTask {
  id: number
  projectId: number
  title: string
  description?: string | null
  phase: string
  assigneeRef?: string | null
  assigneeRole?: string | null
  status: 'NOT_STARTED' | 'IN_PROGRESS' | 'BLOCKED' | 'DONE'
  createdAt: string
  updatedAt: string
}

export interface TaskPage {
  items: ProjectTask[]
  page: number
  pageSize: number
  total: number
}

export interface Deliverable {
  id: number
  projectId: number
  taskId?: number | null
  title: string
  phase: string
  version: string
  sourceRef: string
  reviewStatus: 'DRAFT' | 'PENDING' | 'APPROVED' | 'RETURNED' | 'CLARIFICATION_REQUIRED'
  createdAt: string
}

export interface DeliverablePage {
  items: Deliverable[]
  page: number
  pageSize: number
  total: number
}

export interface OpenIssue {
  id: number
  projectId: number
  code?: string | null
  title: string
  description: string
  impact: string
  decisionRole: string
  status: string
  decision?: string | null
  createdAt: string
  decidedAt?: string | null
}

export interface ProjectResource {
  id: number
  kind: 'TEMPLATE' | 'RULE'
  title: string
  phase: string
  version: string
  sourceRef: string
  sourceStatus: string
  createdAt: string
}

export interface GateCheck {
  id: number
  code: string
  title: string
  status: string
  reviewerRef?: string | null
  comment?: string | null
  evidenceRefs?: string[]
}

export interface ProjectGate {
  id: number
  projectId: number
  phase: string
  status: string
  submittedByRef?: string | null
  decisionOwnerRef?: string | null
  taskIds: number[]
  deliverableIds: number[]
  submittedAt?: string | null
  checks: GateCheck[]
}

export interface RuntimeConfigStatus {
  id?: number | null
  projectId: number
  status: 'UNCONFIGURED' | 'PENDING_APPROVAL' | 'APPROVED' | 'EXPIRED'
  modelRef?: string | null
  approvedAt?: string | null
  expiresAt?: string | null
}

export interface ActivityPage {
  items: Array<{ id: number; objectType: string; objectId: number; action: string; beforeState?: string | null; afterState?: string | null; actorRef: string; comment?: string | null; evidenceRefs?: string[]; occurredAt: string }>
  page: number
  pageSize: number
  total: number
}

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly code?: string,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

/** 通过同源会话Cookie调用后端API，并将不安全错误详情限制在固定提示内。 */
async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  let response: Response
  try {
    response = await fetch(path, {
      ...init,
      credentials: 'include',
      headers: { Accept: 'application/json', ...init.headers },
    })
  } catch {
    throw new ApiError('无法连接服务，请检查后端是否已启动。', 0)
  }

  if (!response.ok) {
    const contentType = response.headers.get('content-type') ?? ''
    if (contentType.includes('application/problem+json')) {
      try {
        const problem = await response.json() as { title?: string; code?: string }
        throw new ApiError(problem.title || '请求未能完成。', response.status, problem.code)
      } catch (error) {
        if (error instanceof ApiError) throw error
      }
    }
    throw new ApiError(response.status === 403 ? '当前账号无权访问此数据。' : '请求未能完成，请稍后重试。', response.status)
  }

  if (response.status === 204) return undefined as T
  try {
    return await response.json() as T
  } catch {
    throw new ApiError('服务返回的数据格式无效。', response.status)
  }
}

/** 读取已认证主体和本次会话的CSRF Token。 */
export function getCurrentSession(): Promise<CurrentSession> {
  return request('/auth/session')
}

/** 使用服务器重定向启动OIDC；配置缺失时返回安全的可操作提示。 */
export async function beginLogin(): Promise<string> {
  let response: Response
  try {
    response = await fetch('/auth/login', { credentials: 'include', redirect: 'manual', headers: { Accept: 'application/json' } })
  } catch {
    throw new ApiError('无法连接登录服务，请检查后端是否已启动。', 0)
  }
  // 浏览器对302/303的手动Fetch响应可能隐藏为opaque redirect；交给顶层导航正常完成OIDC跳转。
  if (response.type === 'opaqueredirect' || response.status === 0) {
    return new URL('/auth/login', window.location.origin).href
  }
  if (response.status === 503) {
    throw new ApiError('企业账号登录暂不可用，请联系管理员检查 OIDC 登录配置后重试。', 503)
  }
  if (response.status < 300 || response.status >= 400) {
    throw new ApiError('无法开始企业账号登录，请联系管理员检查服务状态。', response.status)
  }
  const location = response.headers.get('Location')
  if (!location) throw new ApiError('登录服务未返回身份提供方地址，请联系管理员检查 OIDC 配置。', response.status)
  const target = new URL(location, window.location.origin)
  if (target.origin !== window.location.origin) {
    throw new ApiError('登录服务返回了无效的身份提供方地址，请联系管理员检查配置。', response.status)
  }
  return target.href
}

/** 按批准的页码、页大小和搜索条件读取当前主体可访问项目。 */
export function getProjects(page = 1, pageSize = 20, query?: string): Promise<ProjectPage> {
  const params = new URLSearchParams({ page: String(page), pageSize: String(pageSize) })
  if (query?.trim()) params.set('query', query.trim())
  return request(`/projects?${params.toString()}`)
}

/** 携带当前会话CSRF Token创建项目，不向客户端开放Owner引用字段。 */
export function createProject(input: { name: string; description?: string; csrfToken: string }): Promise<Project> {
  const { csrfToken, ...body } = input
  return request('/projects', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': csrfToken },
    body: JSON.stringify(body),
  })
}

/** 读取当前项目的活动成员和角色，仅后端授权主体可见。 */
export function getProjectMembers(projectId: number): Promise<ProjectMember[]> {
  return request(`/projects/${projectId}/members`)
}

/** 添加已完成企业认证的项目成员。 */
export function addProjectMember(input: { projectId: number; issuer: string; subject: string; roles: string[]; csrfToken: string }): Promise<ProjectMember> {
  const { projectId, csrfToken, ...body } = input
  return request(`/projects/${projectId}/members`, { method: 'POST', headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': csrfToken }, body: JSON.stringify(body) })
}

/** 替换项目成员角色集合。 */
export function replaceProjectMemberRoles(input: { projectId: number; principalRef: string; roles: string[]; csrfToken: string }): Promise<ProjectMember> {
  const { projectId, principalRef, csrfToken, ...body } = input
  return request(`/projects/${projectId}/members/${principalRef}`, { method: 'PATCH', headers: { 'Content-Type': 'application/merge-patch+json', 'X-CSRF-Token': csrfToken }, body: JSON.stringify(body) })
}

/** 撤销项目成员资格。 */
export function removeProjectMember(input: { projectId: number; principalRef: string; csrfToken: string }): Promise<void> {
  return request(`/projects/${input.projectId}/members/${input.principalRef}`, { method: 'DELETE', headers: { 'X-CSRF-Token': input.csrfToken } })
}

/** 读取项目任务，状态过滤和分页均由服务端在项目成员权限范围内执行。 */
export function getProjectTasks(projectId: number, page = 1, pageSize = 20, status?: string): Promise<TaskPage> {
  const params = new URLSearchParams({ page: String(page), pageSize: String(pageSize) })
  if (status) params.set('status', status)
  return request(`/projects/${projectId}/tasks?${params.toString()}`)
}

/** 使用会话CSRF Token创建项目任务，任务管理角色由后端校验。 */
export function createProjectTask(input: { projectId: number; title: string; description?: string; phase: string; assigneeRef?: string; assigneeRole?: string; csrfToken: string }): Promise<ProjectTask> {
  const { projectId, csrfToken, ...body } = input
  return request(`/projects/${projectId}/tasks`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': csrfToken },
    body: JSON.stringify(body),
  })
}

/** 读取项目登记的仓库引用型交付物。 */
export function getProjectDeliverables(projectId: number, page = 1, pageSize = 20): Promise<DeliverablePage> {
  return request(`/projects/${projectId}/deliverables?page=${page}&pageSize=${pageSize}`)
}

/** 登记交付物引用，不上传文件内容。 */
export function createProjectDeliverable(input: { projectId: number; title: string; phase: string; version: string; sourceRef: string; taskId?: number; csrfToken: string }): Promise<Deliverable> {
  const { projectId, csrfToken, ...body } = input
  return request(`/projects/${projectId}/deliverables`, {
    method: 'POST', headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': csrfToken }, body: JSON.stringify(body),
  })
}

/** 记录Open Issue。 */
export function createProjectIssue(input: { projectId: number; title: string; description: string; impact: string; decisionRole: string; code?: string; status?: string; csrfToken: string }): Promise<OpenIssue> {
  const { projectId, csrfToken, ...body } = input
  return request(`/projects/${projectId}/issues`, { method: 'POST', headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': csrfToken }, body: JSON.stringify(body) })
}

/** 记录人工Issue决策。 */
export function decideProjectIssue(input: { issueId: number; decision: string; outcome: string; csrfToken: string }): Promise<OpenIssue> {
  const { issueId, csrfToken, ...body } = input
  return request(`/issues/${issueId}/decisions`, { method: 'POST', headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': csrfToken }, body: JSON.stringify(body) })
}

/** 读取项目Open Issue。 */
export function getProjectIssues(projectId: number): Promise<OpenIssue[]> { return request(`/projects/${projectId}/issues`) }

/** 读取模板与规则资源索引，客户端不请求文件内容。 */
export function getProjectResources(projectId: number, phase?: string, kind?: string): Promise<ProjectResource[]> {
  const params = new URLSearchParams(); if (phase) params.set('phase', phase); if (kind) params.set('kind', kind)
  const suffix = params.toString() ? `?${params.toString()}` : ''
  return request(`/projects/${projectId}/resources${suffix}`)
}

/** 读取项目Gate和检查项。 */
export function getProjectGates(projectId: number): Promise<ProjectGate[]> { return request(`/projects/${projectId}/gates`) }

/** 提交Gate范围供独立Reviewer评审。 */
export function submitGate(input: { gateId: number; decisionOwnerRef: string; taskIds: number[]; deliverableIds: number[]; csrfToken: string }): Promise<ProjectGate> {
  const { gateId, csrfToken, ...body } = input
  return request(`/gates/${gateId}/submission`, { method: 'POST', headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': csrfToken }, body: JSON.stringify(body) })
}

/** 提交Gate检查结论。 */
export function decideGateCheck(input: { gateId: number; checkId: number; status: string; comment?: string; evidenceRefs?: string[]; csrfToken: string }): Promise<GateCheck> {
  const { gateId, checkId, csrfToken, ...body } = input
  return request(`/gates/${gateId}/checks/${checkId}/decision`, { method: 'POST', headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': csrfToken }, body: JSON.stringify(body) })
}

/** 提交Gate最终决定。 */
export function decideGate(input: { gateId: number; decision: string; comment?: string; csrfToken: string }): Promise<ProjectGate> {
  const { gateId, csrfToken, ...body } = input
  return request(`/gates/${gateId}/decision`, { method: 'POST', headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': csrfToken }, body: JSON.stringify(body) })
}

/** 读取Runtime授权状态；客户端永远不接收密钥。 */
export function getRuntimeConfigStatus(projectId: number): Promise<RuntimeConfigStatus> { return request(`/projects/${projectId}/agent-runtime/config-status`) }

/** 分页读取项目审计活动摘要。 */
export function getProjectActivity(projectId: number, page = 1, pageSize = 20, objectType?: string): Promise<ActivityPage> {
  const params = new URLSearchParams({ page: String(page), pageSize: String(pageSize) }); if (objectType) params.set('objectType', objectType)
  return request(`/projects/${projectId}/activity?${params.toString()}`)
}

/** Gate通过后推进项目生命周期。 */
export function advanceProjectStage(projectId: number, targetPhase: string, csrfToken: string): Promise<Project> {
  return request(`/projects/${projectId}/stage-transitions`, { method: 'POST', headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': csrfToken }, body: JSON.stringify({ targetPhase }) })
}

/** 请求Runtime执行；当前按HD-002失败关闭并由调用方展示配置提示。 */
export function requestAgentRun(projectId: number, taskId: number, requestKey: string, csrfToken: string): Promise<never> {
  return request(`/projects/${projectId}/tasks/${taskId}/agent-runs`, { method: 'POST', headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': csrfToken }, body: JSON.stringify({ requestKey }) })
}
