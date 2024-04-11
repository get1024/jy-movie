/*
 * @Date: 2024-03-26 10:23:36
 * @LastEditors: get1024 junyeren@outlook.com
 * @LastEditTime: 2024-04-11 10:41:24
 * @FilePath: \vue-app\src\main.js
 */
import Vue from 'vue'
import App from './App.vue'
import router from './router'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'

Vue.use(ElementUI)

new Vue({
  router,
  render: (h) => h(App),
}).$mount('#app')
