# Sowing Animation & Sound Implementation Plan

## 📋 Overview
This plan integrates the existing animation and sound infrastructure to provide visual feedback and audio cues for sowing and capture events during gameplay.

## 🎯 Objectives
1. ✅ Use **Sowing Steps Tracking** from game engines
2. ✅ Apply **Animation Types** (SEED_ADDED, CAPTURE) from VisualSeedManager
3. ✅ Play **Multiple Sound Effects** (one per seed sowed)
4. ✅ Trigger **Capture Animations** with sound effects

## 📦 Existing Infrastructure (Already in Code)

### 1. Game Engine Components
- **`MoveResult`** - Contains `sowingSteps`, `finalState`, and `captureResult`
- **`SowingStep`** - Data class with `pitIndex`, `pitValueAfterSowing`, `isFinalStep`
- **`makeAnimatedMove()`** - Available in all game engines (Easy, Medium, Hard)
- **`CaptureResult`** - Contains `capturedPitIndices` list

### 2. Visual Components
- **`VisualSeedManager.AnimationType`**:
  - `SEED_ADDED` - Green pulse effect (150ms)
  - `CAPTURE` - Golden glow effect (200ms)
  - `SEED_REMOVED` - Red fade effect (150ms)
  - `NONE` - No animation

### 3. Sound Components
- **`SoundManager.playWoodSound()`** - Sowing sound
- **`SoundManager.playCaptureSound()`** - Capture sound
- **`SoundEventType`** - SEED_ADDED, SEED_REMOVED, MULTIPLE_SEEDS

## 🔧 Implementation Steps

### **Phase 1: Update SinglePlayerGameViewModel**

#### File: `SinglePlayerGameViewModel.kt`

**Changes Required:**
1. Change `makePlayerMove()` to use `makeAnimatedMove()` instead of `makeMove()`
2. Expose `MoveResult` through LiveData for MainActivity to observe
3. Create new LiveData for animation events

**New LiveData Properties:**
```kotlin
private val _moveResult = MutableLiveData<MoveResult?>()
val moveResult: LiveData<MoveResult?> = _moveResult

private val _animationComplete = MutableLiveData<Boolean>()
val animationComplete: LiveData<Boolean> = _animationComplete
```

**Modified Function:**
```kotlin
fun makePlayerMove(pitIndex: Int) {
    // ... validation code ...
    
    viewModelScope.launch {
        try {
            // Use makeAnimatedMove instead of makeMove
            val moveResult = gameEngine.makeAnimatedMove(currentState, pitIndex, 1)
            
            if (moveResult != null) {
                _moveResult.value = moveResult  // Expose for animation
                // Don't update game state yet - wait for animation
            } else {
                _gameMessage.value = "Invalid move! Try another pit."
            }
        } catch (e: Exception) {
            _gameMessage.value = "Error making move: ${e.message}"
        }
    }
}
```

**New Function:**
```kotlin
fun onAnimationComplete(finalState: LocalGameState) {
    _gameState.value = finalState
    _animationComplete.value = true
    
    if (!finalState.gameOver) {
        _gameMessage.value = "Nice move, Ai thinking.."
        // Trigger AI move after animation
        viewModelScope.launch {
            delay(1500)
            makeAIMoveInternal()
            _gameMessage.value = "Play Now!"
        }
    }
}
```

---

### **Phase 2: Update MainActivity - Animation Orchestration**

#### File: `MainActivity.kt`

**Changes Required:**
1. Observe `moveResult` LiveData from ViewModel
2. Create animation coroutine to process sowing steps sequentially
3. Play sound for each sowing step
4. Trigger capture animations after sowing completes

**New Properties:**
```kotlin
private var isAnimating = false
private var animationJob: Job? = null
```

**New Observer in `setupSinglePlayerMode()`:**
```kotlin
private fun setupSinglePlayerMode() {
    singlePlayerViewModel.startNewGame(gameLevel)
    
    // Existing game state observer
    singlePlayerViewModel.gameState.observe(this) { gameState ->
        if (!isAnimating) {
            updateUiForSinglePlayer(gameState)
        }
    }
    
    // NEW: Move result observer for animations
    singlePlayerViewModel.moveResult.observe(this) { moveResult ->
        moveResult?.let {
            animateMoveSequence(it)
        }
    }
    
    // Existing click listeners...
}
```

**New Animation Function:**
```kotlin
private fun animateMoveSequence(moveResult: MoveResult) {
    isAnimating = true
    
    // Cancel any existing animation
    animationJob?.cancel()
    
    animationJob = lifecycleScope.launch {
        try {
            // Phase 1: Animate sowing steps
            animateSowingSteps(moveResult.sowingSteps)
            
            // Phase 2: Animate captures
            animateCaptureSteps(moveResult.captureResult)
            
            // Phase 3: Update final state
            singlePlayerViewModel.onAnimationComplete(moveResult.finalState)
            
        } catch (e: Exception) {
            android.util.Log.e("Animation", "Error during animation: ${e.message}")
            // Fallback: update state immediately
            singlePlayerViewModel.onAnimationComplete(moveResult.finalState)
        } finally {
            isAnimating = false
        }
    }
}
```

**Sowing Animation Function:**
```kotlin
private suspend fun animateSowingSteps(sowingSteps: List<SowingStep>) {
    for (step in sowingSteps) {
        // Update pit visual with animation
        val pitTextId = resources.getIdentifier("pitText_${step.pitIndex}", "id", packageName)
        val pitTextView = pitContainers[step.pitIndex].findViewById<TextView>(pitTextId)
        
        visualSeedManager.updatePitSeeds(
            pitTextView, 
            step.pitValueAfterSowing, 
            VisualSeedManager.AnimationType.SEED_ADDED
        )
        
        // Play wood sound for this seed
        soundManager.playWoodSound(
            volume = 0.7f,
            eventType = com.naijaayo.worldwide.sound.SoundEventType.SEED_ADDED
        )
        
        // Wait for animation to complete (150ms from VisualSeedManager)
        delay(150)
    }
}
```

**Capture Animation Function:**
```kotlin
private suspend fun animateCaptureSteps(captureResult: CaptureResult) {
    if (captureResult.capturedPitIndices.isEmpty()) return
    
    // Play capture sound once
    soundManager.playCaptureSound()
    
    // Animate each captured pit
    for (pitIndex in captureResult.capturedPitIndices) {
        val pitTextId = resources.getIdentifier("pitText_$pitIndex", "id", packageName)
        val pitTextView = pitContainers[pitIndex].findViewById<TextView>(pitTextId)
        
        // Show capture animation (golden glow)
        visualSeedManager.updatePitSeeds(
            pitTextView, 
            0,  // Captured pits become empty
            VisualSeedManager.AnimationType.CAPTURE
        )
    }
    
    // Wait for capture animation to complete (200ms from VisualSeedManager)
    delay(200)
}
```

**Update Click Listeners to Prevent Clicks During Animation:**
```kotlin
private fun setupSinglePlayerMode() {
    // ... existing code ...
    
    for (i in 0..5) {
        pitContainers[i].setOnClickListener { 
            if (!isAnimating && singlePlayerViewModel.isValidMove(i)) {
                soundManager.playClickSound()
                singlePlayerViewModel.makePlayerMove(i) 
            }
        }
    }
}
```

---

### **Phase 3: Update AI Move Animation**

**New Function in MainActivity:**
```kotlin
private suspend fun animateAIMove(moveResult: MoveResult) {
    isAnimating = true
    
    try {
        // Animate AI sowing
        animateSowingSteps(moveResult.sowingSteps)
        
        // Animate AI captures
        animateCaptureSteps(moveResult.captureResult)
        
        // Update state
        _gameState.value = moveResult.finalState
        
    } finally {
        isAnimating = false
    }
}
```

**Update `makeAIMoveInternal()` in ViewModel:**
```kotlin
private suspend fun makeAIMoveInternal() {
    val currentState = _gameState.value ?: return
    if (currentState.gameOver) return
    
    val validMoves = gameEngine.getValidMoves(currentState)
    if (validMoves.isEmpty()) return
    
    // Get AI move with animation data
    val aiPitIndex = validMoves[Random.nextInt(validMoves.size)]
    val moveResult = gameEngine.makeAnimatedMove(currentState, aiPitIndex, 2)
    
    if (moveResult != null) {
        _moveResult.value = moveResult  // Trigger animation in MainActivity
    }
}
```

---

### **Phase 4: Remove Old Sound Logic**

**In MainActivity.kt:**
```kotlin
// DELETE this function - no longer needed
private fun playGameSounds(newState: LocalGameState) {
    // ❌ Remove entire function
}

// UPDATE updateUiForSinglePlayer - remove sound call
private fun updateUiForSinglePlayer(state: LocalGameState) {
    // playGameSounds(state)  // ❌ Remove this line
    previousGameState = state
    
    // ... rest of the function remains the same ...
}
```

---

### **Phase 5: Add Multiplayer Support (Optional)**

**Similar changes for multiplayer:**
1. Update `performMultiplayerMove()` to use animation
2. Listen for Firebase game state changes
3. Animate opponent moves when detected

---

## 📊 Implementation Checklist

### SinglePlayerGameViewModel.kt
- [ ] Add `_moveResult` and `moveResult` LiveData
- [ ] Add `_animationComplete` LiveData
- [ ] Modify `makePlayerMove()` to use `makeAnimatedMove()`
- [ ] Add `onAnimationComplete()` function
- [ ] Update `makeAIMoveInternal()` to use `makeAnimatedMove()`

### MainActivity.kt
- [ ] Add `isAnimating` and `animationJob` properties
- [ ] Add observer for `moveResult` LiveData
- [ ] Create `animateMoveSequence()` function
- [ ] Create `animateSowingSteps()` function
- [ ] Create `animateCaptureSteps()` function
- [ ] Update click listeners to check `isAnimating`
- [ ] Remove `playGameSounds()` function
- [ ] Remove sound call from `updateUiForSinglePlayer()`

### Testing
- [ ] Test single player sowing animation
- [ ] Test capture animation and sound
- [ ] Test AI move animation
- [ ] Verify sounds play for each seed
- [ ] Verify no clicks during animation
- [ ] Test game over scenario

---

## 🎮 Expected Behavior After Implementation

### When Player Makes a Move:
1. ✅ Player clicks pit → Click sound plays
2. ✅ Each seed sowing shows green pulse animation
3. ✅ Wood sound plays for each seed sowed
4. ✅ Captured pits show golden glow animation
5. ✅ Capture sound plays once
6. ✅ Board updates to final state
7. ✅ AI move triggers with same animations

### Visual Feedback:
- **Sowing**: Green pulse (150ms per seed)
- **Capture**: Golden glow (200ms)
- **Total animation time**: ~150ms × number of seeds + 200ms for captures

### Audio Feedback:
- **Click**: When pit is selected
- **Wood sound**: For each seed sowed (multiple times)
- **Capture sound**: Once when capture occurs
- **Win sound**: When game ends

---

## 🚀 Benefits

1. **Enhanced User Experience**: Visual and audio feedback for every action
2. **Game Understanding**: Players can see exactly where each seed lands
3. **Professional Feel**: Smooth animations make the game feel polished
4. **Reusable Code**: Same system works for single player and multiplayer
5. **No Breaking Changes**: Uses existing infrastructure

---

## ⚠️ Important Notes

1. **No Delays Added**: We're using the existing animation durations from VisualSeedManager (150ms, 200ms)
2. **Coroutine-Based**: All animations use Kotlin coroutines for smooth execution
3. **Cancellable**: Animation jobs can be cancelled if needed
4. **Non-Blocking**: UI remains responsive during animations
5. **Backward Compatible**: Falls back gracefully if animation fails

---

## 📝 Next Steps

1. Review this plan
2. Implement changes in order (Phase 1 → Phase 5)
3. Test each phase before moving to the next
4. Adjust animation timings if needed
5. Extend to multiplayer mode

---

**Estimated Implementation Time**: 2-3 hours
**Complexity**: Medium
**Risk**: Low (uses existing code, minimal changes)
