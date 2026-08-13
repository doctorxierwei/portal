import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

/**
 * 全局请求配置: 与后端统一返回结构 R<T> = { code, msg, data } 对齐
 * 约定:
 *   - code === 200  -> 成功, 拦截器直接返回 res.data (业务数据), 页面无需再判断 code
 *   - code !== 200  -> 业务失败, 全局弹窗提示并 reject, 页面用 try/catch 感知
 *   - 401 / 403     -> 清空登录态并跳转登录页
 */
const request = axios.create({
  baseURL: '/',
  timeout: 10000
})

// 与后端 R 的 code 常量保持一致(避免页面散落魔法数字)
export const RESULT_CODE = {
  SUCCESS: 200,
  FAIL: 500,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403
}

let isRedirectingLogin = false

request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers['Authorization'] = 'Bearer ' + token
  }
  return config
})

request.interceptors.response.use(
  response => {
    const res = response.data

    // 非标准结构(理论上后端都走 R<T>, 这里兜底直接返回)
    if (res == null || typeof res.code === 'undefined') {
      return res
    }

    if (res.code === RESULT_CODE.UNAUTHORIZED || res.code === RESULT_CODE.FORBIDDEN) {
      if (!isRedirectingLogin) {
        isRedirectingLogin = true
        ElMessage.error(res.msg || '登录已过期，请重新登录')
        // 注意: 此处不要删除 localStorage 的 token。
        // 否则并发请求中先到的 401 会清掉 token, 导致紧随其后的其他请求(如页面 onMounted 拉数据)
        // 因读不到 token 而级联 401。登录态交由路由守卫统一判定。
        router.replace('/login').finally(() => { isRedirectingLogin = false })
      }
      return Promise.reject(new Error(res.msg || '未授权'))
    }

    if (res.code !== RESULT_CODE.SUCCESS) {
      ElMessage.error(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg || '请求失败'))
    }

    // 成功: 直接把业务数据(data)透传给调用方, 页面无需再处理外层包装
    return res.data
  },
  error => {
    const status = error.response && error.response.status
    const serverMsg = error.response && error.response.data && error.response.data.msg
    if (status === 401 || status === 403) {
      if (!isRedirectingLogin) {
        isRedirectingLogin = true
        ElMessage.error(serverMsg || '登录已过期，请重新登录')
        // 同上: 不删除 token, 避免并发请求级联 401
        router.replace('/login').finally(() => { isRedirectingLogin = false })
      }
    } else {
      ElMessage.error(serverMsg || error.message || '网络错误，请稍后重试')
    }
    return Promise.reject(error)
  }
)

export default request
