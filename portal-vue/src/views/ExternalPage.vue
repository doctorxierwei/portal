<template>
  <div class="external-wrap">
    <el-empty v-if="!url" description="该菜单未配置外链地址，请在「菜单管理」中填写外链地址" />

    <template v-else>
      <div class="external-bar">
        <span class="external-url" :title="url">{{ url }}</span>
        <el-button size="small" type="primary" link @click="openNewWindow">新窗口打开</el-button>
      </div>

      <div class="external-body">
        <iframe ref="frameRef" :src="url" class="external-frame" frameborder="0" @load="onLoad" />

        <!-- 部分站点(如百度)设置了 X-Frame-Options / CSP 禁止被嵌入, iframe 会白屏 -->
        <div v-if="blocked" class="external-mask">
          <el-result icon="warning" title="该网站不允许被嵌入显示">
            <template #sub-title>
              <p>目标站点设置了 X-Frame-Options / CSP 安全策略，禁止在 iframe 中打开。</p>
              <p>请改用「新窗口打开」方式访问，或在菜单管理中将打开方式改为「新窗口打开」。</p>
            </template>
            <template #extra>
              <el-button type="primary" @click="openNewWindow">在新窗口打开</el-button>
            </template>
          </el-result>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, ref, watch, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const url = computed(() => route.meta.link || '')

const frameRef = ref()
const blocked = ref(false)
let timer = null

// iframe 被安全策略拦截时不会触发 load, 用超时兜底提示
function startWatch() {
  blocked.value = false
  clearTimeout(timer)
  if (!url.value) return
  timer = setTimeout(() => { blocked.value = true }, 3000)
}
function onLoad() {
  clearTimeout(timer)
  try {
    // 跨域被拦截时访问 contentDocument 会抛错或为空
    const doc = frameRef.value?.contentDocument
    blocked.value = doc !== null && doc !== undefined && doc.body && doc.body.childElementCount === 0
  } catch (e) {
    // 正常跨域页面会抛异常, 说明已成功加载
    blocked.value = false
  }
}
function openNewWindow() {
  window.open(url.value, '_blank')
}

watch(url, startWatch, { immediate: true })
onBeforeUnmount(() => clearTimeout(timer))
</script>

<style scoped>
.external-wrap { height: 100%; width: 100%; display: flex; flex-direction: column; }
.external-bar { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 6px 10px; background: #fff; border: 1px solid #eee; border-radius: 8px; margin-bottom: 8px; }
.external-url { font-size: 12px; color: #909399; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.external-body { position: relative; flex: 1; min-height: calc(100vh - 160px); }
.external-frame { width: 100%; height: 100%; min-height: calc(100vh - 160px); border: none; background: #fff; border-radius: 8px; }
.external-mask { position: absolute; inset: 0; background: #fff; border-radius: 8px; display: flex; align-items: center; justify-content: center; }
</style>
