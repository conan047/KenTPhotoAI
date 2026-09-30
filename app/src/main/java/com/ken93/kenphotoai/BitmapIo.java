package com.ken93.kenphotoai;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;

public final class BitmapIo {
    private static final AtomicInteger SAVE_COUNTER = new AtomicInteger(0);
    private static final int PROCESS_DECODE_PIXELS = 20_000_000;
    private static final int PREVIEW_DECODE_PIXELS = 3_000_000;

    private BitmapIo() {}

    public static Bitmap load(Context context, Uri uri) throws IOException {
        return load(context, uri, PROCESS_DECODE_PIXELS);
    }

    public static Bitmap loadPreview(Context context, Uri uri) throws IOException {
        return load(context, uri, PREVIEW_DECODE_PIXELS);
    }

    public static Bitmap load(Context context, Uri uri, int maxPixels) throws IOException {
        ContentResolver resolver = context.getContentResolver();
        if (Build.VERSION.SDK_INT >= 28) {
            ImageDecoder.Source source = ImageDecoder.createSource(resolver, uri);
            return ImageDecoder.decodeBitmap(source, (decoder, info, src) -> {
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                decoder.setMutableRequired(false);
                int w = info.getSize().getWidth();
                int h = info.getSize().getHeight();
                int sample = calculateSample(w, h, maxPixels);
                if (sample > 1) decoder.setTargetSampleSize(sample);
            });
        }

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = resolver.openInputStream(uri)) {
            if (in == null) throw new IOException("Không mở được ảnh.");
            BitmapFactory.decodeStream(in, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw new IOException("Không đọc được kích thước ảnh.");

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        opts.inSampleSize = calculateSample(bounds.outWidth, bounds.outHeight, maxPixels);
        try (InputStream in = resolver.openInputStream(uri)) {
            if (in == null) throw new IOException("Không mở được ảnh.");
            Bitmap bitmap = BitmapFactory.decodeStream(in, null, opts);
            if (bitmap == null) throw new IOException("Định dạng ảnh không hỗ trợ.");
            return bitmap;
        }
    }

    private static int calculateSample(int w, int h, int maxPixels) {
        int sample = 1;
        long limit = Math.max(1_000_000, maxPixels);
        while (((long) w / sample) * ((long) h / sample) > limit && sample < 16) sample *= 2;
        return sample;
    }

    public static Uri save(Context context, Bitmap bitmap, String formatName, int quality) throws IOException {
        String fmt = formatName == null ? "JPG" : formatName.toUpperCase();
        Bitmap.CompressFormat compressFormat;
        String ext;
        String mime;
        if (fmt.equals("PNG")) {
            compressFormat = Bitmap.CompressFormat.PNG;
            ext = ".png";
            mime = "image/png";
        } else if (fmt.equals("WEBP")) {
            if (Build.VERSION.SDK_INT >= 30) compressFormat = Bitmap.CompressFormat.WEBP_LOSSLESS;
            else compressFormat = Bitmap.CompressFormat.WEBP;
            ext = ".webp";
            mime = "image/webp";
        } else {
            compressFormat = Bitmap.CompressFormat.JPEG;
            ext = ".jpg";
            mime = "image/jpeg";
        }
        String fileName = "KenPhoto_" + System.currentTimeMillis() + "_" + SAVE_COUNTER.incrementAndGet() + ext;
        quality = Math.max(70, Math.min(100, quality));

        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, mime);
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/KenPhotoAI");
            values.put(MediaStore.Images.Media.IS_PENDING, 1);

            ContentResolver resolver = context.getContentResolver();
            Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new IOException("Không tạo được tệp ảnh đầu ra.");
            try (OutputStream out = resolver.openOutputStream(uri)) {
                if (out == null || !bitmap.compress(compressFormat, quality, out)) {
                    resolver.delete(uri, null, null);
                    throw new IOException("Không lưu được ảnh.");
                }
            }
            values.clear();
            values.put(MediaStore.Images.Media.IS_PENDING, 0);
            resolver.update(uri, values, null, null);
            return uri;
        }

        File base = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (base == null) throw new IOException("Không truy cập được thư mục ảnh của ứng dụng.");
        File dir = new File(base, "KenPhotoAI");
        if (!dir.exists() && !dir.mkdirs()) throw new IOException("Không tạo được thư mục lưu ảnh.");
        File file = new File(dir, fileName);
        try (OutputStream out = new FileOutputStream(file)) {
            if (!bitmap.compress(compressFormat, quality, out)) throw new IOException("Không lưu được ảnh.");
        }
        return Uri.fromFile(file);
    }
}
