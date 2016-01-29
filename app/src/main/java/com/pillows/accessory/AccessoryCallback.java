package com.pillows.accessory;

/**
 * Created by agudz on 18/01/16.
 */
public interface AccessoryCallback {
    static final AccessoryCallback NULL = new AccessoryCallback() {
        @Override public void snakeResponce(int stringId, boolean stopGearWaiting) {}
        @Override public void gearResponse(String data) {}
    };

    void snakeResponce(int stringId, boolean stopGearWaiting);
    void gearResponse(String data);
}
