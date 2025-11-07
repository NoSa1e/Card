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

    <section class="betting">
      <div class="field">
        <label>주 베팅</label>
        <select v-model="main">
          <option value="PLAYER">Player</option>
          <option value="BANKER">Banker</option>
          <option value="TIE">Tie</option>
        </select>
      </div>
      <div class="field amount">
        <label>베팅 금액</label>
        <div class="bet-input">
          <input v-model.number="amount" type="number" min="10" step="10" />
        </div>
        <ChipTray
          class="bet-chips"
          v-model="amount"
          :min="10"
          :disabled="loading"
        />
      </div>
      <div class="switches">
        <label><input type="checkbox" v-model="pairPlayer" /> Player Pair</label>
        <label><input type="checkbox" v-model="pairBanker" /> Banker Pair</label>
        <label><input type="checkbox" v-model="super6" /> Super 6</label>
      </div>
      <div class="actions">
        <button class="primary" :disabled="loading" @click="place">베팅</button>
        <button @click="deal" :disabled="loading">딜 및 정산</button>
        <button @click="refresh" :disabled="loading">상태 새로고침</button>
      </div>
    </section>

    <section class="ledger" v-if="Object.keys(ledger).length">
      <h3>베팅 현황</h3>
      <table>
        <thead>
          <tr><th>플레이어</th><th>메인</th><th>Player Pair</th><th>Banker Pair</th><th>Super6</th></tr>
        </thead>
        <tbody>
          <tr v-for="(entry, name) in ledger" :key="name">
            <td>{{ name }}</td>
            <td>{{ mainOf(entry) }}</td>
            <td>{{ entry.PAIR_P || 0 }}</td>
            <td>{{ entry.PAIR_B || 0 }}</td>
            <td>{{ entry.SUPER6 || 0 }}</td>
          </tr>
        </tbody>
      </table>
    </section>

    <section class="table" v-if="player || banker">
      <div class="side">
        <header>
          <h3>Player</h3>
          <span class="total">{{ player?.total }}</span>
        </header>
        <div class="cards">
          <CardImg
            v-for="(card, index) in player.cards"
            :key="`p-${index}`"
            :rank="card.rank"
            :suit="card.suit"
            :delay="index*50"
          />
        </div>
      </div>
      <div class="side">
        <header>
          <h3>Banker</h3>
          <span class="total">{{ banker?.total }}</span>
        </header>
        <div class="cards">
          <CardImg
            v-for="(card, index) in banker.cards"
            :key="`b-${index}`"
            :rank="card.rank"
            :suit="card.suit"
            :delay="index*50"
          />
        </div>
      </div>
    </section>

    <section class="settle" v-if="Object.keys(settle).length">
      <h3>정산 결과</h3>
      <ul>
        <li v-for="(value, name) in settle" :key="`settle-${name}`">
          {{ name }}: <strong :class="{ win: value>0, lose: value<0 }">{{ value>0?`+${value}`:value }}</strong>
        </li>
      </ul>
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
  decks: { type: Number, default: 6 },
  players: { type: Array, default: () => [] }
})

const main = ref('PLAYER')
const amount = ref(100)
const pairPlayer = ref(false)
const pairBanker = ref(false)
const super6 = ref(false)
const loading = ref(false)
const ledger = reactive({})
const player = ref(null)
const banker = ref(null)
const settle = reactive({})

const user = computed(() => props.user)
const roomId = computed(() => props.roomId)
const roomName = computed(() => props.roomName)
const decks = computed(() => props.decks || 6)
const participants = computed(() => props.players && props.players.length ? props.players : Object.keys(ledger))

onMounted(() => {
  if(roomId.value){
    refresh()
  }
})

function mainOf(entry){
  const key = Object.keys(entry || {}).find(k => k.startsWith('MAIN_'))
  return key ? `${key.replace('MAIN_', '')}=${entry[key]}` : '-'
}

async function place(){
  if(!roomId.value || !user.value) return
  loading.value = true
  try{
    const params = new URLSearchParams({
      roomId: roomId.value,
      user: user.value,
      main: main.value,
      amount: String(Math.max(10, amount.value || 0)),
      pairP: String(pairPlayer.value),
      pairB: String(pairBanker.value),
      super6: String(super6.value),
      decks: String(Math.max(1, decks.value || 1))
    })
    const res = await jpost(`/api/baccarat/room/bet?${params.toString()}`)
    apply(res.detail || res)
  }catch(err){
    console.error(err)
  }finally{
    loading.value = false
  }
}

async function deal(){
  if(!roomId.value) return
  loading.value = true
  try{
    const params = new URLSearchParams({ roomId: roomId.value })
    const res = await jpost(`/api/baccarat/room/deal?${params.toString()}`)
    apply(res.detail || res)
  }catch(err){
    console.error(err)
  }finally{
    loading.value = false
  }
}

async function refresh(){
  if(!roomId.value) return
  loading.value = true
  try{
    const params = new URLSearchParams({ roomId: roomId.value })
    const res = await jget(`/api/baccarat/room/state?${params.toString()}`)
    apply(res.detail || res)
  }catch(err){
    console.error(err)
  }finally{
    loading.value = false
  }
}

function apply(detail){
  if(detail.ledger){
    Object.keys(ledger).forEach(k => delete ledger[k])
    Object.assign(ledger, detail.ledger)
  }else if(detail.ledger === null){
    Object.keys(ledger).forEach(k => delete ledger[k])
  }
  if(detail.player){
    player.value = detail.player
  }else if(detail.player === null){
    player.value = null
  }
  if(detail.banker){
    banker.value = detail.banker
  }else if(detail.banker === null){
    banker.value = null
  }
  if(detail.settle){
    Object.keys(settle).forEach(k => delete settle[k])
    Object.assign(settle, detail.settle)
  }else if(detail.settle === null){
    Object.keys(settle).forEach(k => delete settle[k])
  }
}
</script>
<style scoped>
.game-board{ display:flex; flex-direction:column; gap:24px; }
.info-row{ display:flex; gap:24px; justify-content:space-between; background:rgba(255,255,255,.06); border-radius:16px; padding:18px 24px; flex-wrap:wrap; }
.info-row div{ min-width:180px; }
.label{ display:block; color:rgba(255,255,255,.65); font-size:.85rem; margin-bottom:6px; }
.betting{ display:grid; gap:16px; background:rgba(255,255,255,.06); border-radius:18px; padding:18px 22px; }
.field{ display:flex; flex-direction:column; gap:8px; }
.field.amount{ gap:12px; }
.bet-input{ display:flex; gap:12px; flex-wrap:wrap; align-items:center; }
.bet-input input{ flex:0 0 160px; }
.bet-chips{ width:100%; }
.field label{ color:rgba(255,255,255,.7); font-size:.95rem; }
.field select,.field input{ padding:12px 14px; border-radius:12px; border:1px solid rgba(255,255,255,.18); background:rgba(8,14,24,.75); color:#fff; }
.switches{ display:flex; gap:16px; flex-wrap:wrap; color:rgba(255,255,255,.8); }
.actions{ display:flex; gap:12px; flex-wrap:wrap; }
.actions button{ padding:12px 18px; border-radius:12px; border:none; background:rgba(255,255,255,.08); color:#fff; cursor:pointer; }
.actions .primary{ background:linear-gradient(135deg,#5d9cff,#3d72ff); box-shadow:0 18px 32px rgba(61,114,255,.35); }
.actions button:disabled{ opacity:.5; cursor:not-allowed; box-shadow:none; }
.ledger{ background:rgba(8,14,24,.75); border-radius:18px; padding:24px; border:1px solid rgba(255,255,255,.08); }
.ledger table{ width:100%; border-collapse:collapse; margin-top:12px; }
.ledger th,.ledger td{ padding:10px 12px; border-bottom:1px solid rgba(255,255,255,.08); text-align:left; }
.table{ display:grid; gap:18px; grid-template-columns:repeat(auto-fit,minmax(240px,1fr)); }
.side{ background:rgba(8,14,24,.75); border-radius:18px; padding:24px; border:1px solid rgba(255,255,255,.08); display:flex; flex-direction:column; gap:16px; }
.side header{ display:flex; justify-content:space-between; align-items:center; }
.total{ color:rgba(255,255,255,.7); font-weight:600; }
.cards{ display:flex; flex-wrap:wrap; gap:10px; justify-content:center; }
.settle{ background:rgba(255,255,255,.06); border-radius:18px; padding:18px 22px; }
.settle ul{ margin:0; padding-left:18px; }
.settle li{ margin-bottom:6px; }
.settle strong.win{ color:#53f6b2; }
.settle strong.lose{ color:#ff7a7a; }
.empty{ color:rgba(255,255,255,.6); text-align:center; padding:40px 0; }
</style>
