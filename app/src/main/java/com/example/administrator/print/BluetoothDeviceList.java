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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
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
    public static final int REQUEST_ENABLE_BT = 2;
    public static final int REQUEST_CONNECT_DEVICE = 3;

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
        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mFindBlueToothReceiver, filter);
        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mFindBlueToothReceiver, filter);
        initBluetooth();
    }

    //初始化蓝牙蓝牙模块
    private void initBluetooth() {
        // 获取本地蓝牙适配器
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // 如果适配器为空，则不支持蓝牙
        if (mBluetoothAdapter == null) {
            Utils.toast(this, "设备不支持蓝牙");
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

        // Make sure we're not doing discovery anymore
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
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
        // 数组初始化适配器。一个用于已经配对的设备
        // 一个用于新发现的设备
        mPairedDevicesArrayAdapter = new ArrayAdapter<>(this,
                R.layout.bluetooth_device_name_item);
        mNewDevicesArrayAdapter = new ArrayAdapter<>(this,
                R.layout.bluetooth_device_name_item);
        lvPairedDevice.setAdapter(mPairedDevicesArrayAdapter);
        lvPairedDevice.setOnItemClickListener(mDeviceClickListener);
        lvNewDevice.setAdapter(mNewDevicesArrayAdapter);
        lvNewDevice.setOnItemClickListener(mDeviceClickListener);
//        // 获取本地蓝牙适配器
//        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // 获取一组当前成对的设备
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        // 如果有成功配对的对的设备，将每个设备添加到ArrayAdapter
        if (pairedDevices.size() > 0) {
            tvPairedDevice.setVisibility(View.VISIBLE);
            //检测到有连接过的设备直接选择
            for (BluetoothDevice device : pairedDevices) {
                if (device.getAddress().equals(SharedPreferencesUtil.getMac())) {
                    ConnectToABluetooth(SharedPreferencesUtil.getMac());
                }
                mPairedDevicesArrayAdapter.add(device.getName() + "\n"
                        + device.getAddress());
            }
        } else {
            String noDevices = getResources().getText(R.string.none_paired)
                    .toString();
            mPairedDevicesArrayAdapter.add(noDevices);
        }
    }


    private List<BluetoothDevice> devicesList = new ArrayList<>();

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
                    devicesList.add(device);
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
            // 取消发现，因为它代价高昂 and we're about to connect
            mBluetoothAdapter.cancelDiscovery();
            // 获取设备MAC地址, 视图中的最后17个字符是什么
            String info = ((TextView) v).getText().toString();
            String noDevices = getResources().getText(R.string.none_paired).toString();
            String noNewDevice = getResources().getText(R.string.none_bluetooth_device_found).toString();
            Log.i("tag", info);

            //   PairedDevice(devicesList.get(arg2));

            if (!info.equals(noDevices) && !info.equals(noNewDevice)) {
                String address = info.substring(info.length() - 17);
                // 创建结果意图并包含MAC地址
                Intent intent = new Intent();
                intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
                // 设置结果并完成此活动
                setResult(Activity.RESULT_OK, intent);
                finish();
            }

        }
    };


    //手动配对
    private void PairedDevice(BluetoothDevice device) {


        try {
            Method pair = BluetoothDevice.class.getMethod("createBond");
            Boolean isPairs = (Boolean) pair.invoke(device);
            Log.e("Conker", isPairs + "");
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    //有配对过的直接连接
    private void ConnectToABluetooth(String address) {
        // 取消发现，因为它代价高昂 and we're about to connect
        mBluetoothAdapter.cancelDiscovery();
        // 创建结果意图并包含MAC地址
        Intent intent = new Intent();
        intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
        // 设置结果并完成此活动
        setResult(Activity.RESULT_OK, intent);
        finish();
    }


}
