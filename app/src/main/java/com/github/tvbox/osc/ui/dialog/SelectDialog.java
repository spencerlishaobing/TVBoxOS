package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.ui.adapter.SelectDialogAdapter;
import com.owen.tvrecyclerview.widget.TvRecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SelectDialog<T> extends BaseDialog {
    public SelectDialog(@NonNull @NotNull Context context) {
        super(context);
        setContentView(R.layout.dialog_select);
    }

    public SelectDialog(@NonNull @NotNull Context context, int resId) {
        super(context);
        setContentView(resId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void setTip(String tip) {
        ((TextView) findViewById(R.id.title)).setText(tip);
    }
    /**
     * 设置删除回调（影视仓风格：列表项显示删除按钮，null 则隐藏）
     */
    public void setDelInterface(SelectDialogAdapter.SelectDelInterface delInterface) {
        TvRecyclerView tvRecyclerView = findViewById(R.id.list);
        if (tvRecyclerView.getAdapter() instanceof SelectDialogAdapter) {
            ((SelectDialogAdapter) tvRecyclerView.getAdapter()).setDelInterface(delInterface);
        }
    }

    /**
     * 设置删除回调并按项控制删除按钮显隐（影视仓 showDelete）
     */
    public void setDelInterface(SelectDialogAdapter.SelectDelInterface delInterface, SelectDialogAdapter.SelectDelVisibleInterface delVisibleInterface) {
        TvRecyclerView tvRecyclerView = findViewById(R.id.list);
        if (tvRecyclerView.getAdapter() instanceof SelectDialogAdapter) {
            ((SelectDialogAdapter) tvRecyclerView.getAdapter()).setDelInterface(delInterface, delVisibleInterface);
        }
    }

    public void setAdapter(SelectDialogAdapter.SelectDialogInterface<T> sourceBeanSelectDialogInterface,
                           DiffUtil.ItemCallback<T> sourceBeanItemCallback,
                           List<T> data, int select) {
        final int selectIdx = select;
        SelectDialogAdapter<T> adapter = new SelectDialogAdapter<>(sourceBeanSelectDialogInterface, sourceBeanItemCallback);
        adapter.setData(data, select);
        TvRecyclerView tvRecyclerView = findViewById(R.id.list);
        tvRecyclerView.setAdapter(adapter);
        tvRecyclerView.setSelectedPosition(select);
        if (select<10){
            tvRecyclerView.setSelection(select);
        }
        tvRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                if (selectIdx >= 10) {
                    tvRecyclerView.smoothScrollToPosition(selectIdx);
                    tvRecyclerView.setSelectionWithSmooth(selectIdx);
                }
            }
        });
    }

}
