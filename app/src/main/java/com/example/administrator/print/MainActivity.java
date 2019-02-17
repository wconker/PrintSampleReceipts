package com.example.administrator.print;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.ThumbnailUtils;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.gprinter.command.EscCommand;
import com.gprinter.command.LabelCommand;
import com.yanzhenjie.permission.AndPermission;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static android.hardware.usb.UsbManager.ACTION_USB_DEVICE_DETACHED;
import static com.example.administrator.print.Constant.ACTION_USB_PERMISSION;
import static com.example.administrator.print.Constant.CONN_STATE_DISCONN;
import static com.example.administrator.print.Constant.MESSAGE_UPDATE_PARAMETER;
import static com.example.administrator.print.DeviceConnFactoryManager.ACTION_QUERY_PRINTER_STATE;
import static com.example.administrator.print.DeviceConnFactoryManager.CONN_STATE_FAILED;
import static com.gprinter.command.EscCommand.UNDERLINE_MODE.UNDERLINE_1DOT;

public class MainActivity extends AppCompatActivity {


    private TextView tvConnState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvConnState = (TextView) findViewById(R.id.tv_connState);
        AndPermission.with(MainActivity.this)
                .permission(new String[]{Manifest.permission.BLUETOOTH,Manifest.permission.BLUETOOTH_PRIVILEGED,Manifest.permission.BLUETOOTH_ADMIN,Manifest.permission.ACCESS_COARSE_LOCATION})
                .requestCode(14000)
                .send();

        printHelper = new PrintHelper(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    printHelper.onResultDeal(requestCode,resultCode,data);
    }


    PrintHelper printHelper ;



    @Override
    protected void onStart() {
        super.onStart();


        printHelper.RegistrationOfRadio();

    }

    public void Blt(View view) {
        startActivityForResult(new Intent(this, BluetoothDeviceList.class), Constant.BLUETOOTH_REQUEST_CODE);

    }


    public void print(View view) {
        printHelper.sendReceiptWithResponse();
    }

    public void close(View view) {
        printHelper.close();
    }
}
