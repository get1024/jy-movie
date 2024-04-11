<template>
  <div class="header">
    <div class="a">
      <div class="header-avatar">
        <el-dropdown>
          <el-button type="text">
            <div class="header-name">Admin</div>
            <img style="margin-top: 5px" width="28.9999px" src="../../assets/img/admin.png"></img>
          </el-button>
          <el-dropdown-menu slot="dropdown">
            <!-- admin设置界面，由于upload有bug，决定不给admin这个功能 -->
            <!-- <el-dropdown-item>
              <el-link :underline="false" href="/setting">
                <i style="font-size: 15px; padding-right: 3px" class="el-icon-setting"></i>个人设置
              </el-link>
            </el-dropdown-item> -->
            <el-dropdown-item>
              <el-button type="text" @click="handleLogout">
                <i style="font-size: 15px; padding-right: 3px" class="el-icon-switch-button"></i>退出登录
              </el-button>
            </el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
      </div>

      <div class="header-icon" style="padding-top: 16px">
        <el-dropdown>
          <el-badge :value="list.length" class="item">
            <i style="font-size: 18px" class="el-icon-bell"></i>
          </el-badge>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item v-for="(item, index) in list" :key="index">{{'任务：'+ item.content +'，重要等级：'}}
              <span v-if="item.type === 1" style="color: #55ff2b;font-weight: bold;">普通</span>
              <span v-else-if="item.type === 2" style="color: #ffa500; font-weight: bold;">重要</span>
              <span v-else-if="item.type === 3" style="color: #ff1100;font-weight: bold;">非常重要</span>
            </el-dropdown-item>
            <el-dropdown-item v-if="list.length === 0">今日工作还没有上传</el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
      </div>
      <div class="header-icon">
        <el-tooltip content="JY影院 - 管理系统" placement="top">
          <i style="font-size: 20px" class="el-icon-warning-outline"></i>
        </el-tooltip>
      </div>
    </div>
  </div>
</template>

<script>
import { ListDailyWork } from "@/api/worker";

export default {
  name: "Header",

  data() {
    return {
      list: []
    }
  },

  mounted() {
    ListDailyWork().then(res => {
      setTimeout(() => {
        this.list = res.data
      }, 700)
    })
  },


  methods: {

    handleLogout() {
      localStorage.removeItem("token")
      localStorage.removeItem("role")
      localStorage.removeItem("wid")
      this.$router.push("/login")
    }

  },

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
