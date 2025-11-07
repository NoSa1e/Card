import { ref } from 'vue'

const userRef = ref(localStorage.getItem('userId') || '')

export function getUserId(){
  return userRef.value
}

export function setUserId(id){
  const next = (id || '').trim()
  userRef.value = next
  if(next){
    localStorage.setItem('userId', next)
  }else{
    localStorage.removeItem('userId')
  }
}

export function useUserId(){
  return userRef
}

if(typeof window!=='undefined'){
  window.addEventListener('storage', e => {
    if(e.key==='userId'){
      userRef.value = e.newValue || ''
    }
  })
}
