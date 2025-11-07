<template>
  <div class="game-board" v-if="user">
    <section class="info-row">
      <div>
        <span class="label">보유 칩</span>
        <strong>{{ (state.balance ?? 0).toLocaleString() }}</strong>
      </div>
      <div>
        <span class="label">현재 베팅</span>
        <strong>{{ state.betAmount.toLocaleString() }}</strong>
      </div>
      <div v-if="!state.inProgress">
        <span class="label">마지막 결과</span>
        <strong :class="{ win: state.delta > 0, lose: state.delta < 0 }">
          {{ state.delta>0?`+${state.delta}`:state.delta }}
        </strong>
      </div>
    </section>

    <section class="table">
      <header>
        <h3>딜러</h3>
        <span class="total">{{ dealerTotal }}</span>
      </header>
      <div class="cards">
        <CardImg
          v-for="(card, index) in state.dealer.cards"
          :key="`dealer-${index}`"
          :rank="card.rank"
          :suit="card.suit"
          :flipped="!state.inProgress || index === 0"
          :delay="index*60"
        />
      </div>
    </section>

    <section class="hands">
      <article
        v-for="(hand, idx) in state.playerHands"
        :key="`hand-${idx}`"
        :class="['hand', { active: idx === state.activeIndex && state.inProgress }]"
      >
        <div class="hand-header">
          <h4>{{ user }} <span v-if="state.playerHands.length>1">#{{ idx+1 }}</span></h4>
          <span class="total">{{ hand.total ?? '??' }}</span>
        </div>
        <div class="cards">
          <CardImg
            v-for="(card, index) in hand.cards"
            :key="`player-${idx}-${index}`"
            :rank="card.rank"
            :suit="card.suit"
            :delay="index*50"
          />
        </div>
        <p v-if="hand.done" class="status">완료</p>
      </article>
    </section>

    <section class="controls">
      <div class="bet" v-if="!state.inProgress">
        <div class="bet-header">
          <label>베팅 금액</label>
          <span v-if="state.balance !== null" class="bet-balance">잔액 {{ state.balance.toLocaleString() }}</span>
        </div>
        <div class="bet-input">
          <input v-model.number="bet" type="number" min="10" step="10" />
          <button class="primary" :disabled="loading" @click="start">라운드 시작</button>
        </div>
        <ChipTray
          class="bet-chips"
          v-model="bet"
          :min="10"
          :max="maxBet"
          :balance="state.balance ?? undefined"
          :disabled="loading"
        />
      </div>
      <div class="actions" v-else>
        <button @click="hit" :disabled="loading">히트</button>
        <button @click="stand" :disabled="loading">스탠드</button>
        <button @click="dbl" :disabled="loading">더블다운</button>
        <button @click="split" :disabled="loading">스플릿</button>
        <button @click="surrender" :disabled="loading">서렌더</button>
      </div>
    </section>
  </div>
  <div v-else class="empty">사용자 정보가 없습니다.</div>
</template>
<script setup>
import { computed, reactive, ref, watch } from 'vue'
import CardImg from '../CardImg.vue'
import ChipTray from '../ChipTray.vue'
import { jpost } from '../../api'

const props = defineProps({
  user: { type: String, required: true },
  decks: { type: Number, default: 4 }
})

const bet = ref(100)
const loading = ref(false)
const state = reactive({
  inProgress: false,
  dealer: { cards: [] },
  playerHands: [],
  activeIndex: 0,
  betAmount: 0,
  delta: 0,
  balance: null
})

const user = computed(() => props.user)
const dealerTotal = computed(() => state.inProgress ? '??' : (state.dealer.total ?? ''))
const maxBet = computed(() => typeof state.balance === 'number' ? Math.max(10, state.balance) : Number.POSITIVE_INFINITY)

watch(maxBet, (limit) => {
  if(Number.isFinite(limit) && bet.value > limit){
    bet.value = limit
  }
})

watch(bet, (value, old) => {
  if(value < 10){
    bet.value = 10
  }
}, { flush: 'post' })

watch(() => props.decks, () => {
  if(state.inProgress){
    resetState()
  }
})

function resetState(){
  state.inProgress = false
  state.dealer = { cards: [] }
  state.playerHands = []
  state.activeIndex = 0
  state.betAmount = 0
  state.delta = 0
}

function apply(detail){
  state.inProgress = !!detail.inProgress
  state.dealer = detail.dealer || { cards: [] }
  state.playerHands = detail.playerHands || []
  state.activeIndex = detail.activeIndex ?? 0
  state.betAmount = detail.bet ?? bet.value
  state.delta = detail.delta ?? 0
  if(typeof detail.balance === 'number'){
    state.balance = detail.balance
  }
}

async function start(){
  if(!user.value) return
  loading.value = true
  try{
    const params = new URLSearchParams({
      user: user.value,
      bet: String(Math.max(10, bet.value || 0)),
      decks: String(Math.max(1, props.decks || 1))
    })
    const res = await jpost(`/api/blackjack/solo/start?${params.toString()}`)
    apply(res.detail || res)
  }catch(err){
    console.error(err)
  }finally{
    loading.value = false
  }
}

async function hit(){ await action('/api/blackjack/solo/hit') }
async function stand(){ await action('/api/blackjack/solo/stand') }
async function dbl(){ await action('/api/blackjack/solo/double') }
async function surrender(){ await action('/api/blackjack/solo/surrender') }
async function split(){ await action('/api/blackjack/solo/split') }

async function action(path){
  if(!user.value) return
  loading.value = true
  try{
    const params = new URLSearchParams({ user: user.value })
    const res = await jpost(`${path}?${params.toString()}`)
    apply(res.detail || res)
  }catch(err){
    console.error(err)
  }finally{
    loading.value = false
  }
}
</script>
<style scoped>
.game-board{ display:flex; flex-direction:column; gap:24px; }
.info-row{ display:flex; gap:24px; justify-content:center; background:rgba(255,255,255,.06); border-radius:16px; padding:18px 24px; }
.info-row div{ text-align:center; }
.label{ display:block; color:rgba(255,255,255,.65); font-size:.85rem; margin-bottom:6px; }
.info-row strong{ font-size:1.4rem; }
.info-row strong.win{ color:#53f6b2; }
.info-row strong.lose{ color:#ff7a7a; }
.table{ background:rgba(8,14,24,.75); border-radius:18px; padding:24px; border:1px solid rgba(255,255,255,.08); }
.table header{ display:flex; justify-content:space-between; align-items:center; margin-bottom:16px; }
.table h3{ font-size:1.3rem; }
.total{ color:rgba(255,255,255,.68); font-weight:600; }
.cards{ display:flex; flex-wrap:wrap; gap:10px; justify-content:center; }
.hands{ display:grid; grid-template-columns:repeat(auto-fit,minmax(220px,1fr)); gap:16px; }
.hand{ background:rgba(12,20,36,.65); border:1px solid rgba(255,255,255,.08); border-radius:18px; padding:18px; display:flex; flex-direction:column; gap:12px; }
.hand.active{ border-color:#ffb347; box-shadow:0 0 0 3px rgba(255,179,71,.25); }
.hand-header{ display:flex; justify-content:space-between; align-items:center; }
.hand-header h4{ margin:0; font-size:1.1rem; }
.status{ text-align:right; color:rgba(255,255,255,.6); }
.controls{ display:flex; flex-direction:column; gap:16px; align-items:center; }
.bet{ display:flex; flex-direction:column; gap:16px; align-items:stretch; background:rgba(255,255,255,.06); padding:16px 18px; border-radius:16px; }
.bet-header{ display:flex; justify-content:space-between; align-items:center; gap:12px; }
.bet-header label{ font-weight:600; }
.bet-balance{ color:rgba(255,255,255,.65); font-size:.9rem; }
.bet-input{ display:flex; gap:12px; flex-wrap:wrap; align-items:center; }
.bet input{ flex:0 0 140px; padding:10px 12px; border-radius:10px; border:1px solid rgba(255,255,255,.18); background:rgba(8,14,24,.75); color:#fff; }
.bet button{ padding:12px 20px; border:none; border-radius:12px; cursor:pointer; background:linear-gradient(135deg,#34d899,#1baf75); color:#fff; font-weight:600; box-shadow:0 14px 26px rgba(27,175,117,.28); }
.bet button:disabled{ box-shadow:none; }
.bet-chips{ width:100%; }
.actions{ display:flex; flex-wrap:wrap; gap:12px; justify-content:center; }
.actions button{ padding:12px 18px; border-radius:12px; border:none; background:rgba(255,255,255,.08); color:#fff; cursor:pointer; transition:background .2s ease; }
.actions button:hover{ background:rgba(255,255,255,.16); }
.actions button:disabled{ opacity:.5; cursor:not-allowed; }
.empty{ color:rgba(255,255,255,.6); text-align:center; padding:40px 0; }
</style>
