# 🎵 Naija Ayo Sound Effect Integration Guide

## 📋 Overview
This document provides a complete implementation strategy for adding authentic wood sound effects to enhance the traditional Ayo gameplay experience.

## 🎯 Current Animation System

The game currently features an intelligent animation system that detects different pit change events:

```kotlin
// Animation detection logic in MainActivity.kt
private fun detectAnimationType(pitIndex: Int, currentGameState: GameState): VisualSeedManager.AnimationType {
    val previousState = previousGameState ?: return VisualSeedManager.AnimationType.NONE

    val previousSeedCount = previousState.pits[pitIndex]
    val currentSeedCount = currentGameState.pits[pitIndex]

    // Detect pit capture (needs enhancement)
    if (isPitCapture(pitIndex, previousState, currentGameState)) {
        return VisualSeedManager.AnimationType.CAPTURE
    }

    // Detect seed addition
    if (currentSeedCount > previousSeedCount) {
        return VisualSeedManager.AnimationType.SEED_ADDED
    }

    // Detect seed removal
    if (currentSeedCount < previousSeedCount) {
        return VisualSeedManager.AnimationType.SEED_REMOVED
    }

    return VisualSeedManager.AnimationType.NONE
}
```

## 🔧 Implementation Strategy

### **Phase 1: Basic Sound Integration**

#### Required Assets
```
app/src/main/res/raw/
├── wood_sound.mp3          # Main seed interaction sound
├── capture_sound.mp3       # Special capture effect
├── click_sound.mp3         # UI interaction sounds
└── win_sound.mp3           # Game completion sound
```

#### SoundManager Class
```kotlin
class SoundManager(private val context: Context) {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(5)
        .build()

    private val soundMap = mutableMapOf<String, Int>()

    fun loadSounds() {
        soundMap["wood"] = soundPool.load(context, R.raw.wood_sound, 1)
        soundMap["capture"] = soundPool.load(context, R.raw.capture_sound, 1)
        soundMap["click"] = soundPool.load(context, R.raw.click_sound, 1)
        soundMap["win"] = soundPool.load(context, R.raw.win_sound, 1)
    }

    fun playWoodSound(volume: Float = 0.7f) {
        soundPool.play(soundMap["wood"]!!, volume, volume, 1, 0, 1.0f)
    }

    fun playCaptureSound() {
        soundPool.play(soundMap["capture"]!!, 1.0f, 1.0f, 2, 0, 1.0f)
    }

    fun release() {
        soundPool.release()
    }
}
```

### **Phase 2: Enhanced Capture Detection**

#### Improved Detection Logic
```kotlin
private fun isPitCapture(pitIndex: Int, previousState: GameState, currentState: GameState): Boolean {
    val isPlayer1Pit = pitIndex < 6
    val currentPlayer = currentState.currentPlayer

    // Verify it's the correct player's turn
    if ((isPlayer1Pit && currentPlayer != 1) || (!isPlayer1Pit && currentPlayer != 2)) {
        return false
    }

    // Check for actual seed capture scenario
    if (isPlayer1Pit) {
        // Player 1 captures - check if Player 2 lost seeds AND Player 1 gained in this pit
        val player2TotalSeeds = previousState.pits.slice(6..11).sum()
        val player2CurrentSeeds = currentState.pits.slice(6..11).sum()

        return player2CurrentSeeds < player2TotalSeeds &&
               currentState.pits[pitIndex] > previousState.pits[pitIndex]
    } else {
        // Player 2 captures - check if Player 1 lost seeds AND Player 2 gained in this pit
        val player1TotalSeeds = previousState.pits.slice(0..5).sum()
        val player1CurrentSeeds = currentState.pits.slice(0..5).sum()

        return player1CurrentSeeds < player1TotalSeeds &&
               currentState.pits[pitIndex] > previousState.pits[pitIndex]
    }
}
```

### **Phase 3: Contextual Sound Integration**

#### Sound Trigger Points
```kotlin
private fun detectAnimationType(pitIndex: Int, currentGameState: GameState): VisualSeedManager.AnimationType {
    val previousState = previousGameState ?: return VisualSeedManager.AnimationType.NONE

    val previousSeedCount = previousState.pits[pitIndex]
    val currentSeedCount = currentGameState.pits[pitIndex]

    // Sound integration based on game events
    when {
        currentSeedCount > previousSeedCount -> {
            soundManager.playWoodSound(0.7f) // Seed added - medium volume
            return VisualSeedManager.AnimationType.SEED_ADDED
        }
        currentSeedCount < previousSeedCount -> {
            soundManager.playWoodSound(0.5f) // Seed removed - softer volume
            return VisualSeedManager.AnimationType.SEED_REMOVED
        }
        isPitCapture(pitIndex, previousState, currentGameState) -> {
            soundManager.playCaptureSound() // Capture occurred
            return VisualSeedManager.AnimationType.CAPTURE
        }
    }

    return VisualSeedManager.AnimationType.NONE
}
```

## 🎨 Animation Effects

### **Visual Feedback by Event Type**

| Event Type | Visual Effect | Sound Effect | Use Case |
|------------|---------------|--------------|----------|
| **🏆 Pit Capture** | Golden glow + scale up | `capture_sound.mp3` | When player captures opponent's seeds |
| **➕ Seeds Added** | Green pulse + bounce | `wood_sound.mp3` (70%) | Seeds distributed into pit |
| **➖ Seeds Removed** | Red fade + shrink | `wood_sound.mp3` (50%) | Seeds picked up from pit |
| **None** | Subtle scale | No sound | Routine updates |

## ⚙️ Technical Implementation

### **Android Audio Management**
```kotlin
// Enhanced MainActivity integration
class MainActivity : AppCompatActivity() {
    private lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // ... existing code ...

        // Initialize sound system
        soundManager = SoundManager(this)
        soundManager.loadSounds()
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
    }
}
```

### **Performance Optimization**
- **SoundPool**: Low-latency audio playback for game events
- **Pre-loading**: Load all sounds during app initialization
- **Memory Management**: Proper cleanup in onDestroy()
- **Volume Control**: Contextual volume adjustment based on event type

## 🎮 Game Event Detection

### **Enhanced Detection Algorithm**
```kotlin
private fun analyzeGameEvent(pitIndex: Int, previousState: GameState, currentState: GameState): GameEvent {
    val previousCount = previousState.pits[pitIndex]
    val currentCount = currentState.pits[pitIndex]

    return when {
        // Pit capture detection
        isValidCapture(pitIndex, previousState, currentState) -> GameEvent.CAPTURE

        // Seed addition
        currentCount > previousCount -> GameEvent.SEED_ADDED

        // Seed removal
        currentCount < previousCount -> GameEvent.SEED_REMOVED

        // No change
        else -> GameEvent.NONE
    }
}
```

## 📱 User Experience Features

### **Sound Settings**
- Master game volume control
- Individual sound type toggles
- Accessibility mute option
- Device volume integration

### **Contextual Volume**
- Reduce volume during background music
- Increase volume for capture events
- Adaptive volume based on device settings

## 🧪 Testing Strategy

### **Sound Event Coverage**
1. **Move Distribution**: Wood sound for each pit receiving seeds
2. **Seed Pickup**: Removal sound when starting moves
3. **Pit Capture**: Special capture sound effect
4. **Game End**: Victory/defeat sound effects
5. **UI Interactions**: Click sounds for buttons/menus

## 🎵 Audio Asset Specifications

### **Recommended Audio Properties**
| Sound Type | Format | Duration | Volume | Use Case |
|------------|--------|----------|--------|----------|
| **Wood Sound** | MP3 | 100-200ms | 60-80% | Seed interactions |
| **Capture Sound** | MP3 | 200-300ms | 100% | Pit captures |
| **UI Sounds** | MP3 | 50-100ms | 40-60% | Button clicks |

### **File Structure**
```
app/src/main/res/raw/
├── wood_sound.mp3          # Main seed interaction
├── capture_sound.mp3       # Pit capture effect
├── click_sound.mp3         # UI interactions
├── win_sound.mp3           # Victory sound
└── lose_sound.mp3          # Defeat sound
```

## 🚀 Implementation Benefits

### **Enhanced Player Experience**
- **🎭 Immersive Gameplay**: Audio feedback for all interactions
- **🎯 Clear Event Communication**: Different sounds for different events
- **⚡ Performance Optimized**: Low-latency audio with proper memory management
- **🎚️ User Control**: Volume and sound preferences

### **Cultural Authenticity**
- **Wood Sound Effects**: Authentic feel of physical Ayo game
- **Contextual Audio**: Sounds match traditional game mechanics
- **Progressive Enhancement**: Graceful degradation when sounds disabled

## 📋 Implementation Checklist

- [ ] Create SoundManager class with SoundPool integration
- [ ] Add sound assets to res/raw directory
- [ ] Integrate sound triggers in detectAnimationType()
- [ ] Implement volume controls and user preferences
- [ ] Add sound settings to app preferences
- [ ] Test all sound events with actual gameplay
- [ ] Optimize audio performance and memory usage
- [ ] Add accessibility features (sound toggle)

This implementation will provide rich, contextual audio feedback that enhances the traditional Ayo gameplay experience while maintaining performance and user control. 🎵🎮

---
*Document created: $(date)*
*Path: app/src/main/res/raw/Sound_Effect_Integration_Guide.md*