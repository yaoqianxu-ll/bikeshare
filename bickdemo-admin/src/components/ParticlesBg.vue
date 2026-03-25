<template>
  <canvas ref="canvasRef" class="particles-bg"></canvas>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const canvasRef = ref(null)
let ctx = null
let particles = []
let animationId = null

// 粒子配置 - 增强科技感
const PARTICLE_COUNT = 80
const PARTICLE_SPEED = 0.5
const PARTICLE_SIZE = 2.5
const CONNECTION_DISTANCE = 120

class Particle {
  constructor() {
    this.x = Math.random() * window.innerWidth
    this.y = Math.random() * window.innerHeight
    this.vx = (Math.random() - 0.5) * PARTICLE_SPEED
    this.vy = (Math.random() - 0.5) * PARTICLE_SPEED
    this.alpha = Math.random() * 0.5 + 0.3
    // 蓝紫色光晕
    this.hue = Math.random() * 60 + 220 // 220-280 (蓝到紫)
    this.saturation = Math.random() * 30 + 70
    this.lightness = Math.random() * 20 + 60
  }

  update() {
    this.x += this.vx
    this.y += this.vy

    // 边界反弹
    if (this.x < 0 || this.x > window.innerWidth) this.vx *= -1
    if (this.y < 0 || this.y > window.innerHeight) this.vy *= -1
  }

  draw(ctx) {
    // 绘制光晕
    const gradient = ctx.createRadialGradient(this.x, this.y, 0, this.x, this.y, PARTICLE_SIZE * 2)
    gradient.addColorStop(0, `hsla(${this.hue}, ${this.saturation}%, ${this.lightness}%, ${this.alpha})`)
    gradient.addColorStop(1, `hsla(${this.hue}, ${this.saturation}%, ${this.lightness}%, 0)`)
    ctx.beginPath()
    ctx.arc(this.x, this.y, PARTICLE_SIZE * 2, 0, Math.PI * 2)
    ctx.fillStyle = gradient
    ctx.fill()
  }
}

function drawConnections() {
  for (let i = 0; i < particles.length; i++) {
    for (let j = i + 1; j < particles.length; j++) {
      const dx = particles[i].x - particles[j].x
      const dy = particles[i].y - particles[j].y
      const distance = Math.sqrt(dx * dx + dy * dy)

      if (distance < CONNECTION_DISTANCE) {
        const opacity = (1 - distance / CONNECTION_DISTANCE) * 0.4
        const gradient = ctx.createLinearGradient(particles[i].x, particles[i].y, particles[j].x, particles[j].y)
        gradient.addColorStop(0, `rgba(96, 165, 250, ${opacity})`)
        gradient.addColorStop(1, `rgba(168, 85, 247, ${opacity})`)
        ctx.strokeStyle = gradient
        ctx.lineWidth = 0.8
        ctx.beginPath()
        ctx.moveTo(particles[i].x, particles[i].y)
        ctx.lineTo(particles[j].x, particles[j].y)
        ctx.stroke()
      }
    }
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

  // 先绘制连接线
  drawConnections()

  // 再绘制粒子
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
  z-index: -1;
}
</style>
