package gay.nihil.lena.midikeypad;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

final class MultiKeyMapping {
    static final int STATIC_LANES = 0;
    static final int ADAPTIVE_LANES = 1;
    static final int MIN_KEYS = 2;
    static final int MAX_KEYS = 8;

    private final int keyCount;
    private final int[] notes;
    private final int deadzonePercent;
    private final int historySize;
    private final int maxShiftPercent;
    private final int targetWidthPercent;
    private final boolean adaptive;
    private final boolean screenContextEnabled;
    private final float[] centers;
    private final float[] initialTargetCenters;
    private final float[] sampleSums;
    private final List<Deque<Float>> samples = new ArrayList<>();
    private final ScreenTapContext screenContext;
    private float layoutShiftRatio;
    private float initialLayoutShiftRatio;
    private boolean layoutInitialized;

    MultiKeyMapping(int[] notes, int deadzonePercent, int historySize,
                    int maxShiftPercent, int layout) {
        this(notes, deadzonePercent, historySize, maxShiftPercent, layout, false, 100);
    }

    MultiKeyMapping(int[] notes, int deadzonePercent, int historySize,
                    int maxShiftPercent, int layout, boolean screenContextEnabled) {
        this(notes, deadzonePercent, historySize, maxShiftPercent, layout,
                screenContextEnabled, 40);
    }

    MultiKeyMapping(int[] notes, int deadzonePercent, int historySize,
                    int maxShiftPercent, int layout, boolean screenContextEnabled,
                    int targetWidthPercent) {
        if (notes.length < MIN_KEYS || notes.length > MAX_KEYS) {
            throw new IllegalArgumentException("Key count must be between 2 and 8");
        }

        keyCount = notes.length;
        this.notes = notes.clone();
        this.deadzonePercent = Math.max(0, deadzonePercent);
        this.historySize = Math.max(1, historySize);
        this.maxShiftPercent = Math.max(0, maxShiftPercent);
        this.targetWidthPercent = Math.max(0, Math.min(100, targetWidthPercent));
        adaptive = layout == ADAPTIVE_LANES;
        this.screenContextEnabled = adaptive && screenContextEnabled;
        screenContext = new ScreenTapContext(historySize, keyCount);
        centers = new float[keyCount];
        initialTargetCenters = new float[keyCount];
        sampleSums = new float[keyCount];
        for (int i = 0; i < keyCount; i++) {
            centers[i] = (i + 0.5f) / keyCount;
            samples.add(new ArrayDeque<>());
        }
    }

    Integer noteForTap(float x, float width) {
        return noteForTap(x, width, 0);
    }

    Integer noteForTap(float x, float width, long eventTime) {
        if (width <= 0) {
            return null;
        }

        float position = clamp(x / width, 0f, 0.999999f);
        if (!adaptive) {
            int keyIndex = staticKeyAt(position);
            return keyIndex < 0 ? null : notes[keyIndex];
        }

        int keyIndex = nearestAdaptiveKeyAt(position);
        boolean inDeadzone = adaptiveDeadzoneAt(position);
        if (screenContextEnabled) {
            int deadzoneBoundary = adaptiveDeadzoneBoundaryAt(position);
            if (deadzoneBoundary < 0) {
                deadzoneBoundary = learnedDeadzoneBoundaryAt(position);
            }
            int trackedBoundary = deadzoneBoundary >= 0
                    ? deadzoneBoundary : nearbyDeadzoneBoundaryAt(position);
            float deadzoneCenter = trackedBoundary < 0
                    ? 0f : getDeadzoneCenterRatio(trackedBoundary);
            float[] clusters = screenContext.recordTap(position, deadzonePercent,
                    maxShiftPercent, trackedBoundary, deadzoneCenter);
            applyFingerClusters(clusters);
            if (deadzoneBoundary >= 0 || trackedBoundary >= 0) {
                return null;
            }
        } else if (inDeadzone) {
            recordPosition(keyIndex, position);
            return null;
        }

        if (!screenContextEnabled) {
            float allowedDistance = targetWidthPercent / 200f + maxShiftPercent / 100f;
            if (Math.abs(position - centers[keyIndex]) > allowedDistance) {
                return null;
            }
            recordPosition(keyIndex, position);
        }

        int updatedKeyIndex = nearestAdaptiveKeyAt(position);
        if (adaptiveDeadzoneAt(position)) {
            return null;
        }
        float allowedDistance = targetWidthPercent / 200f + maxShiftPercent / 100f;
        if (Math.abs(position - centers[updatedKeyIndex]) > allowedDistance) {
            return null;
        }
        return notes[updatedKeyIndex];
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

    float getDeadzoneCenterRatio(int boundaryIndex) {
        return (centers[boundaryIndex] + centers[boundaryIndex + 1]) / 2f;
    }

    int getTargetWidthPercent() {
        return targetWidthPercent;
    }

    int getMaxShiftPercent() {
        return maxShiftPercent;
    }

    float getLayoutShiftRatio() {
        return screenContextEnabled ? layoutShiftRatio : 0f;
    }

    float getDeadzoneHalfWidthRatio(int boundaryIndex) {
        float spacing = adaptive
                ? centers[boundaryIndex + 1] - centers[boundaryIndex]
                : 1f / keyCount;
        float requestedHalfWidth = Math.max(0, deadzonePercent) / 200f;
        return Math.min(requestedHalfWidth, spacing * 0.45f);
    }

    float getAcceptedStartRatio(int keyIndex) {
        float zoneStart = keyIndex == 0 ? 0f
                : getDeadzoneCenterRatio(keyIndex - 1)
                + getEffectiveDeadzoneHalfWidthRatio(keyIndex - 1);
        float targetStart = centers[keyIndex]
                - targetWidthPercent / 200f - maxShiftPercent / 100f;
        return Math.max(zoneStart, targetStart);
    }

    float getAcceptedEndRatio(int keyIndex) {
        float zoneEnd = keyIndex == keyCount - 1 ? 1f
                : getDeadzoneCenterRatio(keyIndex)
                - getEffectiveDeadzoneHalfWidthRatio(keyIndex);
        float targetEnd = centers[keyIndex]
                + targetWidthPercent / 200f + maxShiftPercent / 100f;
        return Math.min(zoneEnd, targetEnd);
    }

    boolean hasDeadzoneCluster(int boundaryIndex) {
        return screenContextEnabled && screenContext.hasDeadzoneCluster(boundaryIndex);
    }

    float getDeadzoneClusterCenterRatio(int boundaryIndex) {
        return screenContext.getDeadzoneClusterCenterRatio(
                boundaryIndex, getDeadzoneCenterRatio(boundaryIndex));
    }

    float getDeadzoneClusterRadiusRatio(int boundaryIndex) {
        float clusterCenter = getDeadzoneClusterCenterRatio(boundaryIndex);
        float distanceToTarget = Math.min(
                Math.abs(clusterCenter - centers[boundaryIndex]),
                Math.abs(centers[boundaryIndex + 1] - clusterCenter));
        float maxRadius = distanceToTarget * 0.75f;
        return Math.min(Math.max(0.015f, deadzonePercent / 400f), maxRadius);
    }

    float getEffectiveDeadzoneHalfWidthRatio(int boundaryIndex) {
        float effectiveHalfWidth = screenContextEnabled
                ? getDeadzoneTrackingRadiusRatio(boundaryIndex)
                : getDeadzoneHalfWidthRatio(boundaryIndex);
        if (hasDeadzoneCluster(boundaryIndex)) {
            float clusterCenter = getDeadzoneClusterCenterRatio(boundaryIndex);
            effectiveHalfWidth = Math.max(effectiveHalfWidth,
                    Math.abs(clusterCenter - getDeadzoneCenterRatio(boundaryIndex))
                            + getDeadzoneClusterRadiusRatio(boundaryIndex));
        }
        return effectiveHalfWidth;
    }

    private float getDeadzoneTrackingRadiusRatio(int boundaryIndex) {
        float margin = Math.min(0.02f,
                Math.max(0.01f, targetWidthPercent / 400f));
        float spacing = centers[boundaryIndex + 1] - centers[boundaryIndex];
        return Math.min(spacing * 0.4f,
                getDeadzoneHalfWidthRatio(boundaryIndex) + margin);
    }

    boolean isAdaptive() {
        return adaptive;
    }

    boolean isScreenContextEnabled() {
        return screenContextEnabled;
    }

    int getDeadzonePercent() {
        return deadzonePercent;
    }

    private int staticKeyAt(float position) {
        float scaledPosition = position * keyCount;
        float nearestBoundary = Math.round(scaledPosition);
        if (deadzonePercent > 0 && nearestBoundary > 0 && nearestBoundary < keyCount) {
            float boundaryRatio = nearestBoundary / keyCount;
            if (Math.abs(position - boundaryRatio)
                    <= getDeadzoneHalfWidthRatio((int) nearestBoundary - 1)) {
                return -1;
            }
        }
        return Math.min(keyCount - 1, (int) scaledPosition);
    }

    private boolean adaptiveDeadzoneAt(float position) {
        return adaptiveDeadzoneBoundaryAt(position) >= 0;
    }

    private int adaptiveDeadzoneBoundaryAt(float position) {
        for (int i = 0; i < keyCount - 1; i++) {
            float boundary = (centers[i] + centers[i + 1]) / 2f;
            float deadzoneHalfWidth = getDeadzoneHalfWidthRatio(i);
            if (deadzonePercent > 0
                    && Math.abs(position - boundary) <= deadzoneHalfWidth) {
                return i;
            }
        }
        return -1;
    }

    private int learnedDeadzoneBoundaryAt(float position) {
        for (int i = 0; i < keyCount - 1; i++) {
            float deadzoneCenter = getDeadzoneCenterRatio(i);
            if (screenContext.hasDeadzoneCluster(i)
                    && screenContext.isDeadzoneClusterHit(position, i, deadzoneCenter,
                    getDeadzoneClusterRadiusRatio(i))) {
                return i;
            }
        }
        return -1;
    }

    private int nearbyDeadzoneBoundaryAt(float position) {
        for (int i = 0; i < keyCount - 1; i++) {
            if (Math.abs(position - getDeadzoneCenterRatio(i))
                    <= getEffectiveDeadzoneHalfWidthRatio(i)) {
                return i;
            }
        }
        return -1;
    }

    private int nearestAdaptiveKeyAt(float position) {
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
        if (keyIndex > 0) {
            minCenter = Math.max(minCenter, centers[keyIndex - 1] + 0.1f / keyCount);
        }
        if (keyIndex < keyCount - 1) {
            maxCenter = Math.min(maxCenter, centers[keyIndex + 1] - 0.1f / keyCount);
        }
        if (minCenter > maxCenter) {
            minCenter = maxCenter;
        }
        centers[keyIndex] = clamp(sum / keySamples.size(), minCenter, maxCenter);
    }

    private void applyFingerClusters(float[] clusters) {
        if (clusters.length != keyCount) {
            return;
        }

        float shiftLimit = maxShiftPercent / 100f;
        for (int i = 0; i < keyCount; i++) {
            float nominalCenter = (i + 0.5f) / keyCount;
            centers[i] = clamp(clusters[i],
                    Math.max(0f, nominalCenter - shiftLimit),
                    Math.min(1f, nominalCenter + shiftLimit));
        }
        enforceCenterOrder();

        if (!layoutInitialized) {
            List<Float> offsets = new ArrayList<>();
            for (int i = 0; i < keyCount; i++) {
                initialTargetCenters[i] = centers[i];
                offsets.add(centers[i] - (i + 0.5f) / keyCount);
            }
            Collections.sort(offsets);
            layoutShiftRatio = median(offsets);
            initialLayoutShiftRatio = layoutShiftRatio;
            layoutInitialized = true;
            return;
        }

        updateLayoutShiftFromSharedTargetDrift();
    }

    private void updateLayoutShiftFromSharedTargetDrift() {
        List<Float> drifts = new ArrayList<>();
        for (int i = 0; i < keyCount; i++) {
            drifts.add(centers[i] - initialTargetCenters[i]
                    - (layoutShiftRatio - initialLayoutShiftRatio));
        }
        Collections.sort(drifts);
        float sharedDrift = median(drifts);
        float agreementTolerance = Math.min(0.06f,
                Math.max(0.035f, targetWidthPercent / 100f * 0.15f));
        int agreeingTargets = 0;
        for (float drift : drifts) {
            if (Math.abs(drift - sharedDrift) <= agreementTolerance) {
                agreeingTargets++;
            }
        }

        int requiredTargets = keyCount == 2 ? 2 : (keyCount * 3 + 3) / 4;
        float laneWidth = 1f / keyCount;
        float recenterThreshold = Math.max(0.04f,
                (laneWidth - targetWidthPercent / 100f) / 2f);
        if (agreeingTargets >= requiredTargets
                && Math.abs(sharedDrift) >= recenterThreshold) {
            float shiftLimit = maxShiftPercent / 100f;
            layoutShiftRatio = clamp(layoutShiftRatio + sharedDrift,
                    -shiftLimit, shiftLimit);
        }
    }

    private static float median(List<Float> values) {
        int middle = values.size() / 2;
        if ((values.size() & 1) == 0) {
            return (values.get(middle - 1) + values.get(middle)) / 2f;
        }
        return values.get(middle);
    }

    private void enforceCenterOrder() {
        float minSeparation = 0.1f / keyCount;
        for (int i = 0; i < keyCount; i++) {
            float nominalCenter = (i + 0.5f) / keyCount;
            float minCenter = Math.max(0f, nominalCenter - maxShiftPercent / 100f);
            float maxCenter = Math.min(1f, nominalCenter + maxShiftPercent / 100f);
            if (i > 0) {
                minCenter = Math.max(minCenter, centers[i - 1] + minSeparation);
            }
            centers[i] = clamp(centers[i], minCenter, maxCenter);
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }
}
