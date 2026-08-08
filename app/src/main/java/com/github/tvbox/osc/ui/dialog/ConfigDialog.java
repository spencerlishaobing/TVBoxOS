package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.server.ControlManager;
import com.github.tvbox.osc.ui.tv.QRCodeGen;
import com.github.tvbox.osc.util.HistoryHelper;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import me.jessyan.autosize.utils.AutoSizeUtils;

/**
 * 配置弹窗：二维码扫码推送线路/多仓 + 手动输入（名称 + 推送链接）
 */
public class ConfigDialog extends BaseDialog {
    private ImageView ivQRCode;
    private TextView tvAddress;
    private LinearLayout llNames;
    private TextView tvEmpty;
    private EditText etName;
    private EditText etUrl;

    public ConfigDialog(@NonNull @NotNull Context context) {
        super(context);
        setContentView(R.layout.dialog_config);
        setCanceledOnTouchOutside(false);
        ivQRCode = findViewById(R.id.ivQRCode);
        tvAddress = findViewById(R.id.tvAddress);
        llNames = findViewById(R.id.llNames);
        tvEmpty = findViewById(R.id.tvEmpty);
        etName = findViewById(R.id.etName);
        etUrl = findViewById(R.id.etUrl);
        refreshQRCode();
        loadNames();
        findViewById(R.id.btnSubmit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submit();
            }
        });
    }

    private void refreshQRCode() {
        String address = ControlManager.get().getAddress(false);
        tvAddress.setText(String.format("手机/电脑扫描上方二维码或者直接浏览器访问地址\n%s", address));
        ivQRCode.setImageBitmap(QRCodeGen.generateBitmap(address + "push.html", AutoSizeUtils.mm2px(getContext(), 220), AutoSizeUtils.mm2px(getContext(), 220), 4));
    }

    /**
     * 加载已保存的多仓列表（name\turl），聚焦/点击即选中并回填输入框
     */
    private void loadNames() {
        llNames.removeAllViews();
        ArrayList<String> sources = HistoryHelper.getApiSourceList();
        if (sources.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }
        tvEmpty.setVisibility(View.GONE);
        for (final String source : sources) {
            TextView tv = new TextView(getContext());
            tv.setText(HistoryHelper.getApiLineDisplayName(source));
            tv.setSingleLine(true);
            tv.setEllipsize(TextUtils.TruncateAt.END);
            tv.setGravity(android.view.Gravity.CENTER);
            tv.setTextColor(getContext().getResources().getColor(R.color.dialog_text_primary));
            tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, getContext().getResources().getDimension(R.dimen.ts_22));
            tv.setBackgroundResource(R.drawable.shape_config_item);
            tv.setFocusable(true);
            tv.setPadding(AutoSizeUtils.mm2px(getContext(), 12), AutoSizeUtils.mm2px(getContext(), 8), AutoSizeUtils.mm2px(getContext(), 12), AutoSizeUtils.mm2px(getContext(), 8));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = AutoSizeUtils.mm2px(getContext(), 8);
            tv.setLayoutParams(params);
            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectName(v, source);
                }
            });
            tv.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) selectName(v, source);
                }
            });
            llNames.addView(tv);
        }
    }

    private void selectName(View item, String source) {
        for (int i = 0; i < llNames.getChildCount(); i++) {
            View child = llNames.getChildAt(i);
            if (child != null) child.setSelected(child == item);
        }
        etName.setText(HistoryHelper.getApiLineName(source));
        etUrl.setText(HistoryHelper.getApiLineUrl(source));
        etUrl.setSelection(etUrl.getText() == null ? 0 : etUrl.getText().length());
    }

    private void submit() {
        String name = etName.getText() == null ? "" : etName.getText().toString().trim();
        String url = etUrl.getText() == null ? "" : etUrl.getText().toString().trim();
        if (url.isEmpty()) {
            Toast.makeText(getContext(), "请输入推送链接", Toast.LENGTH_SHORT).show();
            return;
        }
        // 保存进多仓列表（按 url 去重），再回调上层应用线路
        HistoryHelper.addApiSource(HistoryHelper.buildApiLine(name, url));
        if (listener != null) listener.onConfirm(name, url);
        dismiss();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (listener != null) listener.onCancel();
        dismiss();
    }

    public void setOnListener(OnListener listener) {
        this.listener = listener;
    }

    OnListener listener = null;

    public interface OnListener {
        void onConfirm(String name, String url);

        void onCancel();
    }
}
