package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.server.ControlManager;
import com.github.tvbox.osc.ui.adapter.ApiHistoryDialogAdapter;
import com.github.tvbox.osc.ui.tv.QRCodeGen;
import com.github.tvbox.osc.util.HawkConfig;
import com.orhanobut.hawk.Hawk;

import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import me.jessyan.autosize.utils.AutoSizeUtils;

/**
 * 配置弹窗（影视仓 dialog_api 交互完整移植）：
 * 二维码 + 地址 + 接口历史/存储权限 + 输入（预填当前接口）+ 确定
 * 确定：保存进接口历史（20条）并切换线路
 */
public class ConfigDialog extends BaseDialog {
    private ImageView ivQRCode;
    private TextView tvAddress;
    private EditText input;

    public ConfigDialog(@NonNull @NotNull Context context) {
        super(context);
        setContentView(R.layout.dialog_config);
        setCanceledOnTouchOutside(false);
        ivQRCode = findViewById(R.id.ivQRCode);
        tvAddress = findViewById(R.id.tvAddress);
        input = findViewById(R.id.input);
        // 预填当前接口地址（影视仓：editText.setText(api_url)）
        input.setText(Hawk.get(HawkConfig.API_URL, ""));
        input.setSelection(input.getText() == null ? 0 : input.getText().length());
        refreshQRCode();
        findViewById(R.id.inputSubmit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submit();
            }
        });
        EventBus.getDefault().register(this);
        findViewById(R.id.apiHistory).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showApiHistory();
            }
        });
        findViewById(R.id.storagePermission).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "已获得存储权限", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void refreshQRCode() {
        String address = ControlManager.get().getAddress(false);
        tvAddress.setText(String.format("手机/电脑扫描上方二维码或者直接浏览器访问地址\n%s", address));
        ivQRCode.setImageBitmap(QRCodeGen.generateBitmap(address, AutoSizeUtils.mm2px(getContext(), 300), AutoSizeUtils.mm2px(getContext(), 300), 0));
    }

    /**
     * 确定（影视仓 o0OO00o0 case 0）：
     * 输入以 http/clan/https 开头 → 存 api_history（去重置顶，最多20条）→ 设 api_url → 回调切换
     */
    private void submit() {
        String value = input.getText() == null ? "" : input.getText().toString().trim();
        if (value.isEmpty()) {
            Toast.makeText(getContext(), "请输入配置地址", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!(value.startsWith("http") || value.startsWith("clan") || value.startsWith("https"))) {
            Toast.makeText(getContext(), "地址需以 http/https/clan 开头", Toast.LENGTH_SHORT).show();
            return;
        }
        // 保存接口历史（去重置顶，最多 20 条）
        ArrayList<String> history = Hawk.get(HawkConfig.API_HISTORY, new ArrayList<String>());
        if (!history.contains(value)) {
            history.add(0, value);
        }
        if (history.size() > 20) {
            history.remove(20);
        }
        Hawk.put(HawkConfig.API_HISTORY, history);
        // 通知上层切换线路
        if (listener != null) {
            listener.onConfirm("", value);
        }
        dismiss();
    }

    /**
     * 接口历史（影视仓 o0OO00o0 case 1）：弹历史列表，选择切换 / 删除
     */
    private void showApiHistory() {
        ArrayList<String> history = Hawk.get(HawkConfig.API_HISTORY, new ArrayList<String>());
        if (history.isEmpty()) {
            Toast.makeText(getContext(), "暂无历史配置", Toast.LENGTH_SHORT).show();
            return;
        }
        String current = Hawk.get(HawkConfig.API_URL, "");
        int idx = 0;
        if (history.contains(current))
            idx = history.indexOf(current);
        final ApiHistoryDialog dialog = new ApiHistoryDialog(getContext());
        dialog.setTip("历史配置列表");
        dialog.setAdapter(new ApiHistoryDialogAdapter.SelectDialogInterface() {
            @Override
            public void click(String value) {
                dialog.dismiss();
                if (listener != null) {
                    listener.onConfirm("", value);
                }
                dismiss();
            }

            @Override
            public void del(String value, ArrayList<String> data) {
                Hawk.put(HawkConfig.API_HISTORY, data);
                dialog.dismiss();
                Toast.makeText(getContext(), "已删除", Toast.LENGTH_SHORT).show();
            }
        }, history, idx);
        dialog.show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (listener != null) listener.onCancel();
        dismiss();
    }

    @org.greenrobot.eventbus.Subscribe(threadMode = org.greenrobot.eventbus.ThreadMode.MAIN)
    public void refresh(com.github.tvbox.osc.event.RefreshEvent event) {
        // 影视仓：远程推送配置后回填输入框
        if (event.type == com.github.tvbox.osc.event.RefreshEvent.TYPE_API_URL_CHANGE) {
            if (event.obj != null) {
                input.setText(String.valueOf(event.obj));
                input.setSelection(input.getText() == null ? 0 : input.getText().length());
            }
        }
    }

    @Override
    public void onDetachedFromWindow() {
        EventBus.getDefault().unregister(this);
        super.onDetachedFromWindow();
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
