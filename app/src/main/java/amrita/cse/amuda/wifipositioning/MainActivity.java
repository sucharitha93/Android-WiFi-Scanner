package amrita.cse.amuda.wifipositioning;
import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    //sensor variables
    private Sensor acc_sensor;
    private Sensor mag_sensor;
    private Sensor gyro_sensor;
    private SensorManager sensorManager;
    //sensor data strings
    private String s_ax,s_ay,s_az;
    private String s_mx,s_my,s_mz;
    private String s_gx,s_gy,s_gz;

    private WifiManager wifiManager;
    WiFiScanReceiver wifiReceiver;

    private TextView textView1, textView2;
    private EditText xCoordinate, yCoordinate;
    private Button btnScan;

    File fileWifi,fileIMU;

    int[] rssiList = new int[20];
    int[] cnt = new int[20];
    String[] macList = {"EC:1A:59:89:B0:F7","ec:1a:59:89:b0:f9","EC:1A:59:4A:DE:51","EC:1A:59:4A:DE:53","18:D6:C7:79:13:49","18:d6:c7:79:13:48",
            "44:31:92:AF:A4:B0","44:31:92:AF:A4:A0","44:31:92:B0:10:90","44:31:92:B0:10:80","44:31:92:9A:44:D0","44:31:92:9A:44:C0",
            "18:d6:c7:79:14:8d","18:d6:c7:79:14:8c","44:31:92:B0:16:D0","44:31:92:B0:16:C0","EC:1A:59:8A:06:80","EC:1A:59:8A:06:82",
            "EC:1A:59:4A:DB:E4","EC:1A:59:4A:DB:E6"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //assigning layout elements to variables
        btnScan = (Button)findViewById(R.id.buttonScan);
        textView1 = (TextView) findViewById(R.id.data);
        textView2 = (TextView) findViewById(R.id.sensorData);
        xCoordinate = (EditText) findViewById(R.id.xcoordinate);
        yCoordinate = (EditText) findViewById(R.id.ycoordinate);

        //location service enabling
        LocationManager lm = (LocationManager)getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if(!gpsEnabled)
        {
            Toast.makeText(this,"Please TURN ON location service to get data!",Toast.LENGTH_SHORT).show();
        }

        //wifi service enabling
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiReceiver = new WiFiScanReceiver();

        //file path
        fileWifi = new File(Environment.getExternalStorageDirectory()+"//WiFiLog.csv");
        fileIMU = new File(Environment.getExternalStorageDirectory()+"//IMULog.csv");

        //check location permission
        int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if(permissionCheck != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
        //check write to storage permission
        permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(permissionCheck != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        //file create if does not exist
        try{
            if (!fileWifi.exists()) {
                fileWifi.createNewFile();
            }
            if (!fileIMU.exists()) {
                fileIMU.createNewFile();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        //creating a sensor manager
        sensorManager=(SensorManager) getSystemService(SENSOR_SERVICE);

        //accelerometer sensor
        acc_sensor=sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mag_sensor=sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gyro_sensor=sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        //registering the sensor
        sensorManager.registerListener( this,acc_sensor,sensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener( this,mag_sensor,sensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener( this,gyro_sensor,sensorManager.SENSOR_DELAY_NORMAL);
    }

    //on click function for the scan button
    public void btnClick(View v)
    {
        btnScan.setBackgroundColor(Color.LTGRAY);
        String x,y;
        x= xCoordinate.getText().toString();
        y = yCoordinate.getText().toString();

        //check if the co-ordinate points are entered
        if (x.isEmpty() && y.isEmpty())
        {
            Toast.makeText(this,"Enter the co-ordinates to begin SCAN",Toast.LENGTH_SHORT).show();
        }
        else
        {
            scan(v);
        }
    }


    public void scan(View v){

        textView1.setText("");
        IntentFilter filterScanResult = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        IntentFilter filterRSSIChange = new IntentFilter(WifiManager.RSSI_CHANGED_ACTION);
        IntentFilter filterChange = new IntentFilter(WifiManager.ACTION_REQUEST_SCAN_ALWAYS_AVAILABLE);


        if(!wifiManager.isWifiEnabled())
        {
            Toast.makeText(this,"Wifi Turned On",Toast.LENGTH_SHORT).show();
            wifiManager.setWifiEnabled(true);
        }
        Toast.makeText(this,"Wifi Scan started",Toast.LENGTH_SHORT).show();
        this.registerReceiver(wifiReceiver, filterScanResult);
        this.registerReceiver(wifiReceiver, filterRSSIChange);
        this.registerReceiver(wifiReceiver, filterChange);

        wifiManager.startScan();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                unregisterReceiver(wifiReceiver);
                btnScan.setBackgroundColor(Color.GREEN);
                try {
                    FileWriter fwWifi = new FileWriter(fileWifi,true);
                    BufferedWriter bwWifi = new BufferedWriter(fwWifi);
                    FileWriter fwIMU = new FileWriter(fileIMU,true);
                    BufferedWriter bwIMU = new BufferedWriter(fwIMU);

                    bwIMU.append(xCoordinate.getText().toString()+","+yCoordinate.getText().toString()+", ,"+s_ax+","+s_ay+","+s_az+","+", ,"+s_mx+","+s_my+","+s_mz+","+", ,"+s_gx+","+s_gy+","+s_gz+",");
                    bwIMU.append("\n");
                    bwIMU.close();


                    bwWifi.append(xCoordinate.getText().toString()+","+yCoordinate.getText().toString()+",");
                    for(int i = 0;i<20;i++){
                        if(cnt[i] != 0)
                        {
                            bwWifi.append(String.valueOf(rssiList[i] / cnt[i])+",");
                        }
                        else
                        {
                            bwWifi.append(String.valueOf(0)+",");
                        }
                    }
                    bwWifi.append("\n");
                    bwWifi.close();

                    Arrays.fill(rssiList,new Integer(0));
                    Arrays.fill(cnt,new Integer(0));

                    Toast.makeText(MainActivity.this,"20 seconds scan results done",Toast.LENGTH_SHORT).show();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }, 20000);
    }

    class WiFiScanReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action) || WifiManager.RSSI_CHANGED_ACTION.equals(action)|| WifiManager.ACTION_REQUEST_SCAN_ALWAYS_AVAILABLE.equals(action))
            {
                    List<ScanResult> wifiScanResultList = wifiManager.getScanResults();
                    System.out.print(wifiScanResultList.toString());

                    for(int i = 0; i < wifiScanResultList.size(); i++){
                        ScanResult accessPoint = wifiScanResultList.get(i);
                        String listItem = "SSID: "+accessPoint.SSID + "\n" + "MAC Address: "+accessPoint.BSSID + "\n" + "RSSI Signal Level"+accessPoint.level;
                        if(accessPoint.frequency < 5000)
                        {
                            if(macList[0].equalsIgnoreCase(accessPoint.BSSID))
                            {
                                rssiList[0] = rssiList[0]+ accessPoint.level;
                                cnt[0] = cnt[0] + 1;
                            }
                            else if(macList[2].equalsIgnoreCase(accessPoint.BSSID))
                            {
                                rssiList[2] = rssiList[2]+ accessPoint.level;
                                cnt[2] = cnt[2] + 1;
                            }
                            else if(macList[4].equalsIgnoreCase(accessPoint.BSSID))
                            {
                                rssiList[4] = rssiList[4]+ accessPoint.level;
                                cnt[4] = cnt[4] + 1;
                            }
                            else if(macList[6].equalsIgnoreCase(accessPoint.BSSID))
                            {
                                rssiList[6] = rssiList[6]+ accessPoint.level;
                                cnt[6] = cnt[6] + 1;
                            }
                            else if(macList[8].equalsIgnoreCase(accessPoint.BSSID))
                            {
                                rssiList[8] = rssiList[8]+ accessPoint.level;
                                cnt[8] = cnt[8] + 1;
                            }
                            else if(macList[10].equalsIgnoreCase(accessPoint.BSSID))
                            {
                                rssiList[10] = rssiList[10]+ accessPoint.level;
                                cnt[10] = cnt[10] + 1;
                            }
                            else if(macList[12].equalsIgnoreCase(accessPoint.BSSID))
                            {
                                rssiList[12] = rssiList[12]+ accessPoint.level;
                                cnt[12] = cnt[12] + 1;
                            }
                            else if(macList[14].equalsIgnoreCase(accessPoint.BSSID))
                            {
                                rssiList[14] = rssiList[14]+ accessPoint.level;
                                cnt[14] = cnt[14] + 1;
                            }
                            else if(macList[16].equalsIgnoreCase(accessPoint.BSSID))
                            {
                                rssiList[16] = rssiList[16]+ accessPoint.level;
                                cnt[16] = cnt[16] + 1;
                            }
                            else if(macList[18].equalsIgnoreCase(accessPoint.BSSID))
                            {
                                rssiList[18] = rssiList[18]+ accessPoint.level;
                                cnt[18] = cnt[18] + 1;
                            }
                            else
                            {

                            }
                        }
                        else if (accessPoint.frequency > 5000)
                        {
                            if(macList[1].equalsIgnoreCase(accessPoint.BSSID))
                            {
                                rssiList[1] = rssiList[1]+ accessPoint.level;
                                cnt[1] = cnt[1] + 1;
                            }
                            else if(macList[3].equalsIgnoreCase(accessPoint.BSSID))
                            {
                                rssiList[3] = rssiList[3]+ accessPoint.level;
                                cnt[3] = cnt[3] + 1;
                            }
                            else if(macList[5].equalsIgnoreCase(accessPoint.BSSID))
                            {
                                rssiList[5] = rssiList[5]+ accessPoint.level;
                                cnt[5] = cnt[5] + 1;
                            }
                            else if(macList[7].equalsIgnoreCase(accessPoint.BSSID))
                            {
                                rssiList[7] = rssiList[7]+ accessPoint.level;
                                cnt[7] = cnt[7] + 1;
                            }
                            else if(macList[9].equalsIgnoreCase(accessPoint.BSSID))
                            {
                                rssiList[9] = rssiList[9]+ accessPoint.level;
                                cnt[9] = cnt[9] + 1;
                            }
                            else if(macList[11].equalsIgnoreCase(accessPoint.BSSID))
                            {
                                rssiList[11] = rssiList[11]+ accessPoint.level;
                                cnt[11] = cnt[11] + 1;
                            }
                            else if(macList[13].equalsIgnoreCase(accessPoint.BSSID))
                            {
                                rssiList[13] = rssiList[13]+ accessPoint.level;
                                cnt[13] = cnt[13] + 1;
                            }
                            else if(macList[15].equalsIgnoreCase(accessPoint.BSSID))
                            {
                                rssiList[15] = rssiList[15]+ accessPoint.level;
                                cnt[15] = cnt[15] + 1;
                            }
                            else if(macList[17].equalsIgnoreCase(accessPoint.BSSID))
                            {
                                rssiList[17] = rssiList[17]+ accessPoint.level;
                                cnt[17] = cnt[17] + 1;
                            }
                            else if(macList[19].equalsIgnoreCase(accessPoint.BSSID))
                            {
                                rssiList[19] = rssiList[19]+ accessPoint.level;
                                cnt[19] = cnt[19] + 1;
                            }
                            else
                            {

                            }
                        }

                        textView1.append(listItem + "\n\n");
                    }
                    textView1.append("***********************************\n");
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        textView2.append("\n");

        //switch case to know which sensor readings are changed
        switch(event.sensor.getType()){

            case Sensor.TYPE_ACCELEROMETER:
                s_ax=String.valueOf( event.values[0]);
                s_ay=String.valueOf( event.values[1]);
                s_az=String.valueOf( event.values[2]);
                textView2.append("ACCELEROMETER: X = "+s_ax+" Y = "+s_ay+" Z = "+s_az+"\n");
                break;

            case Sensor.TYPE_GYROSCOPE:
                s_gx=String.valueOf( event.values[0]);
                s_gy=String.valueOf( event.values[1]);
                s_gz=String.valueOf( event.values[2]);
                textView2.append("GYROSCOPE: X = "+s_gx+" Y = "+s_gy+" Z = "+s_gz+"\n");
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                s_mx=String.valueOf( event.values[0]);
                s_my=String.valueOf( event.values[1]);
                s_mz=String.valueOf( event.values[2]);
                textView2.append("MAGNETOMETER: X = "+s_mx+" Y = "+s_my+" Z = "+s_mz+"\n");
                break;

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
// not really necessary now
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}
