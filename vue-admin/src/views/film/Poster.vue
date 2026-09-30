<template>
  <div>
    <el-carousel v-if="posterList.length !== 0" height="500px" indicator-position="outside">
      <el-carousel-item v-for="(item, index) in posterList" :key="index">
        <img alt="" :src="item.url" class="image" style="border-radius: 50px;width: 90%;margin-left: 5%">
      </el-carousel-item>
    </el-carousel>

    <div class="content">
      <el-button @click="dialogFormVisible = true" type="primary" icon="el-icon-upload">
        上传轮播海报
      </el-button>
      <el-button style="margin-left: 10px" type="danger" icon="el-icon-delete" @click="deleteAllPoster">
        删除所有海报
      </el-button>
      <el-table v-loading="loading" ref="filterTable" :data="posterList" style="width: 100%;margin-top: 15px;">
        <el-table-column prop="createAt" label="上传时间" width="220">
        </el-table-column>
        <el-table-column prop="title" label="标题" width="180">
        </el-table-column>
        <el-table-column label="地址预览">
          <template slot-scope="scope">
            <a target="_blank" :href="scope.row.url">{{ scope.row.url }}</a>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="上架状态" width="220">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.status" type="success" disable-transitions>上架
            </el-tag>
            <el-tag v-else type="warning" disable-transitions>下架
            </el-tag>
            <el-button v-if="!scope.row.status" style="padding: 0 15px;color: #67C23A" type="text"
              @click="changePosterStatus(scope.$index, scope.row, true)" icon="el-icon-upload2">上架
            </el-button>
            <el-button v-else style="padding: 0 15px;color: #E6A23C" type="text"
              @click="changePosterStatus(scope.$index, scope.row, false)" icon="el-icon-download">下架
            </el-button>
            <template style="padding-left: 100px">
              <el-popconfirm @confirm="handleDeletePoster(scope.$index, scope.row.id)" title="确定要删除这条内容吗？">
                <el-button slot="reference" style="color: #F56C6C" type="text" icon="el-icon-delete">删除
                </el-button>
              </el-popconfirm>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog title="轮播海报" :visible.sync="dialogFormVisible" @open="resetUploadForm" @close="resetUploadForm"
      :close-on-click-modal="!saving" :close-on-press-escape="!saving" :show-close="!saving">
      <el-form :model="uploadPoster">
        <el-form-item label="海报标题" style="margin-bottom: 35px" label-width="120px">
          <el-input style="width: 360px" v-model="uploadPoster.title" autocomplete="off"></el-input>
        </el-form-item>
        <el-form-item label="上架状态" style="margin-bottom: 35px" label-width="120px">
          <el-select style="width: 360px" v-model="uploadPoster.status" placeholder="请选择活动区域">
            <el-option label="上架" :value="true"></el-option>
            <el-option label="下架" :value="false"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="选择图片" label-width="120px">
          <el-upload ref="posterUpload" style="float: left" class="upload-demo" accept=".png,.jpg,.jpeg" :headers="header" :action="uploadAction"
            :before-upload="beforePosterUpload" :on-success="handleUploadSuccess" :on-error="handleUploadError"
            :on-remove="handleUploadRemove" :disabled="saving" :limit="1">
            <el-button type="primary" icon="el-icon-upload">上传轮播海报</el-button>
          </el-upload>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button :disabled="saving" @click="dialogFormVisible = false">取 消</el-button>
        <el-button type="primary" :loading="saving" :disabled="uploading || !uploadPoster.url" @click="submitUpload">确 定</el-button>
      </div>
    </el-dialog>

  </div>
</template>

<script>
import config from "@/config";
import { AddPoster, DeleteAllPoster, DeletePosterById, ListAllPoster, UpdatePoster } from "@/api/film";

const emptyPoster = () => ({ url: '', title: '', status: true, createAt: '' })

export default {
  data() {
    return {
      header: { Authorization: localStorage.getItem('token') },
      dialogFormVisible: false,
      uploadAction: config.API_URL + '/upload',
      uploadPoster: emptyPoster(),
      posterList: [],
      loading: false,
      saving: false,
      uploading: false,
      uploadFileUid: null,
      viewActive: true,
    }
  },

  mounted() {
    this.loadPosters()
  },

  beforeDestroy() {
    this.viewActive = false
    this.resetUploadForm()
  },

  methods: {
    async loadPosters() {
      this.loading = true
      try {
        const res = await ListAllPoster()
        if (res.success && this.viewActive) {
          this.posterList = Array.isArray(res.data) ? res.data : []
        }
      } catch (error) {
        // The request interceptor reports transport errors.
      } finally {
        if (this.viewActive) this.loading = false
      }
    },

    resetUploadForm() {
      // Invalidate callbacks before aborting a pending upload or clearing its file list.
      this.uploadFileUid = null
      if (this.$refs.posterUpload) {
        this.$refs.posterUpload.abort()
        this.$refs.posterUpload.clearFiles()
      }
      this.uploadPoster = emptyPoster()
      this.uploading = false
      this.header.Authorization = localStorage.getItem('token')
    },

    beforePosterUpload(file) {
      this.header.Authorization = localStorage.getItem('token')
      this.uploadFileUid = file.uid
      this.uploadPoster.url = ''
      this.uploading = true
      return true
    },

    handleUploadSuccess(res, file) {
      if (!this.viewActive || !file || file.uid !== this.uploadFileUid) return
      this.uploading = false
      // /upload returns a plain URL, whereas API errors return a JSON envelope.
      if (typeof res !== 'string' || !/^(https?:\/\/|\/)/.test(res.trim())) {
        this.handleUploadError(null, file)
        return
      }
      this.uploadPoster.url = res.trim()
    },

    handleUploadError(error, file) {
      if (!this.viewActive || !file || file.uid !== this.uploadFileUid) return
      this.handleUploadRemove()
      if (this.$refs.posterUpload) this.$refs.posterUpload.clearFiles()
      this.$message({ type: 'error', message: '图片上传失败，请重新选择图片' })
    },

    handleUploadRemove() {
      this.uploadFileUid = null
      this.uploading = false
      this.uploadPoster.url = ''
    },

    async submitUpload() {
      if (this.saving || this.uploading) return
      if (!this.uploadPoster.url || !this.uploadPoster.title.trim()) {
        this.$message({ type: 'warning', message: '请填写海报标题并上传图片' })
        return
      }
      this.saving = true
      try {
        const res = await AddPoster({ ...this.uploadPoster, title: this.uploadPoster.title.trim() })
        if (!res.success || !this.viewActive) return
        this.dialogFormVisible = false
        this.resetUploadForm()
        await this.loadPosters()
      } catch (error) {
        // Keep the completed upload and form available for retry.
      } finally {
        if (this.viewActive) this.saving = false
      }
    },

    async deleteAllPoster() {
      try {
        await this.$confirm('确定删除所有轮播海报吗？', '提示', { type: 'warning' })
        const res = await DeleteAllPoster()
        if (res.success && this.viewActive) this.posterList = []
      } catch (error) {
        // Cancellation and failed requests leave the displayed list intact.
      }
    },

    async changePosterStatus(index, poster, status) {
      try {
        const res = await UpdatePoster({ ...poster, status })
        if (!res.success || !this.viewActive) return
        const current = this.posterList.find(item => item.id === poster.id)
        if (current) current.status = status
        this.$message({ message: '成功消息：操作成功', type: 'success' })
      } catch (error) {
        // Do not change the displayed status until the server accepts it.
      }
    },

    async handleDeletePoster(index, id) {
      try {
        const res = await DeletePosterById(id)
        if (!res.success || !this.viewActive) return
        this.posterList = this.posterList.filter(item => item.id !== id)
        this.$message({ message: '成功消息：删除成功', type: 'success' })
      } catch (error) {
        // Preserve the row on transport failure.
      }
    },
  }
}
</script>

<style scoped>
.content {
  padding: 20px 40px;
}

.el-carousel__item h3 {
  color: #475669;
  font-size: 18px;
  opacity: 0.75;
  line-height: 300px;
  margin: 0;
}

.image {
  width: 100%;
  height: 500px;
}
</style>