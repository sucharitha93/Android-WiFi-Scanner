package amrita.cse.amuda.wifipositioning;
import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private WifiManager wifiManager;
    WiFiScanReceiver wifiReceiver;
    private SensorManager mSensorManager;
    private TextView textView;
    File file;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LocationManager lm = (LocationManager)getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if(!gpsEnabled)
        {
            Toast.makeText(this,"Please TURN ON location service to get data!",Toast.LENGTH_SHORT).show();
        }

        file = new File(Environment.getExternalStorageDirectory()+"//WiFiLog.csv");
        try{
            if (!file.exists()) {
                file.createNewFile();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
/*
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null){
            Toast.makeText(this,"***MAGNETOMETER AVAILABLE***",Toast.LENGTH_LONG).show();
        }
        else {
            Toast.makeText(this,"No MAGNETOMETER AVAILABLE",Toast.LENGTH_LONG).show();
        }
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
            Toast.makeText(this,"***ACCELEROMETER AVAILABLE***",Toast.LENGTH_LONG).show();
        }
        else {
            Toast.makeText(this,"No ACCELEROMETER AVAILABLE",Toast.LENGTH_LONG).show();
        }
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null){
            Toast.makeText(this,"***GYROSCOPE AVAILABLE***",Toast.LENGTH_LONG).show();
        }
        else {
            Toast.makeText(this,"No GYROSCOPE AVAILABLE",Toast.LENGTH_LONG).show();
        }
*/

        textView = (TextView) findViewById(R.id.data);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiReceiver = new WiFiScanReceiver();

    }

    public void btnClick(View v){

        textView.setText("");
        IntentFilter filterScanResult = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        IntentFilter filterRSSIChange = new IntentFilter(WifiManager.RSSI_CHANGED_ACTION);
        int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if(permissionCheck != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
        int permission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(permission != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
        if(!wifiManager.isWifiEnabled())
        {
            Toast.makeText(this,"Wifi Turned On",Toast.LENGTH_SHORT).show();
            wifiManager.setWifiEnabled(true);
        }
        Toast.makeText(this,"Wifi Scan started",Toast.LENGTH_SHORT).show();
        this.registerReceiver(wifiReceiver, filterScanResult);
        this.registerReceiver(wifiReceiver, filterRSSIChange);

        wifiManager.startScan();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                unregisterReceiver(wifiReceiver);
                Toast.makeText(MainActivity.this,"20 seconds scan results done",Toast.LENGTH_SHORT).show();
            }
        }, 20000);
    }

    class WiFiScanReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action) || WifiManager.RSSI_CHANGED_ACTION.equals(action))
            {
                try {
                    List<ScanResult> wifiScanResultList = wifiManager.getScanResults();
                    System.out.print(wifiScanResultList.toString());
                    FileWriter fw = new FileWriter(file,true);
                    BufferedWriter bw = new BufferedWriter(fw);
                    for(int i = 0; i < wifiScanResultList.size(); i++){
                        ScanResult accessPoint = wifiScanResultList.get(i);
                        String listItem = "SSID: "+accessPoint.SSID + "\n" + "MAC Address: "+accessPoint.BSSID + "\n" + "RSSI Signal Level"+accessPoint.level+ "\n" + "TimeStamp: "+accessPoint.timestamp;
                        textView.append(listItem + "\n\n");

                            String content = accessPoint.SSID + ","+accessPoint.BSSID + ","+accessPoint.level+ ","+accessPoint.timestamp+"\n";
                            bw.append(content);
                    }
                    textView.append("***********************************\n");
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}