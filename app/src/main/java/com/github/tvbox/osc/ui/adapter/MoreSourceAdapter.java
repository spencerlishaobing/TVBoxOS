package com.github.tvbox.osc.ui.adapter;

import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.MoreSourceBean;

import java.util.ArrayList;

/**
 * 更多源/线路列表适配器（影视仓交互：选中高亮 + 删除按钮 + 拖动排序）
 */
public class MoreSourceAdapter extends BaseQuickAdapter<MoreSourceBean, BaseViewHolder> {

    public MoreSourceAdapter() {
        super(R.layout.item_dialog_select, new ArrayList<>());
    }

    @Override
    protected void convert(BaseViewHolder helper, MoreSourceBean item) {
        TextView tvName = helper.getView(R.id.tvName);
        // 选中项：白字加粗（蓝色焦点背景）；未选中：深色字
        if (item.isSelected()) {
            tvName.setTextColor(0xffffffff);
            tvName.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        } else {
            tvName.setTextColor(0xcc000000);
            tvName.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
        }
        tvName.setText(item.getSourceName());
        // 删除按钮：showDelete 显示（影视仓：内置源或新添加的源可删除）
        View delView = helper.getView(R.id.tvDel);
        delView.setVisibility(item.isShowDelete() ? View.VISIBLE : View.GONE);
        // TV 焦点在 tvName/tvDel 上，点击监听需直接绑定子 View（itemView 点击不触发）
        helper.addOnClickListener(R.id.tvName);
        helper.addOnClickListener(R.id.tvDel);
    }

    /**
     * 拖动排序（ItemTouchHelper 调用）
     */
    public void onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                java.util.Collections.swap(getData(), i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                java.util.Collections.swap(getData(), i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
    }
}
