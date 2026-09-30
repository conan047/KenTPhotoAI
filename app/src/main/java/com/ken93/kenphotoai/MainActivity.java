package com.ken93.kenphotoai;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends Activity {
    private static final int REQ_PICK = 9001;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<Uri> selectedUris = new ArrayList<>();
    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);

    private ImageView preview;
    private EditText commandInput;
    private TextView commandPreview;
    private TextView status;
    private ProgressBar progress;
    private Spinner formatSpinner;
    private Spinner qualitySpinner;
    private Button processButton;
    private Button batchButton;
    private Button cancelButton;
    private Button saveButton;
    private Button compareButton;

    private Bitmap originalBitmap;
    private Bitmap processedBitmap;
    private boolean showingProcessed = true;
    private boolean batchRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
    }

    private View buildUi() {
        int pad = dp(16);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(247, 248, 252));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(this);
        title.setText("KenPhoto AI");
        title.setTextSize(30);
        title.setTextColor(Color.rgb(30, 58, 138));
        title.setTypeface(null, 1);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("v1.2 • làm nét • tăng độ phân giải • bảo toàn chữ • offline");
        subtitle.setTextSize(14);
        subtitle.setTextColor(Color.DKGRAY);
        subtitle.setPadding(0, dp(4), 0, dp(12));
        root.addView(subtitle);

        Button pick = button("CHỌN ẢNH");
        pick.setOnClickListener(v -> pickImages());
        root.addView(pick, matchWrap());

        preview = new ImageView(this);
        preview.setBackgroundColor(Color.rgb(230, 233, 240));
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams imageLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(300));
        imageLp.setMargins(0, dp(12), 0, dp(12));
        root.addView(preview, imageLp);

        root.addView(label("Câu lệnh tiếng Việt"));

        commandInput = new EditText(this);
        commandInput.setMinLines(2);
        commandInput.setHint("Ví dụ: Làm nét 4K, tăng sáng nhẹ, giữ nguyên chữ và logo");
        commandInput.setText("Làm nét 4K, giữ nguyên chữ và logo");
        commandInput.setTextSize(15);
        commandInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        root.addView(commandInput, matchWrap());

        commandPreview = new TextView(this);
        commandPreview.setTextSize(12);
        commandPreview.setTextColor(Color.rgb(75, 85, 99));
        commandPreview.setPadding(0, dp(6), 0, dp(2));
        root.addView(commandPreview, matchWrap());
        commandInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { refreshCommandPreview(); }
            @Override public void afterTextChanged(Editable s) {}
        });
        refreshCommandPreview();

        TextView presetLabel = label("Preset nhanh");
        presetLabel.setPadding(0, dp(12), 0, dp(6));
        root.addView(presetLabel);

        LinearLayout presets1 = row();
        root.addView(presets1, matchWrap());
        addPreset(presets1, "Tự động", "Tối ưu tự động, làm nét tự nhiên, màu tự nhiên");
        addPreset(presets1, "Nét 2×", "Làm nét 2x tự nhiên");
        addPreset(presets1, "4K", "Làm nét 4K tự nhiên");

        LinearLayout presets2 = row();
        root.addView(presets2, matchWrap());
        addPreset(presets2, "Poster", "Poster văn bản 4K, bảo toàn chữ, số điện thoại và logo");
        addPreset(presets2, "Khử nhiễu", "Khử nhiễu, làm nét tự nhiên");
        addPreset(presets2, "Ảnh cũ", "Phục hồi ảnh cũ, khử nhiễu, tự động cân bằng, làm nét 2x");

        LinearLayout presets3 = row();
        root.addView(presets3, matchWrap());
        addPreset(presets3, "Ảnh người", "Ảnh người, làm nét nhẹ tự nhiên, khử nhiễu");
        addPreset(presets3, "Tăng sáng", "Tăng sáng nhẹ, làm nét tự nhiên");
        addPreset(presets3, "Màu đẹp", "Màu đẹp, tăng tương phản nhẹ, làm nét tự nhiên");

        LinearLayout presets4 = row();
        root.addView(presets4, matchWrap());
        addPreset(presets4, "2K", "Làm nét 2K tự nhiên");
        addPreset(presets4, "1080p", "Làm nét 1080p tự nhiên");
        addPreset(presets4, "Nét mạnh", "Làm nét mạnh, màu tự nhiên");

        LinearLayout actions = row();
        actions.setPadding(0, dp(12), 0, 0);
        root.addView(actions, matchWrap());

        processButton = button("XỬ LÝ");
        processButton.setOnClickListener(v -> processCurrent());
        actions.addView(processButton, weighted());

        compareButton = button("TRƯỚC / SAU");
        compareButton.setEnabled(false);
        compareButton.setOnClickListener(v -> toggleCompare());
        actions.addView(compareButton, weighted());

        LinearLayout secondActions = row();
        root.addView(secondActions, matchWrap());

        batchButton = button("HÀNG LOẠT");
        batchButton.setOnClickListener(v -> processBatch());
        secondActions.addView(batchButton, weighted());

        saveButton = button("LƯU ẢNH");
        saveButton.setEnabled(false);
        saveButton.setOnClickListener(v -> saveCurrent());
        secondActions.addView(saveButton, weighted());

        cancelButton = button("HỦY");
        cancelButton.setEnabled(false);
        cancelButton.setOnClickListener(v -> {
            cancelRequested.set(true);
            status.setText("Đang yêu cầu dừng sau ảnh hiện tại...");
        });
        secondActions.addView(cancelButton, weighted());

        LinearLayout formatRow = row();
        formatRow.setGravity(Gravity.CENTER_VERTICAL);
        formatRow.setPadding(0, dp(8), 0, 0);
        root.addView(formatRow, matchWrap());

        TextView fmt = new TextView(this);
        fmt.setText("Lưu: ");
        fmt.setTextSize(14);
        formatRow.addView(fmt);

        formatSpinner = new Spinner(this);
        formatSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"JPG", "PNG", "WEBP"}));
        formatRow.addView(formatSpinner, new LinearLayout.LayoutParams(dp(105), ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView q = new TextView(this);
        q.setText("  Chất lượng: ");
        q.setTextSize(14);
        formatRow.addView(q);

        qualitySpinner = new Spinner(this);
        qualitySpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"95", "100", "90", "85"}));
        formatRow.addView(qualitySpinner, new LinearLayout.LayoutParams(dp(90), ViewGroup.LayoutParams.WRAP_CONTENT));

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setIndeterminate(true);
        progress.setVisibility(View.GONE);
        LinearLayout.LayoutParams progressLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(18));
        progressLp.setMargins(0, dp(10), 0, dp(4));
        root.addView(progress, progressLp);

        status = new TextView(this);
        status.setText("Chưa chọn ảnh.");
        status.setTextSize(14);
        status.setTextColor(Color.rgb(55, 65, 81));
        status.setPadding(0, dp(8), 0, dp(24));
        root.addView(status);

        return scroll;
    }

    private void pickImages() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQ_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_PICK || resultCode != RESULT_OK || data == null) return;

        selectedUris.clear();
        ClipData clip = data.getClipData();
        if (clip != null) {
            for (int i = 0; i < clip.getItemCount(); i++) selectedUris.add(clip.getItemAt(i).getUri());
        } else if (data.getData() != null) {
            selectedUris.add(data.getData());
        }
        if (selectedUris.isEmpty()) return;
        loadPreview(selectedUris.get(0));
    }

    private void loadPreview(Uri uri) {
        setBusy(true, false, "Đang mở ảnh...");
        executor.execute(() -> {
            try {
                Bitmap bitmap = BitmapIo.loadPreview(this, uri);
                runOnUiThread(() -> {
                    recycleOriginal();
                    originalBitmap = bitmap;
                    recycleProcessed();
                    preview.setImageBitmap(originalBitmap);
                    showingProcessed = false;
                    compareButton.setEnabled(false);
                    saveButton.setEnabled(false);
                    refreshCommandPreview();
                    setBusy(false, false, "Đã chọn " + selectedUris.size() + " ảnh • bản xem trước " + bitmap.getWidth() + "×" + bitmap.getHeight() + " px");
                });
            } catch (Exception e) {
                runOnUiThread(() -> setBusy(false, false, "Lỗi mở ảnh: " + e.getMessage()));
            }
        });
    }

    private void processCurrent() {
        if (selectedUris.isEmpty()) {
            toast("Hãy chọn ảnh trước.");
            return;
        }
        final EnhanceProfile profile = CommandParser.parse(commandInput.getText().toString());
        final Uri uri = selectedUris.get(0);
        cancelRequested.set(false);
        setBusy(true, false, "Đang xử lý • " + profile.summary());
        executor.execute(() -> {
            try {
                Bitmap input = BitmapIo.load(this, uri);
                Bitmap result = ImageProcessor.process(input, profile);
                runOnUiThread(() -> {
                    recycleProcessed();
                    processedBitmap = result;
                    preview.setImageBitmap(processedBitmap);
                    showingProcessed = true;
                    compareButton.setEnabled(true);
                    saveButton.setEnabled(true);
                    setBusy(false, false, "Hoàn tất • " + result.getWidth() + "×" + result.getHeight() + " px • " + profile.summary());
                });
            } catch (OutOfMemoryError e) {
                runOnUiThread(() -> setBusy(false, false, "Thiếu RAM. Hãy dùng 2×/4K hoặc thêm câu 'tiết kiệm RAM'."));
            } catch (Exception e) {
                runOnUiThread(() -> setBusy(false, false, "Lỗi xử lý: " + e.getMessage()));
            }
        });
    }

    private void processBatch() {
        if (selectedUris.isEmpty()) {
            toast("Hãy chọn ảnh trước.");
            return;
        }
        final EnhanceProfile profile = CommandParser.parse(commandInput.getText().toString());
        final String format = String.valueOf(formatSpinner.getSelectedItem());
        final int quality = selectedQuality();
        cancelRequested.set(false);
        batchRunning = true;
        setBusy(true, true, "Bắt đầu xử lý " + selectedUris.size() + " ảnh...");
        executor.execute(() -> {
            int done = 0;
            int failed = 0;
            Bitmap firstResult = null;
            for (int i = 0; i < selectedUris.size(); i++) {
                if (cancelRequested.get()) break;
                try {
                    final int index = i + 1;
                    runOnUiThread(() -> {
                        status.setText("Đang xử lý " + index + "/" + selectedUris.size() + " • " + profile.summary());
                        progress.setProgress(Math.max(1, (index - 1) * 100 / selectedUris.size()));
                    });
                    Bitmap input = BitmapIo.load(this, selectedUris.get(i));
                    Bitmap result = ImageProcessor.process(input, profile);
                    BitmapIo.save(this, result, format, quality);
                    final int finished = index;
                    runOnUiThread(() -> progress.setProgress(finished * 100 / selectedUris.size()));
                    if (firstResult == null) firstResult = result;
                    else result.recycle();
                    done++;
                } catch (Throwable t) {
                    failed++;
                }
            }
            final int ok = done;
            final int bad = failed;
            final boolean cancelled = cancelRequested.get();
            final Bitmap previewResult = firstResult;
            runOnUiThread(() -> {
                batchRunning = false;
                if (previewResult != null) {
                    recycleProcessed();
                    processedBitmap = previewResult;
                    preview.setImageBitmap(processedBitmap);
                    showingProcessed = true;
                    compareButton.setEnabled(true);
                    saveButton.setEnabled(true);
                }
                String msg = cancelled ? "Đã dừng: " + ok + " ảnh đã lưu" : "Hàng loạt hoàn tất: " + ok + " ảnh đã lưu";
                if (bad > 0) msg += " • " + bad + " ảnh lỗi";
                setBusy(false, false, msg + ".");
            });
        });
    }

    private void saveCurrent() {
        if (processedBitmap == null) {
            toast("Chưa có ảnh đã xử lý.");
            return;
        }
        final Bitmap copy = processedBitmap.copy(Bitmap.Config.ARGB_8888, false);
        final String format = String.valueOf(formatSpinner.getSelectedItem());
        final int quality = selectedQuality();
        setBusy(true, false, "Đang lưu ảnh...");
        executor.execute(() -> {
            try {
                Uri saved = BitmapIo.save(this, copy, format, quality);
                copy.recycle();
                runOnUiThread(() -> setBusy(false, false, "Đã lưu vào Pictures/KenPhotoAI • " + saved));
            } catch (Exception e) {
                copy.recycle();
                runOnUiThread(() -> setBusy(false, false, "Lỗi lưu ảnh: " + e.getMessage()));
            }
        });
    }

    private int selectedQuality() {
        try { return Integer.parseInt(String.valueOf(qualitySpinner.getSelectedItem())); }
        catch (Exception ignored) { return 95; }
    }

    private void toggleCompare() {
        if (originalBitmap == null || processedBitmap == null) return;
        showingProcessed = !showingProcessed;
        preview.setImageBitmap(showingProcessed ? processedBitmap : originalBitmap);
        status.setText(showingProcessed ? "Đang xem: SAU xử lý" : "Đang xem: ẢNH GỐC (bản xem trước)");
    }

    private void refreshCommandPreview() {
        if (commandPreview == null || commandInput == null) return;
        EnhanceProfile p = CommandParser.parse(commandInput.getText().toString());
        String text = "Sẽ xử lý: " + p.summary();
        if (originalBitmap != null && !originalBitmap.isRecycled()) {
            int[] size = ImageProcessor.computeOutputDimensions(originalBitmap.getWidth(), originalBitmap.getHeight(), p);
            text += " • xem trước đầu ra khoảng " + size[0] + "×" + size[1] + " px";
        }
        commandPreview.setText(text);
    }

    private void setBusy(boolean busy, boolean canCancel, String message) {
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        if (busy) {
            progress.setIndeterminate(!canCancel);
            if (canCancel) progress.setProgress(0);
        }
        processButton.setEnabled(!busy);
        batchButton.setEnabled(!busy);
        cancelButton.setEnabled(busy && canCancel);
        status.setText(message);
    }

    private void addPreset(LinearLayout row, String title, String command) {
        Button b = button(title);
        b.setTextSize(12);
        b.setOnClickListener(v -> commandInput.setText(command));
        row.addView(b, weighted());
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(13);
        b.setPadding(dp(8), dp(4), dp(8), dp(4));
        return b;
    }

    private TextView label(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(15);
        v.setTextColor(Color.rgb(17, 24, 39));
        v.setTypeface(null, 1);
        return v;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    private LinearLayout.LayoutParams weighted() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(dp(2), dp(2), dp(2), dp(2));
        return lp;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void recycleOriginal() {
        if (originalBitmap != null && !originalBitmap.isRecycled()) originalBitmap.recycle();
        originalBitmap = null;
    }

    private void recycleProcessed() {
        if (processedBitmap != null && !processedBitmap.isRecycled()) processedBitmap.recycle();
        processedBitmap = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelRequested.set(true);
        executor.shutdownNow();
        recycleOriginal();
        recycleProcessed();
    }
}
