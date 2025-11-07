<template>
  <div class="room-wrapper" v-if="user && component">
    <section class="room-header">
      <button class="ghost" @click="leave">← {{ backLabel }}</button>
      <div class="titles">
        <h2>{{ gameTitle }}</h2>
        <p v-if="mode === 'multi'">{{ roomNameDisplay }}</p>
        <p v-else>솔로 플레이 · {{ decks }}덱</p>
      </div>
      <div class="meta" v-if="mode === 'multi'">
        <span>덱 {{ decks }}</span>
        <span v-if="roomPlayers.length">플레이어: {{ roomPlayers.join(', ') }}</span>
      </div>
    </section>
    <component :is="component" v-bind="componentProps" />
  </div>
  <div v-else class="redirect-card">
    <p v-if="!component">지원하지 않는 게임입니다.</p>
    <p v-else>로그인이 필요합니다.</p>
    <router-link class="link" to="/login">로그인 화면으로 이동</router-link>
  </div>
</template>
<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserId } from '../user'
import BlackjackSolo from '../components/games/BlackjackSolo.vue'
import BlackjackMulti from '../components/games/BlackjackMulti.vue'
import BaccaratSolo from '../components/games/BaccaratSolo.vue'
import BaccaratMulti from '../components/games/BaccaratMulti.vue'
import SevenPokerRoom from '../components/games/SevenPokerRoom.vue'

const props = defineProps({
  mode: { type: String, required: true },
  game: { type: String, required: true }
})

const router = useRouter()
const route = useRoute()
const userId = useUserId()
const user = computed(() => userId.value)
const mode = computed(() => props.mode)
const game = computed(() => props.game.toLowerCase())
const decks = computed(() => Number(route.query.decks || 4))
const roomId = computed(() => route.query.roomId || `${user.value || 'solo'}-${game.value}`)
const roomName = computed(() => route.query.name || '')
const host = computed(() => route.query.host || '')
const roomPlayers = computed(() => {
  const raw = route.query.players
  if(typeof raw === 'string' && raw.length){
    return raw.split(',').filter(Boolean)
  }
  if(Array.isArray(raw)){
    return raw.map(String).filter(Boolean)
  }
  return []
})

const mapping = {
  solo: {
    blackjack: BlackjackSolo,
    baccarat: BaccaratSolo,
    seven: SevenPokerRoom
  },
  multi: {
    blackjack: BlackjackMulti,
    baccarat: BaccaratMulti,
    seven: SevenPokerRoom
  }
}

const component = computed(() => mapping[mode.value]?.[game.value] || null)
const componentProps = computed(() => {
  const base = { user: user.value, decks: decks.value }
  if(component.value === BlackjackSolo || component.value === BaccaratSolo){
    return base
  }
  if(component.value === BlackjackMulti){
    return {
      ...base,
      roomId: roomId.value,
      roomName: roomName.value,
      host: host.value,
      players: roomPlayers.value,
      decks: decks.value
    }
  }
  if(component.value === BaccaratMulti){
    return {
      ...base,
      roomId: roomId.value,
      roomName: roomName.value,
      players: roomPlayers.value,
      decks: decks.value
    }
  }
  if(component.value === SevenPokerRoom){
    return {
      user: user.value,
      mode: mode.value,
      roomId: roomId.value,
      players: roomPlayers.value
    }
  }
  return base
})
const gameTitle = computed(() => {
  switch(game.value){
    case 'blackjack': return '블랙잭'
    case 'baccarat': return '바카라'
    case 'seven': return '세븐포커'
    default: return game.value
  }
})
const backLabel = computed(() => mode.value === 'multi' ? '로비로' : '솔로 선택으로')
const roomNameDisplay = computed(() => roomName.value || `${roomId.value} · ${decks.value}덱`)

function leave(){
  if(mode.value === 'multi') router.push('/multiplayer')
  else router.push('/solo')
}
</script>
<style scoped>
.room-wrapper{ width:100%; max-width:1100px; display:flex; flex-direction:column; gap:28px; }
.room-header{ display:flex; align-items:center; gap:18px; background:rgba(12,20,36,.72); border-radius:24px; padding:18px 24px; border:1px solid rgba(255,255,255,.12); }
.ghost{ background:none; border:none; color:#b7c9ff; cursor:pointer; font-size:.95rem; padding:6px 10px; border-radius:999px; transition:background .2s ease; }
.ghost:hover{ background:rgba(255,255,255,.12); }
.titles{ flex:1; }
.titles h2{ margin:0 0 6px; font-size:1.8rem; }
.titles p{ margin:0; color:rgba(255,255,255,.7); }
.meta{ display:flex; flex-direction:column; gap:6px; color:rgba(255,255,255,.75); font-size:.9rem; text-align:right; }
.redirect-card{ background:rgba(12,20,36,.72); border-radius:20px; padding:36px; text-align:center; color:#fff; }
.redirect-card .link{ color:#9db8ff; text-decoration:underline; }
</style>
