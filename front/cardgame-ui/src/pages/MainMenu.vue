<template>
  <div class="menu-container" v-if="user">
    <section class="menu-card">
      <header>
        <h2>환영합니다, {{ user }}님!</h2>
        <p>플레이 모드를 선택하세요</p>
      </header>
      <div class="stat-row" v-if="balance !== null">
        <span class="label">보유 칩</span>
        <span class="value">{{ balance.toLocaleString() }}</span>
        <button class="refresh" @click="loadBalance" :disabled="loading">새로고침</button>
      </div>
      <div class="mode-grid">
        <button class="mode" @click="goMultiplayer">
          <div class="icon">👥</div>
          <h3>멀티플레이</h3>
          <p>다른 플레이어와 함께 게임을 즐기세요</p>
        </button>
        <button class="mode" @click="goSolo">
          <div class="icon">🎯</div>
          <h3>솔로 플레이</h3>
          <p>혼자서 게임을 연습하세요</p>
        </button>
      </div>
    </section>
  </div>
  <div v-else class="redirect-card">
    <p>로그인이 필요합니다.</p>
    <router-link class="link" to="/login">로그인 화면으로 이동</router-link>
  </div>
</template>
<script setup>
import { onMounted, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { jget } from '../api'
import { useUserId } from '../user'

const router = useRouter()
const userId = useUserId()
const balance = ref(null)
const loading = ref(false)
const user = computed(() => userId.value)

onMounted(() => {
  if(!user.value){
    router.replace('/login')
  }else{
    loadBalance()
  }
})

async function loadBalance(){
  if(!user.value) return
  loading.value = true
  try{
    const res = await jget(`/api/balance?user=${encodeURIComponent(user.value)}`)
    const detail = res.detail || res
    balance.value = detail.balance ?? null
  }catch(err){
    console.error(err)
  }finally{
    loading.value = false
  }
}

function goSolo(){
  router.push('/solo')
}
function goMultiplayer(){
  router.push('/multiplayer')
}
</script>
<style scoped>
.menu-container{ width:100%; max-width:960px; }
.menu-card{ background:rgba(12,20,36,.72); border:1px solid rgba(255,255,255,.14); border-radius:24px; padding:40px; backdrop-filter:blur(18px);
  box-shadow:0 32px 80px rgba(0,0,0,.45); display:flex; flex-direction:column; gap:32px; }
header h2{ font-size:2.2rem; margin-bottom:12px; }
header p{ color:rgba(255,255,255,.72); font-size:1.05rem; }
.stat-row{ display:flex; align-items:center; gap:16px; background:rgba(255,255,255,.06); border-radius:14px; padding:14px 20px; }
.label{ color:rgba(255,255,255,.6); font-size:.95rem; }
.value{ font-size:1.5rem; font-weight:700; letter-spacing:.04em; }
.refresh{ margin-left:auto; padding:10px 16px; border-radius:12px; border:1px solid rgba(255,255,255,.18); background:rgba(255,255,255,.08);
  color:#fff; cursor:pointer; transition:background .2s ease, transform .2s ease; }
.refresh:disabled{ opacity:.5; cursor:not-allowed; }
.refresh:not(:disabled):hover{ background:rgba(255,255,255,.18); transform:translateY(-1px); }
.mode-grid{ display:grid; grid-template-columns:repeat(auto-fit,minmax(240px,1fr)); gap:24px; }
.mode{ border:none; border-radius:20px; padding:28px 24px; background:linear-gradient(160deg,rgba(66,136,255,.65),rgba(39,73,180,.78));
  color:#fff; text-align:left; cursor:pointer; position:relative; overflow:hidden; transition:transform .25s ease, box-shadow .25s ease; }
.mode:nth-child(2){ background:linear-gradient(160deg,rgba(163,87,255,.68),rgba(90,42,180,.78)); }
.mode::after{ content:''; position:absolute; inset:-30% -20%; background:radial-gradient(circle at center, rgba(255,255,255,.28), transparent 70%);
  opacity:0; transition:opacity .3s ease; }
.mode:hover{ transform:translateY(-4px); box-shadow:0 22px 45px rgba(0,0,0,.35); }
.mode:hover::after{ opacity:1; }
.mode .icon{ font-size:42px; margin-bottom:18px; display:inline-block; }
.mode h3{ font-size:1.6rem; margin-bottom:10px; }
.mode p{ color:rgba(255,255,255,.82); line-height:1.4; }
.redirect-card{ background:rgba(12,20,36,.72); border-radius:20px; padding:36px; text-align:center; color:#fff; }
.redirect-card .link{ color:#9db8ff; text-decoration:underline; }
@media (max-width:720px){ .menu-card{ padding:32px; } }
</style>
