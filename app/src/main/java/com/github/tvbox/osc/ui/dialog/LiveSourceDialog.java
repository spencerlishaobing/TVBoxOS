package com.github.tvbox.osc.ui.dialog;

import android.app.Activity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.bean.LiveSourceBean;
import com.github.tvbox.osc.server.ControlManager;
import com.github.tvbox.osc.ui.adapter.LiveSourceAdapter;
import com.github.tvbox.osc.ui.tv.QRCodeGen;
import com.github.tvbox.osc.util.HawkConfig;
import com.orhanobut.hawk.Hawk;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import me.jessyan.autosize.utils.AutoSizeUtils;

/**
 * 直播源配置弹窗（影视仓 live_source_dialog_select / r70 交互完整移植）
 * 二维码 + 直播源列表（选中/删除/拖动排序） + 名称/地址输入 + 确定 + 存储权限
 */
public class LiveSourceDialog extends BaseDialog {

    public interface OnListener {
        void onConfirm(String name, String url);

        void onCancel();
    }

    private LiveSourceAdapter adapter;
    private TvRecyclerView listView;
    private EditText etName;
    private EditText etUrl;
    private OnListener listener;

    public LiveSourceDialog(@NonNull @NotNull Activity activity) {
        super(activity);
        setContentView(R.layout.live_source_dialog_select);
        setCanceledOnTouchOutside(false);
        // 点击空白区域关闭弹窗
        enableOutsideTouchDismiss(R.id.content_view);
        initView();
        loadList();
    }

    private void initView() {
        View contentView = findViewById(R.id.content_view);
        if (contentView != null) {
            int width = getContext().getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                    ? AutoSizeUtils.mm2px(getContext(), 720) : AutoSizeUtils.mm2px(getContext(), 760);
            contentView.getLayoutParams().width = width;
        }
        adapter = new LiveSourceAdapter();
        listView = findViewById(R.id.list);
        listView.setLayoutManager(new V7LinearLayoutManager(getContext()));
        listView.setAdapter(adapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                adapter.onItemMove(viewHolder.getBindingAdapterPosition(), target.getBindingAdapterPosition());
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return true;
            }
        });
        touchHelper.attachToRecyclerView(listView);
        // 列表项点击：切换直播源 / 删除按钮（焦点在子 View 上，itemView 点击不触发）
        adapter.setOnItemChildClickListener((a, view, position) -> {
            if (view.getId() == R.id.tvName) {
                selectSource(position);
            } else if (view.getId() == R.id.tvDel) {
                deleteSource(position);
            }
        });
        etName = findViewById(R.id.input_sourceName);
        etUrl = findViewById(R.id.input_source_url);
        findViewById(R.id.inputSubmit).setOnClickListener(v -> submit());
        findViewById(R.id.permission_Submit).setOnClickListener(v ->
                Toast.makeText(getContext(), "已获得存储权限", Toast.LENGTH_SHORT).show());
        String address = ControlManager.get().getAddress(false);
        ImageView qrCode = findViewById(R.id.qrCode);
        if (qrCode != null) {
            qrCode.setImageBitmap(QRCodeGen.generateBitmap(address,
                    AutoSizeUtils.mm2px(getContext(), 200), AutoSizeUtils.mm2px(getContext(), 200), 0));
        }
        TextView jumpWeb = findViewById(R.id.jump_web);
        if (jumpWeb != null) {
            jumpWeb.setText("扫码远程推送\n" + address);
        }
        TextView title = findViewById(R.id.title);
        if (title != null) {
            title.setOnClickListener(v -> loadList());
        }
    }

    private void loadList() {
        List<LiveSourceBean> list = loadFromStore();
        if (list.isEmpty()) {
            adapter.setNewData(new ArrayList<>());
            return;
        }
        adapter.setNewData(list);
        LiveSourceBean selected = Hawk.get(HawkConfig.LIVE_SOURCE_URL_CURRENT, null);
        if (selected != null) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).getUniKey().equals(selected.getUniKey())) {
                    listView.setSelectedPosition(i);
                    break;
                }
            }
        }
    }

    private void selectSource(int position) {
        List<LiveSourceBean> list = adapter.getData();
        if (position < 0 || position >= list.size()) return;
        for (int i = 0; i < list.size(); i++) {
            list.get(i).setSelected(i == position);
        }
        adapter.notifyDataSetChanged();
        LiveSourceBean bean = list.get(position);
        Hawk.put(HawkConfig.LIVE_SOURCE_URL_CURRENT, bean);
        if (listener != null) {
            listener.onConfirm(bean.getSourceName(), bean.getSourceUrl());
        }
        dismiss();
    }

    private void deleteSource(int position) {
        if (position < 0 || position >= adapter.getData().size()) return;
        adapter.remove(position);
        saveStore();
    }

    private void submit() {
        String url = etUrl.getText() == null ? "" : etUrl.getText().toString().trim();
        String name = etName.getText() == null ? "" : etName.getText().toString().trim();
        if (url.isEmpty()) {
            Toast.makeText(getContext(), "请输入直播源地址", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!(url.startsWith("clan://") || url.startsWith("https://") || url.startsWith("http://"))) {
            Toast.makeText(getContext(), "地址需以 http/https/clan 开头", Toast.LENGTH_SHORT).show();
            return;
        }
        String finalUrl = url.startsWith("clan://") ? ApiConfig.get().clanToAddress(url) : url;
        if (TextUtils.isEmpty(name)) {
            name = "自用直播源" + (loadFromStore().size() + 1);
        }
        LiveSourceBean bean = new LiveSourceBean();
        bean.setSourceName(name);
        bean.setSourceUrl(finalUrl);
        bean.setSelected(true);
        bean.setShowDelete(true);
        List<LiveSourceBean> list = loadFromStore();
        if (!list.contains(bean)) {
            list.add(0, bean);
        } else {
            for (LiveSourceBean b : list) {
                b.setSelected(b.getUniKey().equals(bean.getUniKey()));
            }
        }
        saveStore(list);
        Hawk.put(HawkConfig.LIVE_SOURCE_URL_CURRENT, bean);
        etName.setText("");
        etUrl.setText("");
        if (listener != null) {
            listener.onConfirm(name, finalUrl);
        }
        dismiss();
    }

    private List<LiveSourceBean> loadFromStore() {
        List<LiveSourceBean> list = Hawk.get(HawkConfig.LIVE_SOURCE_URL_HISTORY, new ArrayList<LiveSourceBean>());
        return list == null ? new ArrayList<>() : list;
    }

    private void saveStore() {
        Hawk.put(HawkConfig.LIVE_SOURCE_URL_HISTORY, adapter.getData());
    }

    private void saveStore(List<LiveSourceBean> list) {
        Hawk.put(HawkConfig.LIVE_SOURCE_URL_HISTORY, list);
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
}
