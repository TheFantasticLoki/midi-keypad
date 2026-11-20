package gay.nihil.lena.midikeypad;

final class TwoKeyDeadzoneMapping {
    private TwoKeyDeadzoneMapping() {
    }

    static Integer noteForTap(float x, float width, int deadzonePercent) {
        if (width <= 0) {
            return null;
        }

        float split = width / 2;
        float deadzoneHalfWidth = width * deadzonePercent / 200f;
        if (deadzonePercent > 0 && Math.abs(x - split) <= deadzoneHalfWidth) {
            return null;
        }
        return x < split ? 60 : 61;
    }
}
