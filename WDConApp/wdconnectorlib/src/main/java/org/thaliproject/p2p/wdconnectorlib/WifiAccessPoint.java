// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.

package org.thaliproject.p2p.wdconnectorlib;



import android.content.Context;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.os.Handler;
import android.util.Log;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by juksilve on 28.2.2015.
 */
public class WifiAccessPoint implements WifiBase.HandShakeListenCallBack {

    WifiAccessPoint that = this;

    Context context;
    private WifiP2pManager p2p;
    private WifiP2pManager.Channel channel;
    WifiBase.WifiStatusCallBack callback;
    private Handler mHandler = null;

    HandShakeListenerThread mHandShakeListenerThread = null;

    public WifiAccessPoint(Context Context, WifiP2pManager Manager, WifiP2pManager.Channel Channel, WifiBase.WifiStatusCallBack Callback) {
        this.context = Context;
        this.p2p = Manager;
        this.channel = Channel;
        this.callback = Callback;
        this.mHandler = new Handler(this.context.getMainLooper());
    }

    public void Start() {
        startLocalService("ItsMe");
    }

    public void Stop() {
        stopLocalServices();
        removeGroup();
    }

    public void removeGroup() {
        if (p2p != null && channel != null) {
            p2p.requestGroupInfo(channel, new WifiP2pManager.GroupInfoListener() {
                @Override
                public void onGroupInfoAvailable(WifiP2pGroup group) {
                    if (group != null && p2p != null && channel != null && group.isGroupOwner()) {
                        debug_print("Clling for removeGroup");
                        p2p.removeGroup(channel, new WifiP2pManager.ActionListener() {
                            @Override
                            public void onSuccess() {
                                debug_print("removeGroup onSuccess -");
                            }
                            @Override
                            public void onFailure(int reason) {debug_print("removeGroup onFailure -" + reason);}
                        });
                    }
                }
            });
        }
    }

    private void startLocalService(String instance) {

        stopLocalServices();
        reStartHandShakeListening();

        Map<String, String> record = new HashMap<String, String>();
        record.put("available", "visible");

        WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(instance, WifiBase.SERVICE_TYPE, record);

        debug_print("Add local service :" + instance);
        p2p.addLocalService(channel, service, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                debug_print("Added local service");
            }

            public void onFailure(int reason) {
                debug_print("Adding local service failed, error code " + reason);
            }
        });
    }

    private void stopLocalServices() {
        if(mHandShakeListenerThread != null){
            mHandShakeListenerThread.Stop();
            mHandShakeListenerThread = null;
        }

        p2p.clearLocalServices(channel, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                debug_print("Cleared local services");
            }

            public void onFailure(int reason) {
                debug_print("Clearing local services failed, error code " + reason);
            }
        });
    }

    @Override
    public void GotConnection(InetAddress remote, InetAddress local) {
        debug_print("GotConnection to: " + remote + ", from " + local);
        final InetAddress remoteTmp = remote;
        final InetAddress localTmp = local;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                reStartHandShakeListening();
                callback.Connected(remoteTmp,true);
            }
        });
    }

    @Override
    public void ListeningFailed(String reason) {
        debug_print("ListeningFailed: " + reason);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                reStartHandShakeListening();
            }
        });
    }

    private void  reStartHandShakeListening(){
        if(mHandShakeListenerThread != null){
            mHandShakeListenerThread.Stop();
            mHandShakeListenerThread = null;
        }

        mHandShakeListenerThread = new HandShakeListenerThread(that,WifiBase.HandShakeportToUse);
        mHandShakeListenerThread.start();
    }

    private void debug_print(String buffer) {
        //Log.d("WifiAccessPoint", buffer);
    }
}
