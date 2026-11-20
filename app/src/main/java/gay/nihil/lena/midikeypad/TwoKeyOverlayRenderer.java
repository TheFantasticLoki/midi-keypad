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
