package com.ken93.kenphotoai;

import android.graphics.Bitmap;

/**
 * Extension point for a future native AI backend (for example Real-ESRGAN + ncnn/Vulkan).
 * v1.1 uses the built-in deterministic pixel pipeline so text is never regenerated.
 */
public interface UpscaleEngine {
    String name();
    boolean isAvailable();
    Bitmap upscale(Bitmap input, EnhanceProfile profile) throws Exception;
}
