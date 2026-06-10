<script setup>
defineProps({
  visible: Boolean,
  title: String,
  message: String,
  confirmText: {
    type: String,
    default: '确定'
  },
  cancelText: {
    type: String,
    default: '取消'
  }
})

const emit = defineEmits(['confirm', 'cancel'])
</script>

<template>
  <transition name="modal-fade">
    <div v-if="visible" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="fixed inset-0 bg-black/40 backdrop-blur-sm" @click="emit('cancel')"></div>
      <div class="bg-white rounded-2xl shadow-2xl w-full max-w-sm overflow-hidden z-10 transition-all duration-300">
        <div class="p-6">
          <h3 class="text-xl font-bold text-gray-900 mb-2">{{ title }}</h3>
          <p class="text-gray-600 text-sm leading-relaxed">{{ message }}</p>
        </div>
        <div class="flex border-t border-gray-100">
          <button @click="emit('cancel')" class="flex-1 py-4 text-sm font-medium text-gray-500 hover:bg-gray-50 transition-colors">
            {{ cancelText }}
          </button>
          <button @click="emit('confirm')" class="flex-1 py-4 text-sm font-bold text-red-500 hover:bg-red-50 transition-colors border-l border-gray-100">
            {{ confirmText }}
          </button>
        </div>
      </div>
    </div>
  </transition>
</template>

<style scoped>
.modal-fade-enter-active, .modal-fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}
.modal-fade-enter-from, .modal-fade-leave-to {
  opacity: 0;
  transform: scale(0.95);
}
</style>
