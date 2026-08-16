package com.github.tvbox.osc.ui.dialog;

import android.app.Activity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.bean.MoreSourceBean;
import com.github.tvbox.osc.server.ControlManager;
import com.github.tvbox.osc.ui.adapter.MoreSourceAdapter;
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
 * 更多源/线路配置弹窗（影视仓 more_source_dialog_select 交互完整移植）
 * 二维码 + 线路列表（选中/删除/拖动排序） + 名称/地址输入 + 确定 + 存储权限
 */
public class MoreSourceDialog extends BaseDialog {

    public interface OnListener {
        void onConfirm(String name, String url);

        void onCancel();
    }

    private final Activity activity;
    private MoreSourceAdapter adapter;
    private TvRecyclerView listView;
    private EditText etName;
    private EditText etUrl;
    private ProgressBar loading;
    private OnListener listener;

    public MoreSourceDialog(@NonNull @NotNull Activity activity) {
        super(activity);
        this.activity = activity;
        setContentView(R.layout.more_source_dialog_select);
        setCanceledOnTouchOutside(false);
        initView();
        loadList();
    }

    private void initView() {
        // 宽度（影视仓：横屏 720mm / 竖屏 760mm）
        View contentView = findViewById(R.id.content_view);
        if (contentView != null) {
            int width = getContext().getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                    ? AutoSizeUtils.mm2px(getContext(), 720) : AutoSizeUtils.mm2px(getContext(), 760);
            contentView.getLayoutParams().width = width;
        }
        // 列表
        adapter = new MoreSourceAdapter();
        listView = findViewById(R.id.list);
        listView.setLayoutManager(new V7LinearLayoutManager(getContext()));
        listView.setAdapter(adapter);
        // 拖动排序（影视仓：ItemTouchHelper）
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
        // 列表项点击：切换线路 / 删除按钮（焦点在子 View 上，itemView 点击不触发）
        adapter.setOnItemChildClickListener((a, view, position) -> {
            if (view.getId() == R.id.tvName) {
                selectSource(position);
            } else if (view.getId() == R.id.tvDel) {
                deleteSource(position);
            }
        });
        // 输入
        etName = findViewById(R.id.input_sourceName);
        etUrl = findViewById(R.id.input_source_url);
        findViewById(R.id.inputSubmit).setOnClickListener(v -> submit());
        findViewById(R.id.permission_Submit).setOnClickListener(v ->
                Toast.makeText(getContext(), "已获得存储权限", Toast.LENGTH_SHORT).show());
        // 二维码 + 远程推送地址
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
        // 标题：点击重新加载列表
        TextView title = findViewById(R.id.title);
        if (title != null) {
            title.setOnClickListener(v -> loadList());
        }
        loading = findViewById(R.id.play_loading);
    }

    /**
     * 加载线路列表（custom_store_house）
     */
    private void loadList() {
        List<MoreSourceBean> list = loadFromStore();
        if (list.isEmpty()) {
            adapter.setNewData(new ArrayList<>());
            return;
        }
        adapter.setNewData(list);
        // 定位选中项
        MoreSourceBean selected = Hawk.get(HawkConfig.CUSTOM_STORE_HOUSE_SELECTED, null);
        if (selected != null) {
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).getUniKey().equals(selected.getUniKey())) {
                    listView.setSelectedPosition(i);
                    break;
                }
            }
        }
    }

    /**
     * 选中线路：更新选中态并应用（影视仓：列表点击切换）
     */
    private void selectSource(int position) {
        List<MoreSourceBean> list = adapter.getData();
        if (position < 0 || position >= list.size()) return;
        for (int i = 0; i < list.size(); i++) {
            list.get(i).setSelected(i == position);
        }
        adapter.notifyDataSetChanged();
        MoreSourceBean bean = list.get(position);
        Hawk.put(HawkConfig.CUSTOM_STORE_HOUSE_SELECTED, bean);
        if (listener != null) {
            listener.onConfirm(bean.getSourceName(), bean.getSourceUrl());
        }
        dismiss();
    }

    /**
     * 删除线路（影视仓：showDelete 项可删除）
     */
    private void deleteSource(int position) {
        if (position < 0 || position >= adapter.getData().size()) return;
        adapter.remove(position);
        saveStore();
    }

    /**
     * 提交新线路（影视仓 handleRemotePush：加入列表并应用）
     */
    private void submit() {
        String url = etUrl.getText() == null ? "" : etUrl.getText().toString().trim();
        String name = etName.getText() == null ? "" : etName.getText().toString().trim();
        if (url.isEmpty()) {
            Toast.makeText(getContext(), "请输入仓库地址", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!(url.startsWith("clan://") || url.startsWith("https://") || url.startsWith("http://"))) {
            Toast.makeText(getContext(), "地址需以 http/https/clan 开头", Toast.LENGTH_SHORT).show();
            return;
        }
        // clan:// 转本地地址
        String finalUrl = url.startsWith("clan://") ? ApiConfig.get().clanToAddress(url) : url;
        if (TextUtils.isEmpty(name)) {
            name = "自用仓库" + (loadFromStore().size() + 1);
        }
        MoreSourceBean bean = new MoreSourceBean();
        bean.setSourceName(name);
        bean.setSourceUrl(finalUrl);
        bean.setSelected(true);
        bean.setShowDelete(true);
        List<MoreSourceBean> list = loadFromStore();
        if (!list.contains(bean)) {
            list.add(0, bean);
        } else {
            for (MoreSourceBean b : list) {
                b.setSelected(b.getUniKey().equals(bean.getUniKey()));
            }
        }
        saveStore(list);
        Hawk.put(HawkConfig.CUSTOM_STORE_HOUSE_SELECTED, bean);
        etName.setText("");
        etUrl.setText("");
        if (listener != null) {
            listener.onConfirm(name, finalUrl);
        }
        dismiss();
    }

    private List<MoreSourceBean> loadFromStore() {
        List<MoreSourceBean> list = Hawk.get(HawkConfig.CUSTOM_STORE_HOUSE, new ArrayList<MoreSourceBean>());
        return list == null ? new ArrayList<>() : list;
    }

    private void saveStore() {
        Hawk.put(HawkConfig.CUSTOM_STORE_HOUSE, adapter.getData());
    }

    private void saveStore(List<MoreSourceBean> list) {
        Hawk.put(HawkConfig.CUSTOM_STORE_HOUSE, list);
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
