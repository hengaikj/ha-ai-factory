import { afterEach, describe, expect, it, vi } from 'vitest'
import { beginLogin, createProject, getCurrentSession, getProjects } from './api'

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
})
