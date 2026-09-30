/**
 * Shared request policy, kept local so each frontend can be installed independently.
 * Preserve the API's resolved business-error envelopes for existing callers; HTTP
 * and network failures still reject and are always reported in one place.
 */
module.exports = function createHttpClient(axios, options) {
  const { baseURL, storage, notify, onUnauthorized, onForbidden } = options
  const service = axios.create({ baseURL, timeout: 15000 })

  service.interceptors.request.use(request => {
    // Read at send time: logging in/out must not require a browser refresh.
    const token = storage.getItem('token')
    request.authTokenSnapshot = token
    request.headers = request.headers || {}
    if (typeof request.headers.set === 'function') {
      if (token) request.headers.set('Authorization', token)
      else request.headers.delete('Authorization')
    } else if (token) {
      request.headers.Authorization = token
    } else {
      delete request.headers.Authorization
      delete request.headers.authorization
    }
    return request
  })

  function reportFailure(code, message, request) {
    notify.error({
      title: code ? `错误提示: ${code}` : '网络错误',
      message: message || '请求失败，请稍后重试',
    })
    // An old request must not log out or redirect a newer signed-in session.
    const isCurrentSession = request && request.authTokenSnapshot === storage.getItem('token')
    if (isCurrentSession && Number(code) === 401 && onUnauthorized) onUnauthorized()
    if (isCurrentSession && Number(code) === 403 && onForbidden) onForbidden()
  }

  service.interceptors.response.use(response => {
    const result = response.data
    // The upload endpoint intentionally returns a plain URL, not an envelope.
    if (!result || typeof result !== 'object' || typeof result.success !== 'boolean') return result
    if (result.success) {
      if (result.msg) notify.success({ title: '操作成功', message: result.msg })
    } else {
      reportFailure(result.code, result.msg, response.config)
    }
    return result
  }, error => {
    if (!axios.isCancel(error)) {
      const response = error.response
      const result = response && response.data
      reportFailure(
        response && response.status,
        (result && result.msg) || (error.code === 'ECONNABORTED'
          ? '请求超时，请稍后重试'
          : '无法完成请求，请检查网络连接后重试'),
        error.config,
      )
    }
    return Promise.reject(error)
  })

  return service
}
