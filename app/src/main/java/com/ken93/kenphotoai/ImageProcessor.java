package com.ken93.kenphotoai;

import android.graphics.Bitmap;
import android.graphics.Color;

public final class ImageProcessor {
    private ImageProcessor() {}

    public static Bitmap process(Bitmap input, EnhanceProfile profile) {
        Bitmap work = ensureArgb(input);
        if (work != input && !input.isRecycled()) input.recycle();

        if (profile.denoiseStrength > 0) {
            work = edgeAwareDenoise(work, profile.denoiseStrength);
        }

        if (profile.autoContrast) {
            work = autoContrast(work);
        }

        if (profile.brighten || Math.abs(profile.contrastFactor - 1f) > 0.01f || Math.abs(profile.saturationFactor - 1f) > 0.01f) {
            work = adjustColors(work, profile.brightnessFactor, profile.contrastFactor, profile.saturationFactor);
        }

        long predicted = predictOutputPixels(work.getWidth(), work.getHeight(), profile);
        boolean sharpenBeforeResize = profile.sharpen && predicted > 9_000_000L && predicted > (long) work.getWidth() * work.getHeight();

        if (sharpenBeforeResize) {
            work = sharpenCross(work, profile.sharpenAmount);
        }

        work = resize(work, profile);

        if (profile.sharpen && !sharpenBeforeResize) {
            work = sharpenCross(work, profile.sharpenAmount);
        }
        return work;
    }

    private static Bitmap ensureArgb(Bitmap input) {
        if (input.getConfig() == Bitmap.Config.ARGB_8888 && input.isMutable()) return input;
        Bitmap copy = input.copy(Bitmap.Config.ARGB_8888, true);
        if (copy == null) throw new IllegalStateException("Không tạo được bitmap ARGB_8888.");
        return copy;
    }

    public static int[] computeOutputDimensions(int w, int h, EnhanceProfile p) {
        int newW = Math.max(1, w);
        int newH = Math.max(1, h);

        if (p.targetLongEdge > 0) {
            int longEdge = Math.max(w, h);
            boolean shouldResize = p.allowDownscale ? longEdge != p.targetLongEdge : longEdge < p.targetLongEdge;
            if (shouldResize) {
                float ratio = p.targetLongEdge / (float) longEdge;
                newW = Math.max(1, Math.round(w * ratio));
                newH = Math.max(1, Math.round(h * ratio));
            }
        } else if (p.scaleFactor > 1) {
            newW = safeMultiply(w, p.scaleFactor);
            newH = safeMultiply(h, p.scaleFactor);
        }

        long pixels = (long) newW * newH;
        int maxPixels = Math.max(2_000_000, p.maxOutputPixels);
        if (pixels > maxPixels) {
            double ratio = Math.sqrt(maxPixels / (double) pixels);
            newW = Math.max(1, (int) Math.round(newW * ratio));
            newH = Math.max(1, (int) Math.round(newH * ratio));
        }
        return new int[]{newW, newH};
    }

    private static long predictOutputPixels(int w, int h, EnhanceProfile p) {
        int[] size = computeOutputDimensions(w, h, p);
        return (long) size[0] * size[1];
    }

    private static Bitmap resize(Bitmap src, EnhanceProfile p) {
        int w = src.getWidth();
        int h = src.getHeight();
        int[] size = computeOutputDimensions(w, h, p);
        int newW = size[0];
        int newH = size[1];

        if (newW == w && newH == h) return src;
        Bitmap scaled = Bitmap.createScaledBitmap(src, newW, newH, true);
        if (scaled != src) src.recycle();
        return scaled;
    }

    private static int safeMultiply(int value, int factor) {
        long out = (long) value * factor;
        return out > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) out;
    }

    /** Edge-aware 3x3 denoise: averages only neighbours with similar luminance. */
    private static Bitmap edgeAwareDenoise(Bitmap src, int strength) {
        int w = src.getWidth();
        int h = src.getHeight();
        if (w < 3 || h < 3) return src;

        int[] in = new int[w * h];
        int[] out = new int[w * h];
        src.getPixels(in, 0, w, 0, 0, w, h);
        int threshold = strength >= 2 ? 34 : 20;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int idx = y * w + x;
                int center = in[idx];
                int centerY = luma(center);
                int sumA = Color.alpha(center) * 2;
                int sumR = Color.red(center) * 2;
                int sumG = Color.green(center) * 2;
                int sumB = Color.blue(center) * 2;
                int weight = 2;

                for (int dy = -1; dy <= 1; dy++) {
                    int yy = Math.max(0, Math.min(h - 1, y + dy));
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dx == 0 && dy == 0) continue;
                        int xx = Math.max(0, Math.min(w - 1, x + dx));
                        int c = in[yy * w + xx];
                        if (Math.abs(luma(c) - centerY) <= threshold) {
                            sumA += Color.alpha(c);
                            sumR += Color.red(c);
                            sumG += Color.green(c);
                            sumB += Color.blue(c);
                            weight++;
                        }
                    }
                }
                out[idx] = Color.argb(sumA / weight, sumR / weight, sumG / weight, sumB / weight);
            }
        }

        Bitmap result = Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888);
        src.recycle();
        return result;
    }

    /** Clips roughly the darkest/brightest 1% of luminance, preserving hue as much as possible. */
    private static Bitmap autoContrast(Bitmap src) {
        int w = src.getWidth();
        int h = src.getHeight();
        int[] px = new int[w * h];
        int[] hist = new int[256];
        src.getPixels(px, 0, w, 0, 0, w, h);
        for (int c : px) hist[luma(c)]++;

        int total = px.length;
        int clip = Math.max(1, total / 100);
        int low = 0;
        int acc = 0;
        while (low < 254 && acc + hist[low] < clip) acc += hist[low++];
        int high = 255;
        acc = 0;
        while (high > 1 && acc + hist[high] < clip) acc += hist[high--];
        if (high - low < 24) return src;

        float span = high - low;
        for (int i = 0; i < px.length; i++) {
            int c = px[i];
            int y = luma(c);
            float mapped = (y - low) * 255f / span;
            mapped = Math.max(0f, Math.min(255f, mapped));
            float factor = y <= 1 ? 1f : mapped / y;
            int r = clamp(Math.round(Color.red(c) * factor));
            int g = clamp(Math.round(Color.green(c) * factor));
            int b = clamp(Math.round(Color.blue(c) * factor));
            px[i] = Color.argb(Color.alpha(c), r, g, b);
        }

        Bitmap out = Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888);
        src.recycle();
        return out;
    }

    private static Bitmap adjustColors(Bitmap src, float brightness, float contrast, float saturation) {
        int w = src.getWidth();
        int h = src.getHeight();
        int[] px = new int[w * h];
        src.getPixels(px, 0, w, 0, 0, w, h);

        for (int i = 0; i < px.length; i++) {
            int c = px[i];
            float r = Color.red(c) * brightness;
            float g = Color.green(c) * brightness;
            float b = Color.blue(c) * brightness;

            r = (r - 128f) * contrast + 128f;
            g = (g - 128f) * contrast + 128f;
            b = (b - 128f) * contrast + 128f;

            float gray = 0.299f * r + 0.587f * g + 0.114f * b;
            r = gray + (r - gray) * saturation;
            g = gray + (g - gray) * saturation;
            b = gray + (b - gray) * saturation;

            px[i] = Color.argb(Color.alpha(c), clamp(Math.round(r)), clamp(Math.round(g)), clamp(Math.round(b)));
        }

        Bitmap out = Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888);
        src.recycle();
        return out;
    }

    /** Memory-friendlier sharpen than building a second blurred bitmap. */
    private static Bitmap sharpenCross(Bitmap src, float amount) {
        if (amount <= 0f) return src;
        int w = src.getWidth();
        int h = src.getHeight();
        if (w < 3 || h < 3) return src;

        int[] in = new int[w * h];
        int[] out = new int[w * h];
        src.getPixels(in, 0, w, 0, 0, w, h);
        System.arraycopy(in, 0, out, 0, in.length);

        for (int y = 1; y < h - 1; y++) {
            int row = y * w;
            for (int x = 1; x < w - 1; x++) {
                int idx = row + x;
                int c = in[idx];
                int n = in[idx - w];
                int s = in[idx + w];
                int e = in[idx + 1];
                int west = in[idx - 1];

                int r = sharpenChannel(Color.red(c), Color.red(n), Color.red(s), Color.red(e), Color.red(west), amount);
                int g = sharpenChannel(Color.green(c), Color.green(n), Color.green(s), Color.green(e), Color.green(west), amount);
                int b = sharpenChannel(Color.blue(c), Color.blue(n), Color.blue(s), Color.blue(e), Color.blue(west), amount);
                out[idx] = Color.argb(Color.alpha(c), r, g, b);
            }
        }

        Bitmap result = Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888);
        src.recycle();
        return result;
    }

    private static int sharpenChannel(int center, int n, int s, int e, int w, float amount) {
        float detail = 4f * center - n - s - e - w;
        return clamp(Math.round(center + amount * detail));
    }

    private static int luma(int c) {
        return (77 * Color.red(c) + 150 * Color.green(c) + 29 * Color.blue(c)) >> 8;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
