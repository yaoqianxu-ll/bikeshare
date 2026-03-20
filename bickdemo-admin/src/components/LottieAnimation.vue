<template>
  <div ref="lottieContainer" class="lottie-container"></div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, defineAsyncComponent } from 'vue'

const props = defineProps({
  animationData: {
    type: Object,
    required: true
  },
  loop: {
    type: Boolean,
    default: true
  },
  speed: {
    type: Number,
    default: 1
  }
})

const lottieContainer = ref(null)
let animation = null
let lottie = null

// 动态加载 lottie-web
const loadLottie = async () => {
  if (!lottie) {
    const lottieModule = await import('lottie-web')
    lottie = lottieModule.default
  }
  return lottie
}

onMounted(async () => {
  const lottieLib = await loadLottie()
  if (lottieContainer.value && lottieLib) {
    animation = lottieLib.loadAnimation({
      container: lottieContainer.value,
      renderer: 'svg',
      loop: props.loop,
      autoplay: true,
      animationData: props.animationData,
      speed: props.speed
    })
  }
})

watch(() => props.speed, (newSpeed) => {
  if (animation) {
    animation.setSpeed(newSpeed)
  }
})

onBeforeUnmount(() => {
  if (animation) {
    animation.destroy()
  }
})
</script>

<style scoped>
.lottie-container {
  width: 100%;
  height: 100%;
}
</style>
