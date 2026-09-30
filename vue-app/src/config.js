// Vue CLI replaces VUE_APP_* variables at build time. Never put secrets here.
export default {
  API_URL: process.env.VUE_APP_API_URL || 'http://localhost:8888/api',
}
