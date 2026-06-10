<script setup>
import { ref, onMounted } from 'vue'

const props = defineProps({
  message: String,
  type: {
    type: String,
    default: 'success'
  },
  duration: {
    type: Number,
    default: 3000
  }
})

const visible = ref(false)

onMounted(() => {
  visible.value = true
  setTimeout(() => {
    visible.value = false
  }, props.duration)
})
</script>

<template>
  <transition name="toast-fade">
    <div v-if="visible" 
      class="fixed top-4 right-4 px-6 py-3 rounded-lg shadow-xl z-50 flex items-center space-x-2 transition-all duration-300"
      :class="{
        'bg-green-500 text-white': type === 'success',
        'bg-red-500 text-white': type === 'error',
        'bg-blue-500 text-white': type === 'info'
      }"
    >
      <span v-if="type === 'success'" class="i-carbon-checkmark-filled"></span>
      <span v-if="type === 'error'" class="i-carbon-error-filled"></span>
      <span class="font-medium text-sm">{{ message }}</span>
    </div>
  </transition>
</template>

<style scoped>
.toast-fade-enter-active, .toast-fade-leave-active {
  transition: all 0.3s ease;
}
.toast-fade-enter-from {
  opacity: 0;
  transform: translateX(20px);
}
.toast-fade-leave-to {
  opacity: 0;
  transform: translateX(20px);
}
</style>
