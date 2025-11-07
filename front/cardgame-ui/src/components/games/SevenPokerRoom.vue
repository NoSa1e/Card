<template>
  <div class="game-board" v-if="roomId && user">
    <section class="info-row">
      <div>
        <span class="label">방 ID</span>
        <strong>{{ roomId }}</strong>
      </div>
      <div>
        <span class="label">모드</span>
        <strong>{{ modeLabel }}</strong>
      </div>
      <div>
        <span class="label">현재 단계</span>
        <strong>{{ stage }}</strong>
      </div>
      <div>
        <span class="label">포트</span>
        <strong>{{ pot.toLocaleString() }}</strong>
      </div>
    </section>

    <section class="controls">
      <div class="bet">
        <div class="bet-header">
          <label>앤티</label>
        </div>
        <div class="bet-input">
          <input v-model.number="ante" type="number" min="10" step="10" />
          <button class="primary" :disabled="loading || (!isHost && mode==='multi')" @click="start">게임 시작</button>
        </div>
        <ChipTray
          class="bet-chips"
          v-model="ante"
          :min="10"
          :disabled="loading || (!isHost && mode==='multi')"
          :denominations="[10, 50, 100, 500, 1000]"
        />
      </div>
      <div class="actions">
        <button @click="nextStage" :disabled="loading || (!isHost && mode==='multi')">다음 단계</button>
        <button @click="refresh" :disabled="loading">상태 새로고침</button>
      </div>
    </section>

    <section class="players">
      <article v-for="side in players" :key="side.user" :class="['player', { me: side.user === user }]">
        <header>
          <h4>{{ side.user }}</h4>
        </header>
        <div class="cards">
          <CardImg
            v-for="(card, index) in side.cards"
            :key="`${side.user}-${index}`"
            :rank="card.rank"
            :suit="card.suit"
            :flipped="card.rank !== 'BACK'"
            :delay="index*40"
          />
        </div>
        <div v-if="side.user === user" class="player-chips">
          <ChipTray
            v-model="betAmount"
            :min="10"
            :denominations="[10, 50, 100, 500, 1000]"
            :disabled="loading"
          />
        </div>
        <div class="player-actions">
          <button @click="bet(side.user)" :disabled="loading || side.user !== user">베팅 +{{ betAmountDisplay }}</button>
          <button @click="check(side.user)" :disabled="loading || side.user !== user">체크</button>
          <button @click="fold(side.user)" :disabled="loading || side.user !== user">폴드</button>
        </div>
      </article>
    </section>
  </div>
  <div v-else class="empty">방 정보가 없습니다.</div>
</template>
<script setup>
import { computed, ref, onMounted, watch } from 'vue'
import CardImg from '../CardImg.vue'
import ChipTray from '../ChipTray.vue'
import { jget, jpost } from '../../api'

const props = defineProps({
  user: { type: String, required: true },
  mode: { type: String, default: 'solo' },
  roomId: { type: String, required: true },
  players: { type: Array, default: () => [] }
})

const ante = ref(50)
const betAmount = ref(50)
const loading = ref(false)
const stage = ref('READY')
const pot = ref(0)
const playersState = ref([])

const user = computed(() => props.user)
const mode = computed(() => props.mode)
const roomId = computed(() => props.roomId)
const providedPlayers = computed(() => props.players || [])
const modeLabel = computed(() => mode.value === 'multi' ? '멀티플레이' : '솔로 플레이')
const isHost = computed(() => {
  if(mode.value === 'solo') return true
  if(!providedPlayers.value.length) return user.value ? true : false
  return providedPlayers.value[0] === user.value
})

const players = computed(() => playersState.value)
const betAmountDisplay = computed(() => Math.max(10, Math.round(betAmount.value || 0)).toLocaleString())
const betAmountValue = computed(() => Math.max(10, Math.round(betAmount.value || 0)))

watch(ante, (value) => {
  if(value < 10){
    ante.value = 10
  }
})

watch(betAmount, (value) => {
  if(value < 10){
    betAmount.value = 10
  }
})

onMounted(() => {
  if(roomId.value){
    refresh()
  }
})

async function start(){
  if(!roomId.value || !user.value) return
  loading.value = true
  try{
    const list = mode.value === 'solo'
      ? [user.value, `${user.value}_AI1`, `${user.value}_AI2`]
      : (providedPlayers.value.length ? providedPlayers.value : [user.value])
    const params = new URLSearchParams({
      roomId: roomId.value,
      users: list.join(','),
      ante: String(Math.max(10, ante.value || 0))
    })
    await jpost(`/api/seven/start?${params.toString()}`)
    await refresh()
  }catch(err){
    console.error(err)
  }finally{
    loading.value = false
  }
}

async function refresh(){
  if(!roomId.value || !user.value) return
  loading.value = true
  try{
    const params = new URLSearchParams({ roomId: roomId.value, viewer: user.value })
    const res = await jget(`/api/seven/state?${params.toString()}`)
    const detail = res.detail || res
    stage.value = detail.stage || 'READY'
    pot.value = detail.pot || 0
    playersState.value = detail.players || []
  }catch(err){
    console.error(err)
  }finally{
    loading.value = false
  }
}

async function bet(target){ await action('/api/seven/bet', { user: target, amount: betAmountValue.value }) }
async function check(target){ await action('/api/seven/check', { user: target }) }
async function fold(target){ await action('/api/seven/fold', { user: target }) }
async function nextStage(){ await action('/api/seven/next') }

async function action(path, extra={}){
  if(!roomId.value) return
  loading.value = true
  try{
    const params = new URLSearchParams({ roomId: roomId.value })
    if(extra.user){ params.set('user', extra.user) }
    if(extra.amount){ params.set('amount', String(extra.amount)) }
    await jpost(`${path}?${params.toString()}`)
    await refresh()
  }catch(err){
    console.error(err)
  }finally{
    loading.value = false
  }
}
</script>
<style scoped>
.game-board{ display:flex; flex-direction:column; gap:24px; }
.info-row{ display:flex; gap:24px; justify-content:space-between; background:rgba(255,255,255,.06); border-radius:16px; padding:18px 24px; flex-wrap:wrap; }
.info-row div{ min-width:160px; }
.label{ display:block; color:rgba(255,255,255,.65); font-size:.85rem; margin-bottom:6px; }
.controls{ display:flex; flex-direction:column; gap:16px; }
.bet{ display:flex; flex-direction:column; gap:16px; background:rgba(255,255,255,.06); padding:16px 18px; border-radius:16px; }
.bet-header{ display:flex; justify-content:space-between; align-items:center; }
.bet-input{ display:flex; gap:12px; flex-wrap:wrap; align-items:center; }
.bet input{ flex:0 0 140px; padding:10px 12px; border-radius:10px; border:1px solid rgba(255,255,255,.18); background:rgba(8,14,24,.75); color:#fff; }
.bet-chips{ width:100%; }
.primary{ padding:12px 20px; border:none; border-radius:12px; cursor:pointer; background:linear-gradient(135deg,#34d899,#1baf75); color:#fff; font-weight:600; box-shadow:0 14px 26px rgba(27,175,117,.28); }
.primary:disabled{ box-shadow:none; }
.actions{ display:flex; gap:12px; flex-wrap:wrap; }
.actions button{ padding:12px 18px; border-radius:12px; border:none; background:rgba(255,255,255,.08); color:#fff; cursor:pointer; }
.actions button:disabled{ opacity:.5; cursor:not-allowed; }
.players{ display:grid; grid-template-columns:repeat(auto-fit,minmax(240px,1fr)); gap:18px; }
.player{ background:rgba(8,14,24,.75); border-radius:18px; padding:18px; border:1px solid rgba(255,255,255,.08); display:flex; flex-direction:column; gap:12px; }
.player.me{ border-color:#5d9cff; box-shadow:0 0 0 3px rgba(93,156,255,.3); }
.cards{ display:flex; flex-wrap:wrap; gap:10px; justify-content:center; }
.player-chips{ margin-top:8px; }
.player-actions{ display:flex; gap:12px; flex-wrap:wrap; justify-content:center; }
.player-actions button{ padding:10px 14px; border-radius:10px; border:none; background:rgba(255,255,255,.08); color:#fff; cursor:pointer; }
.empty{ color:rgba(255,255,255,.6); text-align:center; padding:40px 0; }
</style>
