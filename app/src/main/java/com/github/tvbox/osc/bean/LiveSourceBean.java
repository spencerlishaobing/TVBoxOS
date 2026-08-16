package com.github.tvbox.osc.bean;

/**
 * 直播源配置项（影视仓 r70 交互模型）
 * 存储于 Hawk: live_source_url_history / live_source_url_current
 */
public class LiveSourceBean {
    private String sourceName = "";
    private String sourceUrl = "";
    private boolean selected = false;
    private boolean showDelete = false;

    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }
    public boolean isShowDelete() { return showDelete; }
    public void setShowDelete(boolean showDelete) { this.showDelete = showDelete; }

    public String getUniKey() { return sourceName + sourceUrl; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LiveSourceBean that = (LiveSourceBean) obj;
        return getUniKey().equals(that.getUniKey());
    }

    @Override
    public int hashCode() { return getUniKey().hashCode(); }
}
