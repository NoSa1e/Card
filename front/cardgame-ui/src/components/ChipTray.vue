<template>
  <div class="chip-tray" :class="{ disabled }">
    <div class="chip-list">
      <button
        v-for="value in chips"
        :key="value"
        class="chip"
        :title="`좌클릭:+${format(value)} / 우클릭:-${format(value)}`"
        :disabled="disabled || isAtLimit(value)"
        @click="adjust(value)"
        @contextmenu.prevent="adjust(-value)"
      >
        <img :src="chipSrc(value)" :alt="`${value} 칩`" loading="lazy" />
        <span> {{ signPrefix(value) }}{{ format(value) }}</span>
      </button>
    </div>
    <div class="chip-actions">
      <button class="chip control" :disabled="disabled || !canDecrease" @click="decrease">
        -{{ format(step) }}
      </button>
      <button class="chip control" :disabled="disabled || current <= minValue" @click="reset">
        리셋
      </button>
    </div>
    <p v-if="typeof balance === 'number'" class="balance">잔액: <strong>{{ format(balance) }}</strong></p>
  </div>
</template>
<script setup>
import { computed, watch } from 'vue'

const props = defineProps({
  modelValue: { type: Number, default: 0 },
  denominations: { type: Array, default: () => [100, 500, 1000, 5000] },
  min: { type: Number, default: 0 },
  max: { type: Number, default: Number.POSITIVE_INFINITY },
  disabled: { type: Boolean, default: false },
  balance: { type: Number, default: undefined }
})

const emit = defineEmits(['update:modelValue', 'change'])

const minValue = computed(() => Number.isFinite(props.min) ? props.min : 0)
const maxValue = computed(() => Number.isFinite(props.max) ? props.max : Number.POSITIVE_INFINITY)

const chips = computed(() => {
  return [...props.denominations].sort((a, b) => a - b)
})

const step = computed(() => chips.value[0] || 10)

const current = computed(() => clamp(props.modelValue))

watch(() => props.modelValue, (val) => {
  const next = clamp(val)
  if(next !== val){
    emit('update:modelValue', next)
    emit('change', next)
  }
}, { immediate: true })

function clamp(value){
  const min = minValue.value
  const max = maxValue.value
  let next = Number.isFinite(value) ? value : min
  if(Number.isFinite(max)){
    next = Math.min(next, max)
  }
  return Math.max(next, min)
}

function adjust(delta){
  if(props.disabled) return
  const next = clamp(current.value + delta)
  if(next === current.value) return
  emit('update:modelValue', next)
  emit('change', next)
}

function decrease(){
  adjust(-step.value)
}

function reset(){
  const next = clamp(minValue.value)
  emit('update:modelValue', next)
  emit('change', next)
}

function chipSrc(amount){
  const known = [100, 500, 1000, 5000]
  const value = known.includes(amount) ? amount : known.reduce((prev, curr) => Math.abs(curr - amount) < Math.abs(prev - amount) ? curr : prev)
  return `/assets/chip_${value}.png`
}

function isAtLimit(delta){
  const max = maxValue.value
  if(!Number.isFinite(max)) return false
  return current.value + delta > max
}

const canDecrease = computed(() => current.value > minValue.value)

function format(value){
  return Math.round(value).toLocaleString()
}

function signPrefix(value){
  return value > 0 ? '+' : ''
}
</script>
<style scoped>
.chip-tray{ display:flex; flex-direction:column; gap:12px; align-items:flex-start; }
.chip-list{ display:flex; flex-wrap:wrap; gap:12px; }
.chip{ display:flex; flex-direction:column; align-items:center; gap:6px; padding:10px 12px; border-radius:14px; border:none; background:rgba(255,255,255,.08); color:#fff; cursor:pointer; transition: transform .15s ease, box-shadow .2s ease, background .2s ease; }
.chip img{ width:54px; height:54px; object-fit:contain; filter: drop-shadow(0 8px 16px rgba(0,0,0,.3)); }
.chip span{ font-size:.85rem; font-weight:600; }
.chip:hover:not(:disabled){ transform: translateY(-3px); box-shadow:0 14px 28px rgba(0,0,0,.32); background:rgba(255,255,255,.12); }
.chip:disabled{ opacity:.45; cursor:not-allowed; box-shadow:none; }
.chip.control{ flex-direction:row; gap:8px; background:rgba(255,255,255,.05); padding:12px 16px; border:1px solid rgba(255,255,255,.08); }
.chip-actions{ display:flex; gap:10px; flex-wrap:wrap; }
.balance{ margin:0; color:rgba(255,255,255,.7); font-size:.9rem; }
.balance strong{ color:#fff; }
.disabled .chip{ cursor:not-allowed; opacity:.4; }
</style>
