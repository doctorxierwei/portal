<template>
  <div class="site-config">
    <el-card class="form-card" shadow="never">
      <template #header><span class="card-title">🎀 博客前台站点设置</span></template>

      <el-form :model="form" label-width="120px" class="cfg-form">
        <el-form-item label="站点名称">
          <el-input v-model="form.siteName" maxlength="32" placeholder="左上角展示的站点名，如：我的博客" />
        </el-form-item>

        <el-form-item label="副标题">
          <el-input v-model="form.slogan" maxlength="64" placeholder="名称下方的小字标语（可留空）" />
        </el-form-item>

        <!-- 顶栏背景 -->
        <el-form-item label="顶栏背景">
          <el-radio-group v-model="form.headerBgType" @change="onHeaderTypeChange">
            <el-radio-button value="color">纯色</el-radio-button>
            <el-radio-button value="gradient">渐变</el-radio-button>
          </el-radio-group>

          <div v-if="form.headerBgType === 'color'" class="color-row">
            <el-color-picker v-model="form.headerBg" show-alpha />
            <el-input v-model="form.headerBg" class="color-input" placeholder="#ffffff 或 rgb(255,255,255)" />
          </div>
          <el-input v-else v-model="form.headerBg" class="gradient-input"
                    placeholder="如 linear-gradient(135deg,#ff9ec4,#c9a7ff)" />
        </el-form-item>

        <el-form-item label="顶栏透明度">
          <el-slider v-model="form.headerOpacity" :min="0.2" :max="1" :step="0.05" :format-tooltip="(v)=>(v*100).toFixed(0)+'%'" style="width:260px" />
          <span class="hint">越小越透（毛玻璃感）</span>
        </el-form-item>

        <!-- 页面背景 -->
        <el-form-item label="页面背景">
          <el-radio-group v-model="form.pageBgType" @change="onPageTypeChange">
            <el-radio-button value="color">纯色</el-radio-button>
            <el-radio-button value="gradient">渐变</el-radio-button>
            <el-radio-button value="image">图片</el-radio-button>
          </el-radio-group>

          <div v-if="form.pageBgType === 'color'" class="color-row">
            <el-color-picker v-model="form.pageBg" show-alpha />
            <el-input v-model="form.pageBg" class="color-input" placeholder="#f5f6f7 或 rgb(245,246,247)" />
          </div>
          <el-input v-else-if="form.pageBgType === 'gradient'" v-model="form.pageBg" class="gradient-input"
                    placeholder="如 linear-gradient(135deg,#fff2f8,#e7d6ff)" />
          <div v-else class="image-row">
            <el-upload :auto-upload="true" :show-file-list="false" :http-request="onBgUpload" accept="image/*">
              <el-button :loading="uploading" type="primary" plain>上传背景图</el-button>
            </el-upload>
            <el-input v-model="form.pageBg" class="gradient-input" placeholder="或粘贴图片地址" />
            <img v-if="form.pageBg" :src="form.pageBg" class="bg-preview" alt="背景预览" />
          </div>
        </el-form-item>

        <el-form-item label="页面透明度">
          <el-slider v-model="form.pageOpacity" :min="0.2" :max="1" :step="0.05" :format-tooltip="(v)=>(v*100).toFixed(0)+'%'" style="width:260px" />
          <span class="hint">越小越透；图片背景会叠加半透明蒙层</span>
        </el-form-item>

        <el-form-item label="页脚文字">
          <el-input v-model="form.footerText" maxlength="128" placeholder="如 © 2026 我的博客" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="onSave">保存配置</el-button>
          <el-button @click="load">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 实时预览 -->
    <el-card class="preview-card" shadow="never">
      <template #header><span class="card-title">👀 实时预览</span></template>
      <div class="preview-wrap">
        <!-- 背景层单独承载背景+透明度 -->
        <div class="preview-bg" :style="previewBgStyle"></div>
        <div class="preview-body">
          <div class="preview-header" :style="previewHeaderStyle">
            <div class="preview-logo">{{ form.siteName || '我的博客' }}</div>
          </div>
          <div class="preview-content">
            <div v-if="form.slogan" class="preview-slogan">{{ form.slogan }}</div>
            <el-card class="preview-post" shadow="hover">这里是文章卡片预览</el-card>
          </div>
          <div class="preview-footer">{{ form.footerText || '© 我的博客' }}</div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getSiteConfig, saveSiteConfig, uploadImage } from '../../api/index.js'

const form = reactive({
  siteName: '',
  slogan: '',
  headerBgType: 'color',
  headerBg: '#ffffff',
  headerOpacity: 1,
  pageBgType: 'color',
  pageBg: '#f5f6f7',
  pageOpacity: 1,
  footerText: ''
})
const saving = ref(false)
const uploading = ref(false)

function isGradient(v) { return typeof v === 'string' && v.includes('gradient(') }

onMounted(load)

async function load() {
  try {
    const cfg = await getSiteConfig()
    const d = cfg || {}
    form.siteName = d.siteName || '我的博客'
    form.slogan = d.slogan || ''
    form.headerBgType = isGradient(d.headerBg) ? 'gradient' : 'color'
    form.headerBg = d.headerBg || '#ffffff'
    form.headerOpacity = d.headerOpacity != null ? Number(d.headerOpacity) : 1
    form.pageBgType = d.pageBgType || 'color'
    form.pageBg = d.pageBg || '#f5f6f7'
    form.pageOpacity = d.pageOpacity != null ? Number(d.pageOpacity) : 1
    form.footerText = d.footerText || ''
  } catch (e) {
    ElMessage.warning('读取当前配置失败，已载入默认值')
  }
}

function onHeaderTypeChange() { if (form.headerBgType === 'color' && isGradient(form.headerBg)) form.headerBg = '#ffffff' }
function onPageTypeChange() { if (form.pageBgType === 'color' && isGradient(form.pageBg)) form.pageBg = '#f5f6f7' }

async function onBgUpload({ file }) {
  uploading.value = true
  try {
    const res = await uploadImage(file)
    if (res && res.url) {
      form.pageBg = res.url
      ElMessage.success('背景图已上传')
    } else {
      ElMessage.error('上传成功但未返回地址')
    }
  } catch (e) {
    ElMessage.error('背景图上传失败')
  } finally {
    uploading.value = false
  }
}

async function onSave() {
  saving.value = true
  try {
    await saveSiteConfig({
      siteName: form.siteName,
      slogan: form.slogan,
      headerBg: form.headerBg,
      headerOpacity: form.headerOpacity,
      pageBgType: form.pageBgType,
      pageBg: form.pageBg,
      pageOpacity: form.pageOpacity,
      footerText: form.footerText
    })
    ElMessage.success('已保存，前台刷新即可生效')
  } catch (e) {
    ElMessage.error('保存失败：' + (e?.message || '未知错误'))
  } finally {
    saving.value = false
  }
}

// ===== 预览样式 =====
const previewBgStyle = computed(() => {
  const style = { opacity: form.pageOpacity }
  if (form.pageBgType === 'image' && form.pageBg) {
    style.backgroundImage = `url(${form.pageBg})`
    style.backgroundSize = 'cover'
    style.backgroundPosition = 'center'
  } else if (form.pageBgType === 'gradient') {
    style.background = form.pageBg
  } else {
    style.background = form.pageBg
  }
  return style
})
const previewHeaderStyle = computed(() => ({
  background: form.headerBg,
  opacity: form.headerOpacity
}))
</script>

<style scoped>
.site-config { display: flex; gap: 16px; flex-wrap: wrap; align-items: flex-start; }
.form-card { flex: 1 1 520px; }
.preview-card { flex: 1 1 360px; }
.card-title { font-weight: 600; }
.cfg-form { margin-top: 8px; }
.color-row { display: inline-flex; align-items: center; gap: 10px; margin-left: 12px; vertical-align: middle; }
.color-input { width: 210px; }
.gradient-input { width: 280px; margin-left: 12px; }
.image-row { display: inline-flex; align-items: center; flex-wrap: wrap; gap: 8px; margin-left: 12px; vertical-align: middle; }
.hint { color: #999; font-size: 12px; margin-left: 12px; }
.bg-preview { width: 80px; height: 50px; object-fit: cover; border-radius: 6px; border: 1px solid #eee; }

.preview-wrap { position: relative; min-height: 320px; border-radius: 12px; overflow: hidden; display: flex; flex-direction: column; }
.preview-bg { position: absolute; inset: 0; z-index: 0; transition: background .3s, opacity .3s; }
.preview-body { position: relative; z-index: 1; display: flex; flex-direction: column; min-height: 320px; }
.preview-header { padding: 14px 20px; }
.preview-logo { font-size: 18px; font-weight: 700; color: #333; }
.preview-content { flex: 1; padding: 16px; }
.preview-slogan { color: #666; margin-bottom: 12px; }
.preview-post { margin-bottom: 12px; }
.preview-footer { padding: 12px; text-align: center; color: #888; font-size: 12px; background: rgba(0,0,0,0.03); }
</style>
