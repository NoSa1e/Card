<template>
  <div class="lobby-wrapper" v-if="user">
    <section class="lobby-card">
      <header class="lobby-header">
        <button class="ghost" @click="goMenu">← 메인 메뉴</button>
        <div class="titles">
          <h2>멀티플레이 로비</h2>
          <p>참여할 방을 선택하거나 새로 만들어보세요</p>
        </div>
        <button class="create" @click="toggleCreate">방 만들기</button>
      </header>

      <transition name="fade">
        <form v-if="showCreate" class="create-form" @submit.prevent="createRoom">
          <div class="field">
            <label>방 이름</label>
            <input v-model="newRoom.name" type="text" placeholder="방 이름" required />
          </div>
          <div class="field">
            <label>게임 선택</label>
            <select v-model="newRoom.game">
              <option value="blackjack">블랙잭</option>
              <option value="baccarat">바카라</option>
              <option value="seven">세븐포커</option>
            </select>
          </div>
          <div class="field">
            <label>덱 개수</label>
            <select v-model.number="newRoom.decks">
              <option v-for="option in deckOptions" :key="option" :value="option">{{ option }}덱</option>
            </select>
          </div>
          <div class="create-actions">
            <button class="secondary" type="button" @click="toggleCreate">취소</button>
            <button class="primary" type="submit" :disabled="creating || !newRoom.name.trim()">생성</button>
          </div>
        </form>
      </transition>

      <div class="list-header">
        <h3>현재 방</h3>
        <button class="ghost" @click="loadRooms" :disabled="loading">새로고침</button>
      </div>

      <div v-if="loading" class="empty">불러오는 중...</div>
      <div v-else-if="rooms.length === 0" class="empty">아직 생성된 방이 없습니다.</div>
      <ul v-else class="room-list">
        <li v-for="room in rooms" :key="room.id" class="room">
          <div>
            <h4>{{ room.name }}</h4>
            <p class="meta">
              {{ gameName(room.game) }} · {{ room.decks }}덱 · {{ room.players.length }}명 참여 중
            </p>
            <p class="meta" v-if="room.host">방장: {{ room.host }}</p>
          </div>
          <button
            class="join"
            :disabled="joining === room.id"
            @click="joinRoom(room)"
          >
            {{ joining === room.id ? '입장 중...' : '입장' }}
          </button>
        </li>
      </ul>
    </section>
  </div>
  <div v-else class="redirect-card">
    <p>로그인이 필요합니다.</p>
    <router-link class="link" to="/login">로그인 화면으로 이동</router-link>
  </div>
</template>
<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { jget, jpost } from '../api'
import { useUserId } from '../user'

const router = useRouter()
const userId = useUserId()
const user = computed(() => userId.value)

const rooms = ref([])
const loading = ref(false)
const joining = ref('')
const showCreate = ref(false)
const creating = ref(false)
const deckOptions = [1, 2, 3, 4, 5, 6, 8]

const newRoom = reactive({
  name: '',
  game: 'blackjack',
  decks: 4
})

onMounted(() => {
  if(!user.value){
    router.replace('/login')
  }else{
    loadRooms()
  }
})

function goMenu(){
  router.push('/menu')
}

function toggleCreate(){
  showCreate.value = !showCreate.value
  if(!showCreate.value){
    newRoom.name = ''
    newRoom.game = 'blackjack'
    newRoom.decks = 4
  }
}

async function loadRooms(){
  if(!user.value) return
  loading.value = true
  try{
    const res = await jget('/api/rooms')
    const detail = res.detail || res
    rooms.value = Array.isArray(detail) ? detail : []
  }catch(err){
    console.error(err)
  }finally{
    loading.value = false
  }
}

async function createRoom(){
  if(!user.value || !newRoom.name.trim()) return
  creating.value = true
  try{
    const params = new URLSearchParams({
      user: user.value,
      game: newRoom.game,
      decks: String(newRoom.decks)
    })
    const res = await jpost(`/api/rooms/create?${params.toString()}`)
    const detail = res.detail || res
    toggleCreate()
    await loadRooms()
    goToRoom(detail)
  }catch(err){
    console.error(err)
  }finally{
    creating.value = false
  }
}

async function joinRoom(room){
  if(!user.value) return
  joining.value = room.id
  try{
    const params = new URLSearchParams({
      user: user.value,
      roomId: room.id
    })
    const res = await jpost(`/api/rooms/join?${params.toString()}`)
    const detail = res.detail || res
    goToRoom(detail)
  }catch(err){
    console.error(err)
  }finally{
    joining.value = ''
  }
}

function goToRoom(room){
  if(!room) return
  router.push({
    path: `/game/multi/${room.game.toLowerCase()}`,
    query: {
      roomId: room.id,
      name: room.name,
      decks: room.decks,
      host: room.host,
      players: (room.players || []).join(',')
    }
  })
}

function gameName(code){
  switch(code){
    case 'BLACKJACK':
    case 'blackjack':
      return '블랙잭'
    case 'BACCARAT':
    case 'baccarat':
      return '바카라'
    case 'SEVEN':
    case 'seven':
      return '세븐포커'
    default:
      return code
  }
}
</script>
<style scoped>
.lobby-wrapper{ width:100%; max-width:960px; }
.lobby-card{ background:rgba(12,20,36,.72); border:1px solid rgba(255,255,255,.14); border-radius:24px; padding:36px; backdrop-filter:blur(18px);
  display:flex; flex-direction:column; gap:24px; box-shadow:0 28px 70px rgba(0,0,0,.45); }
.lobby-header{ display:flex; align-items:center; gap:16px; }
.titles{ flex:1; }
.titles h2{ font-size:2rem; margin-bottom:6px; }
.titles p{ color:rgba(255,255,255,.7); }
.ghost{ background:none; border:none; color:#b7c9ff; cursor:pointer; font-size:.95rem; padding:6px 10px; border-radius:999px;
  transition:background .2s ease; }
.ghost:hover{ background:rgba(255,255,255,.12); }
.create{ padding:10px 16px; border-radius:12px; border:none; cursor:pointer; background:linear-gradient(135deg,#ff9f43,#ff6a3a);
  color:#fff; font-weight:600; box-shadow:0 16px 32px rgba(255,111,58,.35); transition:transform .2s ease, box-shadow .2s ease; }
.create:hover{ transform:translateY(-2px); box-shadow:0 22px 36px rgba(255,111,58,.45); }
.create-form{ background:rgba(255,255,255,.06); border-radius:18px; padding:20px 24px; display:grid; gap:18px; }
.field{ display:flex; flex-direction:column; gap:8px; }
.field label{ color:rgba(255,255,255,.75); font-size:.9rem; }
.field input,.field select{ padding:12px 14px; border-radius:12px; border:1px solid rgba(255,255,255,.18); background:rgba(8,14,24,.75); color:#fff; }
.create-actions{ display:flex; justify-content:flex-end; gap:12px; }
.secondary{ padding:12px 18px; border-radius:12px; border:1px solid rgba(255,255,255,.18); background:rgba(255,255,255,.08); color:#fff; cursor:pointer; }
.primary{ padding:12px 18px; border-radius:12px; border:none; background:linear-gradient(135deg,#34d899,#1baf75); color:#fff; font-weight:600; cursor:pointer; box-shadow:0 16px 32px rgba(27,175,117,.35); }
.primary:disabled{ opacity:.5; cursor:not-allowed; box-shadow:none; }
.list-header{ display:flex; justify-content:space-between; align-items:center; }
.list-header h3{ font-size:1.2rem; }
.room-list{ list-style:none; margin:0; padding:0; display:grid; gap:16px; }
.room{ display:flex; justify-content:space-between; align-items:center; gap:16px; background:rgba(255,255,255,.05); border:1px solid rgba(255,255,255,.12); border-radius:18px; padding:20px 24px; }
.room h4{ font-size:1.3rem; margin-bottom:6px; }
.meta{ color:rgba(255,255,255,.7); font-size:.95rem; }
.join{ padding:12px 18px; border-radius:12px; border:none; background:linear-gradient(135deg,#5d9cff,#3d72ff); color:#fff; font-weight:600; cursor:pointer; box-shadow:0 16px 30px rgba(61,114,255,.35); }
.join:disabled{ opacity:.5; cursor:not-allowed; box-shadow:none; }
.empty{ padding:30px 0; text-align:center; color:rgba(255,255,255,.65); }
.redirect-card{ background:rgba(12,20,36,.72); border-radius:20px; padding:36px; text-align:center; color:#fff; }
.redirect-card .link{ color:#9db8ff; text-decoration:underline; }
.fade-enter-active,.fade-leave-active{ transition:opacity .2s ease; }
.fade-enter-from,.fade-leave-to{ opacity:0; }
@media(max-width:720px){ .lobby-card{ padding:28px; } .room{ flex-direction:column; align-items:flex-start; } .join{ width:100%; text-align:center; } }
</style>
