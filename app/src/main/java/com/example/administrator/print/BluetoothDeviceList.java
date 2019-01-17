package com.example.administrator.print;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

public class BluetoothDeviceList extends Activity {
    /**
     * Debugging
     */
    private static final String DEBUG_TAG = "DeviceListActivity";
    public static LinearLayout deviceNamelinearLayout;

    /**
     * Member fields
     */
    private ListView lvPairedDevice = null, lvNewDevice = null;
    private TextView tvPairedDevice = null, tvNewDevice = null;
    private Button btDeviceScan = null;
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;
    public static final String EXTRA_DEVICE_ADDRESS = "address";
    public static final int    REQUEST_ENABLE_BT      = 2;
    public static final int    REQUEST_CONNECT_DEVICE = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.dialog_bluetooth_list);
        Log.e(DEBUG_TAG, "On Create");
        tvPairedDevice = (TextView) findViewById(R.id.tvPairedDevices);
        lvPairedDevice = (ListView) findViewById(R.id.lvPairedDevices);
        tvNewDevice = (TextView) findViewById(R.id.tvNewDevices);
        lvNewDevice = (ListView) findViewById(R.id.lvNewDevices);
        btDeviceScan = (Button) findViewById(R.id.btBluetoothScan);
        btDeviceScan.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                v.setVisibility(View.GONE);
                discoveryDevice();
            }
        });
        // 当发现设备时，注册广播
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mFindBlueToothReceiver, filter);
        // 发现完成后注册广播
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mFindBlueToothReceiver, filter);
        initBluetooth();
    }

    private void initBluetooth(){
        // 获取本地蓝牙适配器
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // 如果适配器为空，则不支持蓝牙
        if (mBluetoothAdapter == null) {
            Utils.toast(this, "Bluetooth is not supported by the device");
        } else {
            // 如果BT没有打开，请求启用它。
            // 然后在onActivityResult期间调用setupChat()
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(
                        BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent,
                        REQUEST_ENABLE_BT);
            } else {
                getDeviceList();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 确保我们不再做探索
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
        }

        // 注销广播听众
        if (mFindBlueToothReceiver != null) {
            unregisterReceiver(mFindBlueToothReceiver);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                // 蓝牙打开
                getDeviceList();
            } else {
                // 蓝牙未打开
                Toast.makeText(this, R.string.bluetooth_is_not_enabled,
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    protected void getDeviceList() {
        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        mPairedDevicesArrayAdapter = new ArrayAdapter<>(this,
                R.layout.bluetooth_device_name_item);
        mNewDevicesArrayAdapter = new ArrayAdapter<>(this,
                R.layout.bluetooth_device_name_item);
        lvPairedDevice.setAdapter(mPairedDevicesArrayAdapter);
        lvPairedDevice.setOnItemClickListener(mDeviceClickListener);
        lvNewDevice.setAdapter(mNewDevicesArrayAdapter);
        lvNewDevice.setOnItemClickListener(mDeviceClickListener);
//        // Get the local Bluetooth adapter
//        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0) {
            tvPairedDevice.setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                mPairedDevicesArrayAdapter.add(device.getName() + "\n"
                        + device.getAddress());
            }
        } else {
            String noDevices = getResources().getText(R.string.none_paired)
                    .toString();
            mPairedDevicesArrayAdapter.add(noDevices);
        }
    }

    /**
     * 在发现完成时更改标题
     */
    private final BroadcastReceiver mFindBlueToothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed
                // already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mNewDevicesArrayAdapter.add(device.getName() + "\n"
                            + device.getAddress());
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED
                    .equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                setTitle(R.string.select_bluetooth_device);
                Log.i("tag", "finish discovery" + mNewDevicesArrayAdapter.getCount());
                if (mNewDevicesArrayAdapter.getCount() == 0) {
                    String noDevices = getResources().getText(
                            R.string.none_bluetooth_device_found).toString();
                    mNewDevicesArrayAdapter.add(noDevices);
                }
            }
        }
    };


    //DiscoveryDevice
    private void discoveryDevice() {
        // Indicate scanning in the title
        setProgressBarIndeterminateVisibility(true);
        setTitle(R.string.scaning);
        // Turn on sub-title for new devices
        tvNewDevice.setVisibility(View.VISIBLE);

        lvNewDevice.setVisibility(View.VISIBLE);
        // If we're already discovering, stop it
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        // Request discover from BluetoothAdapter
        mBluetoothAdapter.startDiscovery();
    }

    /**
     * 列表视图中所有设备的单击侦听器
     */
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // 取消发现，因为它很昂贵，我们即将连接
            mBluetoothAdapter.cancelDiscovery();
            // 获取设备MAC地址，这是视图中的最后17个字符
            String info = ((TextView) v).getText().toString();
            String noDevices = getResources().getText(R.string.none_paired).toString();
            String noNewDevice = getResources().getText(R.string.none_bluetooth_device_found).toString();
            Log.i("tag", info);
            if (!info.equals(noDevices) && !info.equals(noNewDevice)) {
                String address = info.substring(info.length() - 17);
                // Create the result I包括MAC地址
                Intent intent = new Intent();
                intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
                // 完成这个活动
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        }
    };

}
