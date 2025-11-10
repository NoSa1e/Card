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
      <div>
        <span class="label">현재 턴</span>
        <strong>{{ turnUser || '-' }}</strong>
      </div>
      <div>
        <span class="label">현재 베팅</span>
        <strong>{{ formatChips(currentBet) }}</strong>
      </div>
      <div>
        <span class="label">최소 레이즈</span>
        <strong>{{ formatChips(minRaise) }}</strong>
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
        <button @click="refresh" :disabled="loading">상태 새로고침</button>
      </div>
    </section>

    <section v-if="inProgress && pendingOrder.length" class="pending">
      <p>행동 대기: {{ pendingOrder.join(', ') }}</p>
    </section>

    <section v-if="winners.length" class="showdown">
      <p>
        쇼다운 결과: <strong>{{ winners.join(', ') }}</strong>
        <span v-if="settledPot">(팟 {{ formatChips(settledPot) }})</span>
      </p>
    </section>

    <section class="players">
      <article
        v-for="side in players"
        :key="side.user"
        :class="['player', { me: side.user === user, acting: side.user === turnUser, folded: side.folded, winner: side.winner }]"
      >
        <header>
          <h4>{{ side.user }}</h4>
          <small v-if="side.ai" class="profile">{{ side.profile || 'AI' }}</small>
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
        <div class="player-meta">
          <div class="bet-summary">
            <span>총 베팅</span>
            <strong>{{ formatChips(side.bet) }}</strong>
          </div>
          <div class="bet-summary" v-if="Number(side.toCall) > 0">
            <span>콜 필요</span>
            <strong>{{ formatChips(side.toCall) }}</strong>
          </div>
          <div class="action-status" :class="(side.action || '').toLowerCase()">
            {{ actionDisplay(side) }}
          </div>
          <div v-if="side.handRank" class="hand-rank">족보: {{ handRankLabel(side.handRank) }}</div>
          <div v-if="Number(side.payout)" class="hand-rank">획득: {{ formatChips(side.payout) }}</div>
        </div>
        <div v-if="side.user === user" class="player-chips">
          <ChipTray
            v-model="betAmount"
            :min="betMinimum"
            :denominations="[10, 50, 100, 500, 1000]"
            :disabled="loading"
          />
        </div>
        <div class="player-actions">
          <button @click="bet(side.user)" :disabled="!canRaise(side)">{{ betButtonLabel(side) }}</button>
          <button @click="call(side.user)" :disabled="!canCall(side)">{{ callLabel(side) }}</button>
          <button @click="check(side.user)" :disabled="!canCheck(side)">체크</button>
          <button @click="fold(side.user)" :disabled="!canAct(side)">폴드</button>
        </div>
        <div v-if="side.folded" class="fold-banner">폴드</div>
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
const round = ref(0)
const pot = ref(0)
const playersState = ref([])
const inProgress = ref(false)
const turn = ref(null)
const currentBet = ref(0)
const minRaise = ref(10)
const pendingOrder = ref([])
const winners = ref([])
const settledPot = ref(0)

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
const betAmountDisplay = computed(() => Math.max(betMinimum.value, Math.round(betAmount.value || 0)).toLocaleString())
const betAmountValue = computed(() => Math.max(betMinimum.value, Math.round(betAmount.value || 0)))
const turnUser = computed(() => turn.value)
const betMinimum = computed(() => Math.max(minRaise.value || 10, 10))

const callLabel = (side) => {
  const amount = Number(side?.toCall || 0)
  return amount > 0 ? `콜 ${formatChips(amount)}` : '콜'
}

const betButtonLabel = (side) => {
  if (currentBet.value <= 0) {
    return `베팅 ${betAmountDisplay.value}`
  }
  return `레이즈 +${betAmountDisplay.value}`
}

watch(ante, (value) => {
  if(value < 10){
    ante.value = 10
  }
})

watch(betAmount, (value) => {
  if(value < betMinimum.value){
    betAmount.value = betMinimum.value
  }
})

watch(minRaise, (value) => {
  if(betAmount.value < value){
    betAmount.value = value
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
      ? [user.value, `AI_${user.value}`]
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
    if(typeof detail.round === 'number'){ round.value = detail.round }
    pot.value = detail.pot || 0
    inProgress.value = Boolean(detail.inProgress)
    turn.value = detail.turn || null
    if(detail.ante){ ante.value = detail.ante }
    currentBet.value = detail.currentBet || 0
    minRaise.value = detail.minRaise || Math.max(10, ante.value)
    pendingOrder.value = detail.pending || []
    winners.value = detail.winners || []
    settledPot.value = detail.settledPot || 0
    playersState.value = detail.players || []
  }catch(err){
    console.error(err)
  }finally{
    loading.value = false
  }
}

async function bet(target){ await action('/api/seven/bet', { user: target, amount: betAmountValue.value }) }
async function call(target){ await action('/api/seven/call', { user: target }) }
async function check(target){ await action('/api/seven/check', { user: target }) }
async function fold(target){ await action('/api/seven/fold', { user: target }) }

async function action(path, extra={}){
  if(!roomId.value) return
  if(extra.user){
    const target = findSide(extra.user)
    if(!canAct(target)) return
  }
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

function findSide(uid){
  if(!uid) return null
  return players.value.find(p => p.user === uid) || null
}

function formatChips(value){
  const num = Number(value) || 0
  return Math.max(0, Math.round(num)).toLocaleString()
}

function handRankLabel(rank){
  if(!rank) return ''
  return String(rank).replace(/_/g, ' ')
}

function actionDisplay(side){
  if(!side) return '대기 중'
  const type = side.action
  if(type === 'BET'){
    return `베팅 +${formatChips(side.actionAmount)}`
  }
  if(type === 'RAISE'){
    return `레이즈 +${formatChips(side.actionAmount)}`
  }
  if(type === 'CALL'){
    return `콜 ${formatChips(side.actionAmount)}`
  }
  if(type === 'CHECK') return '체크'
  if(type === 'FOLD') return '폴드'
  if(type === 'WIN') return `승리 +${formatChips(side.actionAmount)}`
  if(type === 'LOSE') return '패배'
  if(type === 'ANTE') return `앤티 ${formatChips(side.actionAmount)}`
  return '대기 중'
}

function canAct(side){
  if(!side) return false
  if(side.user !== user.value) return false
  if(side.folded) return false
  if(!inProgress.value) return false
  if(turn.value && turn.value !== side.user) return false
  return !loading.value
}

function canCall(side){
  if(!canAct(side)) return false
  return Number(side?.toCall || 0) > 0
}

function canCheck(side){
  if(!canAct(side)) return false
  return Number(side?.toCall || 0) <= 0
}

function canRaise(side){
  if(!canAct(side)) return false
  return betAmountValue.value >= betMinimum.value
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
.players{ display:grid; grid-template-columns:repeat(auto-fit,minmax(260px,1fr)); gap:18px; }
.player{ background:rgba(8,14,24,.75); border-radius:18px; padding:20px; border:1px solid rgba(255,255,255,.08); display:flex; flex-direction:column; gap:14px; position:relative; overflow:hidden; transition:border-color .2s ease, box-shadow .2s ease; }
.player.me{ border-color:#5d9cff; box-shadow:0 0 0 3px rgba(93,156,255,.28); }
.player.acting{ border-color:rgba(255,214,120,.8); box-shadow:0 0 0 3px rgba(255,214,120,.25); }
.player.folded{ opacity:.55; }
.player.winner{ border-color:#6bdc8f; box-shadow:0 0 0 3px rgba(107,220,143,.3); }
.player header{ display:flex; justify-content:space-between; align-items:center; gap:12px; }
.player .profile{ padding:4px 10px; border-radius:999px; background:rgba(255,255,255,.1); color:rgba(255,255,255,.75); font-size:.8rem; text-transform:uppercase; letter-spacing:.05em; }
.cards{ display:flex; flex-wrap:wrap; gap:10px; justify-content:center; }
.player-meta{ display:flex; gap:18px; align-items:center; flex-wrap:wrap; justify-content:space-between; }
.bet-summary{ display:flex; flex-direction:column; gap:4px; font-size:.85rem; }
.bet-summary span{ color:rgba(255,255,255,.65); }
.bet-summary strong{ font-size:1.15rem; color:#ffd36b; }
.action-status{ padding:6px 14px; border-radius:999px; background:rgba(255,255,255,.08); font-size:.85rem; letter-spacing:.04em; text-transform:uppercase; }
.action-status.bet{ background:rgba(255,186,120,.18); color:#ffb87b; }
.action-status.check{ background:rgba(120,200,255,.18); color:#7bcaff; }
.action-status.fold{ background:rgba(255,136,152,.18); color:#ff91a3; }
.hand-rank{ font-size:.82rem; color:rgba(255,255,255,.7); }
.player-chips{ margin-top:auto; }
.player-actions{ display:flex; gap:12px; flex-wrap:wrap; justify-content:center; }
.player-actions button{ padding:10px 14px; border-radius:10px; border:none; background:rgba(255,255,255,.08); color:#fff; cursor:pointer; flex:1 1 30%; min-width:110px; }
.player-actions button:disabled{ opacity:.5; cursor:not-allowed; }
.fold-banner{ position:absolute; right:16px; bottom:16px; padding:6px 12px; border-radius:999px; background:rgba(255,136,152,.2); color:#ff91a3; font-size:.8rem; font-weight:600; }
.pending{ background:rgba(255,255,255,.05); border-radius:14px; padding:12px 18px; }
.pending p{ margin:0; color:rgba(255,255,255,.75); font-size:.9rem; }
.showdown{ background:rgba(255,255,255,.08); border-radius:14px; padding:16px 20px; }
.showdown p{ margin:0; font-size:1rem; color:#ffd36b; }
.empty{ color:rgba(255,255,255,.6); text-align:center; padding:40px 0; }
</style>
