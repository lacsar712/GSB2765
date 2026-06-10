import axios from 'axios'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 10000
})

service.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      // Handle error via a custom event or callback
      window.dispatchEvent(new CustomEvent('api-error', { detail: res.message || 'Error' }))
      return Promise.reject(new Error(res.message || 'Error'))
    } else {
      return res.data
    }
  },
  error => {
    window.dispatchEvent(new CustomEvent('api-error', { detail: error.message || 'Network Error' }))
    return Promise.reject(error)
  }
)

export default service
