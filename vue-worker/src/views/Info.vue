<template>
  <section class="statistics">
    <el-alert title="演示数据：以下销量、评论和热度为静态示例，并非实时订单统计" type="info" :closable="false" show-icon />
    <div ref="chart" class="statistics-chart"></div>
  </section>
</template>

<script>
import * as echarts from 'echarts';
import 'echarts/theme/v5';

export default {
  name: 'Chart',
  created() {
    // Keep the ECharts instance outside Vue's reactive observation.
    this.chart = null
    this.chartResizeObserver = null
  },
  mounted() {
    this.initChart()
    window.addEventListener('resize', this.resizeChart)
    if (typeof ResizeObserver !== 'undefined') {
      this.chartResizeObserver = new ResizeObserver(this.resizeChart)
      this.chartResizeObserver.observe(this.$refs.chart)
    }
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.resizeChart)
    if (this.chartResizeObserver) this.chartResizeObserver.disconnect()
    if (this.chart) this.chart.dispose()
    this.chartResizeObserver = null
    this.chart = null
  },
  methods: {
    resizeChart() {
      if (this.chart) this.chart.resize()
    },
    initChart() {
      if (this.chart) this.chart.dispose()
      this.chart = echarts.init(this.$refs.chart, 'v5')

      const option = {
        title: {
          text: '订单信息统计（演示）'
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
            fontSize: 12
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

      this.chart.setOption(option)
    }
  }
}
</script>

<style scoped>
.statistics {
  padding: 24px;
}
.statistics-chart {
  width: 100%;
  height: 650px;
  max-height: calc(100vh - 160px);
  min-height: 350px;
}
</style>
