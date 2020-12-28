package com.example.usbreadwritedemo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.bh.www.bhip101_android.global.Config;
import com.bh.www.bhip101_android.utils.ToastUtils;
import com.github.mjdev.libaums.UsbMassStorageDevice;
import com.github.mjdev.libaums.fs.FileSystem;
import com.github.mjdev.libaums.fs.UsbFile;
import com.github.mjdev.libaums.fs.UsbFileOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * @Description: USB写入Helper
 * @Author: fyq
 * @CreateDate: 2020/8/12 2:52 PM
 */
public class USBWriteHelper {
    private static final String TAG = "USBWriteHelper";
    private static volatile USBWriteHelper instance;
    private Context context;
    //add fyq 下载相关
    private FileSystem fileSystem;
    private UsbMassStorageDevice mUsbdevice;
    private String screenRecorderFileName;//录制完成后的文件名


    private static final String COPY_DIR_NAME = "bh_usb_down_recorder";//copy的文件夹名
    public static final String USB_WRITE_DONE_BROADCAST = "USB_WRITE_DONE_BROADCAST";

    public static USBWriteHelper me() {
        if (null == instance) {
            synchronized (USBWriteHelper.class) {
                if (null == instance) {
                    instance = new USBWriteHelper();
                }
            }
        }
        return instance;
    }

    public void init(Context context) {
        //初始化做的事
        this.context = context;
    }

    //获取到OTG连接的U盘
    public FileSystem otgGet() {
        UsbMassStorageDevice[] devicesArray = UsbMassStorageDevice.getMassStorageDevices(context);
        Log.d(TAG, "otgGet: " + devicesArray.length);
        FileSystem currentFs = null;

        for (UsbMassStorageDevice device : devicesArray) {//一般只有一个OTG接口，所以这里只取第一个
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
                ToastUtils.showToast("U盘格式不支持");
            }
        }
        return currentFs;
    }

    //创建文件到U盘
    public void copyLocalFileToUsb() {
        //获取根目录
        if (fileSystem == null) {
            //请插入
            ToastUtils.showToast("请插入U盘后下载");
            return;
        }
        if (screenRecorderFileName == null || screenRecorderFileName.equals("")) {
            ToastUtils.showToast("请录制后再下载");
            return;
        }

        UsbFile root = fileSystem.getRootDirectory();
        Log.d(TAG, "copyLocalFileToUsb: " + root);
        UsbFile newDir;
        UsbFile newFile = null;
        try {
            //创建文件夹
            newDir = root.createDirectory(COPY_DIR_NAME);
            //创建文件
            newFile = newDir.createFile(screenRecorderFileName);
            Log.d(TAG, "copyLocalFileToUsb: 1");
        } catch (Exception e) {
            //e.printStackTrace();
            try {
                Log.d(TAG, "copyLocalFileToUsb: 2");
                UsbFile searchFile = root.search("/" + COPY_DIR_NAME);
                Log.d(TAG, "copyLocalFileToUsb ser: " + searchFile);
                if (searchFile != null) {
                    Log.d(TAG, "copyLocalFileToUsb: 4");
                    newFile = searchFile.createFile(screenRecorderFileName);
                }
            } catch (IOException ex) {
                Log.d(TAG, "copyLocalFileToUsb: 3");
                //ex.printStackTrace();
                Log.d(TAG, "copyLocalFileToUsb: 创建失败");
            }

        }
        copyFile(newFile);
    }

    private void copyFile(UsbFile usbFile) {
        @SuppressLint("SdCardPath") File file=new File(Config.recording + "/" + screenRecorderFileName);
        Log.d(TAG, "CopyFile: " + file);
        copy(usbFile,  file.getAbsolutePath());
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
    private void copy(UsbFile toFFile, String fromFile) {
        if (toFFile==null)return;
        ToastUtils.showToast("文件正在后台下载中，请您稍后");
        Observable.create(new ObservableOnSubscribe<List>() {
            @Override
            public void subscribe(ObservableEmitter<List> emitter) throws Exception {
                copySdcardFile(toFFile, fromFile);
                //emitter.onNext(drawableRes);
                emitter.onComplete();
            }
        }).flatMap(new Function<List, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(List list) throws Exception {
                return Observable.fromIterable(list);
            }
        }).subscribeOn(Schedulers.io())//在IO线程执行数据库处理操作
                .observeOn(AndroidSchedulers.mainThread())//在UI线程显示图片
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Log.d("----","onSubscribe");
                    }

                    @Override
                    public void onNext(Integer integer) {
                        //imageView.setImageResource(integer);//拿到id,加载图片
                        Log.d("----",integer+"");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d("----",e.toString());
                    }

                    @Override
                    public void onComplete() {
                        ToastUtils.showToast("文件下载完成");
                        Log.d(TAG, "onComplete: " + mUsbdevice);
                        if (mUsbdevice != null) {
                            mUsbdevice.close();
                        }
                        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(USB_WRITE_DONE_BROADCAST));

                        Log.d("----","onComplete");
                    }
                });
    }

    //文件拷贝
    //要复制的目录下的所有非子目录(文件夹)文件拷贝
    private void copySdcardFile(UsbFile tocFolder, String fromFile) {
        try {
            InputStream inusb = new FileInputStream(fromFile);
            UsbFileOutputStream fosto = new UsbFileOutputStream(tocFolder);
            byte[] bt = new byte[1024 * 10];
            int c;

            while ((c = inusb.read(bt)) != -1) {
                fosto.write(bt, 0, c);
                Log.d(TAG, "copySdcardFile: " + c);
            }
            //Log.d(TAG, "copySdcardFile MY: " + mDownFileCont);
            mIdent--;
            Log.i(TAG, " 完成拷贝==:" + mIdent);
            if (mIdent <= 0) {
                Log.i(TAG, " 最终完成拷贝==:");
            }
            fosto.flush();
            inusb.close();
            fosto.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void setScreenRecorderFileName(String screenRecorderFileName) {
        this.screenRecorderFileName = screenRecorderFileName;
    }

    public void setFileSystem(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }
}
