import { createRouter, createWebHistory } from 'vue-router'
import Login from './pages/Login.vue'
import MainMenu from './pages/MainMenu.vue'
import SoloSelect from './pages/SoloSelect.vue'
import MultiplayerLobby from './pages/MultiplayerLobby.vue'
import GameRoom from './pages/GameRoom.vue'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', component: Login },
  { path: '/menu', component: MainMenu },
  { path: '/solo', component: SoloSelect },
  { path: '/multiplayer', component: MultiplayerLobby },
  { path: '/game/:mode/:game', component: GameRoom, props: true },
  { path: '/:pathMatch(.*)*', redirect: '/login' }
]

export default createRouter({ history: createWebHistory(), routes })
