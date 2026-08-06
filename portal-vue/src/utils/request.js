import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({
  baseURL: '/',
  timeout: 10000
})

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
    // 网关/业务统一返回结构 { code, msg, data }
    if (res.code !== undefined) {
      if (res.code === 401 || res.code === 403) {
        ElMessage.error(res.msg || '未授权')
        localStorage.removeItem('token')
        router.push('/login')
        return Promise.reject(new Error(res.msg))
      }
      if (res.code !== 200) {
        ElMessage.error(res.msg || '请求失败')
        return Promise.reject(new Error(res.msg))
      }
      return res.data
    }
    return res
  },
  error => {
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
