// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.

package org.thaliproject.p2p.wdconnectorlib;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.CountDownTimer;
import android.util.Log;

import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION;


/**
 * Created by juksilve on 28.2.2015.
 */
public class WifiConnection{

    WifiConnection that = this;

    Context context;
    private WifiP2pManager p2p;
    private WifiP2pManager.Channel channel;
    WifiBase.WifiStatusCallBack callback;

    private BroadcastReceiver receiver;
    private IntentFilter filter;

    boolean connecting = false;

    CountDownTimer ConnectingTimeOutTimer = new CountDownTimer(60000, 1000) {
        public void onTick(long millisUntilFinished) {
            // not using
        }
        public void onFinish() {
            //lets cancel
            callback.connectionStatusChanged(WifiBase.ConectionState.ConnectingFailed,88767688);
        }
    };

    public WifiConnection(Context Context, WifiP2pManager Manager, WifiP2pManager.Channel Channel, WifiBase.WifiStatusCallBack Callback) {
        this.context = Context;
        this.p2p = Manager;
        this.channel = Channel;
        this.callback = Callback;
    }

    public void Start(String address) {

        connecting = false;
        receiver = new AccessPointReceiver();
        filter = new IntentFilter();
        filter.addAction(WIFI_P2P_CONNECTION_CHANGED_ACTION);
        this.context.registerReceiver(receiver, filter);

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = address;
        config.wps.setup = WpsInfo.PBC;

        final String tmpString = address;
        p2p.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                connecting = true;
                ConnectingTimeOutTimer.start();
                debug_print("Connecting to: " + tmpString);
                callback.connectionStatusChanged(WifiBase.ConectionState.Connecting,0);
            }

            @Override
            public void onFailure(int errorCode) {
                debug_print("Failed connecting to service : " + errorCode);
                callback.connectionStatusChanged(WifiBase.ConectionState.ConnectingFailed,errorCode);
            }
        });
    }

    public void Stop() {
        ConnectingTimeOutTimer.cancel();
        this.context.unregisterReceiver(receiver);

        if(connecting) {
            debug_print("cancelling connect");
            p2p.cancelConnect(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    debug_print("cancelConnect successful");
                }

                @Override
                public void onFailure(int errorCode) {
                    debug_print("Failed cancelling the Connection : " + errorCode);
                }
            });
        }
    }

    private void debug_print(String buffer) {
        //Log.d("WifiConnection", buffer);
    }

    private class AccessPointReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                if (networkInfo.isConnected()) {
                    connecting = false;
                }
            }
        }
    }
}
