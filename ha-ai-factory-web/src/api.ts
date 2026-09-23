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
