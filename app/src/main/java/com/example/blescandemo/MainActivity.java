package com.example.blescandemo;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;

public class MainActivity extends AppCompatActivity {

    BluetoothManager bluetoothManager;
    BluetoothAdapter bluetoothAdapter;
    BluetoothLeScanner bluetoothLeScanner;
    Button btnScan;
    Button btnStopScan;
    TextView peripheralTextView;
    Set<String> set;
    BluetoothDevice targetDevice;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION =1;


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        set=new HashSet<>();

        peripheralTextView = findViewById(R.id.PeripheralTextView);
        peripheralTextView.setMovementMethod(new ScrollingMovementMethod());

        btnScan = findViewById(R.id.btnScan);
        btnStopScan = findViewById(R.id.btnStopScan);

        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               startScanning();
            }
        });

        btnStopScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               stopScanning();
            }
        });

        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            Intent iEnable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(iEnable, 1);
        }

        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder((this));
            builder.setTitle("This app needs access location");
            builder.setMessage("Please grant location  access so this app can detect peripherals");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @RequiresApi(api = Build.VERSION_CODES.M)
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }
    }


             ScanCallback leScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                if(result.getDevice().getName()!=null && result.getDevice().getType()== BluetoothDevice.DEVICE_TYPE_LE && !set.contains(result.getDevice().getName())) {
                    peripheralTextView.append("Device name: " + result.getDevice().getName() + " rssi: " + result.getRssi() + " Device type: BLE " + "\n");
                    set.add(result.getDevice().getName());
                    if(result.getDevice().getName().equals("maggie"))
                        targetDevice=result.getDevice();
                }

                /*final int scrollAmount = peripheralTextView.getLayout().getLineTop(peripheralTextView.getLineCount())-peripheralTextView.getHeight();
                if(scrollAmount>0)
                    peripheralTextView.scrollTo(0,scrollAmount);*/
            }
        };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode){
            case PERMISSION_REQUEST_COARSE_LOCATION:{
                if( grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    System.out.println("coarse location permission granted");
                else{
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover devices when in background");
                    builder.setPositiveButton(android.R.string.ok,null);

                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {

                        }
                    });
                    builder.show();
                }

                return;
            }
        }
    }

    public void startScanning() {
        System.out.println("start scanning");
        peripheralTextView.setText("");
        btnScan.setVisibility(View.INVISIBLE);
        btnStopScan.setVisibility(View.VISIBLE);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                bluetoothLeScanner.startScan(leScanCallback);
            }
        });
    }

    public void stopScanning() {
        System.out.println("stopping scanning");
        peripheralTextView.append("Stopped Scanning");
        btnScan.setVisibility(View.VISIBLE);
        btnStopScan.setVisibility(View.INVISIBLE);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                bluetoothLeScanner.stopScan(leScanCallback);
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void connect(View view) {
        BluetoothGatt bluetoothGatt =targetDevice.connectGatt(this,false,bluetoothGattCallback, TRANSPORT_LE);
    }

    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("onConnectionStateChange", "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED to "+targetDevice.getName());

                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }

        }
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services = gatt.getServices();
            Log.i("onServicesDiscovered", services.size()+ services.toString());
            for (int i = 0; i < services.size(); i++) {
                Log.i("Services UUID", services.get(i).getUuid() +
                        "");
                for (int j = 0; j < services.get(i).getCharacteristics().size(); j++) {
                    //gatt.readCharacteristic(services.get(i).getCharacteristics().get(j));

                    Log.i("Characterstics UUId", services.get(i).getCharacteristics().get(j).getUuid() + "");


                }

            }

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic
                                                 characteristic, int status) {
            Log.i("onCharacteristicRead", characteristic.toString());
        }
    };

}

