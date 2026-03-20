package kst4contest.controller.interfaces;

public interface PstRotatorEventListener {

    void onAzimuthUpdate(double azimuth);
    void onElevationUpdate(double elevation);
    void onModeUpdate(boolean isTracking); // true = Tracking, false = Manual
    void onMessageReceived(String rawMessage); // Debugging usage
//    void setRotorPosition(double azimuth);

}


