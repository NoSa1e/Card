<template>
  <div class="solo-wrapper" v-if="user">
    <section class="solo-card">
      <header>
        <button class="ghost" @click="goMenu">← 메인 메뉴</button>
        <div class="titles">
          <h2>솔로 플레이</h2>
          <p>게임과 덱 개수를 선택하세요</p>
        </div>
      </header>
      <div class="game-list">
        <button
          v-for="game in games"
          :key="game.id"
          class="game-tile"
          :class="{ active: selected === game.id }"
          @click="selected = game.id"
        >
          <span class="emoji">{{ game.icon }}</span>
          <div>
            <h3>{{ game.name }}</h3>
            <p>{{ game.description }}</p>
          </div>
          <span v-if="selected === game.id" class="check">✓</span>
        </button>
      </div>
      <div class="deck-select">
        <label for="deck">덱 개수</label>
        <select id="deck" v-model.number="deckCount">
          <option v-for="option in deckOptions" :key="option" :value="option">{{ option }}덱</option>
        </select>
      </div>
      <button class="start" @click="startGame">게임 시작</button>
    </section>
  </div>
  <div v-else class="redirect-card">
    <p>로그인이 필요합니다.</p>
    <router-link class="link" to="/login">로그인 화면으로 이동</router-link>
  </div>
</template>
<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserId } from '../user'

const router = useRouter()
const userId = useUserId()
const user = computed(() => userId.value)

const games = [
  { id: 'blackjack', name: '블랙잭', description: '딜러와 겨루어 21을 노려보세요', icon: '🃏' },
  { id: 'baccarat', name: '바카라', description: '플레이어 또는 뱅커에 베팅하세요', icon: '🎴' },
  { id: 'seven', name: '세븐포커', description: '7장의 카드로 최고의 족보를 만드세요', icon: '🎰' }
]

const deckOptions = [1, 2, 3, 4, 5, 6, 8]
const selected = ref('blackjack')
const deckCount = ref(4)

onMounted(() => {
  if(!user.value){
    router.replace('/login')
  }
})

function goMenu(){
  router.push('/menu')
}

function startGame(){
  if(!user.value) return
  router.push({
    path: `/game/solo/${selected.value}`,
    query: { decks: deckCount.value }
  })
}
</script>
<style scoped>
.solo-wrapper{ width:100%; max-width:960px; }
.solo-card{ background:rgba(12,20,36,.72); border:1px solid rgba(255,255,255,.14); border-radius:24px; padding:36px; backdrop-filter:blur(18px);
  display:flex; flex-direction:column; gap:28px; box-shadow:0 28px 70px rgba(0,0,0,.45); }
header{ display:flex; align-items:flex-start; gap:16px; }
.titles h2{ font-size:2rem; margin-bottom:6px; }
.titles p{ color:rgba(255,255,255,.7); }
.ghost{ background:none; border:none; color:#b7c9ff; cursor:pointer; font-size:.95rem; padding:6px 10px; border-radius:999px;
  transition:background .2s ease; }
.ghost:hover{ background:rgba(255,255,255,.12); }
.game-list{ display:grid; gap:16px; }
.game-tile{ display:flex; align-items:center; gap:18px; padding:18px 20px; border-radius:18px; border:1px solid rgba(255,255,255,.14);
  background:rgba(255,255,255,.05); color:#fff; cursor:pointer; text-align:left; position:relative; transition:transform .2s ease, border .2s ease, background .2s ease; }
.game-tile.active{ border-color:#ffb347; background:rgba(255,179,71,.2); }
.game-tile:hover{ transform:translateY(-2px); }
.emoji{ font-size:40px; }
.game-tile h3{ font-size:1.35rem; margin-bottom:6px; }
.game-tile p{ color:rgba(255,255,255,.72); }
.check{ position:absolute; right:18px; top:50%; transform:translateY(-50%); background:#ffb347; color:#08111f; border-radius:999px; padding:4px 10px; font-weight:700; }
.deck-select{ display:flex; flex-direction:column; gap:10px; }
.deck-select label{ color:rgba(255,255,255,.75); font-size:.95rem; }
select{ background:rgba(8,14,24,.75); color:#fff; border-radius:14px; border:1px solid rgba(255,255,255,.2); padding:12px 14px; font-size:1rem; }
.start{ align-self:stretch; padding:16px; border-radius:14px; background:linear-gradient(135deg,#34d899,#1baf75); border:none; color:#fff; font-size:1.1rem;
  font-weight:600; cursor:pointer; box-shadow:0 18px 36px rgba(27,175,117,.35); transition:transform .2s ease, box-shadow .2s ease; }
.start:hover{ transform:translateY(-2px); box-shadow:0 26px 40px rgba(27,175,117,.45); }
.redirect-card{ background:rgba(12,20,36,.72); border-radius:20px; padding:36px; text-align:center; color:#fff; }
.redirect-card .link{ color:#9db8ff; text-decoration:underline; }
@media(max-width:720px){ .solo-card{ padding:28px; } .game-tile{ flex-direction:column; align-items:flex-start; } .check{ position:static; transform:none; margin-left:auto; } }
</style>
