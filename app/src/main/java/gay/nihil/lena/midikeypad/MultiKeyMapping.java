package gay.nihil.lena.midikeypad;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

final class MultiKeyMapping {
    static final int STATIC_LANES = 0;
    static final int ADAPTIVE_LANES = 1;
    static final int MIN_KEYS = 2;
    static final int MAX_KEYS = 10;

    private final int keyCount;
    private final int[] notes;
    private final int deadzonePercent;
    private final int historySize;
    private final int maxShiftPercent;
    private final boolean adaptive;
    private final float[] centers;
    private final float[] sampleSums;
    private final List<Deque<Float>> samples = new ArrayList<>();

    MultiKeyMapping(int[] notes, int deadzonePercent, int historySize,
                    int maxShiftPercent, int layout) {
        if (notes.length < MIN_KEYS || notes.length > MAX_KEYS) {
            throw new IllegalArgumentException("Key count must be between 2 and 10");
        }

        keyCount = notes.length;
        this.notes = notes.clone();
        this.deadzonePercent = deadzonePercent;
        this.historySize = Math.max(1, historySize);
        this.maxShiftPercent = maxShiftPercent;
        adaptive = layout == ADAPTIVE_LANES;
        centers = new float[keyCount];
        sampleSums = new float[keyCount];
        for (int i = 0; i < keyCount; i++) {
            centers[i] = (i + 0.5f) / keyCount;
            samples.add(new ArrayDeque<>());
        }
    }

    Integer noteForTap(float x, float width) {
        if (width <= 0) {
            return null;
        }

        float position = clamp(x / width, 0f, 0.999999f);
        int keyIndex = adaptive ? adaptiveKeyAt(position) : staticKeyAt(position);
        if (keyIndex < 0) {
            return null;
        }

        if (adaptive) {
            recordPosition(keyIndex, position);
        }
        return notes[keyIndex];
    }

    int getKeyCount() {
        return keyCount;
    }

    int getNote(int keyIndex) {
        return notes[keyIndex];
    }

    float getCenterRatio(int keyIndex) {
        return centers[keyIndex];
    }

    boolean isAdaptive() {
        return adaptive;
    }

    int getDeadzonePercent() {
        return deadzonePercent;
    }

    private int staticKeyAt(float position) {
        float scaledPosition = position * keyCount;
        float nearestBoundary = Math.round(scaledPosition);
        if (deadzonePercent > 0 && nearestBoundary > 0 && nearestBoundary < keyCount
                && Math.abs(scaledPosition - nearestBoundary) <= deadzonePercent / 200f) {
            return -1;
        }
        return Math.min(keyCount - 1, (int) scaledPosition);
    }

    private int adaptiveKeyAt(float position) {
        for (int i = 0; i < keyCount - 1; i++) {
            float spacing = centers[i + 1] - centers[i];
            float boundary = (centers[i] + centers[i + 1]) / 2f;
            float deadzoneHalfWidth = spacing * deadzonePercent / 200f;
            if (deadzonePercent > 0
                    && Math.abs(position - boundary) <= deadzoneHalfWidth) {
                return -1;
            }
        }

        int nearest = 0;
        float nearestDistance = Math.abs(position - centers[0]);
        for (int i = 1; i < keyCount; i++) {
            float distance = Math.abs(position - centers[i]);
            if (distance < nearestDistance) {
                nearest = i;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private void recordPosition(int keyIndex, float position) {
        Deque<Float> keySamples = samples.get(keyIndex);
        float sum = sampleSums[keyIndex] + position;
        keySamples.addLast(position);
        if (keySamples.size() > historySize) {
            sum -= keySamples.removeFirst();
        }
        sampleSums[keyIndex] = sum;

        float nominalCenter = (keyIndex + 0.5f) / keyCount;
        float shift = maxShiftPercent / 100f;
        float minCenter = Math.max(0f, nominalCenter - shift);
        float maxCenter = Math.min(1f, nominalCenter + shift);
        float minSeparation = 0.1f / keyCount;
        if (keyIndex > 0) {
            minCenter = Math.max(minCenter, centers[keyIndex - 1] + minSeparation);
        }
        if (keyIndex < keyCount - 1) {
            maxCenter = Math.min(maxCenter, centers[keyIndex + 1] - minSeparation);
        }
        if (minCenter > maxCenter) {
            minCenter = maxCenter;
        }
        centers[keyIndex] = clamp(sum / keySamples.size(), minCenter, maxCenter);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }
}
