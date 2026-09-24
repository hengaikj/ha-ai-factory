import { afterEach, describe, expect, it, vi } from 'vitest'
import { beginLogin, createProject, createProjectDeliverable, createProjectTask, getCurrentSession, getProjects, getProjectDeliverables, getProjectMembers, getProjectTasks } from './api'

afterEach(() => vi.unstubAllGlobals())

describe('contract API client', () => {
  it('loads the server session with same-origin cookies', async () => {
    const fetchMock = vi.fn().mockResolvedValue(Response.json({
      principalRef: 'principal-1', displayName: '张三', csrfToken: 'c'.repeat(32),
    }))
    vi.stubGlobal('fetch', fetchMock)

    await expect(getCurrentSession()).resolves.toMatchObject({ displayName: '张三' })
    expect(fetchMock).toHaveBeenCalledWith('/auth/session', expect.objectContaining({
      credentials: 'include',
    }))
  })

  it('keeps missing OIDC configuration actionable in the console', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 503 }))
    vi.stubGlobal('fetch', fetchMock)
    await expect(beginLogin()).rejects.toMatchObject({ status: 503, message: expect.stringContaining('联系管理员') })
    expect(fetchMock).toHaveBeenCalledWith('/auth/login', expect.objectContaining({
      credentials: 'include', redirect: 'manual',
    }))
  })

  it('starts OIDC only at a same-origin authorization route', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 302, headers: { Location: '/oauth2/authorization/enterprise' } }))
    vi.stubGlobal('fetch', fetchMock)
    vi.stubGlobal('window', { location: { origin: 'http://localhost' } })
    await expect(beginLogin()).resolves.toBe(`${window.location.origin}/oauth2/authorization/enterprise`)
  })

  it('falls back to top-level login navigation when the browser hides a manual redirect', async () => {
    const fetchMock = vi.fn().mockResolvedValue({ status: 0, type: 'opaqueredirect' })
    vi.stubGlobal('fetch', fetchMock)
    vi.stubGlobal('window', { location: { origin: 'http://localhost' } })
    await expect(beginLogin()).resolves.toBe('http://localhost/auth/login')
  })

  it('uses the authenticated project-list route and preserves unauthorized state', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 401 }))
    vi.stubGlobal('fetch', fetchMock)

    await expect(getProjects(2, 20, 'factory')).rejects.toMatchObject({ status: 401 })
    expect(fetchMock).toHaveBeenCalledWith('/projects?page=2&pageSize=20&query=factory', expect.objectContaining({
      credentials: 'include',
    }))
  })

  it('sends session CSRF token and only the approved project create fields', async () => {
    const created = { id: 8, name: 'MVP', gateStatus: 'PENDING' }
    const fetchMock = vi.fn().mockResolvedValue(Response.json(created, { status: 201 }))
    vi.stubGlobal('fetch', fetchMock)

    await expect(createProject({ name: 'MVP', description: '说明', csrfToken: 'c'.repeat(32) }))
      .resolves.toMatchObject({ id: 8 })
    expect(fetchMock).toHaveBeenCalledWith('/projects', expect.objectContaining({
      method: 'POST',
      credentials: 'include',
      headers: expect.objectContaining({ 'X-CSRF-Token': 'c'.repeat(32) }),
      body: JSON.stringify({ name: 'MVP', description: '说明' }),
    }))
  })

  it('parses contract problem responses without exposing raw response text', async () => {
    const fetchMock = vi.fn().mockResolvedValue(Response.json({
      type: 'about:blank', title: 'Forbidden', status: 403, code: 'FORBIDDEN', detail: 'private',
    }, { status: 403, headers: { 'content-type': 'application/problem+json' } }))
    vi.stubGlobal('fetch', fetchMock)

    await expect(getProjects()).rejects.toMatchObject({ status: 403, code: 'FORBIDDEN' })
  })

  it('loads project members through the scoped member route', async () => {
    const fetchMock = vi.fn().mockResolvedValue(Response.json([{ principalRef: 'p1', displayName: '张三', roles: ['OWNER'], joinedAt: '2026-01-01T00:00:00Z' }]))
    vi.stubGlobal('fetch', fetchMock)
    await expect(getProjectMembers(8)).resolves.toMatchObject([{ displayName: '张三', roles: ['OWNER'] }])
    expect(fetchMock).toHaveBeenCalledWith('/projects/8/members', expect.objectContaining({ credentials: 'include' }))
  })

  it('loads and creates project tasks through scoped CSRF protected routes', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(Response.json({ items: [], page: 1, pageSize: 20, total: 0 }))
      .mockResolvedValueOnce(Response.json({ id: 3, projectId: 8, title: '建立基础骨架', phase: 'DISCOVERY', status: 'NOT_STARTED' }, { status: 201 }))
    vi.stubGlobal('fetch', fetchMock)

    await expect(getProjectTasks(8)).resolves.toMatchObject({ total: 0 })
    await expect(createProjectTask({ projectId: 8, title: '建立基础骨架', phase: 'DISCOVERY', csrfToken: 'c'.repeat(32) })).resolves.toMatchObject({ id: 3 })
    expect(fetchMock).toHaveBeenNthCalledWith(1, '/projects/8/tasks?page=1&pageSize=20', expect.objectContaining({ credentials: 'include' }))
    expect(fetchMock).toHaveBeenNthCalledWith(2, '/projects/8/tasks', expect.objectContaining({ method: 'POST', headers: expect.objectContaining({ 'X-CSRF-Token': 'c'.repeat(32) }) }))
  })

  it('loads and registers repository referenced deliverables', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(Response.json({ items: [], page: 1, pageSize: 20, total: 0 }))
      .mockResolvedValueOnce(Response.json({ id: 4, projectId: 8, title: '基线', phase: 'DISCOVERY', version: 'v1', sourceRef: 'docs/baseline.md', reviewStatus: 'DRAFT' }, { status: 201 }))
    vi.stubGlobal('fetch', fetchMock)
    await expect(getProjectDeliverables(8)).resolves.toMatchObject({ total: 0 })
    await expect(createProjectDeliverable({ projectId: 8, title: '基线', phase: 'DISCOVERY', version: 'v1', sourceRef: 'docs/baseline.md', csrfToken: 'c'.repeat(32) })).resolves.toMatchObject({ id: 4 })
    expect(fetchMock).toHaveBeenNthCalledWith(1, '/projects/8/deliverables?page=1&pageSize=20', expect.objectContaining({ credentials: 'include' }))
    expect(fetchMock).toHaveBeenNthCalledWith(2, '/projects/8/deliverables', expect.objectContaining({ method: 'POST', body: expect.stringContaining('docs/baseline.md') }))
  })
})
