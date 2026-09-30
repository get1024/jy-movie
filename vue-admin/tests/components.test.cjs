const test = require('node:test')
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const compiler = require('vue-template-compiler')

function component(relative, mocks = {}) {
  const filename = path.join(__dirname, '../src', relative)
  const source = fs.readFileSync(filename, 'utf8')
  const parsed = compiler.parseComponent(source)
  const script = parsed.script.content
    .replace(/^\s*import\s+(?:[\w*$,{} \n]+\s+from\s+)?['"][^'"]+['"];?/gm, '')
    .replace('export default', 'module.exports =')
  const sandbox = {
    module: { exports: {} },
    localStorage: { getItem: () => null, setItem() {}, removeItem() {} },
    config: { API_URL: 'http://localhost:8888/api' },
    ...mocks,
  }
  vm.runInNewContext(script, sandbox, { filename })
  const options = sandbox.module.exports
  const messages = []
  const instance = {
    ...(options.data ? options.data.call({ $route: { query: {} } }) : {}),
    $refs: {},
    $message: message => messages.push(message),
    $router: { push: () => Promise.resolve() },
    $confirm: () => Promise.resolve(),
  }
  for (const [name, method] of Object.entries(options.methods || {})) {
    instance[name] = method.bind(instance)
  }
  if (options.created) options.created.call(instance)
  return { options, instance, messages, source }
}

function deferred() {
  let resolve, reject
  const promise = new Promise((a, b) => { resolve = a; reject = b })
  return { promise, resolve, reject }
}

const plain = value => JSON.parse(JSON.stringify(value))

test('all Vue templates compile and components have no cosmetic timers', () => {
  const root = path.join(__dirname, '../src')
  for (const file of fs.readdirSync(root, { recursive: true }).filter(file => file.endsWith('.vue'))) {
    const source = fs.readFileSync(path.join(root, file), 'utf8')
    const parsed = compiler.parseComponent(source)
    assert.deepEqual(compiler.compile(parsed.template.content).errors, [], file)
    assert.doesNotMatch(source, /\bset(?:Timeout|Interval)\s*\(/, file)
    assert.doesNotMatch(source, /console\.(?:log|debug)\([^)]*(?:[Ll]oginData|password|token)/, file)
    if (parsed.script) {
      const script = parsed.script.content
        .replace(/^\s*import\s+(?:[\w*$,{} \n]+\s+from\s+)?['"][^'"]+['"];?/gm, '')
        .replace('export default', 'module.exports =')
      assert.doesNotThrow(() => new vm.Script(script, { filename: file }), file)
    }
  }
})

function poster(mocks = {}) {
  const fixture = component('views/film/Poster.vue', {
    ListAllPoster: async () => ({ success: true, data: [] }),
    AddPoster: async () => ({ success: true }),
    DeleteAllPoster: async () => ({ success: true }),
    DeletePosterById: async () => ({ success: true }),
    UpdatePoster: async () => ({ success: true }),
    ...mocks,
  })
  fixture.clears = 0
  fixture.aborts = 0
  fixture.instance.$refs.posterUpload = {
    clearFiles() { fixture.clears += 1 },
    abort() { fixture.aborts += 1 },
  }
  return fixture
}

test('posters support repeated uploads with a clean uploader, boolean status and refreshed list', async () => {
  const saved = []
  const fixture = poster({
    AddPoster: async data => { saved.push(data); return { success: true } },
    ListAllPoster: async () => ({ success: true, data: saved }),
  })
  const { instance, source } = fixture
  assert.match(source, /:value="true"/)
  assert.match(source, /:value="false"/)
  for (const uid of [1, 2]) {
    instance.dialogFormVisible = true
    instance.resetUploadForm()
    instance.uploadPoster.title = ' Poster ' + uid + ' '
    instance.beforePosterUpload({ uid })
    instance.handleUploadSuccess('https://example.test/poster-' + uid + '.jpg', { uid })
    await instance.submitUpload()
    assert.equal(instance.saving, false)
    assert.equal(instance.dialogFormVisible, false)
    assert.equal(instance.uploadPoster.url, '')
    assert.equal(instance.uploadPoster.title, '')
    assert.equal(instance.uploadPoster.status, true)
    assert.equal(instance.posterList.length, uid)
  }
  assert.equal(saved.length, 2)
  assert.equal(saved[1].title, 'Poster 2')
  assert.equal(typeof saved[1].status, 'boolean')
  assert.ok(fixture.clears >= 4)
})

test('poster cancel invalidates late upload callbacks; upload errors release the one-file slot', () => {
  const { instance, options, messages } = poster()
  instance.beforePosterUpload({ uid: 1 })
  instance.resetUploadForm()
  instance.handleUploadSuccess('https://example.test/stale.jpg', { uid: 1 })
  assert.equal(instance.uploadPoster.url, '')
  instance.beforePosterUpload({ uid: 2 })
  instance.handleUploadSuccess({ success: false, msg: 'rejected' }, { uid: 2 })
  assert.equal(instance.uploading, false)
  assert.equal(instance.uploadFileUid, null)
  assert.equal(messages[0].type, 'error')
  instance.beforePosterUpload({ uid: 3 })
  options.beforeDestroy.call(instance)
  instance.handleUploadSuccess('https://example.test/destroyed.jpg', { uid: 3 })
  assert.equal(instance.uploadPoster.url, '')
})

test('poster save failure preserves form, prevents duplicate submits and resets loading', async () => {
  const response = deferred()
  let calls = 0
  const { instance } = poster({ AddPoster: () => { calls += 1; return response.promise } })
  instance.dialogFormVisible = true
  instance.uploadPoster = { title: 'test', url: '/image', status: false }
  const first = instance.submitUpload()
  await instance.submitUpload()
  assert.equal(calls, 1)
  response.reject(new Error('offline'))
  await first
  assert.equal(instance.saving, false)
  assert.equal(instance.dialogFormVisible, true)
  assert.equal(instance.uploadPoster.url, '/image')
})

test('poster mutations change UI only after success and delete-all always keeps an array', async () => {
  let success = false
  const { instance } = poster({
    UpdatePoster: async () => ({ success }),
    DeletePosterById: async () => ({ success }),
    DeleteAllPoster: async () => ({ success }),
  })
  const row = { id: 'a', status: true }
  instance.posterList = [row]
  await instance.changePosterStatus(0, row, false)
  assert.equal(row.status, true)
  await instance.handleDeletePoster(0, 'a')
  await instance.deleteAllPoster()
  assert.equal(instance.posterList.length, 1)
  success = true
  await instance.changePosterStatus(0, row, false)
  assert.equal(row.status, false)
  await instance.deleteAllPoster()
  assert.deepEqual(plain(instance.posterList), [])
})

test('admin loading flags reset on business errors and rejected requests', async () => {
  for (const [file, method, api, args] of [
    ['views/order/List.vue', 'loadOrderList', 'FindAllOrder', []],
    ['views/order/Exception.vue', 'loadList', 'ListOrderException', []],
    ['views/film/Arrange.vue', 'load', 'FindAllArrangement', []],
    ['views/worker/Daily.vue', 'loadList', 'ListDailyWork', []],
    ['views/worker/List.vue', 'handleListRole', 'FindWorkerRoles', [{ id: 'worker' }]],
    ['views/worker/Add.vue', 'onSubmit', 'CreateWorker', []],
  ]) {
    for (const response of [async () => ({ success: false }), async () => { throw new Error('offline') }]) {
      const { instance } = component(file, { [api]: response })
      await instance[method](...args)
      assert.equal(instance.loading, false, file)
    }
  }
})

test('admin film list and login release loading on network failure; validation is immediate', async () => {
  const fail = async () => { throw new Error('offline') }
  const film = component('views/film/List.vue', { ListAllFilm: fail })
  await film.options.mounted.call(film.instance)
  assert.equal(film.instance.tableLoading, false)
  const { instance } = component('views/Login.vue', { Login: fail })
  await instance.postLogin()
  assert.equal(instance.loading, false)
  instance.username = ''
  await instance.postLogin()
  assert.equal(instance.usernameError, true)
  assert.equal(instance.loading, false)
})

test('login ignores interrupted success and blocks duplicate submissions', async () => {
  const response = deferred()
  const writes = [], routes = []
  let calls = 0
  const { instance, options, source } = component('views/Login.vue', {
    Login: () => { calls += 1; return response.promise },
    localStorage: { getItem: () => null, setItem: (...args) => writes.push(args) },
  })
  instance.$router.push = route => { routes.push(route); return Promise.resolve() }
  const pending = instance.postLogin()
  await instance.postLogin()
  assert.equal(calls, 1)
  options.beforeDestroy.call(instance)
  response.resolve({ success: true, data: { token: 'demo-token', user: { id: 'user' }, worker: { id: 'worker' } } })
  await pending
  assert.deepEqual(writes, [])
  assert.deepEqual(routes, [])
  assert.equal(instance.loading, false)
  assert.doesNotMatch(source, /console\.log/)
})

test('failed login does not persist authentication or navigate and allows retry', async () => {
  const writes = [], routes = []
  const { instance } = component('views/Login.vue', {
    Login: async () => ({ success: false, data: null }),
    localStorage: { getItem: () => null, setItem: (...args) => writes.push(args) },
  })
  instance.$router.push = route => { routes.push(route); return Promise.resolve() }
  await instance.postLogin()
  assert.equal(instance.loading, false)
  assert.deepEqual(writes, [])
  assert.deepEqual(routes, [])
})

function renderWithRows(relative, overrides = {}, row = {}) {
  const Vue = require('vue')
  const { options, source } = component(relative)
  const template = compiler.parseComponent(source).template.content
  const compiled = compiler.compileToFunctions(template)
  const errors = []
  const previousHandler = Vue.config.errorHandler
  Vue.config.errorHandler = error => errors.push(error)
  let instance
  try {
    instance = new Vue({
      data: () => ({ ...options.data.call({ $route: { query: {} } }), ...overrides }),
      methods: options.methods,
      render: compiled.render,
      staticRenderFns: compiled.staticRenderFns,
    })
    const node = instance._render()
    function inspect(value) {
      if (!value) return
      if (Array.isArray(value)) { value.forEach(inspect); return }
      if (value.data && value.data.scopedSlots && value.data.scopedSlots.default) {
        inspect(value.data.scopedSlots.default({ row, $index: 0 }))
      }
      if (value.children) value.children.forEach(inspect)
    }
    inspect(node)
    assert.deepEqual(errors, [], relative)
    return node
  } finally {
    if (instance) instance.$destroy()
    Vue.config.errorHandler = previousHandler
  }
}
test('order detail scoped slots tolerate deleted film, arrangement and user records', () => {
  const row = { order: { id: 'order', uid: 'former-user', aid: 'former-show', status: 0 }, user: null, film: null, arrangement: null }
  renderWithRows('views/order/List.vue', { orders: [row] }, row)
})
test('opening and canceling exception editing leaves the original row unchanged', () => {
  const { instance, source } = component('views/order/Exception.vue')
  const row = { id: 'exception', status: false, result: 'existing' }
  instance.list = [row]
  instance.handle(0)
  instance.form.status = true
  instance.form.result = 'draft'
  instance.dialogFormVisible = false
  assert.deepEqual(row, { id: 'exception', status: false, result: 'existing' })
  assert.match(source, /:value="true"/)
  assert.match(source, /:value="false"/)
})
