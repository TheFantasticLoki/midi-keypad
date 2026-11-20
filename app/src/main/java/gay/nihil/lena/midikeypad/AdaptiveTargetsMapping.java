package gay.nihil.lena.midikeypad;

import java.util.ArrayDeque;
import java.util.Deque;

final class AdaptiveTargetsMapping {
    private final int deadzonePercent;
    private final int targetWidthPercent;
    private final int historySize;
    private final int maxShiftPercent;
    private final int strategy;
    private final int alternationIntervalMs;
    private final Deque<Float> leftSamples = new ArrayDeque<>();
    private final Deque<Float> rightSamples = new ArrayDeque<>();

    private float leftCenter = 0.25f;
    private float rightCenter = 0.75f;
    private float leftSampleSum;
    private float rightSampleSum;
    private int lastTapCode = -1;
    private long lastTapTime = -1;

    AdaptiveTargetsMapping(int deadzonePercent, int targetWidthPercent, int historySize,
                           int maxShiftPercent, int strategy, int alternationIntervalMs) {
        this.deadzonePercent = deadzonePercent;
        this.targetWidthPercent = targetWidthPercent;
        this.historySize = Math.max(1, historySize);
        this.maxShiftPercent = maxShiftPercent;
        this.strategy = strategy;
        this.alternationIntervalMs = alternationIntervalMs;
    }

    Integer noteForTap(float x, float width, long eventTime) {
        if (width <= 0) {
            return null;
        }

        float position = clamp(x / width, 0f, 1f);
        float split = getDeadzoneCenterRatio();
        boolean rapidTap = lastTapCode != -1
                && eventTime >= lastTapTime
                && eventTime - lastTapTime <= alternationIntervalMs;
        boolean alternated = strategy == AdaptiveSplitMapping.RAPID_ALTERNATION && rapidTap;

        if (deadzonePercent > 0 && Math.abs(position - split) <= deadzonePercent / 200f) {
            return null;
        }

        int code;
        if (alternated) {
            code = opposite(lastTapCode);
        } else {
            code = Math.abs(position - leftCenter) <= Math.abs(position - rightCenter) ? 60 : 61;
        }

        float center = code == 60 ? leftCenter : rightCenter;
        float allowedDistance = targetWidthPercent / 200f + maxShiftPercent / 100f;
        if (!alternated && Math.abs(position - center) > allowedDistance) {
            return null;
        }

        if (!alternated) {
            recordPosition(code, position);
        }
        lastTapCode = code;
        lastTapTime = eventTime;
        return code;
    }

    float getLeftCenterRatio() {
        return leftCenter;
    }

    float getRightCenterRatio() {
        return rightCenter;
    }

    float getDeadzoneCenterRatio() {
        return (leftCenter + rightCenter) / 2f;
    }

    private void recordPosition(int code, float position) {
        Deque<Float> samples = code == 60 ? leftSamples : rightSamples;
        float sum = code == 60 ? leftSampleSum : rightSampleSum;
        samples.addLast(position);
        sum += position;
        if (samples.size() > historySize) {
            sum -= samples.removeFirst();
        }

        float nominalCenter = code == 60 ? 0.25f : 0.75f;
        float shift = maxShiftPercent / 100f;
        float minSeparation = Math.max(0.05f, deadzonePercent / 100f + 0.02f);
        if (code == 60) {
            float minCenter = Math.max(0f, nominalCenter - shift);
            float maxCenter = Math.min(1f, Math.min(nominalCenter + shift,
                    rightCenter - minSeparation));
            if (maxCenter < minCenter) {
                minCenter = maxCenter;
            }
            leftSampleSum = sum;
            leftCenter = clamp(sum / samples.size(), minCenter, maxCenter);
        } else {
            float minCenter = Math.max(0f, Math.max(nominalCenter - shift,
                    leftCenter + minSeparation));
            float maxCenter = Math.min(1f, nominalCenter + shift);
            if (minCenter > maxCenter) {
                maxCenter = minCenter;
            }
            rightSampleSum = sum;
            rightCenter = clamp(sum / samples.size(), minCenter, maxCenter);
        }
    }

    private static int opposite(int code) {
        return code == 60 ? 61 : 60;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }
}
