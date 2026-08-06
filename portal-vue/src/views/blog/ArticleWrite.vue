<template>
  <el-card>
    <el-form label-width="72px">
      <el-form-item label="标题">
        <el-input v-model="form.title" placeholder="请输入标题" />
      </el-form-item>
      <el-form-item label="摘要">
        <el-input v-model="form.summary" type="textarea" :rows="2" placeholder="一句话摘要" />
      </el-form-item>
      <el-row :gutter="12">
        <el-col :span="8">
          <el-form-item label="分类">
            <el-select v-model="form.categoryId" placeholder="选择分类" clearable style="width: 100%">
              <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="16">
          <el-form-item label="标签">
            <el-select v-model="form.tagIds" multiple filterable placeholder="选择已有标签" style="width: 100%">
              <el-option v-for="t in tags" :key="t.id" :label="t.name" :value="t.id" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="封面图">
        <el-input v-model="form.cover" placeholder="图片 URL（可在图片管理复制）" style="width: 60%; margin-right: 8px" />
        <el-button @click="pickImage">从图片库选择</el-button>
      </el-form-item>
      <el-form-item label="正文">
        <div class="editor-wrap">
          <el-tabs v-model="tab">
            <el-tab-pane label="编辑" name="edit">
              <div class="rich-editor">
                <Toolbar class="editor-toolbar" :editor="editorRef" :defaultConfig="toolbarConfig" mode="default" />
                <Editor class="editor-body" v-model="form.content" :defaultConfig="editorConfig" mode="default"
                        @onCreated="onEditorCreated" @customPaste="onCustomPaste" />
              </div>
            </el-tab-pane>
            <el-tab-pane label="HTML 源码" name="html">
              <el-input v-model="form.content" type="textarea" :rows="18"
                        placeholder="可直接粘贴/编辑 HTML 源码" />
            </el-tab-pane>
            <el-tab-pane label="预览" name="preview">
              <div class="preview" v-html="form.content"></div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </el-form-item>
      <el-form-item>
        <el-button @click="save(0)">存为草稿</el-button>
        <el-button type="primary" @click="save(1)">发布</el-button>
        <el-button @click="$router.back()">返回</el-button>
      </el-form-item>
    </el-form>

    <el-dialog v-model="imgDialog" title="图片库" width="720px">
      <el-button type="primary" size="small" @click="uploadClick">上传图片</el-button>
      <input ref="fileRef" type="file" hidden @change="onUpload" />
      <div class="img-grid">
        <div v-for="img in images" :key="img.id" class="img-cell" @click="choose(img)">
          <img :src="img.url" />
          <span>{{ img.name }}</span>
        </div>
      </div>
      <el-pagination small background layout="prev, pager, next" :total="imgTotal"
                     :current-page="imgPage" :page-size="12" @current-change="loadImages" style="margin-top: 8px" />
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, shallowRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import '@wangeditor/editor/dist/css/style.css'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import { getArticle, saveArticle, getCategoryList, getTagList, getImagePage, uploadImage } from '../../api/index.js'

const route = useRoute()
const router = useRouter()
const form = reactive({ id: null, title: '', summary: '', cover: '', content: '', categoryId: null, tagIds: [] })
const categories = ref([])
const tags = ref([])
const tab = ref('edit')

// ============ 富文本编辑器 ============
// editor 实例必须用 shallowRef 保存, 否则会被 Vue 深度代理导致异常
const editorRef = shallowRef()
const toolbarConfig = {
  excludeKeys: ['group-video', 'fullScreen']
}
async function doUploadImage(file) {
  const res = await uploadImage(file)
  console.log('uploadImage resp:', res)
  const url = res?.url
  if (!url) {
    throw new Error('图片上传返回地址为空: ' + JSON.stringify(res))
  }
  return url
}

const editorConfig = {
  placeholder: '请输入正文内容...',
  MENU_CONF: {
    // 图片上传接入后端图片库接口, 上传成功后直接插入编辑器
    uploadImage: {
      async customUpload(file, insertFn) {
        try {
          const url = await doUploadImage(file)
          insertFn(url, file.name, url)
        } catch (err) {
          ElMessage.error('图片上传失败')
        }
      }
    }
  }
}
function onEditorCreated(editor) {
  editorRef.value = editor
}

// 拦截粘贴: 把图片文件上传到后端, 避免 base64/本地路径导致裂图
function onCustomPaste(editor, event) {
  const files = event.clipboardData && event.clipboardData.files
  if (files && files.length) {
    const imageFiles = Array.from(files).filter(f => f.type && f.type.startsWith('image/'))
    if (imageFiles.length) {
      event.preventDefault()
      imageFiles.forEach(file => {
        doUploadImage(file)
          .then(url => {
            editor.dangerouslyInsertHtml(`<img src="${url}" alt="${file.name}"/>`)
          })
          .catch(err => ElMessage.error(err.message || '粘贴图片上传失败'))
      })
      return false
    }
  }
  return true
}
onBeforeUnmount(() => {
  editorRef.value?.destroy()
})

const imgDialog = ref(false)
const images = ref([])
const imgTotal = ref(0)
const imgPage = ref(1)
const fileRef = ref()

async function loadMeta() {
  categories.value = await getCategoryList()
  tags.value = await getTagList()
}
async function loadArticle() {
  if (!route.query.id) return
  const a = await getArticle(route.query.id)
  Object.assign(form, a)
  form.tagIds = a.tagIds || []
}

function pickImage() { imgDialog.value = true; loadImages(1) }
function uploadClick() { fileRef.value.click() }
async function onUpload(e) {
  const file = e.target.files[0]
  if (!file) return
  await uploadImage(file)
  ElMessage.success('上传成功')
  loadImages(imgPage.value)
  e.target.value = ''
}
async function loadImages(p) {
  imgPage.value = p
  const res = await getImagePage({ current: p, size: 12 })
  images.value = res.records || []
  imgTotal.value = res.total || 0
}
function choose(img) { form.cover = img.url; imgDialog.value = false }

async function save(status) {
  if (!form.title) { ElMessage.warning('请填写标题'); return }
  form.status = status
  await saveArticle(form, form.tagIds)
  ElMessage.success(status === 1 ? '已发布' : '已存草稿')
  router.push('/blog/article')
}
onMounted(() => { loadMeta(); loadArticle() })
</script>

<style scoped>
.editor-wrap { width: 100%; }
.rich-editor { border: 1px solid #dcdfe6; border-radius: 8px; overflow: hidden; }
.editor-toolbar { border-bottom: 1px solid #dcdfe6; }
.editor-body { height: 420px !important; overflow-y: auto; }
.preview { padding: 12px; min-height: 320px; border: 1px solid #eee; border-radius: 8px; background: #fff; }
.preview :deep(img) { max-width: 100%; }
.img-grid { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 10px; }
.img-cell { width: 120px; cursor: pointer; text-align: center; font-size: 12px; color: #666; }
.img-cell img { width: 120px; height: 90px; object-fit: cover; border-radius: 6px; border: 1px solid #eee; }
</style>
