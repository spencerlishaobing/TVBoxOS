package com.github.tvbox.osc.server;

/**
 * @author pj567
 * @date :2021/1/5
 * @description:
 */
public interface DataReceiver {

    /**
     * @param text
     */
    void onTextReceived(String text);


    void onApiReceived(String url);

    void onLiveApiReceived(String url);

    void onDanmuApiReceived(String url);

    void onPushReceived(String url);

    /**
     * 推送仓库/多仓（影视仓 do=pushStore）
     */
    void onPushStoreReceived(String name, String url);

    /**
     * 推送直播源（影视仓 do=livePush）
     */
    void onLiveSourceReceived(String name, String url);
}
