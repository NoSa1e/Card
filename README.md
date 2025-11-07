# Card

## Getting the updated code from this workspace

이 컨테이너 안에서 적용된 Vue/백엔드 수정 사항은 아직 GitHub 리포지토리에 push되지 않았습니다. 변경된 내용을 가져가려면 다음 중 한 가지 방법을 사용할 수 있습니다.

1. `git diff`로 패치를 생성하여 로컬 환경에 적용합니다.
   ```bash
   git diff > cardgame.patch
   # 로컬 리포지토리에서
   git apply cardgame.patch
   ```
2. 혹은 변경된 파일을 직접 다운로드해서 기존 리포지토리에 덮어씁니다.

## 로컬 실행 방법

### 백엔드(Spring Boot)
1. Java 17 이상과 Gradle이 설치되어 있어야 합니다.
2. `cardgame` 디렉터리로 이동한 뒤 다음 명령을 실행합니다.
   ```bash
   ./gradlew bootRun
   ```
3. 기본적으로 백엔드는 `http://localhost:8080`에서 REST API를 제공합니다.

### 프런트엔드(Vue 3 + Vite)
1. Node.js 18 이상이 필요합니다.
2. `front/cardgame-ui` 디렉터리에서 의존성을 설치합니다.
   ```bash
   npm install
   ```
3. 개발 서버를 실행합니다.
   ```bash
   npm run dev -- --host
   ```
   이렇게 하면 프런트엔드가 `http://localhost:5173`(혹은 Vite가 표시하는 주소)에서 열립니다.
4. `.env` 혹은 `VITE_API_BASE` 환경 변수를 통해 백엔드 주소(`http://localhost:8080`)와 일치하도록 설정합니다.

백엔드와 프런트엔드를 동시에 띄우면, 프런트에서 제공하는 로그인/로비/게임 화면이 REST API와 연동되어 동작합니다.
