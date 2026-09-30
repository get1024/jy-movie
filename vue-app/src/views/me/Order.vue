<template>
  <div style="width: 100%">
    <h3 style="letter-spacing: 1px;font-weight: 400;padding-bottom: 20px">我的订单</h3>

    <div v-loading="loading">
      <el-card v-for="(item, index) in orderList" :key="index" class="box-card">
        <div>
          <img v-if="item.film" class="item-film-img" alt="" :src="item.film.cover"/>
          <div style="float: left;">
            <div class="item-film-name">《{{ item.film ? item.film.name : '电影已下架' }}》</div>
            <div class="item-film-seat">订购座位 : {{ item.order.seats }}</div>
            <div class="item-film-time">下单时间 : {{ item.order.createAt }}</div>
            <div v-if="item.order.status === 2" class="item-film-time">支付时间 : {{ item.order.payAt }}</div>
          </div>
          <el-button type="text"
                     @click="handlePay(item.order, index)"
                     v-if="item.order.status === 0 && item.arrangement"
                     style="line-height: 75px"
                     class="o1">
            等待支付
          </el-button>
          <div v-if="item.order.status === 0 && !item.arrangement" class="o1">场次已删除，请联系客服</div>
          <el-tooltip class="item" effect="dark" content="请联系工作人员查询您的订单" placement="top">
            <div v-if="item.order.status === 3" style="color: #E6A23C" class="o1">订单异常</div>
          </el-tooltip>
          <div v-if="item.order.status === 2" style="color: #67C23A" class="o1">支付成功</div>
          <div v-if="item.order.status === 1" style="color: #F56C6C" class="o1">订单超时</div>
          <div class="o2">￥{{ item.order.price }}</div>
        </div>
      </el-card>
    </div>

    <el-dialog
        title="模拟支付（演示）"
        :show-close="!paying"
        :close-on-click-modal="!paying"
        :close-on-press-escape="!paying"
        width="30%"
        :visible.sync="centerDialogVisible"
    >
      <el-alert title="当前为演示支付，不会发生真实扣款，也不会验证到账" type="warning" :closable="false" show-icon />
      <span slot="footer" class="dialog-footer">
    <el-button type="primary" :loading="paying" @click="submitPay">确认模拟支付</el-button>
  </span>
    </el-dialog>

  </div>
</template>

<script>
import {FindOrderByUser, PayForOrder} from "@/api/order";

export default {
  name: "Order",

  data() {
    return {
      centerDialogVisible: false,
      loading: false,
      orderList: [],
      payOrderId: '',
      paying: false,
    }
  },

  mounted() {
    this.loadOrder()
  },

  methods: {

    async loadOrder() {
      this.loading = true
      try {
        const res = await FindOrderByUser(localStorage.getItem("uid"))
        if (res.success) this.orderList = Array.isArray(res.data) ? res.data : []
      } catch (error) {
        // The request interceptor reports transport errors.
      } finally {
        this.loading = false
      }
    },

    handlePay(order, index) {
      this.open(order, index);
    },

    open(order, index) {
      this.$confirm('请确认订单金额为 ' + order.price + ' 元（演示支付，不会真实扣款）', '提示', {
        confirmButtonText: '模拟支付',
        cancelButtonText: '取消支付',
        type: 'success',
        center: true
      }).then(() => {
        this.centerDialogVisible = true
        this.payOrderId = order.id
      }).catch(() => {
        this.$message({
          type: 'warning',
          message: '用户已取消支付'
        });
      });
    },

    async submitPay() {
      if (this.paying || !this.payOrderId) return
      this.paying = true
      try {
        const res = await PayForOrder(this.payOrderId)
        if (!res.success) return
        this.centerDialogVisible = false
        this.$message({ type: 'success', message: '模拟支付成功' })
        await this.loadOrder()
      } catch (error) {
        // Keep the dialog available for retry.
      } finally {
        this.paying = false
      }
    },

  },

}
</script>

<style scoped>

.box-card {
  margin-bottom: 10px;
}

>>> .el-card__header {
  background: #C0C4CC;
}

.item-film-img {
  float: left;
  width: 80px;
  height: 100px;
  padding-bottom: 10px;
  padding-right: 10px;
}

.item-film-name {
  letter-spacing: 2px;
  font-weight: 500;
  font-size: 18px;
  padding-bottom: 15px;
  padding-top: 1px;
}

.c-img {
  width: 100%;
  height: 100%;
}

.item-film-seat {
  letter-spacing: 1px;
  font-size: 12px;
  padding-bottom: 5px;
  padding-left: 10px;
  color: #91949c;
}


.item-film-time {
  letter-spacing: 1px;
  font-size: 12px;
  padding-left: 10px;
  padding-bottom: 5px;
  color: #91949c;
}

.o1 {
  float: right;
  line-height: 100px;
  padding-right: 20px
}

.o2 {
  font-size: 20px;
  float: right;
  line-height: 100px;
  padding-right: 100px
}
</style>