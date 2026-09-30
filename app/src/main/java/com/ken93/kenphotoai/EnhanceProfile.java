package com.ken93.kenphotoai;

public class EnhanceProfile {
    public int scaleFactor = 1;
    public int targetLongEdge = 0;
    public boolean allowDownscale = false;

    public boolean sharpen = true;
    public float sharpenAmount = 0.28f;

    /** 0 = off, 1 = gentle, 2 = stronger edge-aware denoise. */
    public int denoiseStrength = 0;

    public boolean brighten = false;
    public float brightnessFactor = 1.0f;
    public float contrastFactor = 1.0f;
    public float saturationFactor = 1.0f;
    public boolean autoContrast = false;

    public boolean preserveText = false;

    /** Memory guard for phones. 12 MP still allows true UHD/4K landscape output. */
    public int maxOutputPixels = 12_000_000;

    public String summary() {
        StringBuilder sb = new StringBuilder();
        if (targetLongEdge > 0) sb.append("Cạnh dài ").append(targetLongEdge).append("px");
        else if (scaleFactor > 1) sb.append("Phóng ").append(scaleFactor).append("×");
        else sb.append("Giữ kích thước");

        if (sharpen) sb.append(" • Nét ").append(Math.round(sharpenAmount * 100)).append("%");
        if (denoiseStrength > 0) sb.append(" • Khử nhiễu ").append(denoiseStrength == 1 ? "nhẹ" : "mạnh");
        if (brighten) sb.append(" • Sáng ").append(Math.round((brightnessFactor - 1f) * 100)).append("%");
        if (autoContrast) sb.append(" • Tự cân tương phản");
        else if (Math.abs(contrastFactor - 1f) > 0.01f) sb.append(" • Tương phản ").append(Math.round((contrastFactor - 1f) * 100)).append("%");
        if (Math.abs(saturationFactor - 1f) > 0.01f) sb.append(" • Màu ").append(Math.round((saturationFactor - 1f) * 100)).append("%");
        if (preserveText) sb.append(" • Bảo toàn chữ");
        return sb.toString();
    }
}
