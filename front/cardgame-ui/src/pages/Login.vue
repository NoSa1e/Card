<template>
  <div class="login-wrapper">
    <section class="login-card">
      <div class="symbols">
        <span class="spade">♠</span>
        <span class="heart">♥</span>
        <span class="diamond">♦</span>
        <span class="club">♣</span>
      </div>
      <h1>온라인 카드게임</h1>
      <p>아이디를 입력하여 로그인하세요</p>
      <form class="login-form" @submit.prevent="submit">
        <input
          v-model="id"
          class="input"
          type="text"
          placeholder="아이디 입력"
          autocomplete="off"
        />
        <button class="primary" type="submit" :disabled="!id.trim()">로그인</button>
      </form>
    </section>
  </div>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getUserId, setUserId } from '../user'

const id = ref('')
const router = useRouter()

onMounted(() => {
  const existing = getUserId()
  if(existing){
    router.replace('/menu')
  }
})

function submit(){
  const name = id.value.trim()
  if(!name) return
  setUserId(name)
  router.push('/menu')
}
</script>
<style scoped>
.login-wrapper{ width:100%; max-width:480px; margin:60px auto; background:rgba(10,18,30,.55); border:1px solid rgba(255,255,255,.12);
  border-radius:18px; padding:40px 36px; box-shadow:0 24px 60px rgba(0,0,0,.45); backdrop-filter:blur(12px); text-align:center; }
.symbols{ display:flex; justify-content:center; gap:22px; font-size:46px; margin-bottom:24px; }
.symbols .spade,.symbols .club{ color:#fff; }
.symbols .heart,.symbols .diamond{ color:#ff5f7a; }
 h1{ font-size:2rem; margin-bottom:8px; }
 p{ color:rgba(255,255,255,.68); margin-bottom:28px; }
.login-form{ display:flex; flex-direction:column; gap:16px; }
.input{ padding:14px 16px; border-radius:12px; border:1px solid rgba(255,255,255,.24); background:rgba(8,14,24,.7); color:#fff;
  font-size:1rem; outline:none; transition:border .2s ease, box-shadow .2s ease; }
.input:focus{ border-color:#ffb347; box-shadow:0 0 0 3px rgba(255,179,71,.3); }
.primary{ padding:14px; border-radius:12px; border:none; font-weight:600; font-size:1rem; cursor:pointer; color:#fff;
  background:linear-gradient(135deg,#f6b73c,#e8891d); box-shadow:0 12px 24px rgba(232,137,29,.35); transition:transform .2s ease, box-shadow .2s ease; }
.primary:disabled{ opacity:.45; cursor:not-allowed; box-shadow:none; }
.primary:not(:disabled):hover{ transform:translateY(-1px); box-shadow:0 18px 30px rgba(232,137,29,.45); }
@media (max-width:520px){ .login-wrapper{ margin:40px 16px; padding:32px 24px; } }
</style>
