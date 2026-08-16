package com.github.tvbox.osc.bean;

/**
 * 更多源/线路配置项（影视仓交互模型）
 * 存储于 Hawk: custom_store_house / custom_store_house_selected
 */
public class MoreSourceBean {
    private String sourceName = "";
    private String sourceUrl = "";
    private boolean selected = false;
    private boolean showDelete = false;

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isShowDelete() {
        return showDelete;
    }

    public void setShowDelete(boolean showDelete) {
        this.showDelete = showDelete;
    }

    /**
     * 唯一键（去重用）：名称 + 地址
     */
    public String getUniKey() {
        return sourceName + sourceUrl;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MoreSourceBean that = (MoreSourceBean) obj;
        return getUniKey().equals(that.getUniKey());
    }

    @Override
    public int hashCode() {
        return getUniKey().hashCode();
    }
}
