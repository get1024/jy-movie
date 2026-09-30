<template>
  <div style="width: 100%">
    <h3 style="letter-spacing: 1px;font-weight: 400;padding-bottom: 20px">购物车</h3>

    <div v-loading="loading">

      <el-card v-for="(item, index) in cartList" :key="item.cart.id" class="box-card">
        <div>
          <div style="float: left;line-height: 100px;padding-right: 20px">
            <el-checkbox :disabled="submitting || checkoutConfirming || !isAvailable(item)" @change="handleCheck" v-model="selectList[index].checked"></el-checkbox>
          </div>
          <img v-if="item.film" class="item-film-img" alt="" :src="item.film.cover" />
          <div style="float: left;">
            <div class="item-film-name">《{{ item.film ? item.film.name : '电影已下架' }}》</div>
            <div class="item-film-seat">座位 : {{ item.cart.seats }}</div>
            <div class="item-film-seat">手机号码 : {{ item.cart.phone }}</div>
            <div class="item-film-time">开场时间 :
              <span v-if="item.arrangement" style="color: red">{{ item.arrangement.date }} - {{ item.arrangement.startTime }}</span>
              <span v-else style="color: red">场次已删除，请移除此购物车记录</span>
            </div>
          </div>
          <div style="float: right;color: #f34d41;letter-spacing: 2px; line-height: 100px;padding-right: 5px">
            ￥{{ item.cart.price }}
            <el-button :disabled="submitting || checkoutConfirming" @click="handleDelete(index)" type="text" style="color: red;padding-left: 50px"
              icon="el-icon-delete">删除
            </el-button>
          </div>
        </div>
      </el-card>

      <div v-if="cartList.length !== 0" style="height: 60px;background: #91949c">
        <div>
          <div style="float: left;line-height: 60px;padding: 0 20px;font-size: 14px">
            <el-checkbox :disabled="submitting || checkoutConfirming" @change="handleCheckAll" v-model="checkAll">全 选</el-checkbox>
          </div>
          <el-button :loading="submitting" :disabled="loading || checkoutConfirming" @click="submitCart" type="text" class="sub-btn">全部结算</el-button>
          <div class="order-footer1">合计 : ￥{{ price }}</div>
        </div>
      </div>
    </div>

    <el-dialog title="模拟支付（演示）" :show-close="false" :close-on-click-modal="false"
      :close-on-press-escape="false" width="30%" :visible.sync="centerDialogVisible">
      <el-alert title="当前为演示支付，不会发生真实扣款，也不会验证到账" type="warning" :closable="false" show-icon />
      <span slot="footer" class="dialog-footer">
        <el-button :disabled="submitting" @click="centerDialogVisible = false; pendingCarts = []">返回购物车</el-button>
        <el-button type="primary" :loading="submitting" @click="submitPay">确认模拟支付</el-button>
      </span>
    </el-dialog>

  </div>
</template>

<script>
import { DeleteCartById, ListCarts } from "@/api/cart"
import { CreateOrder } from "@/api/order";

export default {
  data() {
    return {
      pendingCarts: [],
      createdCartIds: [],
      submitting: false,
      checkoutConfirming: false,
      centerDialogVisible: false,
      loading: false,
      price: 0,
      selectList: [],
      checkAll: false,
      cartList: [],
      uid: localStorage.getItem("uid"),
    }
  },

  mounted() {
    this.loadCarts()
  },

  methods: {

    async loadCarts() {
      this.loading = true
      try {
        const res = await ListCarts(this.uid)
        if (!res.success) return
        const carts = Array.isArray(res.data) ? res.data : []
        // Never offer an already-created order again if a refresh is stale.
        this.cartList = carts.filter(item => !this.createdCartIds.includes(item.cart.id))
        this.selectList = this.cartList.map(item => ({ checked: false, cart: item.cart, available: this.isAvailable(item) }))
        this.checkAll = false
        this.price = 0
      } catch (error) {
        // Keep the previous list if the refresh fails.
      } finally {
        this.loading = false
      }
    },

    isAvailable(item) {
      return Boolean(item && item.film && item.arrangement)
    },

    handleCheck() {
      this.checkOut()
    },

    handleCheckAll() {
      this.changeAllChecked(this.checkAll)
      this.checkOut()
    },

    changeAllChecked(status) {
      this.selectList.forEach(item => { item.checked = status && item.available !== false })
    },

    checkOut() {
      this.price = this.selectList.reduce((total, item) => {
        return total + (item.checked && item.available !== false ? Number(item.cart.price) || 0 : 0)
      }, 0)
      const available = this.selectList.filter(item => item.available !== false)
      this.checkAll = available.length > 0 && available.every(item => item.checked)
    },

    async handleDelete(index) {
      if (this.submitting || this.checkoutConfirming) return
      const item = this.cartList[index]
      if (!item) return
      try {
        const res = await DeleteCartById(item.cart.id)
        if (!res.success) return
        this.cartList = this.cartList.filter(row => row.cart.id !== item.cart.id)
        this.selectList = this.selectList.filter(row => row.cart.id !== item.cart.id)
        this.checkOut()
      } catch (error) {
        // Preserve both the cart and its selection after a failed deletion.
      }
    },

    async submitCart() {
      if (this.loading || this.submitting || this.checkoutConfirming || this.centerDialogVisible) return
      this.pendingCarts = this.selectList
        .filter(item => item.checked && item.available !== false && !this.createdCartIds.includes(item.cart.id))
        .map(item => ({ ...item.cart }))
      if (!this.pendingCarts.length) {
        this.$message({ type: 'warning', message: '请先选择需要结算的电影票' })
        return
      }
      this.checkoutConfirming = true
      try {
        await this.$confirm('请确认订单金额为 ' + this.price + ' 元（演示支付，不会真实扣款）', '提示', {
          confirmButtonText: '模拟支付',
          cancelButtonText: '稍后支付',
          distinguishCancelAndClose: true,
          type: 'warning',
          center: true
        })
        this.centerDialogVisible = true
      } catch (action) {
        if (action === 'cancel') await this.completeCheckout(0)
        else this.pendingCarts = []
      } finally {
        this.checkoutConfirming = false
      }
    },

    submitPay() {
      return this.completeCheckout(2)
    },

    async completeCheckout(status) {
      if (this.submitting || !this.pendingCarts.length) return
      this.submitting = true
      const carts = this.pendingCarts.slice()
      let created = 0
      let failed = 0
      try {
        for (const cart of carts) {
          try {
            const res = await CreateOrder({ ...cart, status })
            if (!res.success) {
              failed += 1
              continue
            }
            created += 1
            this.createdCartIds.push(cart.id)
            // Order creation consumes the source cart in the same server transaction.
            this.cartList = this.cartList.filter(row => row.cart.id !== cart.id)
            this.selectList = this.selectList.filter(row => row.cart.id !== cart.id)
            this.checkOut()
          } catch (error) {
            failed += 1
          }
        }
        this.$message({
          type: failed ? 'warning' : 'success',
          message: '已创建 ' + created + ' 个' + (status === 2 ? '模拟支付订单' : '待支付订单') +
            (failed ? '，' + failed + ' 个未成功，请检查我的订单后再重试' : '')
        })
      } finally {
        this.pendingCarts = []
        this.centerDialogVisible = false
        await this.loadCarts()
        this.submitting = false
      }
    },

  },

}
</script>

<style scoped>
.box-card {
  margin-bottom: 5px;
}

>>>.el-card__header {
  background: #C0C4CC;
}

.item-film-img {
  float: left;
  width: 80px;
  height: 100px;
  padding-bottom: 20px;
  padding-right: 10px;
}

.item-film-name {
  letter-spacing: 2px;
  font-weight: 500;
  font-size: 18px;
  padding-bottom: 13px;
}

.item-film-seat {
  letter-spacing: 1px;
  font-size: 12px;
  padding-bottom: 8px;
  padding-left: 10px;
  color: #91949c;
}


.item-film-time {
  font-size: 12px;
  padding-left: 10px;
  letter-spacing: 1px;
  color: #91949c;
}

.c-img {
  width: 100%;
  height: 100%;
}

.sub-btn {
  float: right;
  color: #000000;
  height: 60px;
  width: 120px;
  background: #f34d41;
  letter-spacing: 2px;
  line-height: 33px;
  text-align: center;
  font-weight: 500;
  font-size: 18px;
}

.order-footer1 {
  float: right;
  line-height: 60px;
  padding-right: 40px;
}

a {
  color: #333333;
}

.di {
  text-align: center;
  font-size: 20px;
  letter-spacing: 3px;
  font-weight: bold;
  padding-top: 10px;
}
</style>