<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'
import UIConfirm from '../components/UIConfirm.vue'

const router = useRouter()
const products = ref([])
const loading = ref(false)
const searchKey = ref('')
const username = ref(localStorage.getItem('username') || '管理员')

// Modal state
const modalVisible = ref(false)
const modalLoading = ref(false)
const isEdit = ref(false)
const currentProduct = ref({
  id: null,
  name: '',
  price: 0,
  category: '',
  description: ''
})

// Delete confirm state
const deleteId = ref(null)
const deleteVisible = ref(false)

const fetchProducts = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/products')
    products.value = res
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  router.push('/login')
}

const filteredProducts = computed(() => {
  if (!searchKey.value) return products.value
  return products.value.filter(p => 
    p.name.toLowerCase().includes(searchKey.value.toLowerCase()) ||
    p.category.toLowerCase().includes(searchKey.value.toLowerCase())
  )
})

const openAddModal = () => {
  isEdit.value = false
  currentProduct.value = { id: null, name: '', price: 0, category: '', description: '' }
  modalVisible.value = true
}

const openEditModal = (product) => {
  isEdit.value = true
  currentProduct.value = { ...product }
  modalVisible.value = true
}

const submitForm = async () => {
  if (!currentProduct.value.name || currentProduct.value.price <= 0) {
    window.dispatchEvent(new CustomEvent('show-toast', { 
      detail: { message: '请填写正确的产品信息', type: 'error' } 
    }))
    return
  }

  modalLoading.value = true
  try {
    if (isEdit.value) {
      await request.put('/api/products', currentProduct.value)
      window.dispatchEvent(new CustomEvent('show-toast', { 
        detail: { message: '更新成功', type: 'success' } 
      }))
    } else {
      await request.post('/api/products', currentProduct.value)
      window.dispatchEvent(new CustomEvent('show-toast', { 
        detail: { message: '添加成功', type: 'success' } 
      }))
    }
    modalVisible.value = false
    fetchProducts()
  } catch (error) {
    console.error(error)
  } finally {
    modalLoading.value = false
  }
}

const confirmDelete = (id) => {
  deleteId.value = id
  deleteVisible.value = true
}

const handleDelete = async () => {
  try {
    await request.delete(`/api/products/${deleteId.value}`)
    window.dispatchEvent(new CustomEvent('show-toast', { 
      detail: { message: '删除成功', type: 'success' } 
    }))
    deleteVisible.value = false
    fetchProducts()
  } catch (error) {
    console.error(error)
  }
}

onMounted(fetchProducts)
</script>

<template>
  <div class="flex flex-col h-screen w-full bg-slate-50">
    <!-- Header -->
    <header class="h-20 bg-white border-b border-gray-100 flex items-center justify-between px-8 shrink-0 shadow-sm z-20">
      <div class="flex items-center space-x-3">
        <div class="w-10 h-10 bg-primary-600 rounded-xl flex items-center justify-center text-white shadow-lg shadow-primary-200">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-6 h-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>
        </div>
        <span class="text-xl font-bold bg-gradient-to-r from-gray-900 to-gray-600 bg-clip-text text-transparent">定价后台</span>
      </div>

      <div class="flex items-center space-x-6">
        <div class="flex items-center space-x-3 pr-6 border-r border-gray-100">
          <div class="text-right">
            <p class="text-sm font-bold text-gray-900">{{ username }}</p>
            <p class="text-xs text-green-500 flex items-center justify-end">
              <span class="w-1.5 h-1.5 bg-green-500 rounded-full mr-1.5 animate-pulse"></span>
              在线
            </p>
          </div>
          <div class="w-10 h-10 bg-gray-100 rounded-full flex items-center justify-center text-gray-500 border-2 border-white shadow-sm font-bold">
            A
          </div>
        </div>
        <button @click="handleLogout" class="text-gray-400 hover:text-red-500 transition-colors p-2 rounded-lg hover:bg-red-50">
          <svg xmlns="http://www.w3.org/2000/svg" class="w-6 h-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
        </button>
      </div>
    </header>

    <!-- Main Content -->
    <main class="flex-1 overflow-auto p-8">
      <div class="max-w-6xl mx-auto">
        <!-- Actions Bar -->
        <div class="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-8">
          <div>
            <h1 class="text-2xl font-bold text-gray-900">产品列表</h1>
            <p class="text-gray-500 text-sm mt-1">管理系统内的所有产品及其定价信息</p>
          </div>
          
          <div class="flex items-center space-x-3">
            <div class="relative min-w-[240px]">
              <span class="absolute inset-y-0 left-0 pl-3 flex items-center text-gray-400">
                <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
              </span>
              <input 
                v-model="searchKey"
                type="text" 
                placeholder="搜索产品或分类..."
                class="w-full pl-10 pr-4 py-2 bg-white border border-gray-200 rounded-xl focus:ring-2 focus:ring-primary-100 focus:border-primary-500 transition-all text-sm outline-none"
              />
            </div>
            <button @click="openAddModal" class="bg-primary-600 hover:bg-primary-700 text-white px-5 py-2 rounded-xl font-bold shadow-lg shadow-primary-200 flex items-center space-x-2 transition-all active:scale-95 text-sm">
              <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
              <span>新增产品</span>
            </button>
          </div>
        </div>

        <!-- Table Card -->
        <div class="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
          <div class="overflow-x-auto">
            <table class="w-full text-left border-collapse">
              <thead>
                <tr class="bg-gray-50/50">
                  <th class="px-6 py-4 text-xs font-bold text-gray-400 uppercase tracking-wider">产品名称</th>
                  <th class="px-6 py-4 text-xs font-bold text-gray-400 uppercase tracking-wider">分类</th>
                  <th class="px-6 py-4 text-xs font-bold text-gray-400 uppercase tracking-wider text-right">价格</th>
                  <th class="px-6 py-4 text-xs font-bold text-gray-400 uppercase tracking-wider">创建时间</th>
                  <th class="px-6 py-4 text-xs font-bold text-gray-400 uppercase tracking-wider text-center">操作</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-50">
                <tr v-if="loading" v-for="i in 5" :key="i" class="animate-pulse">
                  <td class="px-6 py-4"><div class="h-4 bg-gray-100 rounded w-3/4"></div></td>
                  <td class="px-6 py-4"><div class="h-4 bg-gray-100 rounded w-2/3"></div></td>
                  <td class="px-6 py-4"><div class="h-4 bg-gray-100 rounded w-1/2 ml-auto"></div></td>
                  <td class="px-6 py-4"><div class="h-4 bg-gray-100 rounded w-1/2"></div></td>
                  <td class="px-6 py-4 text-center"><div class="h-4 bg-gray-100 rounded w-16 mx-auto"></div></td>
                </tr>
                
                <tr v-else-if="filteredProducts.length === 0">
                  <td colspan="5" class="px-6 py-20 text-center">
                    <div class="flex flex-col items-center">
                      <div class="text-gray-200 mb-2">
                        <svg xmlns="http://www.w3.org/2000/svg" class="w-16 h-16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
                      </div>
                      <p class="text-gray-400">暂无相关产品数据</p>
                    </div>
                  </td>
                </tr>

                <tr v-for="product in filteredProducts" :key="product.id" class="hover:bg-gray-50/80 transition-colors group">
                  <td class="px-6 py-4">
                    <div class="flex items-center">
                      <div class="w-8 h-8 rounded-lg bg-primary-100 text-primary-600 flex items-center justify-center mr-3 font-bold text-xs">
                        {{ product.name.charAt(0) }}
                      </div>
                      <span class="font-bold text-gray-800">{{ product.name }}</span>
                    </div>
                  </td>
                  <td class="px-6 py-4">
                    <span class="inline-flex px-2.5 py-0.5 rounded-full text-xs font-medium bg-secondary-100 text-gray-600 border border-gray-100 bg-gray-100">
                      {{ product.category || '未分类' }}
                    </span>
                  </td>
                  <td class="px-6 py-4 text-right">
                    <span class="text-gray-900 font-bold">¥{{ product.price.toLocaleString(undefined, {minimumFractionDigits: 2}) }}</span>
                  </td>
                  <td class="px-6 py-4 text-sm text-gray-500 whitespace-nowrap">
                    {{ new Date(product.createTime).toLocaleDateString() }}
                  </td>
                  <td class="px-6 py-4">
                    <div class="flex items-center justify-center space-x-2">
                      <button @click="openEditModal(product)" class="p-2 text-gray-400 hover:text-primary-600 hover:bg-primary-50 rounded-lg transition-all">
                        <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                      </button>
                      <button @click="confirmDelete(product.id)" class="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-all">
                        <svg xmlns="http://www.w3.org/2000/svg" class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/><line x1="10" y1="11" x2="10" y2="17"/><line x1="14" y1="11" x2="14" y2="17"/></svg>
                      </button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </main>

    <!-- Modal for Add/Edit -->
    <transition name="modal-fade">
      <div v-if="modalVisible" class="fixed inset-0 z-50 flex items-center justify-center p-4">
        <div class="fixed inset-0 bg-black/40 backdrop-blur-sm" @click="modalVisible = false"></div>
        <div class="bg-white rounded-2xl shadow-2xl w-full max-w-lg overflow-hidden z-10 transition-all duration-300">
          <div class="px-8 py-6 border-b border-gray-100 flex items-center justify-between">
            <h3 class="text-xl font-bold text-gray-900">{{ isEdit ? '编辑产品' : '新增产品' }}</h3>
            <button @click="modalVisible = false" class="text-gray-400 hover:text-gray-600">
              <svg xmlns="http://www.w3.org/2000/svg" class="w-6 h-6" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
            </button>
          </div>
          
          <form @submit.prevent="submitForm" class="p-8 space-y-5">
            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-2">产品名称</label>
              <input 
                v-model="currentProduct.name"
                type="text" 
                placeholder="请输入产品全名"
                class="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl focus:ring-4 focus:ring-primary-100 focus:border-primary-500 transition-all outline-none"
              />
            </div>

            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">价格 (元)</label>
                <input 
                  v-model="currentProduct.price"
                  type="number" 
                  step="0.01"
                  placeholder="0.00"
                  class="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl focus:ring-4 focus:ring-primary-100 focus:border-primary-500 transition-all outline-none"
                />
              </div>
              <div>
                <label class="block text-sm font-semibold text-gray-700 mb-2">分类</label>
                <input 
                  v-model="currentProduct.category"
                  type="text" 
                  placeholder="如：电子产品"
                  class="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl focus:ring-4 focus:ring-primary-100 focus:border-primary-500 transition-all outline-none"
                />
              </div>
            </div>

            <div>
              <label class="block text-sm font-semibold text-gray-700 mb-2">产品描述</label>
              <textarea 
                v-model="currentProduct.description"
                rows="3"
                placeholder="简单描述一下这个产品..."
                class="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl focus:ring-4 focus:ring-primary-100 focus:border-primary-500 transition-all outline-none resize-none"
              ></textarea>
            </div>

            <div class="flex space-x-3 pt-4">
              <button 
                type="button"
                @click="modalVisible = false" 
                class="flex-1 py-3.5 bg-gray-100 hover:bg-gray-200 text-gray-600 font-bold rounded-xl transition-all"
              >
                取消
              </button>
              <button 
                type="submit" 
                :disabled="modalLoading"
                class="flex-1 py-3.5 bg-primary-600 hover:bg-primary-700 text-white font-bold rounded-xl shadow-lg shadow-primary-200 transition-all flex items-center justify-center space-x-2 disabled:opacity-70"
              >
                <span v-if="modalLoading" class="animate-spin h-5 w-5 border-2 border-white border-t-transparent rounded-full"></span>
                <span>{{ isEdit ? '保存修改' : '立即创建' }}</span>
              </button>
            </div>
          </form>
        </div>
      </div>
    </transition>

    <UIConfirm 
      :visible="deleteVisible"
      title="确认删除产品？"
      message="此操作将永久删除该产品信息，删除后无法恢复。"
      @confirm="handleDelete"
      @cancel="deleteVisible = false"
    />
  </div>
</template>

<style scoped>
.modal-fade-enter-active, .modal-fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}
.modal-fade-enter-from, .modal-fade-leave-to {
  opacity: 0;
  transform: scale(0.95);
}

/* Chrome, Safari, Edge, Opera */
input::-webkit-outer-spin-button,
input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

/* Firefox */
input[type=number] {
  -moz-appearance: textfield;
}
</style>
