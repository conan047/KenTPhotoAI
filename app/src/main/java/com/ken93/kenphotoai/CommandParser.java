package com.ken93.kenphotoai;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CommandParser {
    private CommandParser() {}

    public static EnhanceProfile parse(String input) {
        EnhanceProfile p = new EnhanceProfile();
        String s = strip(input == null ? "" : input.toLowerCase(Locale.ROOT));

        // Output size. A target such as 4K only upscales by default; it does not shrink a larger source.
        if (s.contains("4k") || s.contains("uhd")) {
            p.targetLongEdge = 3840;
            p.scaleFactor = 1;
        } else if (s.contains("2k") || s.contains("qhd")) {
            p.targetLongEdge = 2560;
            p.scaleFactor = 1;
        } else if (s.contains("1080") || s.contains("full hd") || s.contains("fhd")) {
            p.targetLongEdge = 1920;
            p.scaleFactor = 1;
        } else if (hasAny(s, "4x", "4 x", "gap 4", "4 lan")) {
            p.scaleFactor = 4;
        } else if (hasAny(s, "2x", "2 x", "gap 2", "2 lan")) {
            p.scaleFactor = 2;
        }
        p.allowDownscale = hasAny(s, "thu nho", "resize chinh xac", "dung kich thuoc");

        boolean auto = hasAny(s, "tu dong", "auto", "toi uu tu dong", "can bang tu dong");
        boolean portrait = hasAny(s, "anh nguoi", "khuon mat", "chan dung", "selfie");
        boolean oldPhoto = hasAny(s, "anh cu", "phuc hoi anh cu", "anh lau nam");

        p.preserveText = hasAny(s,
                "giu nguyen chu", "bao toan chu", "khong doi chu", "khong sua chu",
                "poster", "van ban", "so dien thoai", "logo", "tai lieu", "thong tin");

        p.sharpen = s.trim().isEmpty() || hasAny(s, "net", "sac net", "sharpen", "khu mo", "giam mo", "ro hon", "lam ro", "4k", "2k", "1080");
        if (hasAny(s, "khong lam net", "tat lam net", "bo lam net")) p.sharpen = false;
        if (hasAny(s, "net manh", "sieu net", "sac net manh")) p.sharpenAmount = 0.48f;
        if (hasAny(s, "net nhe", "tu nhien")) p.sharpenAmount = 0.20f;

        Integer sharpPct = extractPercent(s, "net|lam net|sac net");
        if (sharpPct != null) {
            p.sharpen = sharpPct > 0;
            p.sharpenAmount = Math.min(0.60f, Math.max(0f, sharpPct / 100f * 0.60f));
        }

        if (hasAny(s, "khu nhieu manh", "giam nhieu manh", "giam hat manh")) p.denoiseStrength = 2;
        else if (hasAny(s, "khu nhieu", "giam nhieu", "giam hat", "noise", "anh hat")) p.denoiseStrength = 1;
        if (hasAny(s, "khong khu nhieu", "tat khu nhieu")) p.denoiseStrength = 0;

        p.brighten = hasAny(s, "tang sang", "sang hon", "anh toi", "lam sang");
        if (hasAny(s, "sang nhe", "tang sang nhe")) p.brightnessFactor = 1.05f;
        else if (hasAny(s, "sang manh", "tang sang manh")) p.brightnessFactor = 1.16f;
        else if (p.brighten) p.brightnessFactor = 1.08f;
        Integer brightPct = extractPercent(s, "sang|tang sang");
        if (brightPct != null) {
            p.brighten = brightPct != 0;
            p.brightnessFactor = 1f + Math.min(40, Math.max(-40, brightPct)) / 100f;
        }
        if (hasAny(s, "khong tang sang", "tat tang sang")) {
            p.brighten = false;
            p.brightnessFactor = 1f;
        }

        if (hasAny(s, "tuong phan manh", "contrast manh")) p.contrastFactor = 1.16f;
        else if (hasAny(s, "tang tuong phan", "tuong phan", "contrast")) p.contrastFactor = 1.08f;
        Integer contrastPct = extractPercent(s, "tuong phan|contrast");
        if (contrastPct != null) p.contrastFactor = 1f + Math.min(40, Math.max(-40, contrastPct)) / 100f;

        if (hasAny(s, "mau dam", "dam mau", "mau dep", "tang mau", "ruc ro")) p.saturationFactor = 1.10f;
        if (hasAny(s, "mau tu nhien", "giu mau")) p.saturationFactor = 1.02f;
        Integer colorPct = extractPercent(s, "mau|bao hoa|saturation");
        if (colorPct != null) p.saturationFactor = 1f + Math.min(50, Math.max(-50, colorPct)) / 100f;

        p.autoContrast = auto || hasAny(s, "tu can tuong phan", "can bang sang", "auto contrast");

        if (portrait) {
            p.sharpen = true;
            p.sharpenAmount = Math.min(p.sharpenAmount, 0.24f);
            p.denoiseStrength = Math.max(p.denoiseStrength, 1);
            if (Math.abs(p.saturationFactor - 1f) < 0.01f) p.saturationFactor = 1.03f;
        }

        if (oldPhoto) {
            p.denoiseStrength = Math.max(p.denoiseStrength, 1);
            p.autoContrast = true;
            p.sharpen = true;
            p.sharpenAmount = Math.max(p.sharpenAmount, 0.24f);
            if (!p.brighten) {
                p.brighten = true;
                p.brightnessFactor = 1.04f;
            }
        }

        if (p.preserveText) {
            // Text/poster mode deliberately avoids denoise/blur that can soften diacritics and small digits.
            p.denoiseStrength = 0;
            p.autoContrast = false;
            p.sharpen = true;
            p.sharpenAmount = Math.max(p.sharpenAmount, 0.32f);
            if (Math.abs(p.contrastFactor - 1f) < 0.01f) p.contrastFactor = 1.04f;
        }

        if (hasAny(s, "chat luong cao", "16mp", "16 mp")) p.maxOutputPixels = 16_000_000;
        if (hasAny(s, "may yeu", "tiet kiem ram", "an toan bo nho")) p.maxOutputPixels = 8_500_000;

        return p;
    }

    private static Integer extractPercent(String s, String namePattern) {
        Pattern pattern = Pattern.compile("(?:" + namePattern + ")\\s*([+-]?\\d{1,3})\\s*%");
        Matcher m = pattern.matcher(s);
        if (!m.find()) return null;
        try {
            return Integer.parseInt(m.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean hasAny(String s, String... terms) {
        for (String term : terms) if (s.contains(term)) return true;
        return false;
    }

    private static String strip(String value) {
        String n = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd');
        return n.replaceAll("\\s+", " ").trim();
    }
}
