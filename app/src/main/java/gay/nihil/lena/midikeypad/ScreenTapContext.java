package gay.nihil.lena.midikeypad;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * Learns ordered finger-position clusters from taps anywhere on the screen.
 */
final class ScreenTapContext {
    private final int historyCapacity;
    private final int minimumClusterSamples;
    private final int keyCount;
    private final Deque<Float> taps = new ArrayDeque<>();
    private final Deque<Float> recentLiveTaps = new ArrayDeque<>();
    private final List<Deque<Float>> deadzoneTapOffsets = new ArrayList<>();
    private float[] liveCenters;

    ScreenTapContext(int historySize, int keyCount) {
        int samplesPerCluster = Math.max(1, historySize);
        historyCapacity = samplesPerCluster * keyCount;
        minimumClusterSamples = Math.min(2, samplesPerCluster);
        this.keyCount = keyCount;
        for (int i = 0; i < keyCount - 1; i++) {
            deadzoneTapOffsets.add(new ArrayDeque<>());
        }
    }

    float[] recordTap(float position, int deadzonePercent, int maxShiftPercent,
                      int deadzoneBoundaryIndex, float deadzoneCenter) {
        if (deadzoneBoundaryIndex >= 0) {
            recordDeadzoneTap(position, deadzoneBoundaryIndex, deadzoneCenter);
            return liveCenters == null
                    ? getFingerClusters(deadzonePercent) : liveCenters.clone();
        }

        if (liveCenters != null) {
            updateLiveCenters(position, deadzonePercent, maxShiftPercent);
            return liveCenters.clone();
        }

        taps.addLast(position);
        while (taps.size() > historyCapacity) {
            taps.removeFirst();
        }

        float[] clusters = getFingerClusters(deadzonePercent);
        if (clusters.length == keyCount) {
            liveCenters = clusters;
            constrainLiveCenters(maxShiftPercent);
            return liveCenters.clone();
        }
        return clusters;
    }

    boolean isDeadzoneClusterHit(float position, int boundaryIndex,
                                 float deadzoneCenter, float radius) {
        if (boundaryIndex < 0 || boundaryIndex >= deadzoneTapOffsets.size()) {
            return false;
        }
        Deque<Float> offsets = deadzoneTapOffsets.get(boundaryIndex);
        if (offsets.size() < minimumClusterSamples) {
            return false;
        }
        List<Float> sortedOffsets = new ArrayList<>(offsets);
        Collections.sort(sortedOffsets);
        float clusterCenter = deadzoneCenter + median(sortedOffsets);
        return Math.abs(position - clusterCenter) <= radius;
    }

    boolean hasDeadzoneCluster(int boundaryIndex) {
        return boundaryIndex >= 0 && boundaryIndex < deadzoneTapOffsets.size()
                && deadzoneTapOffsets.get(boundaryIndex).size() >= minimumClusterSamples;
    }

    float getDeadzoneClusterCenterRatio(int boundaryIndex, float deadzoneCenter) {
        List<Float> sortedOffsets = new ArrayList<>(deadzoneTapOffsets.get(boundaryIndex));
        Collections.sort(sortedOffsets);
        return deadzoneCenter + median(sortedOffsets);
    }

    float[] getFingerClusters(int deadzonePercent) {
        if (taps.isEmpty()) {
            return new float[0];
        }

        List<Float> sorted = new ArrayList<>(taps);
        Collections.sort(sorted);
        List<List<Float>> clusters = new ArrayList<>();
        clusters.add(new ArrayList<>(sorted));

        float minimumGap = Math.max(0.06f, Math.max(0, deadzonePercent) / 100f);
        while (clusters.size() < keyCount) {
            int splitCluster = -1;
            int splitAt = -1;
            float largestGap = minimumGap;
            for (int clusterIndex = 0; clusterIndex < clusters.size(); clusterIndex++) {
                List<Float> cluster = clusters.get(clusterIndex);
                for (int i = 1; i < cluster.size(); i++) {
                    if (i < minimumClusterSamples
                            || cluster.size() - i < minimumClusterSamples) {
                        continue;
                    }
                    float gap = cluster.get(i) - cluster.get(i - 1);
                    if (gap > largestGap) {
                        largestGap = gap;
                        splitCluster = clusterIndex;
                        splitAt = i;
                    }
                }
            }
            if (splitCluster < 0) {
                break;
            }

            List<Float> cluster = clusters.remove(splitCluster);
            clusters.add(splitCluster, new ArrayList<>(cluster.subList(splitAt, cluster.size())));
            clusters.add(splitCluster,
                    new ArrayList<>(cluster.subList(0, splitAt)));
        }

        float[] centers = new float[clusters.size()];
        for (int i = 0; i < clusters.size(); i++) {
            centers[i] = median(clusters.get(i));
        }
        return centers;
    }

    void clear() {
        taps.clear();
        recentLiveTaps.clear();
        for (Deque<Float> offsets : deadzoneTapOffsets) {
            offsets.clear();
        }
        liveCenters = null;
    }

    private void updateLiveCenters(float position, int deadzonePercent,
                                   int maxShiftPercent) {
        int nearest = 0;
        float distance = Math.abs(position - liveCenters[0]);
        for (int i = 1; i < liveCenters.length; i++) {
            float candidateDistance = Math.abs(position - liveCenters[i]);
            if (candidateDistance < distance) {
                nearest = i;
                distance = candidateDistance;
            }
        }

        if (updateFromSharedTapDrift(position, deadzonePercent, maxShiftPercent)) {
            return;
        }

        float shift = (position - liveCenters[nearest]) * 0.65f;
        float shiftLimit = Math.max(0, maxShiftPercent) / 100f;
        float nominal = (nearest + 0.5f) / keyCount;
        liveCenters[nearest] = clamp(liveCenters[nearest] + shift,
                Math.max(0f, nominal - shiftLimit),
                Math.min(1f, nominal + shiftLimit));
        enforceOrder(maxShiftPercent);

        recentLiveTaps.addLast(position);
        while (recentLiveTaps.size() > Math.max(4, keyCount * 2)) {
            recentLiveTaps.removeFirst();
        }
    }

    private void recordDeadzoneTap(float position, int boundaryIndex,
                                   float deadzoneCenter) {
        Deque<Float> offsets = deadzoneTapOffsets.get(boundaryIndex);
        offsets.addLast(position - deadzoneCenter);
        while (offsets.size() > historyCapacity) {
            offsets.removeFirst();
        }
    }

    private boolean updateFromSharedTapDrift(float position, int deadzonePercent,
                                             int maxShiftPercent) {
        float tolerance = Math.max(0.035f, Math.max(0, deadzonePercent) / 200f);
        float minimumShift = Math.max(0.10f, Math.max(0, deadzonePercent) / 100f);
        float bestDifference = Float.MAX_VALUE;
        int bestLeftTarget = -1;
        int bestRightTarget = -1;
        float bestLeftPosition = 0f;
        float bestRightPosition = 0f;

        for (float previousTap : recentLiveTaps) {
            float leftPosition = Math.min(previousTap, position);
            float rightPosition = Math.max(previousTap, position);
            for (int leftTarget = 0; leftTarget < keyCount - 1; leftTarget++) {
                for (int rightTarget = leftTarget + 1;
                     rightTarget < keyCount; rightTarget++) {
                    float leftShift = leftPosition - liveCenters[leftTarget];
                    float rightShift = rightPosition - liveCenters[rightTarget];
                    float difference = Math.abs(leftShift - rightShift);
                    float sharedShift = (leftShift + rightShift) / 2f;
                    if (leftShift * rightShift >= 0f
                            && difference <= tolerance
                            && Math.abs(sharedShift) >= minimumShift
                            && difference < bestDifference) {
                        bestDifference = difference;
                        bestLeftTarget = leftTarget;
                        bestRightTarget = rightTarget;
                        bestLeftPosition = leftPosition;
                        bestRightPosition = rightPosition;
                    }
                }
            }
        }

        if (bestLeftTarget < 0) {
            return false;
        }

        updateTarget(bestLeftTarget, bestLeftPosition, maxShiftPercent);
        updateTarget(bestRightTarget, bestRightPosition, maxShiftPercent);
        enforceOrder(maxShiftPercent);
        recentLiveTaps.clear();
        return true;
    }

    private void updateTarget(int targetIndex, float position, int maxShiftPercent) {
        float shiftLimit = Math.max(0, maxShiftPercent) / 100f;
        float nominal = (targetIndex + 0.5f) / keyCount;
        liveCenters[targetIndex] = clamp(
                position,
                Math.max(0f, nominal - shiftLimit),
                Math.min(1f, nominal + shiftLimit));
    }

    private void enforceOrder(int maxShiftPercent) {
        float minimumSeparation = 0.1f / keyCount;
        float shiftLimit = Math.max(0, maxShiftPercent) / 100f;
        for (int i = 0; i < keyCount; i++) {
            float nominal = (i + 0.5f) / keyCount;
            float minCenter = Math.max(0f, nominal - shiftLimit);
            float maxCenter = Math.min(1f, nominal + shiftLimit);
            if (i > 0) {
                minCenter = Math.max(minCenter,
                        liveCenters[i - 1] + minimumSeparation);
            }
            liveCenters[i] = clamp(liveCenters[i], minCenter, maxCenter);
        }
    }

    private void constrainLiveCenters(int maxShiftPercent) {
        float shiftLimit = Math.max(0, maxShiftPercent) / 100f;
        for (int i = 0; i < keyCount; i++) {
            float nominal = (i + 0.5f) / keyCount;
            liveCenters[i] = clamp(liveCenters[i],
                    Math.max(0f, nominal - shiftLimit),
                    Math.min(1f, nominal + shiftLimit));
        }
        enforceOrder(maxShiftPercent);
    }

    private static float median(List<Float> values) {
        int middle = values.size() / 2;
        if ((values.size() & 1) == 0) {
            return (values.get(middle - 1) + values.get(middle)) / 2f;
        }
        return values.get(middle);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }
}
