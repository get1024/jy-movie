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

test('chart is responsive, disposes instances and disconnects all callbacks on destroy', () => {
  const charts = [], events = [], observers = []
  const element = {}
  const { instance, options, source } = component('views/Info.vue', {
    echarts: {
      init(node, theme) {
        assert.equal(node, element)
        assert.equal(theme, 'v5')
        const chart = {
          resized: 0, disposed: 0,
          setOption(value) { this.option = value },
          resize() { this.resized += 1 },
          dispose() { this.disposed += 1 },
        }
        charts.push(chart)
        return chart
      },
      graphic: { LinearGradient: function () {} },
    },
    window: {
      addEventListener: (name, handler) => events.push(['add', name, handler]),
      removeEventListener: (name, handler) => events.push(['remove', name, handler]),
    },
    ResizeObserver: class {
      constructor(callback) { this.callback = callback; this.disconnected = false; observers.push(this) }
      observe(node) { assert.equal(node, element) }
      disconnect() { this.disconnected = true }
    },
  })
  instance.$refs.chart = element
  options.mounted.call(instance)
  instance.resizeChart()
  assert.equal(charts[0].resized, 1)
  instance.initChart()
  assert.equal(charts[0].disposed, 1)
  options.beforeDestroy.call(instance)
  assert.equal(charts[1].disposed, 1)
  assert.equal(observers[0].disconnected, true)
  assert.equal(events[0][2], events[1][2])
  assert.equal(instance.chart, null)
  observers[0].callback()
  assert.equal(charts[1].resized, 0)
  assert.match(source, /演示数据/)
  assert.equal((source.match(/<template>/g) || []).length, 1)
})

test('daily task command removes the actual selected id, without relying on an index', async () => {
  const calls = []
  const { instance } = component('components/layout/Header.vue', {
    DeleteDailyWork: async id => { calls.push(id); return { success: true } },
  })
  instance.list = [{ id: 'first' }, { id: 'second', content: 'task' }, { id: 'third' }]
  await instance.handleCommand(instance.list[1])
  assert.deepEqual(calls, ['second'])
  assert.deepEqual(instance.list.map(item => item.id), ['first', 'third'])
  await instance.handleCommand(undefined)
  assert.equal(calls.length, 1)
})

test('daily task failures keep task visible and duplicate clicks send only one request', async () => {
  const response = deferred()
  let calls = 0
  const { instance, messages } = component('components/layout/Header.vue', {
    DeleteDailyWork: () => { calls += 1; return response.promise },
  })
  const task = { id: 'task' }
  instance.list = [task]
  const first = instance.handleCommand(task)
  await instance.handleCommand(task)
  assert.equal(calls, 1)
  response.resolve({ success: false })
  await first
  assert.equal(instance.list.length, 1)
  assert.equal(instance.processingIds.length, 0)
  assert.equal(messages.length, 0)
})

test('worker loaders and login always release loading after rejected requests', async () => {
  const fail = async () => { throw new Error('offline') }
  for (const [file, method, api] of [
    ['components/layout/Header.vue', 'loadList', 'ListDailyWork'],
    ['views/Activity.vue', 'loadList', 'ListAllActivity'],
    ['views/Login.vue', 'postLogin', 'Login'],
  ]) {
    const { instance } = component(file, { [api]: fail })
    await instance[method]()
    assert.equal(instance.loading, false, file)
  }
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
test('customer-service history and active-user rows tolerate deleted user records', () => {
  renderWithRows('views/Phone.vue', { list: [{ user: null }] }, { user: null })
  renderWithRows('views/Leaving.vue', { leavingMessageList: [{ user: null, leavingMessage: {} }] })
})
