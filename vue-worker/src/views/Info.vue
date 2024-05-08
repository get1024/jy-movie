<!-- <template>
  <div class="main">

    <el-table v-loading="loading" :data="orders" style="width: 100%">
      <el-table-column type="expand">
        <template slot-scope="props">
          <el-form label-position="left" inline class="demo-table-expand">
            <el-form-item label="订单ID：">
              <span>{{ props.row.order.id }}</span>
            </el-form-item>
            <el-form-item label="用户ID：">
              <span>{{ props.row.user.id }}</span>
            </el-form-item>
            <el-form-item label="电影ID：">
              <span>{{ props.row.film.id }}</span>
            </el-form-item>
            <el-form-item label="场次ID：">
              <span>{{ props.row.arrangement.id }}</span>
            </el-form-item>
            <el-form-item label="电影名：">
              <span>《 {{ props.row.film.name }} 》</span>
            </el-form-item>
            <el-form-item label="座位号：">
              <span>{{ props.row.order.seats }}</span>
            </el-form-item>
            <el-form-item label="订单金额：">
              <span>{{ props.row.order.price }}</span>
            </el-form-item>
            <el-form-item label="下单时间：">
              <span>{{ props.row.order.createAt }}</span>
            </el-form-item>
            <el-form-item label="支付时间：">
              <span>{{ props.row.order.payAt }}</span>
            </el-form-item>
          </el-form>
        </template>
      </el-table-column>
      <el-table-column label="订单 ID" width="400" prop="order.id">
      </el-table-column>
      <el-table-column width="200" label="电影名" prop="film.name">
      </el-table-column>
      <el-table-column width="200" label="订购座位" prop="order.seats">
      </el-table-column>
      <el-table-column width="200" label="订单金额" prop="order.price">
      </el-table-column>
      <el-table-column label="订单状态">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.order.status === 2" type="success">支付成功</el-tag>
          <el-tag v-if="scope.row.order.status === 0" type="info">等待支付</el-tag>
          <el-tag v-if="scope.row.order.status === 3" type="warning">已被撤销</el-tag>
          <el-tag v-if="scope.row.order.status === 1" type="danger">支付失败</el-tag>
        </template>
      </el-table-column>
    </el-table>

  </div>
</template>

<script>
import { CreateOrderException, FindAllOrder, UpdateOrder } from "@/api/order";

export default {

  data() {
    return {
      loading: false,
      orders: [],
    }
  },

  mounted() {
    this.loadOrderList()
  },

  methods: {

    loadOrderList() {
      this.loading = true
      FindAllOrder().then(res => {
        if (res.success) {
          setTimeout(() => {
            this.orders = res.data
            this.loading = false
          }, 700)
        }
      })
    },

  }

}
</script>

<style scoped>
.main {
  padding: 30px;
}

.demo-table-expand {
  font-size: 0;
}

.demo-table-expand label {
  width: 90px;
  color: #99a9bf;
}

.demo-table-expand .el-form-item {
  margin-right: 0;
  margin-bottom: 0;
  width: 100%;
}
</style> -->

<template>
  <div id="chart" style="width: 1400px; height: 750px;"></div>
</template>

<script>
import * as echarts from 'echarts';

export default {
  name: 'Chart',
  mounted() {
    this.initChart()
  },
  methods: {
    initChart() {
      const chart = echarts.init(document.getElementById('chart'))

      const option = {
        title: {
          text: '订单信息统计'
        },
        tooltip: {
          trigger: 'axis'
        },
        legend: {
          data: ['销量','评论', '热度']
        },
        xAxis: {
          type: 'category',
          data: ['我的姐姐','我们一起摇太阳','人潮汹涌', '哥斯拉大战金刚', '功夫熊猫4','第二十条','红雀', '你好，李焕英','灿烂的她', '刺杀小说家'],
          axisLabel: {
            inside: false,
            color: 'black',
            textstyle: {
              fontSize: '100',
              itemSize:'',
            }
          },
          axisTick: {
            show: true
          },
          axisLine: {
            show: true
          },
        },
        yAxis: {
          type: 'value',
          axisLine: {
            show: false
          },
          axisTick: {
            show: false
          },
          axisLabel: {
            color: '#999'
          }
        },
        dataZoom: [
          {
            type: 'inside'
          }
        ],
        series: [
          {
            name: '销量',
            type: 'bar',
            data: [100, 14, 87, 45, 61, 3, 37, 90, 56, 26],
            type: 'bar',
            showBackground: true,
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#83bff6' },
                { offset: 0.5, color: '#188df0' },
                { offset: 1, color: '#188df0' }
              ])
            },
            emphasis: {
              itemStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                  { offset: 0, color: '#2378f7' },
                  { offset: 0.7, color: '#2378f7' },
                  { offset: 1, color: '#83bff6' }
                ])
              }
            },
          },
          {
            name: '评论',
            type: 'bar',
            emphasis: {
              focus: 'series'
            },
            data: [66, 12, 40, 21, 30,1,15,46,24,11]
          },
          {
            name: '热度',
            type: 'bar',
            emphasis: {
              focus: 'series'
            },
            data: [166, 26, 127, 66, 91,4,52,136,80,37]
          }
        ]
      }

      chart.setOption(option)
    }
  }
}
</script>