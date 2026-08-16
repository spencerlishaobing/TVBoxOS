package com.github.tvbox.osc.ui.adapter;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.github.tvbox.osc.R;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SelectDialogAdapter<T> extends ListAdapter<T, SelectDialogAdapter.SelectViewHolder> {

    class SelectViewHolder extends RecyclerView.ViewHolder {

        public SelectViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);
        }
    }

    public interface SelectDialogInterface<T> {
        void click(T value, int pos);

        String getDisplay(T val);
    }

    /**
     * 删除回调（影视仓风格：列表项带删除按钮，仅设置后显示）
     */
    public interface SelectDelInterface<T> {
        void del(T value, int pos);
    }

    /**
     * 按项控制删除按钮是否显示（影视仓 showDelete）
     */
    public interface SelectDelVisibleInterface<T> {
        boolean showDel(T value);
    }


    public static DiffUtil.ItemCallback<String> stringDiff = new DiffUtil.ItemCallback<String>() {

        @Override
        public boolean areItemsTheSame(@NonNull @NotNull String oldItem, @NonNull @NotNull String newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areContentsTheSame(@NonNull @NotNull String oldItem, @NonNull @NotNull String newItem) {
            return oldItem.equals(newItem);
        }
    };


    private ArrayList<T> data = new ArrayList<>();

    private int select = 0;

    private SelectDialogInterface dialogInterface;

    private SelectDelInterface delInterface = null;

    private SelectDelVisibleInterface delVisibleInterface = null;

    public SelectDialogAdapter(SelectDialogInterface dialogInterface, DiffUtil.ItemCallback diffCallback) {
        super(diffCallback);
        this.dialogInterface = dialogInterface;
    }

    /**
     * 设置删除回调（null 则列表项不显示删除按钮）
     */
    public void setDelInterface(SelectDelInterface delInterface) {
        this.delInterface = delInterface;
        this.delVisibleInterface = null;
    }

    /**
     * 设置删除回调并按项控制删除按钮显隐（visible 为 null 则全部显示）
     */
    public void setDelInterface(SelectDelInterface delInterface, SelectDelVisibleInterface delVisibleInterface) {
        this.delInterface = delInterface;
        this.delVisibleInterface = delVisibleInterface;
    }

    public void setData(List<T> newData, int defaultSelect) {
        data.clear();
        data.addAll(newData);
        select = defaultSelect;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return data.size();
    }


    @Override
    public SelectDialogAdapter.SelectViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        return new SelectDialogAdapter.SelectViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dialog_select, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull SelectDialogAdapter.SelectViewHolder holder, @SuppressLint("RecyclerView") int position) {
        T value = data.get(position);
        String name = dialogInterface.getDisplay(value);
        TextView view = holder.itemView.findViewById(R.id.tvName);
        // 白底弹窗：选中项白字加粗（蓝色焦点背景），未选中项深色字
        if (position == select) {
            view.setTextColor(0xffffffff);
            view.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        } else {
            view.setTextColor(0xcc000000);
            view.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
        }
        view.setText(name);
        // 删除按钮（影视仓风格：设置删除回调时显示）
        View delView = holder.itemView.findViewById(R.id.tvDel);
        if (delView != null) {
            boolean showDel = delInterface != null && (delVisibleInterface == null || delVisibleInterface.showDel(value));
            if (showDel) {
                delView.setVisibility(View.VISIBLE);
                delView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        delInterface.del(value, position);
                    }
                });
            } else {
                delView.setVisibility(View.GONE);
            }
        }
        // TV 焦点在 tvName 上，点击监听需直接绑定 tvName（itemView 点击不触发）
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (position == select)
                    return;
                notifyItemChanged(select);
                select = position;
                notifyItemChanged(select);
                dialogInterface.click(value, position);
            }
        });
    }
}
