<template>
  <div class="card3d" :class="{ flip: showFront }" :style="{ '--delay': `${delay}ms` }">
    <div v-if="!showFront" class="face back">
      <img :src="backSrc" alt="Card back" />
    </div>
    <div v-else class="face front">
      <img :src="src" :alt="alt" @error="onError" />
    </div>
  </div>
</template>
<script setup>
import { computed, ref, watchEffect } from 'vue'

const props = defineProps({ rank:String, suit:String, delay:{type:Number,default:0}, flipped:{type:Boolean,default:true}, size:{ type:Number,default:1} })
const isHidden = computed(()=> ['BACK','HIDDEN'].includes((props.rank||'').toUpperCase()))
const rankMap = {
  ACE:'A', TWO:'2', THREE:'3', FOUR:'4', FIVE:'5', SIX:'6', SEVEN:'7', EIGHT:'8', NINE:'9', TEN:'10', JACK:'J', QUEEN:'Q', KING:'K',
  ONE:'A', I:'A'
}
const suitMap = {
  HEARTS:'H', HEART:'H', DIAMONDS:'D', DIAMOND:'D', CLUBS:'C', CLUB:'C', CLOVER:'C', SPADES:'S', SPADE:'S',
  H:'H', D:'D', C:'C', S:'S'
}

const code = computed(() => {
  const r=(props.rank||''), s=(props.suit||'')
  const rr=(rankMap[r.toUpperCase()]||r).replace(/^T$/,'10')
  const ss=(suitMap[s.toUpperCase()]||s).toUpperCase()
  return `${rr}_${ss}`
})

const alt = computed(() => `${props.rank} of ${props.suit}`)
const showFront = computed(() => props.flipped && !isHidden.value)
const attempts = ref([])
const src = ref('')

function buildAttempts(){
  const base = (code.value || '').trim()
  const suitNames = { H:'HEARTS', D:'DIAMONDS', C:'CLUBS', S:'SPADES' }
  const attempts = []
  const added = new Set()

  function addVariant(name){
    if(!name) return
    const clean = name.replace(/\s+/g, '')
    if(!clean) return
    const forms = [clean, clean.toUpperCase(), clean.toLowerCase()]
    for(const form of forms){
      for(const ext of ['png', 'webp', 'svg']){
        const filename = `${form}.${ext}`
        if(!added.has(filename)){
          added.add(filename)
          attempts.push(filename)
        }
      }
    }
  }

  if(base){
    const [rankPart = '', rawSuitPart = ''] = base.split('_')
    const suitPart = rawSuitPart || ''
    const longSuit = suitNames[suitPart] || suitPart
    const combos = new Set()

    combos.add(`${rankPart}${suitPart ? `_${suitPart}` : ''}`.replace(/_+$/,''))
    combos.add(base)

    if(suitPart){
      combos.add(`${rankPart}_${longSuit}`)
      combos.add(`${rankPart}${suitPart}`)
      combos.add(`${rankPart}${longSuit}`)
      combos.add(`${rankPart}-${suitPart}`)
      combos.add(`${rankPart}-${longSuit}`)
    }

    const compact = base.replace(/_/g, '')
    const dashed = base.replace(/_/g, '-')
    combos.add(compact)
    combos.add(dashed)

    for(const combo of combos){
      addVariant(combo)
    }
  }

  addVariant('BACK')
  return attempts
}

const basePath = (import.meta.env.BASE_URL || '/').replace(/\/$/, '')

function resolve(path){
  return `${basePath}${path}`
}

const backSrc = resolve('/cards/BACK.png')

function nextSrc(){
  const attempt = attempts.value.shift()
  return attempt ? resolve(`/cards/${attempt}`) : backSrc
}

watchEffect(() => {
  attempts.value = buildAttempts()
  const next = attempts.value.shift()
  src.value = next ? resolve(`/cards/${next}`) : backSrc
})

function onError(){
  if(!attempts.value.length){
    src.value = backSrc
    return
  }
  src.value = nextSrc()
}
</script>
<style scoped>
.card3d{ width: calc(var(--card-w)*var(--s,1)); height: calc(var(--card-h)*var(--s,1)); perspective: 600px; display:inline-block; margin: 2px; animation: deal .4s ease var(--delay) both; position: relative; transform-style: preserve-3d; --s: v-bind(size); }
.face{ width:100%; height:100%; position:absolute; top:0; left:0; backface-visibility: hidden; border-radius: 10px; overflow: hidden; box-shadow: 0 6px 14px rgba(0,0,0,.35); transition: transform .45s ease; }
.front{ transform: rotateY(180deg); background:#fff; display:grid; place-items:center; }
.back{ background: radial-gradient(600px 300px at 50% -50%, #2753b6, #0e2a68); border:1px solid #0b2e66; transform: rotateY(0deg); display:grid; place-items:center; }
img{ width:100%; height:100%; object-fit: cover; }
.card3d.flip .front{ transform: rotateY(0deg); }
.card3d.flip .back{ transform: rotateY(180deg); }
@keyframes deal{ from{ transform: translateY(-14px); opacity:0; } to{ transform: translateY(0); opacity:1; } }
</style>
