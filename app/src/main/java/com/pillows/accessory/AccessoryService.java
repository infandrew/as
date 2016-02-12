package com.pillows.accessory;

import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.pillows.accountsafe.R;
import com.pillows.accountsafe.Settings;
import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.accessory.*;

import java.io.IOException;

import static com.pillows.accountsafe.Settings.*;

/**
 * Created by agudz on 06/01/16.
 */
public class AccessoryService extends SAAgent {

    private static final int MAX_ATTEMPS_TO_SEND_DATA = 7;
    private static final Class<ServiceConnection> SASOCKET_CLASS = ServiceConnection.class;
    private final IBinder mBinder = new LocalBinder();
    private ServiceConnection mConnectionHandler = null;
    private AccessoryCallback callbacks = AccessoryCallback.NULL;

    private Handler mHandler = new Handler();
    private SAPeerAgent peerAgent;
    private String delaySendData = null;
    private int attemptToSendData = 0;
    private String lastSendedData = null;
    private boolean reconnect = false;


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
        final int status = result;
        if (status == SAAgent.PEER_AGENT_AVAILABLE)
            this.peerAgent = peerAgent;
        else
            this.peerAgent = null;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (status == SAAgent.PEER_AGENT_AVAILABLE) {
                    callbacks.snakeResponce(R.string.safe_lock_app_available, false);
                } else {
                    closeConnection();
                    callbacks.snakeResponce(R.string.safe_lock_app_not_available, true);
                }
            }
        });
    }

    @Override
    protected void onServiceConnectionResponse(SAPeerAgent peerAgent, SASocket socket, int result) {
        Log.d(TAG, "Connection: " + result);
        switch(result) {
            case SAAgent.CONNECTION_SUCCESS:
            case SAAgent.CONNECTION_ALREADY_EXIST:
                if (socket != null) {
                    mConnectionHandler = (ServiceConnection) socket;
                }
                break;
            case SAAgent.CONNECTION_FAILURE_PEERAGENT_NO_RESPONSE:
                break;
            case SAAgent.CONNECTION_FAILURE_DEVICE_UNREACHABLE:
                closeConnection();
                break;
            default:
                this.peerAgent = null;
                closeConnection();
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
            Log.d(TAG, "RequestServiceConnection()");
            requestServiceConnection(peerAgent);
            return true;
        }
        return false;
    }

    public boolean closeConnection() {
        if (mConnectionHandler != null) {
            mConnectionHandler.close();
            //mConnectionHandler = null;
            Log.d(TAG, "ConnectionClosed()");
            if (reconnect == true) {
                reconnect = false;
                Log.d(TAG, "@2");
                attemptToSendData = MAX_ATTEMPS_TO_SEND_DATA - 2;
                sendData(lastSendedData);
            }
            return true;
        }
        return false;
    }

    public void sendData(final String data) {
        attemptToSendData++;
        Log.d(TAG, "Attempt: " + attemptToSendData);
        if (attemptToSendData > MAX_ATTEMPS_TO_SEND_DATA) {
            attemptToSendData = 0;
            delaySendData = null;
            callbacks.snakeResponce(R.string.gear_send_error, true);
            return;
        }

        if (peerAgent == null) {
            delaySendData = data;
            findPeerAgents();
            return;
        }

        if(mConnectionHandler == null || !mConnectionHandler.isConnected()) {
            delaySendData = data;
            openConnection();
            return;
        }

        try {
            mConnectionHandler.send(ACTION_CHANNEL, data.getBytes());
            if (Settings.ACTION_GET == data)
                callbacks.snakeResponce(R.string.gear_waiting_message, false);
            if (Settings.ACTION_NEW == data)
                callbacks.snakeResponce(R.string.gear_waiting_message_new, false);

            Log.d(TAG, "Sent.");
            attemptToSendData = 0;
            lastSendedData = data;
            delaySendData = null;
            return;
        } catch (IOException e) {
            Log.d(TAG, "Send exception");
            sendData(data);
        }
    }

    public void setCallbacks(AccessoryCallback callbacks) {
        this.callbacks = callbacks;
    }

    public void stopConnecting() {
        delaySendData = null;
        attemptToSendData = 0;
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
                    if ("@".equals(receivedString)) {
                        Log.d(TAG, "@1");
                        reconnect = true;
                    } else if ("~".equals(receivedString))
                        callbacks.snakeResponce(R.string.snake_wrong_key_empty, true);
                    else if (receivedString.length() == 64)
                        callbacks.gearResponse(receivedString);
                    else
                        callbacks.snakeResponce(R.string.snake_wrong_key, true);
                    break;
            }
            closeConnection();
        }

        @Override
        protected void onServiceConnectionLost(int reason) {
            /*closeConnection();*/
        }
    }

    private boolean processUnsupportedException(SsdkUnsupportedException e) {
        e.printStackTrace();
        switch (e.getType()) {
            case SsdkUnsupportedException.VENDOR_NOT_SUPPORTED:
                callbacks.snakeResponce(R.string.accessory_vendor_not_supported, false);
                stopSelf();
                return true;
            case SsdkUnsupportedException.DEVICE_NOT_SUPPORTED:
                callbacks.snakeResponce(R.string.accessory_device_not_found, false);
                stopSelf();
                return true;
            case SsdkUnsupportedException.LIBRARY_NOT_INSTALLED:
                callbacks.snakeResponce(R.string.accessory_install_sdk, false);
                return true;
            case SsdkUnsupportedException.LIBRARY_UPDATE_IS_REQUIRED:
                callbacks.snakeResponce(R.string.accessory_update_required_sdk, false);
                return true;
            case SsdkUnsupportedException.LIBRARY_UPDATE_IS_RECOMMENDED:
                callbacks.snakeResponce(R.string.accessory_update_recommended_sdk, false);
                return false;
        }
        return true;
    }
}
