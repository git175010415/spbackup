package com.sunmi.printerservice.thread;

import android.content.Context;

import com.longcheer.spijni.SpiJni;
import com.sunmi.printerservice.utils.C;
import com.sunmi.printerservice.utils.LogUtils;
import com.sunmi.printerservice.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import woyou.aidlservice.jiuiv5.BuildConfig;

/**
 * 打印机固件更新线程
 */
public class DownloadRunable extends Thread {
    private Context context;
    private SpiJni jni;
    private DownloadInterface mDownloadInterface;
    private byte[] send = new byte[256];
    private byte[] recv = new byte[256];
    private byte[] programe;
    private int rl, gh, zy;
    private boolean once = true;
    private String version;
    private String name;
    private String printerType;

    public DownloadRunable(Context context, SpiJni jni, DownloadInterface mDownloadInterface) {
        this.context = context;
        this.jni = jni;
        this.mDownloadInterface = mDownloadInterface;
        try {
            programe = "Programme".getBytes("US-ASCII");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            programe = null;
        }
    }

    @Override
    public void run() {
        try {
            String bin = "printer.bin";
            int num = C.UPDATE_COUNT;
            do {
                rl = 0;
                gh = 0;
                zy = 0;
                --num;
                //复位打印机
                jni.gpio_write(jni.fd_rsest, 1);
                Thread.sleep(5);
                jni.gpio_write(jni.fd_rsest, 0);
                //理论上只有当IO通讯失败会返回检测不到打印机的报警及广播
                //但如果出现IO正常spi永久错误，将死循环在此，此时只能获取到打印机准备中的广播
                while (true) {
                    Thread.sleep(1000);
                    if (holyShit(C.INVALIDPACKAGE, 2, true)) {
                        checkPrinterDone(false);
                        return;
                    }
                    if ((recv[2] == (byte) 0xC3) || (recv[2] == (byte) 0xA5)) {
                        if (readInfo()) {
                            checkPrinterDone(true);
                            break;
                        }
                    }
                }

                if (!BuildConfig.BIN_COMP.equals("T1mini") && BuildConfig.BIN_COMP.compareTo(version) > 0) {
                    holyShit(C.INVALIDPACKAGE, 2);
                    if (recv[2] == (byte) 0xA5) {
                        holyShit(programe, 20);
                        holyShit(null, 20);
                    } else if (recv[2] == (byte) 0xC3) {
                        if (!version.equals(BuildConfig.BIN_VERSION)) {
                            holyShit(C.ENTER_BOOTLOADER, 20);
                            holyShit(null, 20);
                            holyShit(programe, 20);
                            holyShit(null, 20);
                        } else {
                            if (mDownloadInterface != null) {
                                mDownloadInterface.startPrinter();
                            }
                            return;
                        }
                    }
                } else if (!BuildConfig.BIN_COMP.equals("T1mini")) {
                    if (recv[2] == (byte) 0xA5) {
                        holyShit(C.INVALIDPACKAGE, 20);
                        holyShit(programe, 20);
                        holyShit(null, 20);
                    } else if (recv[2] == (byte) 0xC3) {
                        if (!version.equals(BuildConfig.BIN_VERSION)) {
                            programme();
                        } else {
                            if (mDownloadInterface != null) {
                                mDownloadInterface.startPrinter();
                            }
                            return;
                        }
                    }
                } else if(BuildConfig.BIN_COMP.equals("T1mini")){
                    // 比较参数，决定升级并决定升级哪个bin；
                    // A5表示必须升级bin且以137字段决定升级哪种打印机类型
                    // C3B表示通过80和137共同决定是否升级及打印机类型
                    if (recv[2] == (byte) 0xA5) {
                        holyShit(C.INVALIDPACKAGE, 20);
                        holyShit(programe, 20);
                        holyShit(null, 20);
                        if (name.contains("T1mini-BL-58") && (printerType.contains("PT483-58") || version.equals("0.01\n"))) {
                            bin = "printer_58.bin";
                        } // 升级58
                        else if (name.contains("T1mini-BL-80") && (printerType.contains("CAPD347-80") || version.equals("0.01\n"))) {
                            bin = "printer_80.bin";
                        } // 升级80
                        else {
                            if (mDownloadInterface != null) {
                                mDownloadInterface.updateStatus(false);
                            }
                            return;
                        }//如果bootloader不匹配打印机类型则不升级
                    }else if (recv[2] == (byte) 0xC3) {
                        if (name.contains("T1mini-58") && printerType.contains("PT483-58")) {
                            if (!version.equals(BuildConfig.BIN_VERSION)) {
                                programme();
                                if(!(name.contains("T1mini-BL-58") && printerType.contains("PT483-58"))){
                                    if (mDownloadInterface != null) {
                                        mDownloadInterface.updateStatus(false);
                                    }
                                    return;
                                }
                                bin = "printer_58.bin";
                            }else{
                                if (mDownloadInterface != null) {
                                    mDownloadInterface.startPrinter();
                                }
                                return;
                            }
                        } // 走58逻辑
                        else if (name.contains("T1mini-80") && printerType.contains("CAPD347-80")) {
                            if (!version.equals(BuildConfig.BIN2_VERSION)) {
                                programme();
                                if(!(name.contains("T1mini-BL-80") && printerType.contains("CAPD347-80"))){
                                    if (mDownloadInterface != null) {
                                        mDownloadInterface.updateStatus(false);
                                    }
                                    return;
                                }
                                bin = "printer_80.bin";
                            }else{
                                if (mDownloadInterface != null) {
                                    mDownloadInterface.startPrinter();
                                }
                                return;
                            }
                        } // 走80逻辑
                        else if (printerType.contains("PT483-58")) {
                            programme();
                            if(!(name.contains("T1mini-BL-58") &&printerType.contains("PT483-58"))){
                                if (mDownloadInterface != null) {
                                    mDownloadInterface.updateStatus(false);
                                }
                                return;
                            }

                        } // 升级58
                        else if (printerType.contains("CAPD347-80")) {
                            programme();
                            if(!(name.contains("T1mini-BL-80") && printerType.contains("CAPD347-80"))){
                                if (mDownloadInterface != null) {
                                    mDownloadInterface.updateStatus(false);
                                }
                                return;
                            }
                            bin = "printer_80.bin";
                        } // 升级80
                        else if(name.contains("T1mini-58") && printerType.equals("")){
                            if(!version.equals(BuildConfig.BIN_VERSION)){
                                programme();
                                if(version.equals("0.01\n")){
                                    bin = "printer_58.bin";
                                }else{
                                    if (mDownloadInterface != null) {
                                        mDownloadInterface.updateStatus(false);
                                    }
                                    return;
                                }
                            }else{
                                if (mDownloadInterface != null) {
                                    mDownloadInterface.startPrinter();
                                }
                                return;
                            }
                        }//兼容旧的58的bootloader和app
                        else if((name.contains("T1mini-80") || name.contains("T1-GPIOINT")) && printerType.equals("")){
                            if(!version.equals(BuildConfig.BIN2_VERSION)){
                                programme();
                                if(version.equals("0.01\n")){
                                    bin = "printer_80.bin";
                                }else{
                                    if (mDownloadInterface != null) {
                                        mDownloadInterface.updateStatus(false);
                                    }
                                    return;
                                }
                            }else{
                                if (mDownloadInterface != null) {
                                    mDownloadInterface.startPrinter();
                                }
                                return;
                            }
                        }//兼容旧的80的bootloader和app
                        else{
                            if (mDownloadInterface != null) {
                                mDownloadInterface.updateStatus(false);
                            }
                            return;
                        }
                    }
                }
            } while (num != 0 && !updateBin(bin));


            if (mDownloadInterface != null) {
                if (num != 0) {
                    mDownloadInterface.updateStatus(true);
                    mDownloadInterface.startPrinter();
                } else {
                    mDownloadInterface.updateStatus(false);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //通过读busy1 busy2的高低电平来发spi
    private void holyShit(byte da[], int sleep) {
        holyShit(da, sleep, false);
    }

    //通过读busy1 busy2的高低电平来发spi 无时间限制 true: 超时 false: 不超时
    private boolean holyShit(byte da[], int sleep, boolean hasTimeLimit)  {
        int rl_, gh_, zy_;
        int no_printer = 0;
        if (da != null && da.length < 256) {
            System.arraycopy(da, 0, send, 0, da.length);
            Arrays.fill(send, da.length, send.length, (byte) 0);
            da = send;
        }
        while (true) {
            if (hasTimeLimit && no_printer > 20000) {
                return true;
            }
            gh_ = jni.readbusy1();
            zy_ = jni.readbusy2();
            try {
                Thread.sleep(sleep);
                no_printer += sleep;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            rl_ = gh_ + zy_;
            if (rl_ == 1 && (gh_ != gh || zy_ != zy)) {
                if (da != null) {
                    jni.spi_transfer(jni.fd_spi, send.length, send, recv);
                    gh = gh_;
                    zy = zy_;
                    rl = 0;
                }
                break;
            }
            if (rl_ == 2 && rl != rl_) {
                jni.spi_transfer(jni.fd_spi, C.INVALIDPACKAGE.length, C.INVALIDPACKAGE, recv);
                gh = 1;
                zy = 1;
                rl = 0;
                if (da == null) {
                    break;
                }
            } else if (rl_ == 0) {
                gh = 0;
                zy = 0;
                rl = 0;
            }
        }
        return false;
    }

    private void checkPrinterDone(boolean i) {
/*        if (i) {
            Settings.Global.putInt(context.getContentResolver(), "hasPrinter", 0);
        } else {
            Settings.Global.putInt(context.getContentResolver(), "hasPrinter", 1);
        }*/
        if (mDownloadInterface != null) {
            mDownloadInterface.hasPrinter(i);
        }
    }

    private void programme() {
        byte dd[] = new byte[256];
        dd[0] = 1;
        jni.readblock1();
        jni.spi_transfer(jni.fd_spi, dd.length, dd, recv);
        System.arraycopy(C.ENTER_BOOTLOADER, 0, dd, 1, C.ENTER_BOOTLOADER.length);
        jni.readblock1();
        jni.spi_transfer(jni.fd_spi, dd.length, dd, recv);
        holyShit(C.INVALIDPACKAGE, 20);
        holyShit(programe, 20);
        readInfo();
        holyShit(null, 20);
    }

    private boolean readInfo() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        for (int i = 96; i <= 111; i++) {
            if (recv[i] != 0) {
                buffer.write(recv[i]);
            } else {
                break;
            }
        }
        version = buffer.toString();
        LogUtils.d("打印机固件版本号：" + version);
        buffer.reset();
        for (int i = 80; i <= 95; i++) {
            if (recv[i] != 0) {
                buffer.write(recv[i]);
            } else {
                break;
            }
        }
        name = buffer.toString();
        LogUtils.d("打印机名称：" + name);
        buffer.reset();
        for (int i = 137; i <= 152; i++) {
            if (recv[i] != 0) {
                buffer.write(recv[i]);
            } else {
                break;
            }
        }
        printerType = buffer.toString();
        LogUtils.d("打印机BL类型：" + printerType);
        try {
            buffer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return !version.equals("") && !name.equals("");
    }

    private boolean updateBin(String bin) throws IOException, InterruptedException {
        if(bin == null || bin.equals("")){
            LogUtils.e("获取到bin信息为空！");
            return  false;
        }
        if (once) {
            mDownloadInterface.startUpdate();
            once = false;
        }
        byte i = 0;
        int readLength;
        long fileLength = 0;
        int realLength = 128;
        InputStream inputStream = context.getAssets().open(bin);
        while (inputStream.read() != -1) {
            fileLength++;
        }
        inputStream.close();
        send[0] = C.MODEM_SOH;
        send[1] = (byte) 0x00;
        send[2] = (byte) 0xFF;
        byte[] name = "printer.bin\0".getBytes("US-ASCII");
        System.arraycopy(name, 0, send, 3, name.length);
        byte[] length = (fileLength + "\0").getBytes();
        System.arraycopy(length, 0, send, 3 + name.length, length.length);
        Utils.CRC_16(send);
        holyShit(send, 2);
        if (recv[2] != 0) {
            return false;
        }
        //刷头
        inputStream = context.getAssets().open(bin);
        while ((readLength = inputStream.read(send, 3, realLength)) != -1) {
            if (i == (byte) 0xFF) {
                i = 0;
            } else {
                i++;
            }
            send[0] = C.MODEM_SOH;
            send[1] = i;
            send[2] = (byte) ~i;
            for (int j = readLength; j < realLength; j++) {
                send[readLength + 3 + j] = (byte) 0x1A;
            }
            Arrays.fill(send, realLength + 3, send.length - 2, (byte) 0);
            Utils.CRC_16(send);
            holyShit(send, 2);
            if (recv[2] != 0)
                return false;
        }
        Arrays.fill(send, 0, send.length, C.MODEM_EOT);
        holyShit(send, 2);
        //刷尾
        holyShit(null, 2);
        inputStream.close();
        return (recv[2] == 6);
    }
}
