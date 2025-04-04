package flutter.moum.headset_connection_event;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import java.util.HashMap;
import java.util.Map;

/**
 * HeadsetConnectionEventPlugin
 */
public class HeadsetConnectionEventPlugin implements FlutterPlugin, MethodCallHandler {
    public static int currentState = -1;
    private AudioManager audioManager;

    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private MethodChannel channel;

    private final HeadsetEventListener headsetEventListener = new HeadsetEventListener() {
        @Override
        public void onHeadsetConnect() {
            currentState = 1;
            channel.invokeMethod("connect", "true");
        }

        @Override
        public void onHeadsetDisconnect() {
            currentState = 0;
            channel.invokeMethod("disconnect", "true");
        }

        @Override
        public void onNextButtonPress() {
            channel.invokeMethod("nextButton", "true");
        }

        @Override
        public void onPrevButtonPress() {
            channel.invokeMethod("prevButton", "true");
        }
    };

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "flutter.moum/headset_connection_event");
        channel.setMethodCallHandler(this);

        final HeadsetBroadcastReceiver hReceiver = new HeadsetBroadcastReceiver(headsetEventListener);
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
        filter.addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        flutterPluginBinding.getApplicationContext().registerReceiver(hReceiver, filter);

        audioManager = (AudioManager)flutterPluginBinding.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        currentState = getConnectedHeadset(audioManager) ? 1: 0;
    }

    private boolean getConnectedHeadset(AudioManager audioManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return audioManager.isWiredHeadsetOn() || audioManager.isBluetoothA2dpOn();
        } else {
            AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS);

            for (AudioDeviceInfo device : devices) {
                if (device.getType() == AudioDeviceInfo.TYPE_WIRED_HEADSET
                        || device.getType() == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                        || device.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                        || device.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                        || device.getType() == AudioDeviceInfo.TYPE_USB_DEVICE
                        || device.getType() == AudioDeviceInfo.TYPE_USB_HEADSET) {
                    return true;
                }
            }
            return false;
        }
    }

    private String getConnectedHeadsetName(AudioManager audioManager) {
        
        String headsetName = "";
            AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS);

            for (AudioDeviceInfo device : devices) {
                if (device.getType() == AudioDeviceInfo.TYPE_WIRED_HEADSET
                        || device.getType() == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                        || device.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                        || device.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                        || device.getType() == AudioDeviceInfo.TYPE_USB_DEVICE
                        || device.getType() == AudioDeviceInfo.TYPE_USB_HEADSET) {

                    headsetName += "\nINPUT=>Pname=" + device.getProductName() + " Type:" + device.getType() + " isSink:";
                    headsetName += device.isSink();
                     headsetName += " isSource:" ;
                      headsetName += device.isSource();
                }
            }

            AudioDeviceInfo[] odevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);

            for (AudioDeviceInfo device : odevices) {
                if (device.getType() == AudioDeviceInfo.TYPE_WIRED_HEADSET
                        || device.getType() == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                        || device.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                        || device.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                        || device.getType() == AudioDeviceInfo.TYPE_USB_DEVICE
                        || device.getType() == AudioDeviceInfo.TYPE_USB_HEADSET) {

                    headsetName += "\nOUTPUT=>Pname=" + device.getProductName() + " Type:" + device.getType() + " isSink:";
                    headsetName += device.isSink();
                     headsetName += " isSource:" ;
                      headsetName += device.isSource();
                }
            }
            return headsetName;
    }

    private Map<String, Object> getConnectedHeadsetInfo(AudioManager audioManager) {
            AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS);

            for (AudioDeviceInfo device : devices) {
                if (device.getType() == AudioDeviceInfo.TYPE_WIRED_HEADSET
                        || device.getType() == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                        || device.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                        || device.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                        || device.getType() == AudioDeviceInfo.TYPE_USB_DEVICE
                        || device.getType() == AudioDeviceInfo.TYPE_USB_HEADSET) {

                        Map<String, Object> data = new HashMap<>();
                        data.put("pname", device.getProductName());
                        data.put("ptype", device.getType());
                        data.put("pId", device.getId());
                        return data;
                }
            }
            return new HashMap<>();

    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        if (call.method.equals("getCurrentState")) {
            currentState = getConnectedHeadset(audioManager) ? 1: 0;
            result.success(currentState);
        } else if (call.method.equals("getCurrentStateInfo")) {
            result.success(getConnectedHeadsetName(audioManager));
        } else if (call.method.equals("getConnectedHeadsetInfo")) {
            result.success(getConnectedHeadsetInfo(audioManager));
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }
}
