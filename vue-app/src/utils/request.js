import axios from 'axios'
import { Notification } from 'element-ui'
import config from '@/config'
import router from '@/router'
import createHttpClient from './http-client.cjs'

function redirect(path) {
  if (router.currentRoute.path !== path) {
    // Concurrent failed requests may request the same navigation.
    router.replace(path).catch(() => {})
  }
}

export default createHttpClient(axios, {
  baseURL: config.API_URL,
  storage: localStorage,
  notify: Notification,
  onUnauthorized: () => {
    ;['token', 'uid', 'wid', 'role'].forEach(key => localStorage.removeItem(key))
    redirect('/login')
  },
})
