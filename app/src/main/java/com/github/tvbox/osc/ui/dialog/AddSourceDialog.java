package com.github.tvbox.osc.ui.dialog;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.R;

import org.jetbrains.annotations.NotNull;

/**
 * 首页添加仓（多线路集合地址）输入对话框
 */
public class AddSourceDialog extends BaseDialog {
    private EditText input;

    public AddSourceDialog(@NonNull @NotNull Context context) {
        super(context);
        setOwnerActivity((Activity) context);
        setContentView(R.layout.dialog_add_source);
        input = findViewById(R.id.input);
        findViewById(R.id.inputSubmit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = input.getText().toString().trim();
                if (!url.isEmpty()) {
                    listener.onChange(url);
                    dismiss();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        listener.onCancel();
        dismiss();
    }

    public void setOnListener(OnListener listener) {
        this.listener = listener;
    }

    OnListener listener = null;

    public interface OnListener {
        void onChange(String url);

        void onCancel();
    }
}
