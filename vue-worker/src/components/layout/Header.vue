<template>
  <div class="header">
    <div class="a">
      <div class="header-avatar">
        <el-dropdown>
          <el-button type="text">
            <div class="header-name">{{ worker.nickname }}</div>
            <img style="margin-top: 5px;width: 29px;height: 29px;border-radius: 50%" :src="worker.avatar" alt="null" />
          </el-button>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item>
              <el-link :underline="false" href="/setting">
                <i style="font-size: 15px; padding-right: 3px" class="el-icon-setting"></i>个人设置
              </el-link>
            </el-dropdown-item>
            <el-dropdown-item divided>
              <el-button type="text" @click="handleLogout">
                <i style="font-size: 15px; padding-right: 3px" class="el-icon-switch-button"></i>退出登录
              </el-button>
            </el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
      </div>

      <div class="header-icon" style="padding-top: 16px">
        <el-dropdown @command="handleCommand">
          <el-badge :value="list.length" class="item">
            <i style="font-size: 18px" class="el-icon-bell"></i>
          </el-badge>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item v-for="item in list" :key="item.id" :command="item"
              :disabled="processingIds.includes(item.id)">{{ '任务：' + item.content + '，重要等级：' }}
              <span v-if="item.type === 1" style="color: #55ff2b;font-weight: bold;">普通</span>
              <span v-else-if="item.type === 2" style="color: #ffa500; font-weight: bold;">重要</span>
              <span v-else-if="item.type === 3" style="color: #ff1100;font-weight: bold;">非常重要</span>
            </el-dropdown-item>
            <el-dropdown-item v-if="list.length === 0" disabled>还没有安排今日工作</el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
      </div>
      <div class="header-icon">
        <el-tooltip content="JY影院 - 客服系统" placement="top">
          <i style="font-size: 20px" class="el-icon-warning-outline"></i>
        </el-tooltip>
      </div>
    </div>
  </div>
</template>

<script>
import { FindWorkerById, DeleteDailyWork, ListDailyWork } from "@/api/worker";

export default {
  name: "Header",

  data() {
    return {
      worker: {
        nickname: '',
        password: '',
        phone: '',
        gender: '',
        avatar: '',
        department: '',
      },
      list: [],
      loading: false,
      processingIds: []
    }
  },

  mounted() {
    FindWorkerById(localStorage.getItem("wid")).then(res => {
      this.worker = res.data;
    }).catch(() => {})
    this.loadList()
  },

  methods: {
    async loadList() {
      this.loading = true
      try {
        const res = await ListDailyWork()
        if (res.success) this.list = Array.isArray(res.data) ? res.data : []
      } catch (error) {
        // The request interceptor reports errors.
      } finally {
        this.loading = false
      }
    },
    handleLogout() {
      localStorage.removeItem("wid")
      localStorage.removeItem("token")
      this.$router.push('/login')
    },
    async handleCommand(command) {
      if (!command || !command.id || this.processingIds.includes(command.id)) return
      this.processingIds.push(command.id)
      try {
        const res = await DeleteDailyWork(command.id)
        if (!res.success) return
        // Element UI emits the command only, not the item's index.
        this.list = this.list.filter(item => item.id !== command.id)
        this.$message({
          type: 'success',
          message: command.content + ' 已被 ' + this.worker.nickname + ' 处理，已从每日任务删除记录！',
        })
      } catch (error) {
        // A failed deletion leaves the task available for retry.
      } finally {
        this.processingIds = this.processingIds.filter(id => id !== command.id)
      }
    }
  }


}
</script>

<style scoped>
.header {}

.header-icon {
  float: right;
  font-size: 20px;
  padding-top: 19px;
  padding-right: 30px;
}

.header-avatar {
  float: right;
  padding-left: 20px;
}

.header-name {
  float: right;
  padding-top: 12px;
  font-size: 15px;
  padding-right: 10px;
  padding-left: 9px;
}

.el-button {
  padding-top: 8px;
  color: #000000;
  letter-spacing: 2px;
}
</style>
