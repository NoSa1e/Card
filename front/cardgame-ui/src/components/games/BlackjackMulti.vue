<template>
  <div class="game-board" v-if="user && roomId">
    <section class="info-row">
      <div>
        <span class="label">방 이름</span>
        <strong>{{ roomName || roomId }}</strong>
      </div>
      <div>
        <span class="label">덱</span>
        <strong>{{ decks }}덱</strong>
      </div>
      <div>
        <span class="label">플레이어</span>
        <strong>{{ (participants || []).join(', ') }}</strong>
      </div>
    </section>

    <section class="controls">
      <div class="bet">
        <div class="bet-header">
          <label>베팅 금액</label>
          <span class="hint" v-if="!isHost">방장만 시작할 수 있습니다.</span>
        </div>
        <div class="bet-input">
          <input v-model.number="bet" type="number" min="10" step="10" />
          <button class="primary" :disabled="loading || !isHost" @click="start">
            라운드 시작
          </button>
        </div>
        <ChipTray
          class="bet-chips"
          v-model="bet"
          :min="10"
          :disabled="loading || !isHost"
        />
      </div>
      <div class="actions">
        <button @click="hit" :disabled="loading">히트</button>
        <button @click="stand" :disabled="loading">스탠드</button>
        <button @click="refresh" :disabled="loading">상태 새로고침</button>
      </div>
    </section>

    <section class="table">
      <header>
        <h3>딜러</h3>
        <span class="total">{{ dealerTotal }}</span>
      </header>
      <div class="cards">
        <CardImg
          v-for="(card, index) in dealer.cards"
          :key="`dealer-${index}`"
          :rank="card.rank"
          :suit="card.suit"
          :flipped="card.rank !== 'BACK'"
          :delay="index*60"
        />
      </div>
    </section>

    <section class="hands">
      <article v-for="hand in hands" :key="hand.user" class="hand" :class="{ me: hand.user === user }">
        <div class="hand-header">
          <h4>{{ hand.user }}</h4>
          <span class="total">{{ hand.total ?? '??' }}</span>
        </div>
        <div class="cards">
          <CardImg
            v-for="(card, index) in hand.cards"
            :key="`${hand.user}-${index}`"
            :rank="card.rank"
            :suit="card.suit"
            :flipped="card.rank !== 'BACK'"
            :delay="index*50"
          />
        </div>
      </article>
    </section>
  </div>
  <div v-else class="empty">방 정보가 올바르지 않습니다.</div>
</template>
<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import CardImg from '../CardImg.vue'
import ChipTray from '../ChipTray.vue'
import { jget, jpost } from '../../api'

const props = defineProps({
  user: { type: String, required: true },
  roomId: { type: String, required: true },
  roomName: { type: String, default: '' },
  decks: { type: Number, default: 4 },
  host: { type: String, default: '' },
  players: { type: Array, default: () => [] }
})

const bet = ref(100)
const loading = ref(false)
const state = reactive({
  inProgress: false,
  dealer: { cards: [] },
  hands: [],
  betAmount: 0
})

const user = computed(() => props.user)
const roomId = computed(() => props.roomId)
const roomName = computed(() => props.roomName)
const decks = computed(() => props.decks || 4)
const host = computed(() => props.host)
const participants = computed(() => props.players && props.players.length ? props.players : state.hands.map(h => h.user))
const isHost = computed(() => user.value && user.value === host.value)
const dealer = computed(() => state.dealer || { cards: [] })
const hands = computed(() => state.hands || [])
const dealerTotal = computed(() => state.inProgress ? '??' : (state.dealer.total ?? ''))

onMounted(() => {
  if(roomId.value){
    refresh()
  }
})

function apply(detail){
  state.inProgress = !!detail.inProgress
  state.dealer = detail.dealer || { cards: [] }
  state.hands = detail.hands || []
  state.betAmount = detail.bet ?? bet.value
}

async function start(){
  if(!roomId.value || !user.value) return
  loading.value = true
  try{
    const params = new URLSearchParams({
      roomId: roomId.value,
      host: user.value,
      bet: String(Math.max(10, bet.value || 0)),
      decks: String(Math.max(1, decks.value || 1))
    })
    const res = await jpost(`/api/blackjack/room/start?${params.toString()}`)
    apply(res.detail || res)
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
    const res = await jget(`/api/blackjack/room/state?${params.toString()}`)
    apply(res.detail || res)
  }catch(err){
    console.error(err)
  }finally{
    loading.value = false
  }
}

async function hit(){ await action('/api/blackjack/room/hit') }
async function stand(){ await action('/api/blackjack/room/stand') }

async function action(path){
  if(!roomId.value || !user.value) return
  loading.value = true
  try{
    const params = new URLSearchParams({ roomId: roomId.value, user: user.value })
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
.info-row div{ min-width:180px; }
.label{ display:block; color:rgba(255,255,255,.65); font-size:.85rem; margin-bottom:6px; }
.controls{ display:flex; flex-direction:column; gap:16px; }
.bet{ display:flex; flex-direction:column; gap:16px; background:rgba(255,255,255,.06); padding:16px 18px; border-radius:16px; }
.bet-header{ display:flex; justify-content:space-between; align-items:center; gap:12px; }
.bet-header label{ font-weight:600; }
.bet-input{ display:flex; gap:12px; flex-wrap:wrap; align-items:center; }
.bet input{ flex:0 0 140px; padding:10px 12px; border-radius:10px; border:1px solid rgba(255,255,255,.18); background:rgba(8,14,24,.75); color:#fff; }
.bet-chips{ width:100%; }
.primary{ padding:12px 20px; border:none; border-radius:12px; cursor:pointer; background:linear-gradient(135deg,#34d899,#1baf75); color:#fff; font-weight:600; box-shadow:0 14px 26px rgba(27,175,117,.28); }
.primary:disabled{ box-shadow:none; }
.hint{ color:rgba(255,255,255,.6); font-size:.85rem; }
.actions{ display:flex; flex-wrap:wrap; gap:12px; }
.actions button{ padding:12px 18px; border-radius:12px; border:none; background:rgba(255,255,255,.08); color:#fff; cursor:pointer; }
.actions button:disabled{ opacity:.5; cursor:not-allowed; }
.table{ background:rgba(8,14,24,.75); border-radius:18px; padding:24px; border:1px solid rgba(255,255,255,.08); }
.table header{ display:flex; justify-content:space-between; align-items:center; margin-bottom:16px; }
.total{ color:rgba(255,255,255,.68); font-weight:600; }
.cards{ display:flex; flex-wrap:wrap; gap:10px; justify-content:center; }
.hands{ display:grid; grid-template-columns:repeat(auto-fit,minmax(220px,1fr)); gap:16px; }
.hand{ background:rgba(12,20,36,.65); border:1px solid rgba(255,255,255,.08); border-radius:18px; padding:18px; display:flex; flex-direction:column; gap:12px; }
.hand.me{ border-color:#5d9cff; box-shadow:0 0 0 3px rgba(93,156,255,.3); }
.hand-header{ display:flex; justify-content:space-between; align-items:center; }
.hand-header h4{ margin:0; font-size:1.1rem; }
.empty{ color:rgba(255,255,255,.6); text-align:center; padding:40px 0; }
</style>
