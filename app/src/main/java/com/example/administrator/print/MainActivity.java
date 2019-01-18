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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                /*蓝牙连接*/
                case Constant.BLUETOOTH_REQUEST_CODE: {
                    /*获取蓝牙mac地址*/
                    String macAddress = data.getStringExtra(BluetoothDeviceList.EXTRA_DEVICE_ADDRESS);
                    //初始化话DeviceConnFactoryManager
                    new DeviceConnFactoryManager.Build()
                            .setId(id)
                            //设置连接方式
                            .setConnMethod(DeviceConnFactoryManager.CONN_METHOD.BLUETOOTH)
                            //设置连接的蓝牙mac地址
                            .setMacAddress(macAddress)
                            .build();
                    //打开端口
                    DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].openPort();
                    break;
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(ACTION_USB_DEVICE_DETACHED);
        filter.addAction(ACTION_QUERY_PRINTER_STATE);
        filter.addAction(DeviceConnFactoryManager.ACTION_CONN_STATE);
        registerReceiver(receiver, filter);
    }

    public void Blt(View view) {
        startActivityForResult(new Intent(this, BluetoothDeviceList.class), Constant.BLUETOOTH_REQUEST_CODE);

    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case ACTION_USB_PERMISSION:
                    synchronized (this) {
                        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            if (device != null) {
                                System.out.println("permission ok for device " + device);

                            }
                        } else {
                            System.out.println("permission denied for device " + device);
                        }
                    }
                    break;
                //Usb连接断开、蓝牙连接断开广播
                case ACTION_USB_DEVICE_DETACHED:
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    mHandler.obtainMessage(CONN_STATE_DISCONN).sendToTarget();
                    break;
                case DeviceConnFactoryManager.ACTION_CONN_STATE:
                    int state = intent.getIntExtra(DeviceConnFactoryManager.STATE, -1);
                    int deviceId = intent.getIntExtra(DeviceConnFactoryManager.DEVICE_ID, -1);
                    switch (state) {
                        case DeviceConnFactoryManager.CONN_STATE_DISCONNECT:
                            if (id == deviceId) {
                                tvConnState.setText(getString(R.string.str_conn_state_disconnect));
                            }
                            break;
                        case DeviceConnFactoryManager.CONN_STATE_CONNECTING:
                            tvConnState.setText(getString(R.string.str_conn_state_connecting));
                            break;
                        case DeviceConnFactoryManager.CONN_STATE_CONNECTED:
                            tvConnState.setText(getString(R.string.str_conn_state_connected) + "\n" + getConnDeviceInfo());
                            break;
                        case CONN_STATE_FAILED:
                            Utils.toast(MainActivity.this, getString(R.string.str_conn_fail));
                            tvConnState.setText(getString(R.string.str_conn_state_disconnect));
                            break;
                        default:
                            break;
                    }
                    break;
                case ACTION_QUERY_PRINTER_STATE:
                    if (counts > 0) {
                        sendContinuityPrint();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private String getConnDeviceInfo() {
        String str = "";
        DeviceConnFactoryManager deviceConnFactoryManager = DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id];
        if (deviceConnFactoryManager != null
                && deviceConnFactoryManager.getConnState()) {
            if ("USB".equals(deviceConnFactoryManager.getConnMethod().toString())) {
                str += "USB\n";
                str += "USB Name: " + deviceConnFactoryManager.usbDevice().getDeviceName();
            } else if ("WIFI".equals(deviceConnFactoryManager.getConnMethod().toString())) {
                str += "WIFI\n";
                str += "IP: " + deviceConnFactoryManager.getIp() + "\t";
                str += "Port: " + deviceConnFactoryManager.getPort();
            } else if ("BLUETOOTH".equals(deviceConnFactoryManager.getConnMethod().toString())) {
                str += "BLUETOOTH\n";
                str += "MacAddress: " + deviceConnFactoryManager.getMacAddress();
            } else if ("SERIAL_PORT".equals(deviceConnFactoryManager.getConnMethod().toString())) {
                str += "SERIAL_PORT\n";
                str += "Path: " + deviceConnFactoryManager.getSerialPortPath() + "\t";
                str += "Baudrate: " + deviceConnFactoryManager.getBaudrate();
            }
        }
        return str;
    }

    private void sendContinuityPrint() {
        ThreadPool.getInstantiation().addTask(new Runnable() {
            @Override
            public void run() {
                if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] != null
                        && DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState()) {
                    ThreadFactoryBuilder threadFactoryBuilder = new ThreadFactoryBuilder("MainActivity_sendContinuity_Timer");
                    ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1, threadFactoryBuilder);
                    scheduledExecutorService.schedule(threadFactoryBuilder.newThread(new Runnable() {
                        @Override
                        public void run() {

                        }
                    }), 1000, TimeUnit.MILLISECONDS);
                }
            }
        });
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CONN_STATE_DISCONN:
                    if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] != null) {
                        DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].closePort(id);
                    }
                    break;
                case PRINTER_COMMAND_ERROR:
                    Utils.toast(MainActivity.this, getString(R.string.str_choice_printer_command));
                    break;
                case CONN_PRINTER:
                    Utils.toast(MainActivity.this, getString(R.string.str_cann_printer));
                    break;
                case MESSAGE_UPDATE_PARAMETER:
                    String strIp = msg.getData().getString("Ip");
                    String strPort = msg.getData().getString("Port");
                    //初始化端口信息
                    new DeviceConnFactoryManager.Build()
                            //设置端口连接方式
                            .setConnMethod(DeviceConnFactoryManager.CONN_METHOD.WIFI)
                            //设置端口IP地址
                            .setIp(strIp)
                            //设置端口ID（主要用于连接多设备）
                            .setId(id)
                            //设置连接的热点端口号
                            .setPort(Integer.parseInt(strPort))
                            .build();
                    threadPool = ThreadPool.getInstantiation();
                    threadPool.addTask(new Runnable() {
                        @Override
                        public void run() {
                            DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].openPort();
                        }
                    });
                    break;
                default:
                    break;
            }
        }
    };

    private int id = 0;
    private ThreadPool threadPool;
    /**
     * TSC查询打印机状态指令
     */
    private byte[] tsc = {0x1b, '!', '?'};

    private static final int CONN_MOST_DEVICES = 0x11;
    private static final int CONN_PRINTER = 0x12;

    private static final int REQUEST_CODE = 0x004;

    /**
     * 连接状态断开
     */
    private static final int CONN_STATE_DISCONN = 0x007;
    /**
     * 使用打印机指令错误
     */
    private static final int PRINTER_COMMAND_ERROR = 0x008;


    /**
     * ESC查询打印机实时状态指令
     */
    private byte[] esc = {0x10, 0x04, 0x02};
    private int counts;

    public void PrintDemo(View view) {
        //由于要下载二维码图片，需要存储权限
        if (AndPermission.hasPermission(MainActivity.this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE})) {
            threadPool = ThreadPool.getInstantiation();
            threadPool.addTask(new Runnable() {
                @Override
                public void run() {
                    if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] == null ||
                            !DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState()) {
                        mHandler.obtainMessage(CONN_PRINTER).sendToTarget();
                        return;
                    }
                    if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getCurrentPrinterCommand() == PrinterCommand.ESC) {
                        postParameter();
                    } else {
                        mHandler.obtainMessage(PRINTER_COMMAND_ERROR).sendToTarget();
                    }
                }
            });
        } else {
            AndPermission.with(MainActivity.this)
                    .permission(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE})
                    .requestCode(10000)
                    .send();
        }
    }

    /**
     * 网络请求接口，获取订单数据
     */
    public void postParameter() {
        OkHttpClient client = new OkHttpClient();
        //构建FormBody，传入要提交的参数
        FormBody formBody = new FormBody
                .Builder()
                .add("method", "order_details")
                .add("orderCode", "2018111601007501")
                .build();
        final Request request = new Request.Builder()
                .url("http://testcrm.guoss.cn:8084/gss_crm/server/pda.do?")
                .post(formBody)
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("lixl", "Post Parameter 失败");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.e("lixl", "Post Parameter 成功");
                final String responseStr = response.body().string();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Gson gson = new Gson();
                        OrderDetailBean bean = gson.fromJson(responseStr, OrderDetailBean.class);
                        OrderDetailBean.DataBean dataBean = bean.getData();
                        GSSReceipts(dataBean, "杭州站朱高飞", "18315318515");
                    }
                });
            }
        });
    }

    /**
     * 网络请求接口，获取二维码图片
     * @return  BMP版
     */
    private Bitmap interPicture() {
        final Bitmap[] bm = {null};
        final File file = new File(Environment.getExternalStorageDirectory(), "erweima.bmp");
        if (file.exists()) {
            bm[0] = BitmapFactory.decodeFile(file.getAbsolutePath());
        } else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        URL url = new URL("http://app.guoss.cn/app_qr/ios_android.bmp");
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setConnectTimeout(5000);
                        conn.setRequestMethod("GET");
                        int code = conn.getResponseCode();
                        if (code == 200) {
                            InputStream is = conn.getInputStream();
                            FileOutputStream fos = new FileOutputStream(file);
                            int len = -1;
                            byte[] buffer = new byte[1024];
                            while ((len = is.read(buffer)) != -1) {
                                fos.write(buffer, 0, len);
                            }
                            is.close();
                            fos.close();
                            bm[0] = BitmapFactory.decodeFile(file.getAbsolutePath());
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this, "图片获取失败", Toast.LENGTH_SHORT).show();
                                }
                            });

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "连接网络失败", Toast.LENGTH_SHORT).show();
                            }
                        });

                    }
                }
            }).start();
        }
        return bm[0];
    }

    /**
     * 打印格式
     * @param dataBean
     * @param printName
     * @param printMoible
     */
    private void GSSReceipts(OrderDetailBean.DataBean dataBean, String printName, String printMoible) {
        EscCommand esc = new EscCommand();
        esc.addInitializePrinter();

//        //绘制简体中文
//        //图片处理
//        Bitmap b = convertToBMW(BitmapFactory.decodeResource(getResources(),
//                R.mipmap.title_pic), 200, 50);
//        //选择对齐方式：居中
//        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);
//        //打印图片
//        esc.addOriginRastBitImage(b, 200, 0);
//
//        //从左边开始对齐
//        esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);
//        esc.addText("-------------------------------");
//        //打印并换行
//        esc.addPrintAndLineFeed();
//
//        esc.addText("商户名称：" + dataBean.getShopEntityName() + "\n");
//        esc.addText("联系人：" + dataBean.getCustomName() + " " +  dataBean.getCustomMobile() +  "\n");
//        esc.addText("订单编号："+ dataBean.getOrderCode() + "\n");
//        esc.addText("下单时间："+ dataBean.getCreateTime() + "\n");
//        esc.addText("配货时间："+ dataBean.getSendTime() + "\n");
//        esc.addText("收货时间："+ dataBean.getReceiveTime() + "\n");
//        esc.addText("支付方式：" + dataBean.getPayName() + "\n");
//        esc.addText("备注：" + dataBean.getNote() + "\n");
//        esc.addText("\r\n");
//        //打印并走纸1行
//        esc.addPrintAndFeedLines((byte) 1);
//
//        //订单数据区
//        esc.addText(autoPrint(dataBean.getOrderDetailsList()));
//        //打印并走纸1行
//        esc.addPrintAndFeedLines((byte) 1);
//
//        //金额展示
//        esc.addText(forAligning("", "送优惠劵：", ("".equals(dataBean.getSalesGiftCoupon()) ? "0" : dataBean.getSalesGiftCoupon()), 0));
//        esc.addText(forAligning("", "送果币：", ("".equals(dataBean.getSalesGiftScore()) ? "0" : dataBean.getSalesGiftScore()), 1));
//        esc.addText("\r\n");
//        esc.addText(forAligning("", "订单物品金额：", ("".equals(dataBean.getGoodsMoney()) ? "0" : dataBean.getGoodsMoney()), 0));
//        esc.addText(forAligning("", "配送费用：", ("".equals(dataBean.getPostCost()) ? "0" : dataBean.getPostCost()), 0));
//        esc.addText(forAligning("", "VIP折扣：", ("".equals(dataBean.getVipMoney()) ? "0" : dataBean.getVipMoney()), 0));
//        esc.addText(forAligning("", "促销优惠金额：", ("".equals(dataBean.getOffMoney()) ? "0" : dataBean.getOffMoney()), 0));
//        esc.addText(forAligning("", "直接减免金额：", ("".equals(dataBean.getGoodsDiscountMoney()) ? "0" : dataBean.getGoodsDiscountMoney()), 0));
//        esc.addText(forAligning("", "优惠劵金额：", ("0.0".equals(dataBean.getCouponMoney()) ? "0" : dataBean.getCouponMoney()), 0));
//        esc.addText(forAligning("", "订单差额：", ("".equals(dataBean.getUpdateSomeMoney()) ? "0" : dataBean.getUpdateSomeMoney()), 0));
//        esc.addText(forAligning("", "订单应付金额:", ("".equals(dataBean.getShouldPayMoney()) ? "0" : dataBean.getShouldPayMoney()), 0));
//        esc.addText(forAligning("", "订单实付金额：", ("".equals(dataBean.getRealPayMoney()) ? "0" : dataBean.getRealPayMoney()), 0));
//        //打印并走纸1行
//        esc.addPrintAndFeedLines((byte) 1);
//
//        //结束部分
//        //选择对齐方式：居中
//        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);
//        esc.addText("----欢迎您再次光临果速送平台----");
//        //打印并走纸1行
//        esc.addPrintAndFeedLines((byte) 1);
//
//        //选择左对齐方式
//        esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);
//        esc.addText("果速送网址：www.guoss.cn\n");
//        esc.addText("果速送热线：400-0169-682\n");
//        esc.addText("APP二维码：\n");

        //选择对齐方式：居中
        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);
        //打印图片
        Bitmap erCode = convertToBMW(interPicture(), 300, 150);
        esc.addOriginRastBitImage(erCode, 300, 0);
        //打印并走纸1行
        esc.addPrintAndFeedLines((byte) 1);

//        esc.addText("打单时间:" + getNowDate() + "\n");
//        esc.addText("打单人:" + printName + "(" + printMoible+ ")" + "\n");
//
//        //打印并走纸5行
//        esc.addPrintAndFeedLines((byte) 5);

        // 加入查询打印机状态，打印完成后，此时会接收到GpCom.ACTION_DEVICE_STATUS广播
        esc.addQueryPrinterStatus();

        Vector<Byte> datas = esc.getCommand();
        // 发送数据
        if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] == null) {
            return;
        }
        DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately(datas);
    }

    /**
     * 转为二值图像
     *
     * @param bmp 原图bitmap
     * @param w   转换后的宽
     * @param h   转换后的高
     * @return
     */
    public static Bitmap convertToBMW(Bitmap bmp, int w, int h) {
        int width = bmp.getWidth(); // 获取位图的宽
        int height = bmp.getHeight(); // 获取位图的高
        int[] pixels = new int[width * height]; // 通过位图的大小创建像素点数组
        // 设定二值化的域值，默认值为100
        int tmp = 0;
        bmp.getPixels(pixels, 0, width, 0, 0, width, height);
        int alpha = 0xFF << 24;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int grey = pixels[width * i + j];
                // 分离三原色
                alpha = ((grey & 0xFF000000) >> 24);
                int red = ((grey & 0x00FF0000) >> 16);
                int green = ((grey & 0x0000FF00) >> 8);
                int blue = (grey & 0x000000FF);
                if (red > tmp) {
                    red = 255;
                } else {
                    red = 0;
                }
                if (blue > tmp) {
                    blue = 255;
                } else {
                    blue = 0;
                }
                if (green > tmp) {
                    green = 255;
                } else {
                    green = 0;
                }
                pixels[width * i + j] = alpha << 24 | red << 16 | green << 8
                        | blue;
                if (pixels[width * i + j] == -1) {
                    pixels[width * i + j] = -1;
                } else {
                    pixels[width * i + j] = -16777216;
                }
            }
        }
        // 新建图片
        Bitmap newBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        // 设置图片数据
        newBmp.setPixels(pixels, 0, width, 0, 0, width, height);
        Bitmap resizeBmp = ThumbnailUtils.extractThumbnail(newBmp, w, h);
        return resizeBmp;
    }

    /**
     * 集合数据拼接
     * @param mList
     * @return
     */
    private String autoPrint(List<OrderDetailBean.DataBean.OrderDetailsListBean> mList) {
        String bill = "";
        int c = 0;
        for (int i = 0; i < mList.size(); i++) {
            bill += mList.get(i).getGoodsName() + "\n";
            bill += mList.get(i).getAfterWholePriceSize() + mList.get(i).getPriceUnit() + "/"
                    + mList.get(i).getBuyCount() + " X " + mList.get(i).getAfterWholePriceUnit() + "元" + " = " + mList.get(i).getAfterCostMoney() + "\n";
            bill += "----------------------------\n";
            c++;
        }
        return bill;
    }

    /**
     * 用来对齐格式
     *
     * @param left
     * @param right
     */
    private String forAligning(String kaishi, String left, Object right, Integer util) {
        //分别获取左边，右边的长度
        double leftlength = getLength(left);
        double rightlength = getLength(right.toString());
        kaishi += left;
        double v = 32 - leftlength - rightlength - 4;
        for (int i = 0; i < v; i++) {
            kaishi += " ";
        }
        if (util == 0) {
            //表示单位是元
            kaishi += right + "元" + "\n";
        } else {
            //表示单位是分数
            kaishi += right + "个" + "\n";
        }
        return kaishi;
    }

     /*
    得到一个字符串的长度,显示的长度,一个汉字或日韩文长度为1,英文字符长度为0.5
     */
    public static double getLength(String s) {
        double valueLength = 0;
        String chinese = "[\u4e00-\u9fa5]";
        // 获取字段值的长度，如果含中文字符，则每个中文字符长度为2，否则为1
        for (int i = 0; i < s.length(); i++) {
            // 获取一个字符
            String temp = s.substring(i, i + 1);
            // 判断是否为中文字符
            if (temp.matches(chinese)) {
                // 中文字符长度为1
                valueLength += 2;
            } else {
                // 其他字符长度为0.5
                valueLength += 1;
            }
        }
        //进位取整
        return Math.ceil(valueLength);
    }

    /**
     * 时间格式转换
     * @return
     */
    public static String getNowDate() {
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(date);
    }


    /**
     * 发送票据
     */
    void sendReceiptWithResponse() {
        EscCommand esc = new EscCommand();
        esc.addInitializePrinter();
        esc.addPrintAndFeedLines((byte) 3);
        // 设置打印居中
        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);
        // 设置为倍高倍宽
        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.ON, EscCommand.ENABLE.ON, EscCommand.ENABLE.OFF);
        // 打印文字
        esc.addText("Sample\n");
        esc.addPrintAndLineFeed();

        /* 打印文字 */
        // 取消倍高倍宽
        esc.addSelectPrintModes(EscCommand.FONT.FONTA, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF, EscCommand.ENABLE.OFF);
        // 设置打印左对齐
        esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);
        // 打印文字
        esc.addText("Print text\n");
        // 打印文字
        esc.addText("Welcome to use SMARNET printer!\n");

        /* 打印繁体中文 需要打印机支持繁体字库 */
        String message = "佳博智匯票據打印機\n";
        esc.addText(message, "GB2312");
        esc.addPrintAndLineFeed();

        /* 绝对位置 具体详细信息请查看GP58编程手册 */
        esc.addText("智汇");
        esc.addSetHorAndVerMotionUnits((byte) 7, (byte) 0);
        esc.addSetAbsolutePrintPosition((short) 6);
        esc.addText("网络");
        esc.addSetAbsolutePrintPosition((short) 10);
        esc.addText("设备");
        esc.addPrintAndLineFeed();

        /* 打印图片 */
        // 打印文字
        esc.addText("Print bitmap!\n");
        Bitmap b = BitmapFactory.decodeResource(getResources(),
                R.mipmap.title_pic);
//         打印图片
        esc.addOriginRastBitImage(b, 384, 0);

        /* 打印一维条码 */
        // 打印文字
        esc.addText("Print code128\n");
        esc.addSelectPrintingPositionForHRICharacters(EscCommand.HRI_POSITION.BELOW);
        // 设置条码可识别字符位置在条码下方
        // 设置条码高度为60点
        esc.addSetBarcodeHeight((byte) 60);
        // 设置条码单元宽度为1
        esc.addSetBarcodeWidth((byte) 1);
        // 打印Code128码
        esc.addCODE128(esc.genCodeB("SMARNET"));
        esc.addPrintAndLineFeed();

        /*
         * QRCode命令打印 此命令只在支持QRCode命令打印的机型才能使用。 在不支持二维码指令打印的机型上，则需要发送二维条码图片
         */
        // 打印文字
        esc.addText("Print QRcode\n");
        // 设置纠错等级
        esc.addSelectErrorCorrectionLevelForQRCode((byte) 0x31);
        // 设置qrcode模块大小
        esc.addSelectSizeOfModuleForQRCode((byte) 3);
        // 设置qrcode内容
        esc.addStoreQRCodeData("www.smarnet.cc");
        esc.addPrintQRCode();// 打印QRCode
        esc.addPrintAndLineFeed();

        // 设置打印左对齐
        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);
        //打印文字
        esc.addText("Completed!\r\n");

        // 开钱箱
        esc.addGeneratePlus(LabelCommand.FOOT.F5, (byte) 255, (byte) 255);
        esc.addPrintAndFeedLines((byte) 8);
        // 加入查询打印机状态，打印完成后，此时会接收到GpCom.ACTION_DEVICE_STATUS广播
        esc.addQueryPrinterStatus();
        Vector<Byte> datas = esc.getCommand();
        // 发送数据
        DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately(datas);
    }


    private void sendLabel() {
        LabelCommand tsc = new LabelCommand();
        // 设置标签尺寸，按照实际尺寸设置
        tsc.addSize(60, 60);
        // 设置标签间隙，按照实际尺寸设置，如果为无间隙纸则设置为0
        tsc.addGap(0);
        // 设置打印方向 向后打印，普通
        tsc.addDirection(LabelCommand.DIRECTION.BACKWARD, LabelCommand.MIRROR.NORMAL);
        // 开启带Response的打印，用于连续打印
        tsc.addQueryPrinterStatus(LabelCommand.RESPONSE_MODE.ON);
        // 设置原点坐标
        tsc.addReference(0, 0);
        // 撕纸模式开启
        tsc.addTear(EscCommand.ENABLE.ON);
        // 清除打印缓冲区
        tsc.addCls();
        // 绘制简体中文
        tsc.addText(20, 20, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                "Welcome to use SMARNET printer!");


        tsc.addQRCode(250, 80, LabelCommand.EEC.LEVEL_L, 5, LabelCommand.ROTATION.ROTATION_0, " www.smarnet.cc");
        // 绘制一维条码
        tsc.add1DBarcode(20, 250, LabelCommand.BARCODETYPE.CODE128, 100, LabelCommand.READABEL.EANBEL, LabelCommand.ROTATION.ROTATION_0, "SMARNET");
        // 打印标签
        tsc.addPrint(1, 1);
        // 打印标签后 蜂鸣器响

        tsc.addSound(2, 100);
        tsc.addCashdrwer(LabelCommand.FOOT.F5, 255, 255);
        Vector<Byte> datas = tsc.getCommand();
        // 发送数据
        if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] == null) {
            return;
        }
        DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately(datas);
    }
}
