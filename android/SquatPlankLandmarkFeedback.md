# Squat and Plank Landmark Feedback Logic

## Overview
This document describes how to generate real-time coaching feedback for squats and planks using pose landmarks.

The core approach is:
1. Read landmarks each frame.
2. Compute stable movement/alignment signals.
3. Evaluate exercise-specific rules.
4. Emit concise cues only when issues persist.

## Scope
- Input: 2D/3D pose landmarks with per-point visibility/confidence.
- Output: simple feedback payload per frame/window (score, cues, optional phase).
- Goal: stable, actionable cues for form quality.

## Shared Logic

### 1) Landmark Quality Gate
- For each frame, verify key landmarks are visible enough (confidence threshold).
- If critical points are missing, return neutral guidance (for example: "Step into frame") instead of evaluating form.

### 2) Side Selection
- Select one body side (left or right) based on higher landmark visibility.
- Keep side selection consistent to reduce noisy jumps in angles.

### 3) Feature Extraction
Compute a small set of robust biomechanical signals:
- Joint angles from three points (for example, hip-knee-ankle).
- Segment alignment (for example, shoulder-hip-ankle body line).
- Relative offsets (for example, knee horizontal position vs ankle).

### 4) Temporal Stabilization
- Smooth landmarks/angles over a short frame window (for example, ~5 frames).
- Trigger correction cues only if a rule fails continuously for a short duration (for example, 300-500 ms).
- This avoids flicker and cue spam from frame-to-frame jitter.

## Squat Feedback Logic

### 1) Phase Tracking (State Machine)
Track movement phase from knee angle trend and thresholds:
- Standing: knee nearly extended.
- Descending: knee angle decreasing.
- Bottom: depth reached.
- Ascending: knee angle increasing toward standing.

Phase context makes feedback more accurate than single-frame checks.

### 2) Rule Checks
Evaluate these checks with phase-aware logic:
- Depth check: at bottom, detect if squat is too shallow.
- Torso check: detect excessive forward lean ("chest down").
- Knee tracking check: detect knee drifting too far from mid-foot alignment.

### 3) Cues and Scoring
- Start from a perfect score and subtract penalties for each failed rule.
- Return short, direct cues tied to failing checks (for example, "Go lower", "Keep chest up", "Keep knee over mid-foot").
- If no rule fails, return a positive confirmation (for example, "Good rep").

## Plank Feedback Logic

### 1) Body Line Assessment
- Use shoulder-hip-ankle geometry to evaluate whether the body is close to a straight line.
- Use hip offset from expected line/midline to detect:
  - Sagging hips
  - Piked hips

### 2) Rule Checks
- If body line deviates too much, issue straight-line correction.
- If hips sag, prompt to lift slightly.
- If hips pike, prompt to lower slightly.

### 3) Cues and Scoring
- Apply penalties per failed rule from a baseline score.
- Emit one or two highest-priority cues to keep feedback clear.
- If alignment is good, return positive confirmation (for example, "Good plank").

## Output Shape (Conceptual)
For each evaluation cycle, produce:
- score: normalized quality score (for example, 0-100)
- cues: list of user-facing corrections/confirmations
- phase: optional movement phase (mainly for squats)

## Integration Notes
- Keep evaluation and state in the ViewModel/domain layer.
- Keep UI as a pure observer of score/cues/phase.
- Use unidirectional data flow to simplify updates and testing.

## Practical Defaults
- Visibility threshold: moderate (only evaluate when key points are reliable).
- Smoothing window: short (around 5 frames).
- Cue persistence gate: around 300-500 ms.
- Start with conservative thresholds, then calibrate on real recordings.

