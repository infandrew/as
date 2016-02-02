package com.pillows.accessory;

import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.pillows.accountsafe.R;
import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.accessory.*;

import java.io.IOException;

import static com.pillows.accountsafe.Settings.*;

/**
 * Created by agudz on 06/01/16.
 */
public class AccessoryService extends SAAgent {

    private static final Class<ServiceConnection> SASOCKET_CLASS = ServiceConnection.class;
    private final IBinder mBinder = new LocalBinder();
    private ServiceConnection mConnectionHandler = null;
    private AccessoryCallback callbacks = AccessoryCallback.NULL;

    private Handler mHandler = new Handler();
    private SAPeerAgent peerAgent;
    private String delaySendData = null;

    public AccessoryService() {
        super(TAG, SASOCKET_CLASS);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        SA mAccessory = new SA();
        try {
            mAccessory.initialize(this);
        } catch (SsdkUnsupportedException e) {
            // try to handle SsdkUnsupportedException
            if (processUnsupportedException(e) == true) {
                return;
            }
        } catch (Exception e1) {
            e1.printStackTrace();
            /*
             * Your application can not use Samsung Accessory SDK. Your application should work smoothly
             * without using this SDK, or you may want to notify user and close your application gracefully
             * (release resources, stop Service threads, close UI thread, etc.)
             */
            stopSelf();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private String s(int resourceId) {
        return getResources().getString(resourceId);
    }

    @Override
    protected void onFindPeerAgentResponse(SAPeerAgent peerAgent, int result) {
        switch (result) {
            case SAAgent.PEER_AGENT_FOUND:
                this.peerAgent = peerAgent;
                callbacks.snakeResponce(R.string.safe_lock_app_available, false);
                if (delaySendData != null)
                {
                    sendData(delaySendData);
                }
                break;
            case SAAgent.FINDPEER_DEVICE_NOT_CONNECTED:
                this.peerAgent = null;
                callbacks.snakeResponce(R.string.gear_not_connected, true);
                break;
            case SAAgent.FINDPEER_SERVICE_NOT_FOUND:
            default:
                this.peerAgent = null;
                callbacks.snakeResponce(R.string.safe_lock_app_not_available, true);
                break;
        }
    }

    @Override
    protected void onPeerAgentUpdated(SAPeerAgent peerAgent, int result) {
        this.peerAgent = peerAgent;
        final int status = result;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (status == SAAgent.PEER_AGENT_AVAILABLE) {
                    callbacks.snakeResponce(R.string.safe_lock_app_available, false);
                } else {
                    callbacks.snakeResponce(R.string.safe_lock_app_not_available, true);
                }
            }
        });
    }

    @Override
    protected void onServiceConnectionResponse(SAPeerAgent peerAgent, SASocket socket, int result) {
        switch(result) {
            case SAAgent.CONNECTION_SUCCESS:
                if (socket != null) {
                    mConnectionHandler = (ServiceConnection) socket;
                }
                break;
            case SAAgent.CONNECTION_ALREADY_EXIST:
                break;
            default:
                this.peerAgent = null;
                this.mConnectionHandler = null;
                break;
        }

        if (delaySendData != null)
        {
            sendData(delaySendData);
        }
    }

    @Override
    protected void onError(SAPeerAgent peerAgent, String errorMessage, int errorCode) {
        super.onError(peerAgent, errorMessage, errorCode);
    }

    public void findPeers() {
        findPeerAgents();
    }

    public boolean openConnection() {
        if (peerAgent != null) {
            requestServiceConnection(peerAgent);
            return true;
        }
        return false;
    }

    public void sendData(final String data) {
        if (mConnectionHandler != null && peerAgent != null) {
            try {
                mConnectionHandler.send(ACTION_CHANNEL, data.getBytes());
                delaySendData = null;
                return;
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
        delaySendData = data;
        if (peerAgent == null) {
            findPeerAgents();
        } else if (mConnectionHandler == null) {
            openConnection();
        }
    }

    public boolean closeConnection() {
        if (mConnectionHandler != null) {
            mConnectionHandler.close();
            mConnectionHandler = null;
            return true;
        }
        return false;
    }

    public void setCallbacks(AccessoryCallback callbacks) {
        this.callbacks = callbacks;
    }

    public class LocalBinder extends Binder {
        public AccessoryService getService() {
            return AccessoryService.this;
        }
    }

    public class ServiceConnection extends SASocket {
        public ServiceConnection() {
            super(ServiceConnection.class.getName());
        }

        @Override
        public void onError(int channelId, String errorMessage, int errorCode) {
        }

        @Override
        public void onReceive(int channelId, byte[] data) {
            if (mConnectionHandler == null) {
                return;
            }
            final String receivedString = new String(data);

            switch(channelId)
            {
                case ACTION_CHANNEL:
                    callbacks.gearResponse(receivedString);
                    break;
            }
            closeConnection();
        }

        @Override
        protected void onServiceConnectionLost(int reason) {
            closeConnection();
        }
    }

    private boolean processUnsupportedException(SsdkUnsupportedException e) {
        e.printStackTrace();
        switch (e.getType()) {
            case SsdkUnsupportedException.VENDOR_NOT_SUPPORTED:
                Log.e(TAG, "VENDOR_NOT_SUPPORTED");
                stopSelf();
                return true;
            case SsdkUnsupportedException.DEVICE_NOT_SUPPORTED:
                Log.e(TAG, "DEVICE_NOT_SUPPORTED");
                stopSelf();
                return true;
            case SsdkUnsupportedException.LIBRARY_NOT_INSTALLED:
                Log.e(TAG, "You need to install Samsung Accessory SDK to use this application.");
                return true;
            case SsdkUnsupportedException.LIBRARY_UPDATE_IS_REQUIRED:
                Log.e(TAG, "You need to update Samsung Accessory SDK to use this application.");
                return true;
            case SsdkUnsupportedException.LIBRARY_UPDATE_IS_RECOMMENDED:
                Log.e(TAG, "We recommend that you update your Samsung Accessory SDK before using this application.");
                return false;
        }
        return true;
    }
}
