package gay.nihil.lena.midikeypad;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;

final class TwoKeyOverlayRenderer {
    private TwoKeyOverlayRenderer() {
    }

    static void drawSplit(Canvas canvas, int width, int height, float density,
                          float splitRatio, int deadzonePercent, String caption) {
        float split = clamp(splitRatio, 0f, 1f) * width;
        float gapWidth = width * clamp(deadzonePercent, 0, 100) / 100f;
        float gapLeft = clamp(split - gapWidth / 2f, 0f, width);
        float gapRight = clamp(split + gapWidth / 2f, 0f, width);

        canvas.drawRect(0, 0, gapLeft, height, fill(0x8842A5F5));
        canvas.drawRect(gapRight, 0, width, height, fill(0x8874C69D));
        drawDeadzone(canvas, gapLeft, gapRight, height, density);
        if (gapWidth == 0) {
            drawLine(canvas, split, height, density);
        }
        drawCaption(canvas, width, density, caption);
    }

    static void drawTargets(Canvas canvas, int width, int height, float density,
                            float leftCenterRatio, float rightCenterRatio,
                            float deadzoneCenterRatio, int deadzonePercent,
                            int targetWidthPercent, int maxShiftPercent, String caption) {
        float leftCenter = clamp(leftCenterRatio, 0f, 1f) * width;
        float rightCenter = clamp(rightCenterRatio, 0f, 1f) * width;
        float split = clamp(deadzoneCenterRatio, 0f, 1f) * width;
        float deadzoneWidth = width * clamp(deadzonePercent, 0, 100) / 100f;
        float gapLeft = clamp(split - deadzoneWidth / 2f, 0f, width);
        float gapRight = clamp(split + deadzoneWidth / 2f, 0f, width);
        float targetHalfWidth = width * clamp(targetWidthPercent, 0, 100) / 200f;
        float adaptationRange = width * clamp(maxShiftPercent, 0, 100) / 100f;
        float acceptanceRadius = targetHalfWidth + adaptationRange;

        float leftStart = Math.max(0f, leftCenter - acceptanceRadius);
        float leftEnd = Math.min(gapLeft, leftCenter + acceptanceRadius);
        float rightStart = Math.max(gapRight, rightCenter - acceptanceRadius);
        float rightEnd = Math.min(width, rightCenter + acceptanceRadius);
        canvas.drawRect(leftStart, 0, leftEnd, height, fill(0x8842A5F5));
        canvas.drawRect(rightStart, 0, rightEnd, height, fill(0x8874C69D));

        Paint targetPaint = fill(0x55FFFFFF);
        float leftTargetStart = clamp(leftCenter - targetHalfWidth, 0f, width);
        float leftTargetEnd = clamp(leftCenter + targetHalfWidth, 0f, width);
        float rightTargetStart = clamp(rightCenter - targetHalfWidth, 0f, width);
        float rightTargetEnd = clamp(rightCenter + targetHalfWidth, 0f, width);
        canvas.drawRect(leftTargetStart, 0, leftTargetEnd, height, targetPaint);
        canvas.drawRect(rightTargetStart, 0, rightTargetEnd, height, targetPaint);

        Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
        outline.setColor(Color.WHITE);
        outline.setAlpha(220);
        outline.setStyle(Paint.Style.STROKE);
        outline.setStrokeWidth(Math.max(1f, 2f * density));
        outline.setPathEffect(new DashPathEffect(new float[]{8f * density, 5f * density}, 0));
        canvas.drawRect(leftStart, 0, leftEnd, height, outline);
        canvas.drawRect(rightStart, 0, rightEnd, height, outline);

        drawDeadzone(canvas, gapLeft, gapRight, height, density);
        if (deadzoneWidth == 0) {
            drawLine(canvas, split, height, density);
        }
        drawLine(canvas, leftCenter, height, density);
        drawLine(canvas, rightCenter, height, density);
        drawCaption(canvas, width, density, caption);
    }

    static void drawMultiKey(Canvas canvas, int width, int height, float density,
                             MultiKeyMapping mapping, int midiChannel,
                             String layoutName, String deadzoneLabel) {
        int keyCount = mapping.getKeyCount();
        float[] centers = new float[keyCount];
        float laneShift = mapping.getLayoutShiftRatio();
        if (mapping.isScreenContextEnabled()) {
            canvas.drawRect(0, 0, width, height, fill(0x66303030));
        }

        for (int i = 0; i < keyCount; i++) {
            centers[i] = mapping.getCenterRatio(i);
            float left = 0f;
            float right = width;
            if (mapping.isScreenContextEnabled()) {
                left = clamp((i / (float) keyCount + laneShift) * width, 0f, width);
                right = clamp(((i + 1f) / keyCount + laneShift) * width, 0f, width);
            } else if (i > 0) {
                left = (centers[i] + centers[i - 1]) * width / 2f
                        + mapping.getDeadzoneHalfWidthRatio(i - 1) * width;
            }
            if (!mapping.isScreenContextEnabled() && i < keyCount - 1) {
                right = (centers[i] + centers[i + 1]) * width / 2f
                        - mapping.getDeadzoneHalfWidthRatio(i) * width;
            }

            float hue = 360f * i / keyCount;
            int laneColor = Color.HSVToColor(136, new float[]{hue, 0.65f, 0.82f});
            if (mapping.isScreenContextEnabled()) {
                canvas.drawRect(left, 0, right, height, fill(0x22303030));
                Paint buttonOutline = new Paint(Paint.ANTI_ALIAS_FLAG);
                buttonOutline.setColor(Color.WHITE);
                buttonOutline.setAlpha(70);
                buttonOutline.setStyle(Paint.Style.STROKE);
                buttonOutline.setStrokeWidth(Math.max(1f, density));
                canvas.drawRect(left, 0, right, height, buttonOutline);
            } else {
                canvas.drawRect(left, 0, right, height, fill(laneColor));
            }

            if (mapping.isAdaptive()) {
                float acceptedLeft = mapping.isScreenContextEnabled()
                        ? mapping.getAcceptedStartRatio(i) * width
                        : Math.max(left, centers[i] * width
                        - (mapping.getTargetWidthPercent() / 2f
                        + mapping.getMaxShiftPercent()) * width / 100f);
                float acceptedRight = mapping.isScreenContextEnabled()
                        ? mapping.getAcceptedEndRatio(i) * width
                        : Math.min(right, centers[i] * width
                        + (mapping.getTargetWidthPercent() / 2f
                        + mapping.getMaxShiftPercent()) * width / 100f);
                canvas.drawRect(acceptedLeft, 0, acceptedRight, height,
                        fill(mapping.isScreenContextEnabled() ? laneColor : 0x33FFFFFF));
                Paint acceptedOutline = new Paint(Paint.ANTI_ALIAS_FLAG);
                acceptedOutline.setColor(Color.WHITE);
                acceptedOutline.setAlpha(150);
                acceptedOutline.setStyle(Paint.Style.STROKE);
                acceptedOutline.setStrokeWidth(Math.max(1f, 1f * density));
                acceptedOutline.setPathEffect(
                        new DashPathEffect(new float[]{5f * density, 5f * density}, 0));
                canvas.drawRect(acceptedLeft, 0, acceptedRight, height, acceptedOutline);

                float halfTargetWidth = mapping.getTargetWidthPercent() * width / 200f;
                float targetLeft = Math.max(acceptedLeft, centers[i] * width - halfTargetWidth);
                float targetRight = Math.min(acceptedRight, centers[i] * width + halfTargetWidth);
                Paint targetFill = fill(0x55FFFFFF);
                canvas.drawRect(targetLeft, 0, targetRight, height, targetFill);
                Paint targetOutline = new Paint(Paint.ANTI_ALIAS_FLAG);
                targetOutline.setColor(Color.WHITE);
                targetOutline.setAlpha(220);
                targetOutline.setStyle(Paint.Style.STROKE);
                targetOutline.setStrokeWidth(Math.max(1f, 2f * density));
                targetOutline.setPathEffect(
                        new DashPathEffect(new float[]{8f * density, 5f * density}, 0));
                canvas.drawRect(targetLeft, 0, targetRight, height, targetOutline);
            }

            Paint notePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            notePaint.setColor(Color.WHITE);
            notePaint.setTextSize(Math.min(14f * density, (right - left) * 0.35f));
            notePaint.setFakeBoldText(true);
            String note = Integer.toString(mapping.getNote(i));
            canvas.drawText(note, centers[i] * width - notePaint.measureText(note) / 2f,
                    height * 0.58f, notePaint);
            if (mapping.isAdaptive()) {
                String center = Math.round(centers[i] * 100) + "%";
                Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                centerPaint.setColor(Color.WHITE);
                centerPaint.setTextSize(Math.min(10f * density, (right - left) * 0.25f));
                float centerTextWidth = centerPaint.measureText(center);
                canvas.drawText(center, centers[i] * width - centerTextWidth / 2f,
                        height * 0.66f, centerPaint);
            }
            drawLine(canvas, centers[i] * width, height, density);
        }

        Paint gapPaint = fill(0xDD202020);
        Paint gapEdge = new Paint(Paint.ANTI_ALIAS_FLAG);
        gapEdge.setColor(Color.WHITE);
        gapEdge.setAlpha(210);
        gapEdge.setStrokeWidth(Math.max(1f, 1.5f * density));
        for (int i = 0; i < keyCount - 1; i++) {
            float midpoint = (centers[i] + centers[i + 1]) * width / 2f;
            float halfGap = mapping.getEffectiveDeadzoneHalfWidthRatio(i) * width;
            if (halfGap > 0) {
                canvas.drawRect(midpoint - halfGap, 0, midpoint + halfGap, height, gapPaint);
                canvas.drawLine(midpoint - halfGap, 0, midpoint - halfGap, height, gapEdge);
                canvas.drawLine(midpoint + halfGap, 0, midpoint + halfGap, height, gapEdge);
            } else {
                drawLine(canvas, midpoint, height, density);
            }
            if (mapping.hasDeadzoneCluster(i)) {
                float clusterCenter = mapping.getDeadzoneClusterCenterRatio(i) * width;
                float clusterRadius = mapping.getDeadzoneClusterRadiusRatio(i) * width;
                drawDeadzone(canvas, clusterCenter - clusterRadius,
                        clusterCenter + clusterRadius, height, density);
            }
        }

        String targetLabel = mapping.isAdaptive()
                ? " | target " + mapping.getTargetWidthPercent() + "%" : "";
        drawCaption(canvas, width, density, keyCount + " keys | " + layoutName
                + " | MIDI ch " + midiChannel + targetLabel + " | " + deadzoneLabel);
    }

    private static void drawDeadzone(Canvas canvas, float left, float right,
                                     int height, float density) {
        if (right <= left) {
            return;
        }
        canvas.drawRect(left, 0, right, height, fill(0xDD202020));
        Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
        edge.setColor(Color.WHITE);
        edge.setAlpha(210);
        edge.setStrokeWidth(Math.max(1f, 1.5f * density));
        canvas.drawLine(left, 0, left, height, edge);
        canvas.drawLine(right, 0, right, height, edge);
    }

    private static void drawLine(Canvas canvas, float x, int height, float density) {
        Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        line.setColor(Color.WHITE);
        line.setAlpha(220);
        line.setStrokeWidth(Math.max(1f, 2f * density));
        canvas.drawLine(x, 0, x, height, line);
    }

    private static void drawCaption(Canvas canvas, int width, float density, String text) {
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        float textSize = 14f * density;
        float maxTextWidth = Math.max(1f, width - 28f * density);
        while (textSize > 9f * density) {
            textPaint.setTextSize(textSize);
            if (textPaint.measureText(text) <= maxTextWidth) {
                break;
            }
            textSize -= density;
        }
        textPaint.setTextSize(textSize);
        textPaint.setFakeBoldText(true);
        float horizontalPadding = 10f * density;
        float verticalPadding = 7f * density;
        float textWidth = textPaint.measureText(text);
        float left = Math.max(4f * density, (width - textWidth - horizontalPadding * 2) / 2f);
        float top = 12f * density;
        float textHeight = textPaint.getTextSize();
        RectF background = new RectF(left, top, left + textWidth + horizontalPadding * 2,
                top + textHeight + verticalPadding * 2);
        canvas.drawRoundRect(background, 6f * density, 6f * density, fill(0xDD202020));
        canvas.drawText(text, left + horizontalPadding,
                top + verticalPadding + textHeight, textPaint);
    }

    private static Paint fill(int color) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        return paint;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }
}
