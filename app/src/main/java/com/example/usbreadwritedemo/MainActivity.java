package com.example.usbreadwritedemo;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;


import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.fs.UsbFileOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private FileSystem fileSystem;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private UsbMassStorageDevice mUsbdevice;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        verifyStoragePermissions(MainActivity.this);

        Button button = findViewById(R.id.btCopyToUSBOne);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fileSystem = otgGet(MainActivity.this);
                copyLocalFileToUsb();
            }
        });

        Button button2 = findViewById(R.id.btCopyToUSBTwo);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fileSystem = otgGet(MainActivity.this);
                copyLocalFileToUsbTwo();
            }
        });



    }
    public static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.ACCESS_WIFI_STATE",
            "android.permission.READ_PHONE_STATE",
            "android.permission.READ_LOGS"};


    //然后通过一个函数来申请
    public void verifyStoragePermissions(Activity activity) {
        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,REQUEST_EXTERNAL_STORAGE);
            } else {

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void copyLocalFileToUsb(){
        //获取根目录
        if (fileSystem == null) {
            //请插入
        }

        UsbFile root = fileSystem.getRootDirectory();
        Log.d(TAG, "copyLocalFileToUsb: " + root);
        UsbFile newDir;
        UsbFile newFile = null;
        try {
            //创建文件夹
            newDir = root.createDirectory("record");
            //创建文件
            newFile = newDir.createFile("1.mp4");
            Log.d(TAG, "copyLocalFileToUsb: 1");
        } catch (Exception e) {
            //e.printStackTrace();
            try {
                Log.d(TAG, "copyLocalFileToUsb: 2");
                UsbFile searchFile = root.search("/record");
                Log.d(TAG, "copyLocalFileToUsb ser: " + searchFile);
                if (searchFile != null) {
                    Log.d(TAG, "copyLocalFileToUsb: 4");
                    newFile = searchFile.createFile("1.mp4");
                }
            } catch (IOException ex) {
                Log.d(TAG, "copyLocalFileToUsb: 3");
                //ex.printStackTrace();
                Log.d(TAG, "copyLocalFileToUsb: 创建失败");
            }

        }
        copyFile(newFile);

//        // 写入文件
//        OutputStream os = new UsbFileOutputStream(newFile, true);
//        os.write("hello".getBytes());
//        os.close();
//        Log.d(TAG, "doIt: ddd");
        //最后关闭
        //        device.close();


    }

    private void copyLocalFileToUsbTwo(){
        //        //获取根目录
        UsbFile root = fileSystem.getRootDirectory();
        UsbFile newDir;
        UsbFile newFile = null;
        try {
            //创建文件夹
            newDir = root.createDirectory("record");
            //创建文件
            newFile = newDir.createFile("2.mp4");
        } catch (Exception e) {
            //e.printStackTrace();
            try {
                UsbFile searchFile = root.search("/record");
                if (searchFile != null) {
                    newFile = searchFile.createFile("2.mp4");
                }
            } catch (IOException ex) {
                //ex.printStackTrace();
                Log.d(TAG, "copyLocalFileToUsb: 创建失败");
            }

        }
        copyFile2(newFile);
    }




    //获取到OTG连接的U盘
    private FileSystem otgGet(Context context) {
        UsbMassStorageDevice[] devicesArray = UsbMassStorageDevice.getMassStorageDevices(context);
        Log.d(TAG, "otgGet: " + devicesArray.length);
        FileSystem currentFs = null;

        for (UsbMassStorageDevice device : devicesArray) {//一般只有一个OTG借口，所以这里只取第一个
            try {
                device.init();
                //如果设备不支持一些格式的U盘，这里会有异常
                device.getPartitions();
                if (device.getPartitions().get(0) == null) {
                    return null;
                } else {
                    device.getPartitions().get(0).getFileSystem();
                }
                mUsbdevice = device;
                currentFs = device.getPartitions().get(0).getFileSystem();
                Log.e("OTG", "容量: " + currentFs.getCapacity());
                Log.e("OTG", "已使用空间: " + currentFs.getOccupiedSpace());
                Log.e("OTG", "剩余空间: " + currentFs.getFreeSpace());
                Log.e("OTG", "block数目: " + currentFs.getChunkSize());
            } catch (Exception e) {
                e.printStackTrace();
                mUsbdevice = null;
            }
        }
        return currentFs;
    }


    private void copyFile(UsbFile usbFile) {
        @SuppressLint("SdCardPath") File file=new File("/mnt/sdcard/1.mp4");

//        if (file.exists()){
//            //EventBus.getDefault().post(new MessageEvent(MyDataType.FILEDETEING));//开始删除原文件
//            //deleteFile(file);
//        }
        Log.d(TAG, "CopyFile: " + file.length() % 10240);//知道大小=====TODO
        //file.mkdir();

        copy(usbFile,  file.getAbsolutePath());
    }

    private void copyFile2(UsbFile usbFile) {
        @SuppressLint("SdCardPath") File file=new File("/mnt/sdcard/2.mp4");
//        if (file.exists()){
//            //EventBus.getDefault().post(new MessageEvent(MyDataType.FILEDETEING));//开始删除原文件
//            //deleteFile(file);
//        }
        Log.d(TAG, "CopyFile: " + file);
        //file.mkdir();

        copy(usbFile,  file.getAbsolutePath());
    }


    //删除源文件
    private void deleteFile(File file) {

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                File f = files[i];
                deleteFile(f);
            }
            file.delete();//如要保留文件夹，只删除文件，请注释这行
        } else if (file.exists()) {
            // Log.i("songkunjian","文件大小::"+file.length());
            file.delete();
        }
    }
    /**
     *
     * 复制文件
     *
     * @param fromFile 要复制的文件目录
     * @param toFile   要粘贴的文件目录
     * @return 是否复制成功
     */
    private   int mIdent=0;
    public  boolean copy(UsbFile toFFile, String fromFile) {
        if (toFFile==null)return false;

        copySdcardFile(toFFile, fromFile);
        return true;
    }

    //文件拷贝
    //要复制的目录下的所有非子目录(文件夹)文件拷贝
    public void copySdcardFile(UsbFile tocFolder, String fromFile) {
        //EventBus.getDefault().post(new MessageEvent(MyDataType.COPYING,cFolder.getLength()));//正在拷贝文件
        try {
            InputStream inusb = new FileInputStream(fromFile);
            UsbFileOutputStream fosto = new UsbFileOutputStream(tocFolder);
//            UsbFileInputStream inusb = new UsbFileInputStream(tocFolder);
//            OutputStream fosto = new FileOutputStream(fromFile);

            byte[] bt = new byte[1024 * 10];
            int c;
            while ((c = inusb.read(bt)) != -1) {
                fosto.write(bt, 0, c);
                Log.d(TAG, "copySdcardFile: " + c);
                //TODO===创建一个float来相加，然后除以文件大小，再显示百分比
            }
            mIdent--;
            Log.i(" songkunjian", " 完成拷贝==:" + mIdent);
            if (mIdent <= 0) {
                Log.i(" songkunjian", " 最终完成拷贝==:");
                //EventBus.getDefault().post(new MessageEvent(MyDataType.COPYSTOP));
            }
            fosto.flush();
            inusb.close();
            fosto.close();
            Log.d(TAG, "copySdcardFile: " + mUsbdevice);
            if (mUsbdevice != null) {
                mUsbdevice.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}