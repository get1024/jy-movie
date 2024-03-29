# 项目参考文档

基于SpringBoot的前后端分离的电影购票系统

## 介绍

SpringBoot+SpringSecurity+MyBatis-Plus 框架的前后端电影项目，前端使用vue+element ui，后端SpringBoot。
前台主要有根据类型和地区进行电影的筛选、电影的搜索、热门榜单、活动的发布、留言的发布、电影的购票、电影的在线选座位、我的购物车、我的订单等功能。
后台主要有影片的增删查改、电影的拍片、海报的管理、用户和员工的管理等等功能。

## 软件架构

前后端分离项目

## 关于

- api 目录下为Spring boot项目代码，是整个项目的后端接口
- vue-admin 目录下为网站管理后台代码
- vue-app 目录下为网站前台代码
- vue-worker 目录下为网站客服代码

- model数据、view视图、controller接口、三层

## 改动日志

### 2024年03月26日 18:37:48

1. admin和worker端任务互通（原本显示的是new，现在只要新增任务就会实时更新）；

   ![Image text](https://github.com/get1024/jy-movie/blob/main/readme_assets/3568e16f0274d59fedbc7215e578a0a.png)![Image text](https://github.com/get1024/jy-movie/blob/main/readme_assets/94c4b700faa932878d0c3397ba74d82.png)

2. app首页轮播图样式修改（我觉得这样好看)；

   ![Image text](https://github.com/get1024/jy-movie/blob/main/readme_assets/8f5932eef4305840b2fe8082494f5f3.png)

3. 热门榜单的样式优化、后端数据库热度调整；

   ![Image text](https://github.com/get1024/jy-movie/blob/main/readme_assets/5bec3bebf6bbf54e5590e5b446672ca.png)

4. 留言reply功能优化；

   ![Image text](https://github.com/get1024/jy-movie/blob/main/readme_assets/870cab076bb917b8baada674d0cd92d.png)

5. admin个人设置功能删除（对应router中的‘setting’和setting.vue，这些我都注释了，没有删掉，后续可以解开）；

   ![Image text](https://github.com/get1024/jy-movie/blob/main/readme_assets/4968933f3fca7184dde79ad16d15ec8.png)

## 未修复bug

### admin

1. `vue-admin\src\components\layout\Aside.vue`上对员工列表的请求会报403，目前不清楚原因以及解决方法；
2. `vue-admin\src\views\film\Poster.vue`上传轮播图海报，上传一个需要刷新一次界面（为了让数据出去，方便下一次上传的数据占用）；如果不进行刷新，会报数据类型不匹配错误，实际上代码是没问题的。具体怎么彻底解决也没有头绪；

### worker

### app