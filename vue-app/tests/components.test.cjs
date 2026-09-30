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

test('movie search returns an empty list immediately for blank input without a request', async () => {
  let calls = 0
  const { instance } = component('components/layout/Header.vue', { SearchFilm: () => { calls += 1 } })
  let result
  await instance.querySearchAsync('  ', list => { result = list })
  assert.equal(calls, 0)
  assert.deepEqual(plain(result), [])
})

test('movie search ignores out-of-order, cleared and destroyed responses', async () => {
  const requests = []
  const { instance, options } = component('components/layout/Header.vue', {
    SearchFilm: () => { const request = deferred(); requests.push(request); return request.promise },
  })
  const seen = []
  const first = instance.querySearchAsync('old', list => seen.push(plain(list)))
  const second = instance.querySearchAsync('new', list => seen.push(plain(list)))
  requests[1].resolve({ success: true, data: [{ id: 'new', name: 'New movie' }] })
  await second
  requests[0].resolve({ success: true, data: [{ id: 'old', name: 'Old movie' }] })
  await first
  assert.deepEqual(seen, [[{ id: 'new', value: 'New movie' }]])
  const third = instance.querySearchAsync('to clear', list => seen.push(plain(list)))
  await instance.querySearchAsync('', list => seen.push(plain(list)))
  requests[2].resolve({ success: true, data: [{ id: 3, name: 'stale' }] })
  await third
  assert.deepEqual(seen[1], [])
  const fourth = instance.querySearchAsync('to destroy', list => seen.push(plain(list)))
  options.beforeDestroy.call(instance)
  requests[3].resolve({ success: true, data: [{ id: 4, name: 'destroyed' }] })
  await fourth
  assert.equal(seen.length, 2)
})

test('movie search releases autocomplete after current request errors', async () => {
  const { instance } = component('components/layout/Header.vue', {
    SearchFilm: async () => { throw new Error('offline') },
  })
  let result
  await instance.querySearchAsync('film', list => { result = list })
  assert.deepEqual(plain(result), [])
})

function cartFixture(mocks = {}) {
  const carts = ['one', 'two', 'three'].map((id, index) => ({ cart: { id, price: index + 1, status: 0 } }))
  const created = [], deleted = []
  const fixture = component('views/me/Cart.vue', {
    ListCarts: async () => ({ success: true, data: carts }),
    CreateOrder: async data => { created.push(data); return { success: true } },
    DeleteCartById: async id => { deleted.push(id); return { success: true } },
    ...mocks,
  })
  fixture.instance.cartList = carts.slice()
  fixture.instance.selectList = carts.map(row => ({ checked: true, cart: row.cart }))
  fixture.instance.checkOut()
  return { ...fixture, carts, created, deleted }
}

test('checkout pays every selected cart without mutating cart status and resets selection', async () => {
  const { instance, created, deleted, carts, source } = cartFixture()
  await instance.submitCart()
  assert.equal(instance.centerDialogVisible, true)
  await instance.submitPay()
  assert.deepEqual(created.map(cart => cart.id), ['one', 'two', 'three'])
  assert.ok(created.every(cart => cart.status === 2))
  assert.ok(carts.every(row => row.cart.status === 0))
  assert.deepEqual(deleted, [], 'successful create atomically consumes the server cart')
  assert.equal(instance.submitting, false)
  assert.equal(instance.centerDialogVisible, false)
  assert.equal(instance.selectList.length, 0)
  assert.equal(instance.price, 0)
  assert.match(source, /演示支付/)
})

test('checkout deferred payment creates all pending orders; closing confirmation creates none', async () => {
  for (const action of ['cancel', 'close']) {
    const { instance, created } = cartFixture()
    instance.$confirm = () => Promise.reject(action)
    await instance.submitCart()
    assert.equal(created.length, action === 'cancel' ? 3 : 0)
    assert.ok(created.every(cart => cart.status === 0))
    assert.equal(instance.centerDialogVisible, false)
    assert.equal(instance.checkoutConfirming, false)
    assert.equal(instance.pendingCarts.length, 0)
  }
})

test('checkout ignores duplicate submissions while a request is running', async () => {
  const response = deferred()
  let calls = 0
  const { instance } = cartFixture({ CreateOrder: () => { calls += 1; return response.promise } })
  instance.pendingCarts = [{ id: 'one' }]
  const first = instance.submitPay()
  await instance.submitPay()
  assert.equal(calls, 1)
  response.resolve({ success: true })
  await first
  assert.equal(instance.submitting, false)
})

test('partial checkout only removes successful items and warns instead of claiming success', async () => {
  const { instance, deleted, messages } = cartFixture({
    CreateOrder: async cart => {
      if (cart.id === 'one') return { success: true }
      if (cart.id === 'two') return { success: false }
      throw new Error('offline')
    },
  })
  await instance.submitCart()
  await instance.submitPay()
  assert.deepEqual(deleted, [], 'failed order creation must not delete a cart')
  assert.deepEqual(plain(instance.cartList.map(row => row.cart.id)), ['two', 'three'])
  assert.equal(messages[0].type, 'warning')
  assert.match(messages[0].message, /2 个未成功/)
  assert.equal(instance.submitting, false)
})

test('failed refresh cannot resubmit an already created order during this view session', async () => {
  const { instance, created } = cartFixture({ ListCarts: async () => { throw new Error('offline') } })
  await instance.submitCart()
  await instance.submitPay()
  await instance.loadCarts()
  assert.equal(instance.cartList.length, 0)
  assert.equal(instance.selectList.length, 0)
  await instance.submitCart()
  assert.equal(created.length, 3)
})

test('deleting a cart keeps selections aligned and recalculates the total', async () => {
  const { instance } = cartFixture()
  await instance.handleDelete(1)
  assert.deepEqual(instance.cartList.map(row => row.cart.id), ['one', 'three'])
  assert.deepEqual(instance.selectList.map(row => row.cart.id), ['one', 'three'])
  assert.equal(instance.price, 4)
  assert.equal(instance.checkAll, true)
})

test('cart and order loading states reset after network errors', async () => {
  const fail = async () => { throw new Error('offline') }
  const cart = cartFixture({ ListCarts: fail })
  await cart.instance.loadCarts()
  assert.equal(cart.instance.loading, false)
  const { instance } = component('views/me/Order.vue', { FindOrderByUser: fail, PayForOrder: fail })
  await instance.loadOrder()
  assert.equal(instance.loading, false)
  instance.payOrderId = 'order'
  instance.centerDialogVisible = true
  await instance.submitPay()
  assert.equal(instance.paying, false)
  assert.equal(instance.centerDialogVisible, true)
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
test('deleted films, arrangements and users render safely; stale carts remain removable but unselectable', async () => {
  const row = { cart: { id: 'stale', price: 10 }, film: null, arrangement: null }
  const { instance, deleted, created } = cartFixture({ ListCarts: async () => ({ success: true, data: [row] }) })
  await instance.loadCarts()
  instance.checkAll = true
  instance.handleCheckAll()
  assert.equal(instance.selectList[0].checked, false)
  assert.equal(instance.price, 0)
  renderWithRows('views/me/Cart.vue', { cartList: [row], selectList: instance.selectList })
  await instance.submitCart()
  assert.equal(created.length, 0)
  await instance.handleDelete(0)
  assert.deepEqual(deleted, ['stale'])
  renderWithRows('views/me/Order.vue', { orderList: [{ order: { id: 'order', status: 0 }, film: null, arrangement: null }] })
  renderWithRows('views/Leaving.vue', { leavingMessageList: [{ user: null, leavingMessage: {} }] })
  renderWithRows('views/film/Info.vue', { evaluateList: [{ user: null, filmEvaluate: {} }] })
})
