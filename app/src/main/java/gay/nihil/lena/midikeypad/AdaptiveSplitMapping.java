package gay.nihil.lena.midikeypad;

import java.util.ArrayDeque;
import java.util.Deque;

final class AdaptiveSplitMapping {
    static final int POSITION_HISTORY = 0;
    static final int RAPID_ALTERNATION = 1;

    private final int deadzonePercent;
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

    AdaptiveSplitMapping(int deadzonePercent, int historySize, int maxShiftPercent,
                         int strategy, int alternationIntervalMs) {
        this.deadzonePercent = deadzonePercent;
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
        float split = getSplitRatio();
        boolean inDeadzone = deadzonePercent > 0
                && Math.abs(position - split) <= deadzonePercent / 200f;
        boolean rapidTap = lastTapCode != -1
                && eventTime >= lastTapTime
                && eventTime - lastTapTime <= alternationIntervalMs;

        if (inDeadzone) {
            return null;
        }

        int code;
        boolean alternated = strategy == RAPID_ALTERNATION && rapidTap;
        if (alternated) {
            code = opposite(lastTapCode);
        } else {
            code = position < split ? 60 : 61;
        }

        if (!alternated) {
            recordPosition(code, position);
        }
        lastTapCode = code;
        lastTapTime = eventTime;
        return code;
    }

    float getSplitRatio() {
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

        if (code == 60) {
            leftSampleSum = sum;
            leftCenter = clamp(sum / samples.size(), 0.25f - maxShiftPercent / 100f,
                    0.25f + maxShiftPercent / 100f);
        } else {
            rightSampleSum = sum;
            rightCenter = clamp(sum / samples.size(), 0.75f - maxShiftPercent / 100f,
                    0.75f + maxShiftPercent / 100f);
        }
    }

    private static int opposite(int code) {
        return code == 60 ? 61 : 60;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }
}
