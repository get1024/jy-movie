- [ 📃一、介绍](#head1)
- [ 📱二、软件架构](#head2)
- [ 🗂️三、关于](#head3)
- [ 🐞四、未修复bug](#head4)
	- [ ⚙️admin](#head5)
	- [ 🛡️worker](#head6)
	- [ 🌐app](#head7)
- [ 🔧五、改动日志](#head8)
	- [1️⃣2024 年 03 月 26 日 18:37:48](#head9)
	- [2️⃣2024 年 03 月 29 日 23:25:42](#head10)
	- [3️⃣2024 年 03 月 30 日 17:57:28](#head11)
	- [4️⃣2024 年 04 月 01 日 18:20:39](#head12)
	- [5️⃣2024 年 04 月 08 日 17:59:25](#head13)
	- [6️⃣2024 年 04 月 11 日 12:26:33](#head14)
- [ 🙌🏼六、结尾碎碎念](#head15)

>什么项目？
>🖥️基于SpringBoot➕vue的前后端分离的电影购票系统
![app-home-vue](https://github.com/get1024/jy-movie/blob/main/readme_assets/app_home_vue.png)
# <span id="head1"> 📃一、介绍</span>
- SpringBoot+SpringSecurity+MyBatis-Plus 框架的前后端电影项目，前端使用vue+element ui，后端SpringBoot。
- 前台主要有根据类型和地区进行电影的筛选、电影的搜索、热门榜单、活动的发布、留言的发布、电影的购票、电影的在线选座位、我的购物车、我的订单等功能。
- 后台主要有影片的增删查改、电影的排片、海报的管理、用户和员工的管理等等功能。
# <span id="head2"> 📱二、软件架构</span>
- 前后端分离项目
# <span id="head3"> 🗂️三、关于</span>
- api 目录下为Spring boot项目代码，是整个项目的后端接口
- vue-admin 目录下为网站管理后台代码
- vue-app 目录下为网站前台代码
- vue-worker 目录下为网站客服代码
- model数据、view视图、controller接口、三层
# <span id="head4"> 🐞四、未修复bug</span>
## <span id="head5"> ⚙️admin</span>
1. `vue-admin\src\components\layout\Aside.vue` 上对员工列表的请求偶尔会报403，目前不清楚原因以及解决方法；
	1. 更新（2024 年 03 月 29 日 23:31:41）：报 403 应该是内存占用的问题，但是目前我还是不清楚为什么会因为内存过高而引起这个 bug🐛……
2. `vue-admin\src\views\film\Poster.vue`上传轮播图海报，上传一个需要刷新一次界面（为了让数据出去，方便下一次上传的数据占用）；如果不进行刷新，会报数据类型不匹配错误，实际上代码是没问题的。具体怎么彻底解决也没有头绪；
3. `admin` 端管理员信息修改提交显示成功，但实际后台数据并未更新，原因是一开始我偷懒想直接使用 `worker` 的 `api` 接口实现，但是后来根据 `role` 来进行角色授权、认证这一部分没有完善，就导致这一想法并没有落地。（🙌🏼：菜就多练🙈）
## <span id="head6"> 🛡️worker</span>
> 暂时没发现 bug……
## <span id="head7"> 🌐app</span>
1. 订单超时处理没效果……（后端部分代码是低代码平台基础上开发的，逻辑我还没核对完）

# <span id="head8"> 🔧五、改动日志</span>
## <span id="head9">1️⃣2024 年 03 月 26 日 18:37:48</span>
1. admin和worker端任务互通（原本显示的是new，现在只要新增任务就会实时更新）；
2. app首页轮播图样式修改（我觉得这样好看)；
3. 热门榜单的样式优化、后端数据库热度调整；
4. 留言reply 不显示回复 bug 解决；
5. admin个人设置功能删除（对应router中的‘setting’和setting.vue，这些我都注释了，没有删掉，后续可以解开）；
## <span id="head10">2️⃣2024 年 03 月 29 日 23:25:42</span>
1. 三个面板的忘记密码修改：由点击跳转改为 `<el-tooltip>` 提示，因为没做 `/forget` 页面不好意思。💔
## <span id="head11">3️⃣2024 年 03 月 30 日 17:57:28</span>
1. 删除 sql 脚本文件夹；（原因：🌟是本人的毕设项目，等毕业了再给大家嫖一下。🙏🏼）
## <span id="head12">4️⃣2024 年 04 月 01 日 18:20:39</span>
1. 解决 `vue-app --> cart.vue` 的海报不动态更新 bug；
## <span id="head13">5️⃣2024 年 04 月 08 日 17:59:25</span>
1. 三面板 `login.vue` 样式优化；
## <span id="head14">6️⃣2024 年 04 月 11 日 12:26:33</span>
1. `vue-worker` 、`vue-admin` 端 `header` 组件上 `daily_work` 的 `badge` 优化 `v-for` 结构，由 `v-for="item in list" :key="item"` 优化为 `v-for="(item,index) in list" :key="index"`。解决了前端控制台爆红警告⚠️问题。
2. 增添功能：`vue-worker` 端点击下拉框中的每日任务后，会视作客服已经处理所点击任务，将会有弹窗提示，并且调用 `api` 删除后端数据库相应任务记录。
3. `vue-worker` 和 `vue-admin` 的 `header` 组件右上角下拉框条目新增信息：显示当前的任务主要内容以及重要程度。
# <span id="head15"> 🙌🏼六、结尾碎碎念</span>
- Ok👌🏼，期待大家 `fork`。
- 个人联系方式 [personal home](https://barry-maverick-ren.notion.site/Personal-Home-38a5d924805a4afe866e2509e672fa6e?pvs=4)，有兴趣可以联系讨论交流。