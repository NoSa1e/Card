<template>
  <div class="game-board" v-if="user">
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
          :balance="balance"
          :disabled="loading"
        />
      </div>
      <div class="switches">
        <label><input type="checkbox" v-model="pairPlayer" /> Player Pair</label>
        <label><input type="checkbox" v-model="pairBanker" /> Banker Pair</label>
        <label><input type="checkbox" v-model="super6" /> Super 6</label>
      </div>
      <button class="primary" :disabled="loading" @click="betting">베팅 및 딜</button>
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

    <section class="result" v-if="result">
      <p>정산: <strong :class="{ win: result.delta > 0, lose: result.delta < 0 }">{{ result.delta>0?`+${result.delta}`:result.delta }}</strong></p>
      <p>현재 칩: <strong>{{ result.balance.toLocaleString() }}</strong></p>
    </section>
  </div>
  <div v-else class="empty">사용자 정보가 없습니다.</div>
</template>
<script setup>
import { computed, ref } from 'vue'
import CardImg from '../CardImg.vue'
import ChipTray from '../ChipTray.vue'
import { jpost } from '../../api'

const props = defineProps({
  user: { type: String, required: true },
  decks: { type: Number, default: 6 }
})

const main = ref('PLAYER')
const amount = ref(100)
const pairPlayer = ref(false)
const pairBanker = ref(false)
const super6 = ref(false)
const loading = ref(false)
const player = ref(null)
const banker = ref(null)
const result = ref(null)

const balance = computed(() => {
  return typeof result.value?.balance === 'number' ? result.value.balance : undefined
})

const user = computed(() => props.user)

async function betting(){
  if(!user.value) return
  loading.value = true
  try{
    const params = new URLSearchParams({
      user: user.value,
      amount: String(Math.max(10, amount.value || 0)),
      main: main.value,
      pairPlayer: String(pairPlayer.value),
      pairBanker: String(pairBanker.value),
      super6: String(super6.value),
      decks: String(Math.max(1, props.decks || 1))
    })
    const res = await jpost(`/api/baccarat/solo/bet?${params.toString()}`)
    const detail = res.detail || res
    player.value = detail.player || null
    banker.value = detail.banker || null
    if(detail.delta !== undefined || typeof detail.balance === 'number'){
      result.value = {
        delta: detail.delta ?? 0,
        balance: typeof detail.balance === 'number' ? detail.balance : (result.value?.balance ?? 0)
      }
    }else{
      result.value = null
    }
  }catch(err){
    console.error(err)
  }finally{
    loading.value = false
  }
}
</script>
<style scoped>
.game-board{ display:flex; flex-direction:column; gap:24px; }
.betting{ display:grid; gap:16px; background:rgba(255,255,255,.06); border-radius:18px; padding:18px 22px; }
.field{ display:flex; flex-direction:column; gap:8px; }
.field.amount{ gap:12px; }
.bet-input{ display:flex; gap:12px; flex-wrap:wrap; align-items:center; }
.bet-input input{ flex:0 0 160px; }
.bet-chips{ width:100%; }
.field label{ color:rgba(255,255,255,.7); font-size:.95rem; }
.field select,.field input{ padding:12px 14px; border-radius:12px; border:1px solid rgba(255,255,255,.18); background:rgba(8,14,24,.75); color:#fff; }
.switches{ display:flex; gap:16px; flex-wrap:wrap; color:rgba(255,255,255,.8); }
.primary{ justify-self:flex-start; padding:12px 20px; border-radius:12px; border:none; background:linear-gradient(135deg,#5d9cff,#3d72ff);
  color:#fff; font-weight:600; cursor:pointer; box-shadow:0 18px 32px rgba(61,114,255,.35); }
.primary:disabled{ opacity:.5; cursor:not-allowed; box-shadow:none; }
.table{ display:grid; gap:18px; grid-template-columns:repeat(auto-fit,minmax(240px,1fr)); }
.side{ background:rgba(8,14,24,.75); border-radius:18px; padding:24px; border:1px solid rgba(255,255,255,.08); display:flex; flex-direction:column; gap:16px; }
.side header{ display:flex; justify-content:space-between; align-items:center; }
.total{ color:rgba(255,255,255,.7); font-weight:600; }
.cards{ display:flex; flex-wrap:wrap; gap:10px; justify-content:center; }
.result{ background:rgba(255,255,255,.06); border-radius:16px; padding:16px 20px; text-align:center; color:rgba(255,255,255,.8); }
.result strong{ font-size:1.2rem; }
.result strong.win{ color:#53f6b2; }
.result strong.lose{ color:#ff7a7a; }
.empty{ color:rgba(255,255,255,.6); text-align:center; padding:40px 0; }
</style>
