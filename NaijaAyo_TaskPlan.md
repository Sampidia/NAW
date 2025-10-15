# Naija Ayo Worldwide: Project Task Plan

This document outlines the current project status and the necessary tasks to complete the game.

---

### 1. Current Status

**Summary:** The application is now functional after a major architectural refactoring. The multiplayer lobby is accessible, and the core data layer has been stabilized. The immediate next steps are to fix the single-player game logic and then prepare the application for release.

**✅ What's Working:**
- Stable Client-Server Architecture (post-refactor).
- REST and WebSocket Communication with a local server.
- Functional Multiplayer Lobby screen (crash resolved).
- Server-Side Game Logic.
- Avatar Selection and Leaderboard features.

**❌ What's Broken:**
- **Single-Player Mode:** It is currently using the multiplayer `GameViewModel` and is not playable locally.

---

### 2. Core Gameplay & Initial Features: COMPLETE (with major refactoring)

-   **[x] Task 1-6: Core Gameplay, UI, Profile, and Leaderboard**
    -   **Status:** **DONE.** These tasks are functionally complete. However, their implementation revealed critical architectural flaws that required a major refactoring of the `ViewModel` and `Repository` layers to use a singleton pattern, fixing numerous crashes and static UI issues.

### 3. UI Enhancement & Visual Polish: IN PROGRESS

**Current Progress:** 6/13 core features completed (46%)

**✅ Completed Core Features:**
-   **[x] Set AndroidManifest.xml for landscape orientation (MainActivity only)**
-   **[x] Create enhanced landscape layout with header**
-   **[x] Add avatar management system for players and AI**
-   **[x] Implement header with avatars, scores, and icons**
-   **[x] Create hamburger menu with navigation items**
-   **[x] Update MainActivity.kt for new UI components**

**🎯 Wooden Board Enhancement Tasks:**
-   **[ ] Create wooden board background drawable with carved pits**
-   **[ ] Design individual pit shapes with wood grain texture**
-   **[ ] Implement visual seed system (1-4 seeds per pit)**
-   **[ ] Add small number labels below each pit**
-   **[ ] Update layout for authentic Ayo board appearance**
-   **[ ] Add wood texture and shadows for realism**
-   **[ ] Implement seed animation - subtle animation when seeds are added**

---

### 3. Immediate Next Steps: Stabilize and Refine

This is now our highest priority before adding new features.

-   **[ ] Task 7: Fix Single-Player Mode**
    -   **Goal:** Decouple Single-Player from the networking code.
    -   **Action:** Create a separate, local-only game logic handler or `ViewModel` for single-player games that does not rely on the `GameRepository` or any networking. The "Single Player" button should launch a game that is fully playable offline.

-   **[ ] Task 8: Re-enable Multiplayer Lobby Logic**
    -   **Goal:** Restore the full functionality of the multiplayer lobby screen.
    -   **Action:** Uncomment the logic that was temporarily disabled in `MultiplayerLobbyActivity.kt` to allow creating and joining rooms.

---

### 4. Pre-Publishing Checklist

This section contains critical tasks that **must** be completed before the app is ready for the Google Play Store.

-   **[ ] CRITICAL: Replace Hardcoded IP Address**
    -   **Goal:** The app must point to a production server, not a local development machine.
    -   **Action:** Find all instances of the hardcoded IP address (`192.168.0.227`) in files like `RetrofitClient.kt` and `SocketManager.kt` and replace them with the final, public-facing production server URL.

-   **[ ] Final End-to-End Testing**
    -   **Goal:** Ensure all features work together seamlessly.
    -   **Action:** Test profile creation, avatar selection, creating/joining a multiplayer game, completing a full game, and checking the leaderboard.

-   **[ ] Code Cleanup & Obfuscation**
    -   **Goal:** Prepare the codebase for release.
    -   **Action:** Remove all temporary debugging code, `println` statements, and unnecessary comments. Enable and configure ProGuard/R8 to shrink and obfuscate the code.

-   **[ ] Generate Signed Release APK/AAB**
    -   **Goal:** Create the final distributable package for the Play Store.

---

### 5. Future Features (Post-Release)

-   **[ ] Task 9: Implement the Friends System**
-   **[ ] Task 10: In-Game & World Chat**
-   **[ ] Task 11: Presence & Push Notifications (FCM)**
