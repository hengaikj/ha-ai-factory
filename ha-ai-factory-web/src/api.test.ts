import { afterEach, describe, expect, it, vi } from 'vitest'
import { addProjectMember, beginLogin, createProject, createProjectDeliverable, createProjectTask, decideGate, decideProjectIssue, getCurrentSession, getProjects, getProjectActivity, getProjectDeliverables, getProjectGates, getProjectIssues, getProjectMembers, getProjectResources, getProjectTasks, getRuntimeConfigStatus, logout, requestAgentRun, reviewProjectDeliverable, updateProject, updateProjectTask, submitGate } from './api'

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

  it('loads issues and resource indexes within project scope', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(Response.json([]))
      .mockResolvedValueOnce(Response.json([]))
    vi.stubGlobal('fetch', fetchMock)
    await expect(getProjectIssues(8)).resolves.toEqual([])
    await expect(getProjectResources(8, 'DISCOVERY', 'RULE')).resolves.toEqual([])
    expect(fetchMock).toHaveBeenNthCalledWith(1, '/projects/8/issues', expect.objectContaining({ credentials: 'include' }))
    expect(fetchMock).toHaveBeenNthCalledWith(2, '/projects/8/resources?phase=DISCOVERY&kind=RULE', expect.objectContaining({ credentials: 'include' }))
  })

  it('loads and submits Gate scope with CSRF protection', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(Response.json([]))
      .mockResolvedValueOnce(Response.json({ id: 2, projectId: 8, phase: 'DISCOVERY', status: 'READY_FOR_REVIEW', taskIds: [3], deliverableIds: [], checks: [] }))
    vi.stubGlobal('fetch', fetchMock)
    await expect(getProjectGates(8)).resolves.toEqual([])
    await expect(submitGate({ gateId: 2, decisionOwnerRef: 'p1', taskIds: [3], deliverableIds: [], csrfToken: 'c'.repeat(32) })).resolves.toMatchObject({ status: 'READY_FOR_REVIEW' })
    expect(fetchMock).toHaveBeenNthCalledWith(2, '/gates/2/submission', expect.objectContaining({ method: 'POST', headers: expect.objectContaining({ 'X-CSRF-Token': 'c'.repeat(32) }) }))
  })

  it('shows runtime as unconfigured without exposing credentials', async () => {
    const fetchMock = vi.fn().mockResolvedValue(Response.json({ projectId: 8, status: 'UNCONFIGURED', modelRef: null }))
    vi.stubGlobal('fetch', fetchMock)
    await expect(getRuntimeConfigStatus(8)).resolves.toMatchObject({ projectId: 8, status: 'UNCONFIGURED' })
    expect(fetchMock).toHaveBeenCalledWith('/projects/8/agent-runtime/config-status', expect.objectContaining({ credentials: 'include' }))
  })

  it('loads paged project activity summaries', async () => {
    const fetchMock = vi.fn().mockResolvedValue(Response.json({ items: [], page: 2, pageSize: 10, total: 0 }))
    vi.stubGlobal('fetch', fetchMock)
    await expect(getProjectActivity(8, 2, 10, 'PROJECT')).resolves.toMatchObject({ page: 2 })
    expect(fetchMock).toHaveBeenCalledWith('/projects/8/activity?page=2&pageSize=10&objectType=PROJECT', expect.objectContaining({ credentials: 'include' }))
  })

  it('keeps Agent Runtime execution fail closed when configuration is not approved', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 409 }))
    vi.stubGlobal('fetch', fetchMock)
    await expect(requestAgentRun(8, 3, '00000000-0000-4000-8000-000000000001', 'c'.repeat(32))).rejects.toMatchObject({ status: 409 })
    expect(fetchMock).toHaveBeenCalledWith('/projects/8/tasks/3/agent-runs', expect.objectContaining({ method: 'POST' }))
  })

  it('sends approved mutation routes with the session CSRF token', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(Response.json({ principalRef: 'p1', displayName: '李四', roles: ['ENGINEER'], joinedAt: '2026-01-01T00:00:00Z' }))
      .mockResolvedValueOnce(Response.json({ id: 1, projectId: 8, phase: 'DISCOVERY', status: 'APPROVED', checks: [] }))
      .mockResolvedValueOnce(Response.json({ id: 2, projectId: 8, title: 'Issue', status: 'DECIDED' }))
    vi.stubGlobal('fetch', fetchMock)
    await addProjectMember({ projectId: 8, issuer: 'https://idp.example', subject: 'bob', roles: ['ENGINEER'], csrfToken: 'c'.repeat(32) })
    await decideGate({ gateId: 1, decision: 'APPROVED', comment: '通过', csrfToken: 'c'.repeat(32) })
    await decideProjectIssue({ issueId: 2, decision: '采用', outcome: 'DECIDED', csrfToken: 'c'.repeat(32) })
    expect(fetchMock).toHaveBeenNthCalledWith(1, '/projects/8/members', expect.objectContaining({ method: 'POST', headers: expect.objectContaining({ 'X-CSRF-Token': 'c'.repeat(32) }) }))
  })

  it('updates tasks and records independent deliverable reviews', async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(Response.json({ id: 3, status: 'DONE' }))
      .mockResolvedValueOnce(Response.json({ id: 4, outcome: 'APPROVED' }, { status: 201 }))
    vi.stubGlobal('fetch', fetchMock)
    await expect(updateProjectTask({ taskId: 3, status: 'DONE', csrfToken: 'c'.repeat(32) })).resolves.toMatchObject({ status: 'DONE' })
    await expect(reviewProjectDeliverable({ deliverableId: 4, outcome: 'APPROVED', comment: '通过', csrfToken: 'c'.repeat(32) })).resolves.toMatchObject({ outcome: 'APPROVED' })
    expect(fetchMock).toHaveBeenNthCalledWith(1, '/tasks/3', expect.objectContaining({ method: 'PATCH' }))
    expect(fetchMock).toHaveBeenNthCalledWith(2, '/deliverables/4/reviews', expect.objectContaining({ method: 'POST' }))
  })

  it('updates project metadata with merge-patch and CSRF protection', async () => {
    const fetchMock = vi.fn().mockResolvedValue(Response.json({ id: 8, name: '新名称' }))
    vi.stubGlobal('fetch', fetchMock)
    await expect(updateProject({ projectId: 8, name: '新名称', csrfToken: 'c'.repeat(32) })).resolves.toMatchObject({ name: '新名称' })
    expect(fetchMock).toHaveBeenCalledWith('/projects/8', expect.objectContaining({ method: 'PATCH', headers: expect.objectContaining({ 'Content-Type': 'application/merge-patch+json' }) }))
  })

  it('logs out through the server session endpoint', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }))
    vi.stubGlobal('fetch', fetchMock)
    await expect(logout('c'.repeat(32))).resolves.toBeUndefined()
    expect(fetchMock).toHaveBeenCalledWith('/auth/logout', expect.objectContaining({ method: 'POST', headers: expect.objectContaining({ 'X-CSRF-Token': 'c'.repeat(32) }) }))
  })
})
