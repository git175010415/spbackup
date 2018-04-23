package com.sunmi.printerservice.render;

import android.content.Context;
import android.graphics.Bitmap;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.sunmi.printerservice.cell.DataCell;
import com.sunmi.printerservice.commandbean.ITask;
import com.sunmi.printerservice.entity.ServiceValue;
import com.sunmi.printerservice.exception.ExceptionConst;
import com.sunmi.printerservice.exception.PrinterException;
import com.sunmi.printerservice.utils.DataServiceUtils;
import com.sunmi.printerservice.utils.Utils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

import woyou.aidlservice.jiuiv5.ICallback;
import woyou.aidlservice.jiuiv5.ILcdCallback;

/**
 * 每个调用打印服务的app单独持有一个BitmapCreator
 * 每个BitmapCreator都单独持有一个render用于渲染
 * BitmapCreator用来解析字节流同时管理render的渲染属性
 */
public class BitmapCreator {
    private static final byte ESC = 0x1B;// esc命令
    private static final byte FS = 0x1C;// fs命令
    private static final byte GS = 0x1D;// gs命令
    private static final byte DLE = 0x10;

    private Context mContext;
    private int currentMemory;
    private RawPrintInterface rawPrinter;
    private byte[] headData;
    private Queue<ITask> taskList;
    private boolean isBufferModen;
    private Render render;
    private long taskId;
    private ServiceValue sv;

    public BitmapCreator(Context context, RawPrintInterface rawPrintInterface, ServiceValue serviceValue) {
        mContext = context;
        currentMemory = 0;
        rawPrinter = rawPrintInterface;
        headData = null;
        taskList = new LinkedBlockingQueue<>();
        isBufferModen = false;
        taskId = -1;
        sv = serviceValue;

        render = new Render(serviceValue, new Print() {

            @Override
            public void bytePrinting(byte[] bitmap) {
                rawPrinter.sendDataCell(new DataCell(bitmap, DataCell.PRINT_DATA));
            }
        });
    }

    public void excuteTaskList(ExecutorService executors) {
        while (!taskList.isEmpty()) {
            executors.execute(taskList.poll());
        }
    }

    public void addTaskToBuffer(ITask r) {
        taskList.offer(r);
    }

    public void cancleTaskList() {
        taskList.clear();
    }

    public boolean isNullTaskList() {
        return taskList.isEmpty();
    }

    //设置当前为缓冲模式/非换从模式
    public void setBufferModen(boolean flag) {
        this.isBufferModen = flag;
    }

    //获取当前是否是缓冲模式
    public boolean getBufferModen() {
        return isBufferModen;
    }

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    public int getCurrentMemory() {
        return currentMemory;
    }

    public void addCurrentMemory(int memory) {
        this.currentMemory += memory;
    }

    public void delCurrentMemory(int memory) {
        this.currentMemory -= memory;
        if (currentMemory < 0) {
            currentMemory = 0;
        }
    }

    //字体暂不能设置
/*    public boolean setFontName(String fontname) {
        return false;
    }

    public boolean setFontName(Typeface typeface) {
        if (typeface != null) {
            render.printerSet.textFont = typeface;
            render.printerSet.refresh();
            return true;
        }
        return false;
    }*/

    //设置字体大小
    public boolean setFontSize(float size) {
        if (size == 0 || size > render.printerSet.drawSpace) {
            return false;
        }
        render.printerSet.textSize = size;
        render.printerSet.refresh();
        return true;
    }

    //获取字体大小
    public float getFontSize() {
        return render.printerSet.textSize * render.printerSet.scaleX;
    }

    // 初始化渲染设置并清空未打印数据
    private void setPrinterDefault() {
        render.reset();
        headData = null;
        // 初始化打印机
        //rawPrinter.printBytes(new DataCell(new byte[] { 0x1B, 0x40 }, DataCell.PRINT_DATA));
    }

    //解析字节流 返回true 成功 返回false失败
    public boolean sendRAWData(byte[] command_tem, ICallback callback) {
        int i = 0;
        int skip;
        byte[] command;
        if (headData != null) {
            command = new byte[command_tem.length + headData.length];
            System.arraycopy(headData, 0, command, 0, headData.length);
            System.arraycopy(command_tem, 0, command, headData.length, command_tem.length);
            headData = null;
        } else {
            command = command_tem;
        }
        while (i < command.length) {
            try {
                if (oneBytecommand(command[i])) {
                    i++;
                } else if (twoBytecommand(command, i)) {
                    i += 2;
                } else if (threeBytecommand(command, i)) {
                    i += 3;
                } else if (fourBytecommand(command, i)) {
                    i += 4;
                } else if ((skip = rasterpiccommand(i, command)) != 0) {
                    i += skip;
                } else if ((skip = realpiccommand(i, command)) != 0) {
                    i += skip;
                } else if ((skip = tabPositions(i, command)) != 0) {
                    i += skip;
                } else if ((skip = filtercutpapercommand(i, command)) != 0) {
                    i += skip;
                } else if ((skip = selfcheckcommand(i, command)) != 0) {
                    i += skip;
                } else if ((skip = setPrinterHead(i, command)) != 0) {
                    i += skip;
                } else if ((skip = updatevaluescommand(i, command)) != 0) {
                    i += skip;
                } else if ((skip = hexPrintModeCommand(i, command)) != 0) {
                    i += skip;
                } else if ((skip = setPrintStrengthCommand(i, command)) != 0) {
                    i += skip;
                } else if ((skip = setPrintStrengthCommand1(i, command)) != 0) {
                    i += skip;
                }else if ((skip = QRCodeCommand(i, command)) != 0) {
                    i += skip;
                } else if ((skip = printBarCode(i, command)) != 0) {
                    i += skip;
                } else if ((skip = opendrawer(i, command)) != 0) {
                    i += skip;
                } else {
                    try {
                        skip = decodeChar(i, command);
                        if (skip == 0) {
                            throw new PrinterException(ExceptionConst.UNSUPPORTENCODING,
                                    ExceptionConst.UNSUPPORTUNSUPPORTENCODING_MSG.replace("#",
                                            render.printerSet.codeSystem));
                        }
                        i += skip;
                    } catch (UnsupportedEncodingException e) {
                        throw new PrinterException(ExceptionConst.UNSUPPORTENCODING,
                                ExceptionConst.UNSUPPORTUNSUPPORTENCODING_MSG.replace("#",
                                        render.printerSet.codeSystem));
                    }
                }
            } catch (IndexOutOfBoundsException e) {
                headData = new byte[command.length - i];
                System.arraycopy(command, i, headData, 0, headData.length);
                break;
            } catch (PrinterException e) {
                try {
                    if (callback != null) {
                        callback.onRaiseException(e.code, e.msg);
                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                return false;
            } catch (Exception e) {
                try {
                    if (callback != null) {
                        callback.onRaiseException(-1, e.getMessage());
                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                return false;
            }
        }
        return true;
    }

    private int decodeChar(int i, byte[] command) throws PrinterException, UnsupportedEncodingException, IndexOutOfBoundsException {
        if (render.printerSet.isSingleByteChar) {
            String s = new String(command, i, 1, render.printerSet.codeSystem);
            sv.appendUploadData(s);
            render.addString(s);
            return 1;
        } else {
            if (render.printerSet.codeSystem.equals("GB18030")) {
                if ((command[i] & 0x80) != 0x80) { // 单字节编码
                    String s = new String(command, i, 1, render.printerSet.codeSystem);
                    sv.appendUploadData(s);
                    render.addString(s);
                    return 1;
                } else {
                    if ((command[i + 1] & 0xFF) > 0x39) {
                        // GB18030， 双字节编码, 等同GBK
                        String s = new String(command, i, 2, render.printerSet.codeSystem);
                        sv.appendUploadData(s);
                        render.addString(s);
                        return 2;
                    } else if ((command[i + 1] & 0xFF) < 0x40 && (command[i + 2] & 0xFF) > 0x80
                            && (command[i + 3] & 0xFF) < 0x40) {
                        // GB18030 四字节编码，
                        // 包含日韩等汉字
                        String s = new String(command, i, 4, render.printerSet.codeSystem);
                        sv.appendUploadData(s);
                        render.addString(s);
                        return 4;
                    }
                }
                throw new PrinterException(ExceptionConst.UNSUPPORT, ExceptionConst.UNSUPPORT_MSG.replace("#", i + ""));
            }
            if (render.printerSet.codeSystem.equals("utf-8")) {
                int j;
                for (j = 0; j < 8; j++) {
                    if (((0x80 >> j) & command[i]) == 0x00) {
                        break;
                    }
                }
                if (j == 0) {
                    j++;
                }
                String s = new String(command, i, j, render.printerSet.codeSystem);
                sv.appendUploadData(s);
                render.addString(s);
                return j;
            }
        }
        return 0;
    }

    //以系统设置打印字符串
    public boolean printText(String s) {
        sv.appendUploadData(s);
        return render.addString(s);
    }

    //打印字符串以指定字体大小和字库（字库默认不使用）
    public boolean printText(String s, String fontname, float size) {
        sv.appendUploadData(s);
        float temp2 = render.printerSet.textSize;
        setFontSize(size);
        boolean res = render.addString(s);
        setFontSize(temp2);
        return res;
    }

    private void Tab() {
        float tabWidth = render.printerSet.tabWidth;
        float gh = 0;
        float currentPos = render.getCurentX() + render.printerSet.xLinePadding;
        if (render.printerSet.posX >= 0) {
            currentPos = render.printerSet.posX;
        }

        if (render.printerSet.tabPos != null) {
            for (byte i : render.printerSet.tabPos) {
                gh = i * tabWidth;
                if (gh > currentPos) {
                    break;
                }
            }
            if (gh <= currentPos) {
                return;
            }
        } else {
            tabWidth *= 4;
            while (gh <= currentPos) {
                gh += tabWidth;
            }
        }

        if (gh > render.printerSet.canvasWidth) {
            render.printerSet.posX = render.printerSet.canvasWidth;
        } else {
            render.printerSet.posX = gh;
        }
        render.printerSet.refresh();

    }

    private void setDefaultLineHeight() {
        render.printerSet.lineHeight = PrinterSet.default_lineHeight;
    }

    //单字节指令
    private boolean oneBytecommand(byte command) {
        switch (command) {
            case 0x09: // tab
                sv.appendUploadData(" ");
                Tab();
                return true;
            case 0x0A: // 打印换行
                sv.appendUploadData("\n");
                render.addString("\n");
                return true;
            case 0x0D://回车指令
                return true;
            default:
                return false;
        }
    }

    //双字节指令
    private boolean twoBytecommand(byte[] command, int i) {
        switch (command[i]) {
            case ESC:
                switch (command[i + 1]) {
                    case 0x32: // 设置为默认行高
                        setDefaultLineHeight();
                        return true;
                    case 0x40: // 初始化打印机 清除打印缓冲区数据，打印模式被设为上电时的默认值模式
                        setPrinterDefault();
                        return true;
                    default:
                        return false;
                }
            case FS:
                switch (command[i + 1]) {
                    case 0x26: // 打开多字节编码
                        render.printerSet.isSingleByteChar = false;
                        return true;
                    case 0x2E: // 关闭多字节编码
                        render.printerSet.isSingleByteChar = true;
                        return true;
                    default:
                        return false;
                }
            case GS:
                return false;
            default:
                return false;
        }
    }

    private void setxWorldRigthPadding(byte command) {
        render.printerSet.xWorldPadding = command;
        render.printerSet.refresh();
    }

    private void setPrintMode(byte command) {
        render.printerSet.isFakeBoldText = ((command & 0x08) == 0x08);
        if ((command & 0x10) == 0x10) {
            render.printerSet.TextTimesHeight = 2;
        } else {
            render.printerSet.TextTimesHeight = 1;
        }
        if ((command & 0x20) == 0x20) {
            render.printerSet.TextTimesWidth = 2;
        } else {
            render.printerSet.TextTimesWidth = 1;
        }
        render.printerSet.isUnderlineText = ((command & 0x80) == 0x80);
        render.printerSet.isTextTimes = true;
        render.printerSet.refresh();
    }

    private void setIsUnderline(byte command) {
        render.printerSet.isUnderlineText = (command != 0 && command != 48);
        render.printerSet.refresh();
    }

    private void setLineHeight(byte command) {
        render.printerSet.lineHeight = command;
    }

    private void setisFakeBoldText(byte command) {
        render.printerSet.isFakeBoldText = ((command & 0x01) == 0x01);
        render.printerSet.refresh();
    }

    private void setJustificationMode(byte command) {
        if (command == 0 || command == 48) {
            render.printerSet.justificationMode = 0;
        } else if (command == 1 || command == 49) {
            render.printerSet.justificationMode = 1;
        } else if (command == 2 || command == 50) {
            render.printerSet.justificationMode = 2;
        }
    }

    //三字节指令
    private boolean threeBytecommand(byte[] command, int i) {
        switch (command[i]) {
            case ESC:
                switch (command[i + 1]) {
                    case 0x20: // 设置字符右间距
                        setxWorldRigthPadding(command[i + 2]);
                        return true;
                    case 0x21: // 选择打印模式
                        setPrintMode(command[i + 2]);
                        return true;
                    case 0x2D: // 选择/取消下划线模式
                        setIsUnderline(command[i + 2]);
                        return true;
                    case 0x33: // 设置行高
                        setLineHeight(command[i + 2]);
                        return true;
                    case 0x45: // 选择/取消加粗模式
                        setisFakeBoldText(command[i + 2]);
                        return true;
                    case 0x47: // 选择/取消双重打印模式
                        setisFakeBoldText(command[i + 2]);
                        return true;
                    case 0x4A: // 打印缓冲内容而走纸 像素
                        sv.appendUploadData("\n");
                        runByPixel(command[i + 2]);
                        return true;
                    case 0x52: // 选择国际字符集
                        return true;
                    case 0x4D: // 选择字体
                        return true;
                    case 0x61: // 选择字符对齐模式 0,48 左对齐 1, 49 中间对齐 2, 50 右对齐
                        setJustificationMode(command[i + 2]);
                        return true;
                    case 0x64: // 打印缓冲内容并向前走纸 n 行
                        sv.appendUploadData("\n");
                        runByPixel((int) (command[i + 2] * render.printerSet.lineHeight));
                        return true;
                    case 0x74: // 单字节选择选择字符集
                        setCodeSystem(command[i + 2], true);
                        return true;
                    default:
                        return false;
                }
            case FS:
                switch (command[i + 1]) {
                    case 0x57: // 选择/取消汉字倍高倍宽 当 n 的最低位为 0，取消汉字倍高倍宽模式。 n 的最低位为
                        // 1，选择汉字倍高倍宽模式。
                        setTextMode(command[i + 2]);
                        return true;
                    case 0x43: // 多字节选择选择字符集
                        setCodeSystem(command[i + 2], false);
                        return true;
                    case 0x21: // 设置汉字字符模式
                        setPrintMode(command[i + 2]);
                        return true;
                    default:
                        return false;
                }
            case GS:
                switch (command[i + 1]) {
                    case 0x49: // 发送打印机 ID,查询指令
                        return true;
                    case 0x21: // 选择字符大小
                        setTextSize(command[i + 2]);
                        return true;
                    case 0x42: // 选择/取消黑白反显打印模式 当 n 的最低位为 0 时，取消反显打印。 当 n 的最低位为 1
                        // 时，选择反显打印。
                        setReversePrintingMode(command[i + 2]);
                        return true;
                    case 0x48: // 选择 HRI 字符的打印位置
                        render.printerSet.BarHRIPos = command[i + 2] % 48;
                        return true;
                    case 0x68: // 选择条码高度
                        render.printerSet.BarHeight = 0xff & command[i + 2];
                        return true;
                    case 0x66: // 设置条码HRI字体
                        // sendToPrinter(command, i, 3);
                        return true;
                    case 0x77: // 设置条码宽度 最细的一条线用几个像素点 2 -6
                        render.printerSet.BarWidth = command[i + 2];
                        return true;
                    default:
                        return false;
                }
            case DLE:
                switch (command[i + 1]) {
                    case 0x04: // 实时状态传输
                        // sendToPrinter(command, i, 3);
                        return true;
                    default:
                        return false;
                }
            default:
                return false;
        }
    }

    private void runByPixel(int b) {
        if (!render.isEmpty() || render.printerSet.posX > 0) {
            render.addString("\n");
        }
        byte[] data = new byte[b + 8];
        data[0] = 0x1D;
        data[1] = 0x76;
        data[2] = 0x30;
        data[3] = 0x00;
        data[4] = (byte) 1;
        data[5] = 0;
        data[6] = (byte) (b);
        data[7] = (byte) (b >> 8);
        rawPrinter.sendDataCell(new DataCell(data, DataCell.PRINT_DATA));
    }

    private void setTextMode(byte command) {
        render.printerSet.isTextTimes = ((command & 0x01) == 0x01);
        render.printerSet.refresh();
    }

    private void setReversePrintingMode(byte command) {
        render.printerSet.reversePrintingMode = ((command & 0x01) == 0x01);
    }

    private void setTextSize(byte command) {
        render.printerSet.TextTimesHeight = (command & 0x0f) + 1;
        render.printerSet.TextTimesWidth = ((command & 0xf0) / 16) + 1;
        render.printerSet.isTextTimes = true;
        render.printerSet.refresh();
    }

    private void setxLeftPadding(byte command1, byte command2) {
        int offset = ((command2 & 0xff) << 8) | (command1 & 0xff);
        render.printerSet.setxLinePadding(offset);
    }

    private void setxPrintWidth(byte command1, byte command2) {
        int offset = ((command2 & 0xff) << 8) | (command1 & 0xff);
        render.printerSet.setDrawSpace(offset);
    }

    private void setCurrentX1(byte command1, byte command2) {
        int detaX = ((command2 & 0xff) << 8) | (command1 & 0xff);

        if (detaX < render.printerSet.canvasWidth) {
            render.printerSet.posX = detaX;
        } else {
            render.printerSet.posX = render.printerSet.canvasWidth;
        }
        render.printerSet.justificationMode = render.printerSet.isRtl == 2 ? 2 : 0;
        render.printerSet.refresh();
    }

    private void setCodeSystem(byte command, boolean singleByte) {
        if (singleByte) {
            if (command == 0) {
                render.printerSet.codeSystem = "CP437";
                return;
            }
            if (command == 2) {
                render.printerSet.codeSystem = "CP850";
                return;
            }
            if (command == 3) {
                render.printerSet.codeSystem = "CP860";
                return;
            }
            if (command == 4) {
                render.printerSet.codeSystem = "CP863";
                return;
            }
            if (command == 5) {
                render.printerSet.codeSystem = "CP865";
                return;
            }
            if (command == 13) {
                render.printerSet.codeSystem = "CP857";
                return;
            }
            if (command == 14) {
                render.printerSet.codeSystem = "CP737";
                return;
            }
            if (command == 15) {
                render.printerSet.codeSystem = "CP928";
                return;
            }
            if (command == 16) {
                render.printerSet.codeSystem = "Windows-1252";
                return;
            }
            if (command == 17) {
                render.printerSet.codeSystem = "CP866";
                return;
            }
            if (command == 18) {
                render.printerSet.codeSystem = "CP852";
                return;
            }
            if (command == 19) {
                render.printerSet.codeSystem = "CP858";
                return;
            }
            if (command == 21) {
                render.printerSet.codeSystem = "CP874";
                return;
            }
            if (command == 33) {
                render.printerSet.codeSystem = "Windows-775";
                return;
            }
            if (command == 34) {
                render.printerSet.codeSystem = "CP855";
                return;
            }
            if (command == 36) {
                render.printerSet.codeSystem = "CP862";
                return;
            }
            if (command == 37) {
                render.printerSet.codeSystem = "CP864";
                return;
            }
            if (command == 41) {
                render.printerSet.codeSystem = "CP1098"; // 不支持
                return;
            }
            if (254 == (int) command) {
                render.printerSet.codeSystem = "CP855";
            }
        } else {
            if (command == 0x00 || command == 0x48) {
                render.printerSet.codeSystem = "GB18030";
                return;
            }
            if (command == 0x01 || command == 0x49) {
                render.printerSet.codeSystem = "BIG5";
                return;
            }
            if (command == 0x02 || command == 0x50) {
                render.printerSet.codeSystem = "KSC5601";
                return;
            }
            if (command == (byte) 0xff) {
                render.printerSet.codeSystem = "utf-8";
            }
        }
    }

    private void setCurrentX2(byte command1, byte command2) {
        int detaX = ((command2 & 0xff) << 8) | (command1 & 0xff);
        float currentPos = render.getCurentX();
        if (currentPos + detaX < render.printerSet.canvasWidth) {
            render.printerSet.posX = detaX + currentPos;
        } else {
            render.printerSet.posX = render.printerSet.canvasWidth;
        }
        render.printerSet.refresh();
    }

    private void setxWorldLeftRightPadding(byte command1, byte command2) {
        render.printerSet.xWorldPadding = command1 + command2;
        render.printerSet.refresh();
    }

    //四字节指令
    private boolean fourBytecommand(byte[] command, int i) {
        switch (command[i]) {
            case ESC:
                switch (command[i + 1]) {
                    case 0x24: // 设置绝对打印位置 将当前位置设置到距离行首（nL+nH×256）点（8点为1mm）处
                        sv.appendUploadData(" ");
                        setCurrentX1(command[i + 2], command[i + 3]);
                        return true;
                    case 0x5C: // 设置相对横向打印位置 该指令将打印位置设置到距当前位置(nL+nH×256)点处
                        sv.appendUploadData(" ");
                        setCurrentX2(command[i + 2], command[i + 3]);
                        return true;
                    default:
                        return false;
                }
            case FS:
                switch (command[i + 1]) {
                    case 0x53: // 设置汉字字符左右间距
                        setxWorldLeftRightPadding(command[i + 2], command[i + 3]);
                        return true;
                    default:
                        return false;
                }
            case GS:
                switch (command[i + 1]) {
                    case 0x4C: // 设置左边距 将左边距设置为(nL+nH×256)点
                        setxLeftPadding(command[i + 2], command[i + 3]);
                        return true;
                    case 0x57: // 设置打印区域宽度 将左边距设置为(nL+nH×256)点
                        setxPrintWidth(command[i + 2], command[i + 3]);
                        return true;
                    default:
                        return false;
                }
            default:
                return false;
        }
    }

    // 打印机自检指令 0x1F, 0x1B, 0x1F, 0x53
    private int selfcheckcommand(int i, byte[] command) {
        if (command[i] == 0x1F && command[i + 1] == 0x1B && command[i + 2] == 0x1F && command[i + 3] == 0x53) {
            rawPrinter.sendDataCell(new DataCell(new byte[]{0x1F, 0x1B, 0x1F, 0x53}, DataCell.PRINT_DATA));
            return 4;
        } else {
            return 0;
        }
    }

    // 打印机设置打印头指令 0x1F,0X1B,0X1F,0X50, 0X00/0x01 精工打印头/普瑞特打印头
    private int setPrinterHead(int i, byte[] command) {
        if (command[i] == 0x1F && command[i + 1] == 0x1B && command[i + 2] == 0x1F && command[i + 3] == 0x50) {
            rawPrinter.sendDataCell(new DataCell(new byte[]{0x1F, 0x1B, 0x1F, 0x50, command[i + 4]}, DataCell.COMMAND_DATA));
            return 5;
        } else {
            return 0;
        }
    }

    // 过滤切刀指令 GS V m 或者 GS V m n
    private int filtercutpapercommand(int i, byte[] command) {
        if (command[i] == 0x1D && command[i + 1] == 0x56) {
            if (command[i + 2] == 0x42) {
                rawPrinter.sendDataCell(new DataCell(new byte[]{0x1D, 0x56, 0x42, command[i + 3]}, DataCell.PRINT_DATA));
                return 4;
            }
            if (command[i + 2] == 0x41) {  //用来适配老的打印服务
                rawPrinter.sendDataCell(new DataCell(new byte[]{0x1D, 0x56, 0x00}, DataCell.PRINT_DATA));
                return 4;
            } else if (command[i + 2] == 0 || command[i + 2] == 1 || command[i + 2] == 48 || command[i + 2] == 49) {
                rawPrinter.sendDataCell(new DataCell(new byte[]{0x1D, 0x56, command[i + 2]}, DataCell.PRINT_DATA));
                return 3;
            }
        }
        return 0;
    }

    // 打印机改写寿命记录指令 0x1F, 0x1B, 0x1F, 0x72， 指令长度11字节
    private int updatevaluescommand(int i, byte[] command) {
        if (command[i] == 0x1F && command[i + 1] == 0x1B && command[i + 2] == 0x1F && command[i + 3] == 0x72) {
            byte[] data = new byte[11];
            System.arraycopy(command, i, data, 0, 11);
            rawPrinter.sendDataCell(new DataCell(data, DataCell.PRINT_DATA));
            return 11;
        } else {
            return 0;
        }
    }

    // 打开钱箱
    private int opendrawer(int i, byte[] command) {
        if (command[i] == 0x10 && command[i + 1] == 0x14) {
            rawPrinter.openBox();
            return 5;
        }
        return 0;
    }

    // 打印条码
    private int printBarCode(int i, byte[] command) throws PrinterException {
        try {
            StringBuilder stringBuilder = new StringBuilder();
            if (command[i] == 0x1D && command[i + 1] == 0x6B && command[i + 2] < 6) {
                int j = 3;
                while (command[i + j] != 0x00) {
                    try {
                        stringBuilder.append(new String(command, i + j++, 1, "US-ASCII"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
                j++;
                sv.appendUploadData("BarCode:" + stringBuilder.toString());
                render.addBarCode(stringBuilder.toString(), command[i + 2]);
                return j;
            } else if (command[i] == 0x1D && command[i + 1] == 0x6B && command[i + 2] > 6) {
                int j = 4;
                int length = (command[i + 3] & 0xff) + 4;
                while (j < length) {
                    try {
                        stringBuilder.append(new String(command, i + j++, 1, "US-ASCII"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
                sv.appendUploadData("BarCode:" + stringBuilder.toString());
                render.addBarCode(stringBuilder.toString(), command[i + 2]);
                return length;
            }
        } catch (Exception e) {
            throw new PrinterException(ExceptionConst.IllegalParameter, ExceptionConst.IllegalParameter_MSG);
        }
        return 0;
    }

    // 选择位图指令
    private int realpiccommand(int i, byte[] command) {
        if (command[i] == 0x1B && command[i + 1] == 0x2A) {
            int skip = 0;
            if (command[i + 2] == (byte) 0 || command[i + 2] == (byte) 1) {
                skip = ((command[i + 4] & 0xff) << 8) | (command[i + 3] & 0xff);
            } else if (command[i + 2] == (byte) 32 || command[i + 2] == (byte) 33) {
                skip = (((command[i + 4] & 0xff) << 8) | (command[i + 3] & 0xff)) * 3;
            }
            render.addBitImage(((command[i + 4] & 0xff) << 8) | (command[i + 3] & 0xff), command[i + 2], command, i + 5, skip);
            skip += 5;
            return skip;
        } else {
            return 0;
        }
    }

    // 16进制打印模式指令 GS ( A pL pH m m
    // 1D 28 41 02 00 00 01
    private int hexPrintModeCommand(int i, byte[] command) {
        if (command[i] == 0x1D && command[i + 1] == 0x28 && command[i + 2] == 0x41) {
            byte[] data = new byte[7];
            System.arraycopy(command, i, data, 0, 7);
            rawPrinter.sendDataCell(new DataCell(data, DataCell.PRINT_DATA));
            return 7;
        } else {
            return 0;
        }
    }

    // 设置及打印二维码指令 GS ( k pL pH 后面接(pL+PH*256)个字节
    // 1d 28 6b 03 00 31 45 30
    // 1d 28 6b 0b 00 31 50 30 47 70 72 69 6e 74 65 72
    private int QRCodeCommand(int i, byte[] command) {
        if (command[i] == 0x1D && command[i + 1] == 0x28 && command[i + 2] == 0x6B) {
            int k = ((command[i + 4] & 0xff) << 8) | (command[i + 3] & 0xff);
            if (command[i + 6] == 67) // 大小
            {
                render.printerSet.QrSize = command[i + 7];
            }
            if (command[i + 6] == 69) // 纠错等级
            {
                switch (command[i + 7]) {
                    case 48:
                        render.printerSet.errorCorrectionLevel = ErrorCorrectionLevel.L;
                        break;
                    case 49:
                        render.printerSet.errorCorrectionLevel = ErrorCorrectionLevel.M;
                        break;
                    case 50:
                        render.printerSet.errorCorrectionLevel = ErrorCorrectionLevel.Q;
                        break;
                    case 51:
                        render.printerSet.errorCorrectionLevel = ErrorCorrectionLevel.H;
                        break;
                    default:
                        break;
                }
            }
            if (command[i + 6] == 80) // 存入二维码
            {
                byte[] data = new byte[k - 3];
                System.arraycopy(command, i + 8, data, 0, k - 3);
                render.printerSet.qr_data = data;
            }
            if (command[i + 6] == 81) // 打印二维码
            {
                if(render.printerSet.qr_data != null){
                    try {
                        sv.appendUploadData("QrCode:" + new String(render.printerSet.qr_data, "GB18030")+ "\n");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                render.printQrBar();
            }
            return k + 5;
        } else {
            return 0;
        }
    }

    // 16进制打印模式指令 GS ( A pL pH m m
    // 1D 28 45 04 00 05 05 nL nH
    private int setPrintStrengthCommand(int i, byte[] command) {
        if (command[i] == 0x1D && command[i + 1] == 0x28 && command[i + 2] == 0x45) {
            int length = (command[i + 3]&0xff) + (command[i + 4]&0xff)*256 + 5;
            byte[] data = new byte[length];
            System.arraycopy(command, i, data, 0, length);
            rawPrinter.sendDataCell(new DataCell(data, DataCell.PRINT_DATA));
            return length;
        } else {
            return 0;
        }
    }

    // 16进制打印模式指令 FS ( L pL pH fn m
    // 1C 28 4C 04 00 05 05 nL nH
    private int setPrintStrengthCommand1(int i, byte[] command) {
        if (command[i] == 0x1C && command[i + 1] == 0x28 && command[i + 2] == 0x4C) {
            int length = (command[i + 3]&0xff) + (command[i + 4]&0xff)*256 + 5;
            byte[] data = new byte[length];
            System.arraycopy(command, i, data, 0, length);
            rawPrinter.sendDataCell(new DataCell(data, DataCell.PRINT_DATA));
            return length;
        } else {
            return 0;
        }
    }

    //光栅图形将转为Bitmap（会将缓冲区数据打印出来，光栅图形单独占一行）
    private int rasterpiccommand(int i, byte[] command) {
        if (command[i] == 0x1D && command[i + 1] == 0x76 && command[i + 2] == 0x30) {
            int flag = command[i + 3];
            int w = ((command[i + 5] & 0xff) << 8) | (command[i + 4] & 0xff);
            int h = ((command[i + 7] & 0xff) << 8) | (command[i + 6] & 0xff);
            int skip = w * h;
            float temp_lineheight = render.printerSet.lineHeight;
            byte[] data = new byte[skip];
            System.arraycopy(command, i + 8, data, 0, skip);
            if (!render.isEmpty() || render.printerSet.posX > 0) {
                render.addString("\n");
            }
            render.printerSet.lineHeight = 0;
            render.rasterToBitmap(data, w, h, flag);
            render.addString("\n");
            render.printerSet.lineHeight = temp_lineheight;
            return skip + 8;
        } else {
            return 0;
        }
    }

    private int tabPositions(int i, byte[] command) {
        if (command[i] == 0x1B && command[i + 1] == 0x44) {
            int skip = 0;
            if (command[i + 2 + skip] == 0) {
                render.printerSet.tabPos = null;
                render.printerSet.tabWidth = PrinterSet.default_textSize;
            } else {
                if (render.printerSet.tabPos == null) {
                    render.printerSet.tabPos = new ArrayList<>();
                } else {
                    render.printerSet.tabPos.clear();
                }
                render.printerSet.tabPos.add(command[i + 2 + skip]);
                for (skip = 1; skip < 32; skip++) {
                    if ((command[i + 2 + skip] != 0x00) && (command[i + 2 + skip] > command[i + 1 + skip])) {
                        render.printerSet.tabPos.add(command[i + 2 + skip]);
                    } else {
                        break;
                    }
                }
                render.printerSet.tabWidth = render.printerSet.textSize * render.printerSet.TextTimesWidth;
            }
            return 3 + skip;
        } else {
            return 0;
        }
    }

    public boolean printBitmap(Bitmap data) {
        return render.addBitmap(data, true);
    }


    //发送头尾包
    public void sendSeriesData(int series, ICallback callback) {
        rawPrinter.sendDataCell(new DataCell(series, callback));
    }

    //打印表格数据
    public boolean printColumnsText(String[] colsTextArr, int[] colsWidthArr, int[] colsAlign) {
        for(String s: colsTextArr){
            sv.appendUploadData(s + " ");
        }
        sv.appendUploadData("\n");
        return render.addTableString(colsTextArr, colsWidthArr, colsAlign);
    }

    /**
     * 打印数据处理前操作
     * @param creatTime 此数据建立的时间
     */
    public void runtime(long creatTime){
        render.printerSet.runtime();
        uploadData(creatTime);
    }

    /**
     * 上传dataservice 打印数据，间隔200ms内且数据量超过8个字符的发送数据会上报
     * @param creatTime
     */
    private void uploadData(long creatTime) {
        if(sv.getInitTime() == -1){
            sv.setInitTime(creatTime);
            sv.setUploadTime(DataServiceUtils.getNetTime(mContext));
        }else if(creatTime - sv.getInitTime() > 200){
            if(sv.getUploadData().length() > 8){
                rawPrinter.addOneOrder();
                DataServiceUtils.addPrintText(mContext, sv.getUploadData(), sv.getUploadTime());
            }
            sv.clearUploadData();
            sv.setInitTime(creatTime);
            sv.setUploadTime(DataServiceUtils.getNetTime(mContext));
        }
    }

    public void printLcdText(String text, ILcdCallback callback){
        sendLcdMultiData(render.showLcdString(text), callback);
    }

    public void printLcdText(String tText, String bText, ILcdCallback callback){
        sendLcdMultiData(render.showLcdString(tText, bText), callback);
    }

    public void printLcdBitmap(Bitmap bitmap, ILcdCallback callback){
        sendLcdMultiData(render.showLcdBitmap(bitmap), callback);
    }

    //  发送复合LCD数据
    private void sendLcdMultiData(byte[] bitmap, ILcdCallback callback){
        for(int times = 1; times < 4; times++){
            byte[] pack = new byte[255];
            Arrays.fill(pack, (byte) 0x0);
            pack[0] = 0x1A;
            pack[1] = 0x1C;
            pack[2] = 0x10;
            pack[3] = (byte) times;
            int start = (times-1)*246;
            System.arraycopy(bitmap, start, pack, 4, ((bitmap.length - start) > 246)?246:(bitmap.length - start));
            if(times == 3){
                int crc = Utils.CRC_16(pack, 4, 152);
                pack[251] = (byte) crc;
                pack[252] = (byte) (crc>>8);
                crc = Utils.CRC_16(bitmap, 0, bitmap.length);
                pack[253] = (byte) crc;
                pack[254] = (byte) (crc>>8);
                rawPrinter.sendDataCell(new DataCell(pack, callback));
            }else{
                int crc = Utils.CRC_16(pack, 4, 250);
                pack[253] = (byte) crc;
                pack[254] = (byte) (crc>>8);
                rawPrinter.sendDataCell(new DataCell(pack, null));
            }
        }
    }
}
