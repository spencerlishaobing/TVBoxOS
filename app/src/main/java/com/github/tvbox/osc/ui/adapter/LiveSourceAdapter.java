package com.github.tvbox.osc.ui.adapter;

import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.LiveSourceBean;

import java.util.ArrayList;

/**
 * 直播源列表适配器（影视仓 r70 交互：选中高亮 + 删除按钮 + 拖动排序）
 */
public class LiveSourceAdapter extends BaseQuickAdapter<LiveSourceBean, BaseViewHolder> {

    public LiveSourceAdapter() {
        super(R.layout.item_dialog_select, new ArrayList<>());
    }

    @Override
    protected void convert(BaseViewHolder helper, LiveSourceBean item) {
        TextView tvName = helper.getView(R.id.tvName);
        if (item.isSelected()) {
            tvName.setTextColor(0xffffffff);
            tvName.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        } else {
            tvName.setTextColor(0xcc000000);
            tvName.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
        }
        tvName.setText(item.getSourceName());
        View delView = helper.getView(R.id.tvDel);
        delView.setVisibility(item.isShowDelete() ? View.VISIBLE : View.GONE);
        // TV 焦点在 tvName/tvDel 上，点击监听需直接绑定子 View（itemView 点击不触发）
        helper.addOnClickListener(R.id.tvName);
        helper.addOnClickListener(R.id.tvDel);
    }

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
