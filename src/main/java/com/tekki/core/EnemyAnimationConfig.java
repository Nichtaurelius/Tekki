package com.tekki.core;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class EnemyAnimationConfig {

    public enum AnimationType {
        IDLE,
        RUN,
        JUMP,
        FALL,
        ATTACK1,
        ATTACK2,
        ATTACK3,
        TAKE_HIT,
        DEATH
    }

    public static final class AnimationSet {
        private static final AnimationSet EMPTY = new AnimationSet(Collections.emptyMap());

        private final Map<AnimationType, Integer> frameCounts;

        private AnimationSet(Map<AnimationType, Integer> frameCounts) {
            this.frameCounts = frameCounts;
        }

        public Integer getFrameCount(AnimationType type) {
            return frameCounts.get(type);
        }

        public boolean hasAnimation(AnimationType type) {
            return frameCounts.containsKey(type);
        }

        public int getAttackAnimationCount() {
            int count = 0;
            if (hasAnimation(AnimationType.ATTACK1)) {
                count++;
            }
            if (hasAnimation(AnimationType.ATTACK2)) {
                count++;
            }
            if (hasAnimation(AnimationType.ATTACK3)) {
                count++;
            }
            return count;
        }
    }

    private static final Map<String, AnimationSet> ENEMY_CONFIGS = createConfigs();

    private EnemyAnimationConfig() {
    }

    public static AnimationSet forEnemy(String enemyId) {
        return ENEMY_CONFIGS.getOrDefault(enemyId, AnimationSet.EMPTY);
    }

    private static Map<String, AnimationSet> createConfigs() {
        Map<String, AnimationSet> configs = new HashMap<>();
        configs.put("enemy1", buildEnemy1Config());
        configs.put("enemy2", buildEnemy2Config());
        return Collections.unmodifiableMap(configs);
    }

    private static AnimationSet buildEnemy1Config() {
        Map<AnimationType, Integer> frames = new EnumMap<>(AnimationType.class);
        frames.put(AnimationType.IDLE, 10);
        frames.put(AnimationType.RUN, 8);
        frames.put(AnimationType.JUMP, 3);
        frames.put(AnimationType.FALL, 3);
        frames.put(AnimationType.ATTACK1, 7);
        frames.put(AnimationType.ATTACK2, 6);
        frames.put(AnimationType.ATTACK3, 9);
        frames.put(AnimationType.TAKE_HIT, 3);
        frames.put(AnimationType.DEATH, 11);
        return new AnimationSet(Collections.unmodifiableMap(frames));
    }

    private static AnimationSet buildEnemy2Config() {
        Map<AnimationType, Integer> frames = new EnumMap<>(AnimationType.class);
        frames.put(AnimationType.IDLE, 4);
        frames.put(AnimationType.RUN, 8);
        frames.put(AnimationType.JUMP, 2);
        frames.put(AnimationType.FALL, 2);
        frames.put(AnimationType.ATTACK1, 4);
        frames.put(AnimationType.ATTACK2, 4);
        frames.put(AnimationType.TAKE_HIT, 3);
        return new AnimationSet(Collections.unmodifiableMap(frames));
    }
}
