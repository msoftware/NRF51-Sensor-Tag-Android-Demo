package com.jentsch.nrf51.sensortag;

import static com.jentsch.nrf51.sensortag.UartService.EXTRA_RSSI;
import static com.jentsch.nrf51.sensortag.UartService.EXTRA_STATUS;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.jentsch.nrf51.sensortag.view.SensorBarView;
import com.jentsch.nrf51.sensortag.view.SpiderView;
import com.jentsch.nrf51.sensortag.view.LineChartView;

public class MainActivity extends Activity implements RadioGroup.OnCheckedChangeListener {
    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    public static final String TAG = "nRFUART";
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;
    private static final int STATE_OFF = 10;
    public static final int MY_PERMISSIONS_REQUEST = 1012;

    private int mState = UART_PROFILE_DISCONNECTED;
    private UartService mService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdapter = null;
    private ListView messageListView;
    private ArrayAdapter<String> listAdapter;
    private Button btnConnectDisconnect;
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
    private Handler handler;
    // private SpiderView sensorView;
    private LineChartView lineChartView;
    private long lastUpdateA;
    private long lastUpdateG;
    private TextView refreshRateText;
    private TextView rssiTextView;

    @Override

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        handler = new Handler(Looper.getMainLooper());
        // sensorView = findViewById(R.id.spiderView);
        lineChartView = findViewById(R.id.line_chart_view);
        messageListView = findViewById(R.id.listMessage);
        refreshRateText = findViewById(R.id.refreshRate);
        rssiTextView = findViewById(R.id.rssiTextView);

        listAdapter = new ArrayAdapter<>(this, R.layout.message_detail);
        messageListView.setAdapter(listAdapter);
        messageListView.setDivider(null);
        btnConnectDisconnect = findViewById(R.id.btn_select);
        service_init();

        // Handle Disconnect & Connect button
        btnConnectDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBtAdapter.isEnabled()) {
                    Log.i(TAG, "onClick - BT not enabled yet");
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                } else {
                    if (btnConnectDisconnect.getText().equals("Connect")) {
                        //Connect button pressed, open DeviceListActivity class, with popup windows that scan for devices
                        Intent newIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                    } else {
                        //Disconnect button pressed
                        if (mDevice != null) {
                            mService.disconnect();
                        }
                    }
                }
            }
        });
    }

    private void enableAccelerometerAndGyro() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mService.enableTXNotification(UartService.TX_CHAR_UUID1);
            }
        }, 1000);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mService.enableTXNotification(UartService.TX_CHAR_UUID2);
            }
        }, 1200);
    }

    //UART service connected/disconnected
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService = ((UartService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected mService= " + mService);
            if (!mService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }
        public void onServiceDisconnected(ComponentName classname) {
            mService = null;
        }
    };

    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            final Intent mIntent = intent;
            if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "UART_CONNECT_MSG");
                        btnConnectDisconnect.setText("Disconnect");
                        ((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName() + " - ready");
                        listAdapter.add("[" + currentDateTimeString + "] Connected to: " + mDevice.getName());
                        messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
                        mState = UART_PROFILE_CONNECTED;
                        enableAccelerometerAndGyro();
                    }
                });
            } else if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "UART_DISCONNECT_MSG");
                        btnConnectDisconnect.setText("Connect");
                        ((TextView) findViewById(R.id.deviceName)).setText("Not Connected");
                        listAdapter.add("[" + currentDateTimeString + "] Disconnected to: " + mDevice.getName());
                        mState = UART_PROFILE_DISCONNECTED;
                        mService.close();
                    }
                });
            } else if (action.equals(UartService.ACTION_DATA_AVAILABLE)) {

                final byte[] txValue = intent.getByteArrayExtra(UartService.EXTRA_DATA);
                final String uuid = intent.getStringExtra(UartService.EXTRA_UUID);
                final long time = intent.getLongExtra(UartService.EXTRA_TIME, 0);
                final String[] values = uuid.split("-");
                updateScreen(txValue, time, values[0]);
            } else if (action.equals(UartService.RSSI_DATA_AVAILABLE)) {

                int rssi = intent.getIntExtra(EXTRA_RSSI, 0);
                int status = intent.getIntExtra(EXTRA_STATUS, -1);
                final long time = intent.getLongExtra(UartService.EXTRA_TIME, 0);
                updateRSSI(rssi, status, time);
            } else if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)) {
                showMessage("Device doesn't support UART. Disconnecting");
                mService.disconnect();
            } else {
                Log.d(TAG, "Unknown Action: " + action);
            }
        }
    };

    private void updateRSSI(final int rssi, final int status, final long time) {
        runOnUiThread(new Runnable() {
            public void run() {
                try {
                    rssiTextView.setText("RSSI:" + rssi);
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
            }
        });
    }

    private void updateScreen(final byte[] txValue, final long time, final String value) {
        runOnUiThread(new Runnable() {
            public void run() {
                try {
                    if (value.equalsIgnoreCase("6e400002")) {
                        updateTime(SensorType.A);
                        long x = UartService.getLongValue(txValue[0], txValue[1]);
                        long y = UartService.getLongValue(txValue[2], txValue[3]);
                        long z = UartService.getLongValue(txValue[4], txValue[5]);
                        lineChartView.addAx(x, y, z);
                        //sensorView.getAxArray()[0] = UartService.getDoubleValue(txValue[0],txValue[1]);
                        //sensorView.getAxArray()[1] = UartService.getDoubleValue(txValue[2],txValue[3]);
                        //sensorView.getAxArray()[2] = UartService.getDoubleValue(txValue[4],txValue[5]);
                        //sensorView.invalidate();
                    }
                    if (value.equalsIgnoreCase("6e400003")) {
                        long x = UartService.getLongValue(txValue[0], txValue[1]);
                        long y = UartService.getLongValue(txValue[2], txValue[3]);
                        long z = UartService.getLongValue(txValue[4], txValue[5]);
                        lineChartView.addGx(x, y, z);
                        //sensorView.getGxArray()[0] = UartService.getDoubleValue(txValue[0],txValue[1]);
                        //sensorView.getGxArray()[1] = UartService.getDoubleValue(txValue[2],txValue[3]);
                        //sensorView.getGxArray()[2] = UartService.getDoubleValue(txValue[4],txValue[5]);
                        //sensorView.invalidate();
                    }
                    if (value.equalsIgnoreCase("6e400004")) {
                        setPressureValue(txValue, time);
                    }
                    lineChartView.invalidate();
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
            }
        });
    }

    private void updateTime(SensorType st) {
        long now = System.currentTimeMillis();
        switch (st) {
            case A:
                updateRefreshRate(refreshRateText, now - lastUpdateA);
                lastUpdateA = now;
                break;
        }
    }

    private void updateRefreshRate(TextView refreshTextView, long l) {
        refreshTextView.setText(l + " ms");
    }

    private void setPressureValue(byte[] txValue, long time) {
        /*
         * Slow update (Only once per second or less)
         */
        int type = SensorData.PRESSURE;
        Long val = UartService.getLongValue(txValue[2], txValue[3]);
        Log.d(TAG, "setAccelerationValue " + time + " " + " " + type + " " + val);
    }

    /*
    private void setGyroscopeValue(byte[] txValue, long time) {
        int type = SensorData.GYROSCOPE;
        int x = (int) UartService.getLongValue(txValue[0], txValue[1]);
        int y = (int) UartService.getLongValue(txValue[2], txValue[3]);
        int z = (int) UartService.getLongValue(txValue[4], txValue[5]);
        SensorData data = new SensorData(type, x, y, z, time);
        //saveSensorData(gyroscopeContainer, data);
    }

    private void setAccelerationValue(byte[] txValue, long time) {
        int type = SensorData.ACCELERATION;
        int x = (int) UartService.getLongValue(txValue[0], txValue[1]);
        int y = (int) UartService.getLongValue(txValue[2], txValue[3]);
        int z = (int) UartService.getLongValue(txValue[4], txValue[5]);
        SensorData data = new SensorData(type, x, y, z, time);
        //saveSensorData(accelerationContainer, data);
    }
     */

    private void service_init() {
        Intent bindIntent = new Intent(this, UartService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
        intentFilter.addAction(UartService.RSSI_DATA_AVAILABLE);
        return intentFilter;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
        unbindService(mServiceConnection);
        mService.stopSelf();
        mService = null;

    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    public void onResume() {
        super.onResume();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Toast.makeText(this, "The permission to get BLE location data is required", Toast.LENGTH_SHORT).show();
                } else {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST);
                }
            } else {
                Toast.makeText(this, "Location permissions already granted", Toast.LENGTH_SHORT).show();
                continueAfterPermissionGranted();
            }
        } else {
            continueAfterPermissionGranted();
        }

        Log.d(TAG, "onResume");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    continueAfterPermissionGranted();
                } else {
                    Toast.makeText(MainActivity.this, "No permission", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }

    public void continueAfterPermissionGranted() {
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        } else {
            if (!mBtAdapter.isEnabled()) {
                Log.i(TAG, "onResume - BT not enabled yet");
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            }
        }
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case REQUEST_SELECT_DEVICE:
                //When the DeviceListActivity return, with the selected device address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);

                    Log.d(TAG, "... onActivityResultdevice.address==" + mDevice + "mserviceValue" + mService);
                    ((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName() + " - connecting");
                    mService.connect(deviceAddress);


                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();

                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                Log.e(TAG, "wrong request code");
                break;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {

    }


    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onBackPressed() {
        if (mState == UART_PROFILE_CONNECTED) {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
            showMessage("nRFUART's running in background.\n             Disconnect to exit");
        } else {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.popup_title)
                    .setMessage(R.string.popup_message)
                    .setPositiveButton(R.string.popup_yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton(R.string.popup_no, null)
                    .show();
        }
    }
}
