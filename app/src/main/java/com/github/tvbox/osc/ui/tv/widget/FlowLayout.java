package com.github.tvbox.osc.ui.tv.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * 瀑布流（流式）布局：
 * 子 item 宽度自适应内容，一行放不下自动换行；
 * 内容超出容器高度时，焦点移动会自动滚动到聚焦项。
 *
 * @author pj567
 * @date :2020/12/23
 * @description:
 */
public class FlowLayout extends ViewGroup {

    private final List<List<View>> lines = new ArrayList<>();
    private final List<Integer> lineHeights = new ArrayList<>();
    private int contentHeight = 0;

    public FlowLayout(Context context) {
        this(context, null);
    }

    public FlowLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FlowLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();
        int maxChildWidth = Math.max(0, widthSize - paddingLeft - paddingRight);

        lines.clear();
        lineHeights.clear();

        int lineWidth = 0;
        int lineHeight = 0;
        List<View> currentLine = new ArrayList<>();
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == View.GONE) {
                continue;
            }
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            measureChildInLine(child, Math.max(0, maxChildWidth - lineWidth), heightMeasureSpec, paddingTop + paddingBottom);
            int childWidth = child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
            int childHeight = child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
            if (lineWidth + childWidth > maxChildWidth && lineWidth > 0) {
                lines.add(currentLine);
                lineHeights.add(lineHeight);
                currentLine = new ArrayList<>();
                lineWidth = 0;
                lineHeight = 0;
                // 换行后剩余宽度变大，重新测量
                measureChildInLine(child, maxChildWidth, heightMeasureSpec, paddingTop + paddingBottom);
                childWidth = child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
                childHeight = child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;
            }
            currentLine.add(child);
            lineWidth += childWidth;
            lineHeight = Math.max(lineHeight, childHeight);
        }
        if (!currentLine.isEmpty()) {
            lines.add(currentLine);
            lineHeights.add(lineHeight);
        }

        int totalHeight = paddingTop + paddingBottom;
        for (int h : lineHeights) {
            totalHeight += h;
        }
        setMeasuredDimension(
                resolveSizeAndState(widthSize, widthMeasureSpec, 0),
                resolveSizeAndState(totalHeight, heightMeasureSpec, 0));
    }

    private void measureChildInLine(View child, int maxChildWidth, int heightMeasureSpec, int verticalPadding) {
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if (lp.width == LayoutParams.MATCH_PARENT) {
            child.measure(MeasureSpec.makeMeasureSpec(maxChildWidth, MeasureSpec.EXACTLY),
                    getChildMeasureSpec(heightMeasureSpec, verticalPadding, lp.height));
        } else {
            child.measure(MeasureSpec.makeMeasureSpec(maxChildWidth, MeasureSpec.AT_MOST),
                    getChildMeasureSpec(heightMeasureSpec, verticalPadding, lp.height));
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int x = paddingLeft;
        int y = paddingTop;
        for (int i = 0; i < lines.size(); i++) {
            List<View> line = lines.get(i);
            int lineHeight = lineHeights.get(i);
            for (View child : line) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                int left = x + lp.leftMargin;
                int top = y + lp.topMargin;
                child.layout(left, top, left + child.getMeasuredWidth(), top + child.getMeasuredHeight());
                x += child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
            }
            x = paddingLeft;
            y += lineHeight;
        }
        contentHeight = y + getPaddingBottom();
    }

    @Override
    public void onViewAdded(View child) {
        super.onViewAdded(child);
        final OnFocusChangeListener original = child.getOnFocusChangeListener();
        child.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (original != null) {
                    original.onFocusChange(v, hasFocus);
                }
                if (hasFocus) {
                    scrollToChild(v);
                }
            }
        });
    }

    private void scrollToChild(View child) {
        int scrollY = getScrollY();
        int visibleHeight = getHeight();
        int newScrollY = scrollY;
        if (child.getTop() < scrollY) {
            newScrollY = Math.max(0, child.getTop());
        } else if (child.getBottom() > scrollY + visibleHeight) {
            newScrollY = child.getBottom() - visibleHeight;
        }
        newScrollY = Math.max(0, Math.min(newScrollY, Math.max(0, contentHeight - visibleHeight)));
        if (newScrollY != scrollY) {
            scrollTo(getScrollX(), newScrollY);
        }
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        if (p instanceof MarginLayoutParams) {
            return new LayoutParams((MarginLayoutParams) p);
        }
        return new LayoutParams(p.width, p.height);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    public static class LayoutParams extends MarginLayoutParams {
        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(LayoutParams source) {
            super(source);
        }
    }
}
