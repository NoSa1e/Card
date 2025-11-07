<template>
  <div class="app-shell">
    <header class="app-header">
      <router-link class="brand" to="/menu">CardGame</router-link>
      <div class="header-actions">
        <span v-if="user" class="user-chip">{{ user }}</span>
        <router-link v-if="user" to="/menu" class="link">메뉴</router-link>
        <router-link v-if="user" to="/multiplayer" class="link">로비</router-link>
      </div>
    </header>
    <main class="app-content">
      <router-view />
    </main>
  </div>
</template>
<script setup>
import { computed } from 'vue'
import { useUserId } from './user'
const user = computed(() => useUserId().value)
</script>
<style scoped>
.app-shell{ min-height:100vh; background: radial-gradient(1400px 800px at 50% -200px, rgba(255,255,255,.12), transparent) #08111f;
  color:#f6f9ff; display:flex; flex-direction:column; }
.app-header{ display:flex; justify-content:space-between; align-items:center; padding:16px 24px; position:sticky; top:0; z-index:10;
  background:linear-gradient(180deg,rgba(10,18,34,.9),rgba(8,12,24,.75)); border-bottom:1px solid rgba(255,255,255,.08); }
.brand{ font-size:1.3rem; font-weight:700; letter-spacing:.08em; color:#fff; text-decoration:none; }
.header-actions{ display:flex; align-items:center; gap:12px; }
.user-chip{ background:rgba(255,255,255,.12); border:1px solid rgba(255,255,255,.16); border-radius:999px; padding:6px 14px; font-size:.85rem; }
.link{ color:#d1ddff; text-decoration:none; font-size:.9rem; padding:6px 10px; border-radius:999px; transition:background .25s ease; }
.link.router-link-active, .link:hover{ background:rgba(255,255,255,.12); color:#fff; }
.app-content{ flex:1; padding:32px 16px 48px; display:flex; justify-content:center; }
</style>
