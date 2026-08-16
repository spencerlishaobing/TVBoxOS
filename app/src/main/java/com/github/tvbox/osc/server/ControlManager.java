package com.github.tvbox.osc.server;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.github.tvbox.osc.bean.LiveSourceBean;
import com.github.tvbox.osc.bean.MoreSourceBean;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.receiver.PushReceiver;
import com.github.tvbox.osc.receiver.SearchReceiver;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.HistoryHelper;
import com.orhanobut.hawk.Hawk;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * @author pj567
 * @date :2021/1/4
 * @description:
 */
public class ControlManager {
    private static ControlManager instance;
    private RemoteServer mServer = null;
    public static Context mContext;

    private ControlManager() {

    }

    public static ControlManager get() {
        if (instance == null) {
            synchronized (ControlManager.class) {
                if (instance == null) {
                    instance = new ControlManager();
                }
            }
        }
        return instance;
    }

    public static void init(Context context) {
        mContext = context;
    }

    public String getAddress(boolean local) {
        if (mServer == null || !mServer.isStarting()) {
            startServer();
        }
        if (mServer == null || !mServer.isStarting()) {
            return "";
        }
        return local ? mServer.getLoadAddress() : mServer.getServerAddress();
    }

    public void startServer() {
        if (mServer != null && mServer.isStarting()) {
            return;
        }
        do {
            mServer = new RemoteServer(RemoteServer.serverPort, mContext);
            mServer.setDataReceiver(new DataReceiver() {
                @Override
                public void onTextReceived(String text) {
                    if (!TextUtils.isEmpty(text)) {
                        Intent intent = new Intent();
                        Bundle bundle = new Bundle();
                        bundle.putString("title", text);
                        intent.setAction(SearchReceiver.action);
                        intent.setPackage(mContext.getPackageName());
                        intent.setComponent(new ComponentName(mContext, SearchReceiver.class));
                        intent.putExtras(bundle);
                        mContext.sendBroadcast(intent);
                    }
                }

                @Override
                public void onApiReceived(String url) {
                    if (!TextUtils.isEmpty(url)) {
                        // 远程推送接口地址：直接应用并通知首页刷新
                        Hawk.put(HawkConfig.API_URL, url);
                        Hawk.put(HawkConfig.LIVE_API_URL, url);
                        HistoryHelper.setApiHistory(url);
                        HistoryHelper.setLiveApiHistory(url);
                    }
                    EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_API_URL_CHANGE, url));
                }

                @Override
                public void onLiveApiReceived(String url) {
                    if (!TextUtils.isEmpty(url)) {
                        Hawk.put(HawkConfig.LIVE_API_URL, url);
                        HistoryHelper.setLiveApiHistory(url);
                    }
                    EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_LIVE_API_URL_CHANGE, url));
                }

                @Override
                public void onDanmuApiReceived(String url) {
                    Hawk.put(HawkConfig.DANMU_API, TextUtils.isEmpty(url) ? "" : url);
                    EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_SET_DANMU_SETTINGS, false));
                }

                @Override
                public void onPushReceived(String url) {
                    PushReceiver.send(mContext, url);
                }

                @Override
                public void onPushStoreReceived(String name, String url) {
                    if (TextUtils.isEmpty(url)) return;
                    // 影视仓 qp0(14) 对应：仓库加入列表并选中
                    List<MoreSourceBean> list = Hawk.get(HawkConfig.CUSTOM_STORE_HOUSE, new ArrayList<MoreSourceBean>());
                    if (list == null) list = new ArrayList<>();
                    MoreSourceBean bean = new MoreSourceBean();
                    String storeName = (name == null || name.trim().isEmpty()) ? ("配置仓库" + (list.size() + 1)) : name;
                    bean.setSourceName(storeName);
                    bean.setSourceUrl(url);
                    if (!list.contains(bean)) {
                        list.add(0, bean);
                    }
                    Hawk.put(HawkConfig.CUSTOM_STORE_HOUSE, list);
                    Hawk.put(HawkConfig.CUSTOM_STORE_HOUSE_SELECTED, bean);
                    EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_PUSH_STORE, url));
                }

                @Override
                public void onLiveSourceReceived(String name, String url) {
                    if (TextUtils.isEmpty(url)) return;
                    // 影视仓 livePush 对应：直播源加入历史列表
                    List<LiveSourceBean> list = Hawk.get(HawkConfig.LIVE_SOURCE_URL_HISTORY, new ArrayList<LiveSourceBean>());
                    if (list == null) list = new ArrayList<>();
                    LiveSourceBean bean = new LiveSourceBean();
                    String srcName = (name == null || name.trim().isEmpty()) ? ("自用直播源" + (list.size() + 1)) : name;
                    bean.setSourceName(srcName);
                    bean.setSourceUrl(url);
                    bean.setShowDelete(true);
                    if (!list.contains(bean)) {
                        list.add(0, bean);
                    }
                    Hawk.put(HawkConfig.LIVE_SOURCE_URL_HISTORY, list);
                }
            });
            try {
                mServer.start();
                com.github.catvod.Proxy.set(RemoteServer.serverPort);
                IjkMediaPlayer.setDotPort(Hawk.get(HawkConfig.DOH_URL, 0) > 0, RemoteServer.serverPort);
                break;
            } catch (IOException ex) {
                RemoteServer.serverPort++;
                mServer.stop();
            }
        } while (RemoteServer.serverPort < 9999);
    }

    public void stopServer() {
        if (mServer != null && mServer.isStarting()) {
            mServer.stop();
        }
        mServer = null;
    }
}
