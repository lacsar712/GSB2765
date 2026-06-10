<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import UIToast from './components/UIToast.vue'

const toasts = ref([])

const addToast = (message, type = 'success') => {
  const id = Date.now()
  toasts.value.push({ id, message, type })
  setTimeout(() => {
    toasts.value = toasts.value.filter(t => t.id !== id)
  }, 3500)
}

const handleApiError = (event) => {
  addToast(event.detail, 'error')
}

onMounted(() => {
  window.addEventListener('api-error', handleApiError)
  window.addEventListener('show-toast', (e) => addToast(e.detail.message, e.detail.type))
})

onUnmounted(() => {
  window.removeEventListener('api-error', handleApiError)
})
</script>

<template>
  <router-view></router-view>
  
  <div class="fixed top-4 right-4 z-50 flex flex-col space-y-2">
    <UIToast 
      v-for="toast in toasts" 
      :key="toast.id" 
      :message="toast.message" 
      :type="toast.type" 
    />
  </div>
</template>

<style>
/* Global fade transition for routes */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
