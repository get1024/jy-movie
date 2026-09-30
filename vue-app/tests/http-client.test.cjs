const { test } = require('node:test')
const assert = require('node:assert/strict')
const axios = require('axios')
const createHttpClient = require('../src/utils/http-client.cjs')

function setup() {
  const state = new Map()
  const successes = []
  const failures = []
  let unauthorized = 0
  let forbidden = 0
  const client = createHttpClient(axios, {
    baseURL: 'http://localhost:8888/api',
    storage: { getItem: key => state.get(key) || null },
    notify: { success: value => successes.push(value), error: value => failures.push(value) },
    onUnauthorized: () => unauthorized++,
    onForbidden: () => forbidden++,
  })
  return { client, state, successes, failures, counts: () => ({ unauthorized, forbidden }) }
}

const respond = data => config => Promise.resolve({
  data, status: 200, statusText: 'OK', headers: {}, config,
})

test('uses the current token for every request, including login, rotation, and logout', async () => {
  const { client, state } = setup()
  const observed = []
  client.defaults.adapter = config => {
    observed.push(config.headers.get('Authorization'))
    return respond({ success: true, data: [] })(config)
  }
  await client.get('/film')
  state.set('token', 'first-token')
  await client.get('/film')
  state.set('token', 'new-token')
  await client.get('/film')
  state.delete('token')
  await client.get('/film', { headers: { authorization: 'stale-token' } })
  assert.deepEqual(observed, [undefined, 'first-token', 'new-token', undefined])
})

test('unwraps success data and only notifies non-empty messages', async () => {
  const { client, successes, failures } = setup()
  const result = { success: true, msg: null, data: [{ id: 1 }] }
  assert.deepEqual(await client.get('/film', { adapter: respond(result) }), result)
  await client.get('/film', { adapter: respond({ success: true, msg: '' }) })
  await client.get('/film', { adapter: respond({ success: true, msg: '保存成功' }) })
  assert.equal(successes.length, 1)
  assert.equal(successes[0].message, '保存成功')
  assert.equal(failures.length, 0)
})

test('preserves business-error envelopes and reports forbidden once without returning undefined', async () => {
  const { client, failures, counts } = setup()
  const result = { success: false, code: 403, msg: '没有权限', data: null }
  assert.deepEqual(await client.get('/worker', { adapter: respond(result) }), result)
  assert.equal(failures.length, 1)
  assert.deepEqual(counts(), { unauthorized: 0, forbidden: 1 })
})

test('handles unauthorized envelopes and leaves validation errors to their existing callers', async () => {
  const { client, failures, counts } = setup()
  const validation = { success: false, code: 400, msg: '参数错误', data: null }
  assert.deepEqual(await client.post('/user', {}, { adapter: respond(validation) }), validation)
  await client.get('/user', { adapter: respond({ success: false, code: 401 }) })
  assert.equal(failures.length, 2)
  assert.deepEqual(counts(), { unauthorized: 1, forbidden: 0 })
})

test('rejects HTTP failures with the original error and reports the server message', async () => {
  const { client, failures, counts } = setup()
  const error = new axios.AxiosError('Forbidden', 'ERR_BAD_REQUEST', {}, null, {
    status: 403, data: { msg: '访问被拒绝' },
  })
  await assert.rejects(client.get('/worker', { adapter: config => { error.config = config; return Promise.reject(error) } }), value => value === error)
  assert.equal(failures[0].message, '访问被拒绝')
  assert.deepEqual(counts(), { unauthorized: 0, forbidden: 1 })
})

test('rejects timeout and network errors with a useful notification', async () => {
  const { client, failures } = setup()
  for (const code of ['ECONNABORTED', 'ERR_NETWORK']) {
    const error = new axios.AxiosError('failed', code)
    await assert.rejects(client.get('/film', { adapter: () => Promise.reject(error) }), value => value === error)
  }
  assert.match(failures[0].message, /超时/)
  assert.match(failures[1].message, /网络/)
})

test('does not show network errors for deliberately canceled requests', async () => {
  const { client, failures } = setup()
  const error = new axios.CanceledError('canceled')
  await assert.rejects(client.get('/film', { adapter: () => Promise.reject(error) }), value => value === error)
  assert.equal(failures.length, 0)
})

test('preserves plain upload URLs and empty responses', async () => {
  const { client, failures, successes } = setup()
  assert.equal(await client.post('/upload', {}, { adapter: respond('https://example.com/poster.jpg') }), 'https://example.com/poster.jpg')
  assert.equal(await client.get('/empty', { adapter: respond(null) }), null)
  assert.equal(failures.length + successes.length, 0)
})

for (const code of [401, 403]) {
  for (const kind of ['http', 'envelope']) {
    test(`delayed ${kind} ${code} cannot invalidate a newer login`, async () => {
      const { client, state, failures, counts } = setup()
      state.set('token', 'old-session')
      let complete
      let started
      const adapterStarted = new Promise(resolve => { started = resolve })
      const pending = client.get('/protected', {
        adapter: config => new Promise((resolve, reject) => {
          complete = () => {
            if (kind === 'envelope') {
              resolve({ data: { success: false, code, msg: '过期请求' }, status: 200, config })
            } else {
              reject(new axios.AxiosError('expired request', 'ERR_BAD_REQUEST', config, null, {
                status: code, data: { msg: '过期请求' },
              }))
            }
          }
          started()
        }),
      })
      await adapterStarted
      state.set('token', 'new-session')
      complete()
      if (kind === 'http') await assert.rejects(pending)
      else assert.equal((await pending).success, false)
      assert.deepEqual(counts(), { unauthorized: 0, forbidden: 0 })
      assert.equal(failures.length, 1)
      assert.equal(state.get('token'), 'new-session')
    })
  }
}
