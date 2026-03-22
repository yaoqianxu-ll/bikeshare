<template>
  <canvas ref="canvasRef" class="particles-bg"></canvas>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const canvasRef = ref(null)
let ctx = null
let particles = []
let animationId = null

// 粒子配置
const PARTICLE_COUNT = 50
const PARTICLE_SPEED = 0.3
const PARTICLE_SIZE = 2

class Particle {
  constructor() {
    this.x = Math.random() * window.innerWidth
    this.y = Math.random() * window.innerHeight
    this.vx = (Math.random() - 0.5) * PARTICLE_SPEED
    this.vy = (Math.random() - 0.5) * PARTICLE_SPEED
    this.alpha = Math.random() * 0.5 + 0.3
  }

  update() {
    this.x += this.vx
    this.y += this.vy

    // 边界反弹
    if (this.x < 0 || this.x > window.innerWidth) this.vx *= -1
    if (this.y < 0 || this.y > window.innerHeight) this.vy *= -1
  }

  draw(ctx) {
    ctx.beginPath()
    ctx.arc(this.x, this.y, PARTICLE_SIZE, 0, Math.PI * 2)
    ctx.fillStyle = `rgba(255, 255, 255, ${this.alpha})`
    ctx.fill()
  }
}

function init() {
  const canvas = canvasRef.value
  if (!canvas) return

  canvas.width = window.innerWidth
  canvas.height = window.innerHeight
  ctx = canvas.getContext('2d')

  particles = []
  for (let i = 0; i < PARTICLE_COUNT; i++) {
    particles.push(new Particle())
  }
}

function animate() {
  if (!ctx) return

  ctx.clearRect(0, 0, canvasRef.value.width, canvasRef.value.height)

  particles.forEach(particle => {
    particle.update()
    particle.draw(ctx)
  })

  animationId = requestAnimationFrame(animate)
}

function handleResize() {
  if (canvasRef.value) {
    canvasRef.value.width = window.innerWidth
    canvasRef.value.height = window.innerHeight
    init()
  }
}

onMounted(() => {
  init()
  animate()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (animationId) cancelAnimationFrame(animationId)
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.particles-bg {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
}
</style>
