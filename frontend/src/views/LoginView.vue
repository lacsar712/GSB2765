<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'

const router = useRouter()
const username = ref('')
const password = ref('')
const loading = ref(false)

const handleLogin = async () => {
  if (!username.value || !password.value) {
    window.dispatchEvent(new CustomEvent('show-toast', { 
      detail: { message: '请输入用户名和密码', type: 'error' } 
    }))
    return
  }

  loading.ref = true
  try {
    const res = await request.post('/api/auth/login', {
      username: username.value,
      password: password.value
    })
    localStorage.setItem('token', res.token)
    localStorage.setItem('username', res.username)
    
    window.dispatchEvent(new CustomEvent('show-toast', { 
      detail: { message: '登录成功，欢迎回来', type: 'success' } 
    }))
    
    router.push('/')
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen flex w-full">
    <!-- Left: Branding/Hero (Visual) -->
    <div class="hidden lg:flex w-1/2 bg-gradient-to-br from-primary-600 to-primary-900 items-center justify-center p-12 text-white relative overflow-hidden">
      <div class="absolute -top-24 -left-24 w-96 h-96 bg-primary-400/20 rounded-full blur-3xl"></div>
      <div class="absolute -bottom-24 -right-24 w-96 h-96 bg-primary-300/20 rounded-full blur-3xl"></div>
      
      <div class="z-10 max-w-md">
        <h1 class="text-5xl font-extrabold mb-6 leading-tight">产品定价<br/>管理系统</h1>
        <p class="text-primary-100 text-lg leading-relaxed">
          专业的高效定价工具，助力企业实现精准市场定位与价值管控。
        </p>
        <div class="mt-12 flex space-x-4">
          <div class="w-12 h-1 bg-white/30 rounded"></div>
          <div class="w-4 h-1 bg-white/10 rounded"></div>
          <div class="w-4 h-1 bg-white/10 rounded"></div>
        </div>
      </div>
    </div>

    <!-- Right: Login Form -->
    <div class="w-full lg:w-1/2 flex items-center justify-center p-8 bg-gray-50">
      <div class="w-full max-w-md">
        <div class="text-center mb-10">
          <div class="inline-flex items-center justify-center w-16 h-16 bg-primary-100 text-primary-600 rounded-2xl mb-4 shadow-sm">
            <svg xmlns="http://www.w3.org/2000/svg" class="w-8 h-8" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
          </div>
          <h2 class="text-3xl font-bold text-gray-900">欢迎登录</h2>
          <p class="text-gray-500 mt-2">请输入您的凭据以访问管理后台</p>
        </div>

        <form @submit.prevent="handleLogin" class="space-y-6">
          <div>
            <label class="block text-sm font-semibold text-gray-700 mb-2">用户名</label>
            <div class="relative">
              <span class="absolute inset-y-0 left-0 pl-4 flex items-center text-gray-400">
                <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
              </span>
              <input 
                v-model="username"
                type="text" 
                placeholder="admin"
                class="w-full pl-11 pr-4 py-3 bg-white border border-gray-200 rounded-xl focus:ring-4 focus:ring-primary-100 focus:border-primary-500 transition-all outline-none text-gray-900"
              />
            </div>
          </div>

          <div>
            <label class="block text-sm font-semibold text-gray-700 mb-2">密码</label>
            <div class="relative">
              <span class="absolute inset-y-0 left-0 pl-4 flex items-center text-gray-400">
                <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
              </span>
              <input 
                v-model="password"
                type="password" 
                placeholder="123456"
                class="w-full pl-11 pr-4 py-3 bg-white border border-gray-200 rounded-xl focus:ring-4 focus:ring-primary-100 focus:border-primary-500 transition-all outline-none text-gray-900"
              />
            </div>
          </div>

          <div class="flex items-center justify-between text-sm">
            <label class="flex items-center space-x-2 cursor-pointer group">
              <input type="checkbox" class="w-4 h-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500 cursor-pointer">
              <span class="text-gray-600 group-hover:text-primary-600 transition-colors">记住我</span>
            </label>
          </div>

          <button 
            type="submit" 
            :disabled="loading"
            class="w-full py-4 bg-primary-600 hover:bg-primary-700 text-white font-bold rounded-xl shadow-lg shadow-primary-200 transform active:scale-[0.98] transition-all flex items-center justify-center space-x-2 disabled:opacity-70 disabled:cursor-not-allowed"
          >
            <span v-if="loading" class="animate-spin h-5 w-5 border-2 border-white border-t-transparent rounded-full mr-2"></span>
            <span>立即登录</span>
          </button>
        </form>
      </div>
    </div>
  </div>
</template>
