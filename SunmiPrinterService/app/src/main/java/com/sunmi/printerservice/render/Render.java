package com.sunmi.printerservice.render;

import java.text.Bidi;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;

import com.google.zxing.WriterException;
import com.google.zxing.oned.CodaBarWriter;
import com.google.zxing.oned.Code39Writer;
import com.google.zxing.oned.Code93Writer;
import com.google.zxing.oned.EAN13Writer;
import com.google.zxing.oned.EAN8Writer;
import com.google.zxing.oned.ITFWriter;
import com.google.zxing.oned.UPCEWriter;
import com.google.zxing.qrcode.encoder.QRCode;
import com.sunmi.printerservice.barcode.MyCode128Writer;
import com.sunmi.printerservice.barcode.MyQrEncoder;
import com.sunmi.printerservice.cell.CharCell;
import com.sunmi.printerservice.cell.PaintCell;
import com.sunmi.printerservice.entity.ServiceValue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Typeface;


public class Render {
    PrinterSet printerSet;
    private ArrayList<CharCell> charCells;
    private ArrayList<PaintCell> paintCells;
    private float bufferCharLength;
    private Print print;

    private Comparator<CharCell> Order = new Comparator<CharCell>() {
        public int compare(CharCell o1, CharCell o2) {
            return o1.offset - o2.offset;
        }
    };


    public Render(ServiceValue serviceValue, Print print) {
        printerSet = new PrinterSet(serviceValue);
        charCells = new ArrayList<>();
        paintCells = new ArrayList<>();
        bufferCharLength = 0;
        this.print = print;
    }

    void reset() {
        charCells.clear();
        paintCells.clear();
        printerSet.reset();
        bufferCharLength = 0;
    }

    void addBitImage(int width, int type, byte[] data, int offset, int length) {
        int gh = 0;
        int zy = 0;
        if (type == 0 || type == 32) {
            gh = 2;
        }
        if (type == 1 || type == 33) {
            gh = 1;
        }
        if (type == 0 || type == 1) {
            zy = 3;
        }
        if (type == 32 || type == 33) {
            zy = 1;
        }
        Bitmap bitmap = Bitmap.createBitmap(width * gh, 24, Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawRect(0, 0, bitmap.getWidth(), bitmap.getHeight(), printerSet.backgroundPaint);
        int j = 0, x = 0, y = 0;
        for (int i = offset; i < length + offset; ) {
            if (((0x80 >> j) & data[i]) == (0x80 >> j)) {
                canvas.drawRect(x, y, x + gh, y + zy, printerSet.textPaint);
            }
            j++;
            y += zy;
            if (j == 8) {
                i++;
                j = 0;
            }
            if (y == 24) {
                y = 0;
                x += gh;
            }
        }
        addString("\n");
        addBitmap(bitmap, false);
    }

    void addBarCode(String content, byte codeSystem) throws IllegalArgumentException {
        boolean[] result = null;
        switch (codeSystem) {
            case 0:
            case 65:
                content = '0' + content;
            case 2:
            case 67:
                EAN13Writer w1 = new EAN13Writer();
                result = w1.encode(content);
                break;
            case 1:
            case 66:
                UPCEWriter w2 = new UPCEWriter();
                result = w2.encode(content);
                break;
            case 3:
            case 68:
                EAN8Writer w3 = new EAN8Writer();
                result = w3.encode(content);
                break;
            case 4:
            case 69:
                Code39Writer w4 = new Code39Writer();
                result = w4.encode(content);
                break;
            case 5:
            case 70:
                ITFWriter w5 = new ITFWriter();
                result = w5.encode(content);
                break;
            case 6:
            case 71:
                CodaBarWriter w6 = new CodaBarWriter();
                result = w6.encode(content);
                break;
            case 72:
                Code93Writer w7 = new Code93Writer();
                result = w7.encode(content);
                break;
            case 73:
                StringBuilder stringBuilder = new StringBuilder();
                MyCode128Writer w8 = new MyCode128Writer();
                result = w8.encode(content, stringBuilder);
                content = stringBuilder.toString();
                break;
        }
        if (result != null) {
            int height = printerSet.BarHeight;
            float bar_top = 0;
            float bar_left = PrinterSet.default_BarLRpadding;
            if (printerSet.BarHRIPos == 1) {
                height += Math.ceil(printerSet.textHeight);
                bar_top = height - printerSet.BarHeight;
            }
            if (printerSet.BarHRIPos == 2) {
                height += Math.ceil(printerSet.textHeight);
                bar_top = 0;
            }
            if (printerSet.BarHRIPos == 3) {
                height += Math.ceil(printerSet.textHeight) * 2;
                bar_top = height - printerSet.BarHeight - (int) Math.ceil(printerSet.textHeight);
            }
            Bitmap bitmap = Bitmap.createBitmap(result.length * printerSet.BarWidth + PrinterSet.default_BarLRpadding,
                    height, Config.RGB_565);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawRect(0, 0, bitmap.getWidth(), bitmap.getHeight(), printerSet.backgroundPaint);
            for (boolean b : result) {
                if (b) {
                    canvas.drawRect(bar_left, bar_top, bar_left + printerSet.BarWidth, bar_top + printerSet.BarHeight,
                            printerSet.textPaint);
                }
                bar_left += printerSet.BarWidth;
            }
            if (printerSet.BarHRIPos == 1) {
                printBarHint(content, canvas, printerSet.textHeight, 10, bitmap.getWidth() - 20);
            }
            if (printerSet.BarHRIPos == 2) {
                printBarHint(content, canvas,
                        bitmap.getHeight() - Math.abs(printerSet.textPaint.getFontMetrics().bottom), 10,
                        bitmap.getWidth() - 20);
            }
            if (printerSet.BarHRIPos == 3) {
                printBarHint(content, canvas, printerSet.textHeight, 10, bitmap.getWidth() - 20);
                printBarHint(content, canvas,
                        bitmap.getHeight() - Math.abs(printerSet.textPaint.getFontMetrics().bottom), 10,
                        bitmap.getWidth() - 20);
            }
            addBitmap(bitmap, true);
        }

    }

    private void printBarHint(String content, Canvas canvas, float bottom, float left, int width) {
        char c[] = content.toCharArray();
        float c_l = printerSet.textPaint.measureText(content);
        float space = (width - c_l) / (c.length - 1);
        for (int i = 0; i < c.length; i++) {
            canvas.drawText(c, i, 1, left, bottom, printerSet.textPaint);
            left += space + printerSet.textPaint.measureText(c, i, 1);
        }
    }

    boolean addBitmap(Bitmap bitmap, boolean autoNewLine) {
        if (bitmap != null) {
            if (printerSet.posX >= 0) {
                bufferCharLength += printerSet.posX - getCurentX() - printerSet.xLinePadding;
            }
            if (autoNewLine && (bufferCharLength
                    + (paintCells.size() == 0 ? 0 : paintCells.get(paintCells.size() - 1).measureLength)
                    + bitmap.getWidth()) > printerSet.getLineSpaceWidth()) {
                if (charCells.size() != 0) {
                    CharCell charCell[] = new CharCell[charCells.size()];
                    charCells.toArray(charCell);
                    byte[] levels = new byte[charCell.length];
                    for (int j = 0; j < levels.length; j++) {
                        levels[j] = charCell[j].isRtl;
                    }
                    Bidi.reorderVisually(levels, 0, charCell, 0, levels.length);
                    Paint temp_paint = charCell[0].paint;
                    Queue<CharCell> customerPriorityQueue = new PriorityQueue<>(charCell.length, Order);
                    for (int j = 0; j < charCell.length; j++) {
                        if (temp_paint != charCell[j].paint) {
                            printerSet.draw(customerPriorityQueue);
                            temp_paint = charCell[j].paint;
                            j--;
                            continue;
                        }
                        if (temp_paint == charCell[j].paint) {
                            customerPriorityQueue.offer(charCell[j]);
                        }
                        if (j == charCell.length - 1) {
                            printerSet.draw(customerPriorityQueue);
                        }
                    }
                }
                bufferCharLength = 0;
                charCells.clear();
                paintCells.clear();
                byte data[] = printerSet.clearLineToByte();
                if (data != null)
                    print.bytePrinting(data);
            }
            if (paintCells.size() != 0) {
                bufferCharLength += paintCells.get(paintCells.size() - 1).measureLength;
            }
            PaintCell paintCell = new PaintCell(bitmap, printerSet.textPaint, printerSet.posX);
            paintCells.add(paintCell);
            CharCell cell = new CharCell(bitmap, charCells.size(),
                    (byte) (printerSet.isRtl == 0 ? 1 : printerSet.isRtl), printerSet.baseLine, bitmap.getHeight(),
                    printerSet.posX);
            charCells.add(cell);
            printerSet.posX = -1;
            return true;
        }
        return false;
    }

    boolean addString(String s) {
        if (s != null && !s.equals("")) {
            char[] char_s = s.toCharArray();
            printerSet.isRtl(new char[]{char_s[0]});
            Bidi bidi = new Bidi(s, printerSet.isRtl == 2 ? Bidi.DIRECTION_RIGHT_TO_LEFT : Bidi.DIRECTION_LEFT_TO_RIGHT);
            for (int i = 0; i < char_s.length; i++) {
                boolean isOverSize = false;
                printerSet.isRtl(new char[]{char_s[i]});
                CharCell cell = new CharCell(char_s[i], printerSet.textPaint, charCells.size(),
                        (byte) bidi.getLevelAt(i), printerSet.baseLine, printerSet.textHeight, printerSet.posX);
                charCells.add(cell);
                if (printerSet.posX >= 0) {
                    bufferCharLength += printerSet.posX - getCurentX() - printerSet.xLinePadding;
                    printerSet.posX = -1;
                }
                if (paintCells.size() == 0) {
                    PaintCell paintCell = new PaintCell(char_s[i], printerSet.textPaint);
                    paintCells.add(paintCell);
                    if (paintCell.measureLength > printerSet.getLineSpaceWidth()) {
                        isOverSize = true;
                    }
                } else {
                    if (!paintCells.get(paintCells.size() - 1).addChar(printerSet.textPaint, char_s[i])) {
                        bufferCharLength += paintCells.get(paintCells.size() - 1).measureLength;
                        PaintCell paintCell = new PaintCell(char_s[i], printerSet.textPaint);
                        paintCells.add(paintCell);
                    }
                }
                boolean newLine = bufferCharLength + paintCells.get(paintCells.size() - 1).measureLength > printerSet
                        .getLineSpaceWidth();
                if (newLine || char_s[i] == 0x0A) {
                    if (newLine && !isOverSize && char_s[i] != 0x0A) {
                        i--;
                        charCells.remove(charCells.size() - 1);
                    }
                    if (charCells.size() != 0) {
                        CharCell charCell[] = new CharCell[charCells.size()];
                        charCells.toArray(charCell);
                        byte[] levels = new byte[charCell.length];
                        for (int j = 0; j < levels.length; j++) {
                            levels[j] = charCell[j].isRtl;
                        }
                        Bidi.reorderVisually(levels, 0, charCell, 0, levels.length);
                        Paint temp_paint = charCell[0].paint;
                        Queue<CharCell> customerPriorityQueue = new PriorityQueue<>(charCell.length, Order);
                        for (int j = 0; j < charCell.length; j++) {
                            if (temp_paint != charCell[j].paint) {
                                printerSet.draw(customerPriorityQueue);
                                temp_paint = charCell[j].paint;
                                j--;
                                continue;
                            }
                            if (temp_paint == charCell[j].paint) {
                                customerPriorityQueue.offer(charCell[j]);
                            }
                            if (j == charCell.length - 1) {
                                printerSet.draw(customerPriorityQueue);
                            }
                        }
                    }
                    bufferCharLength = 0;
                    charCells.clear();
                    paintCells.clear();
                    byte data[] = printerSet.clearLineToByte();
                    if (data != null)
                        print.bytePrinting(data);
                }
            }
            return true;
        }
        return false;
    }

    void printQrBar() {
        if (printerSet.qr_data != null) {
            QRCode qrCode = null;
            try {
                qrCode = MyQrEncoder.encode(printerSet.qr_data, printerSet.errorCorrectionLevel, null);
            } catch (WriterException e) {
                e.printStackTrace();
            }
            if (qrCode != null) {
                int w = qrCode.getMatrix().getWidth();
                int h = qrCode.getMatrix().getHeight();
                Bitmap bitmap = Bitmap.createBitmap(w * printerSet.QrSize, h * printerSet.QrSize, Config.RGB_565);
                Canvas canvas = new Canvas(bitmap);
                canvas.drawRect(0, 0, bitmap.getWidth(), bitmap.getHeight(), printerSet.backgroundPaint);
                int x = 0, y = 0;
                for (int j = 0; j < h; j++) {
                    for (int i = 0; i < w; i++) {
                        if (qrCode.getMatrix().get(i, j) == 1) {
                            canvas.drawRect(x, y, x + printerSet.QrSize, y + printerSet.QrSize, printerSet.textPaint);
                        }
                        x += printerSet.QrSize;
                    }
                    y += printerSet.QrSize;
                    x = 0;
                }
                addBitmap(bitmap, true);
            }
        }
    }

    boolean isEmpty() {
        return charCells.isEmpty();
    }

    float getCurentX() {
        if (paintCells.size() != 0) {
            return bufferCharLength + paintCells.get(paintCells.size() - 1).measureLength;
        } else {
            return bufferCharLength;
        }
    }

    //光栅转位图并输出
    void rasterToBitmap(byte[] data, int w, int h, int flag){
        int x = 0, y = 0;
        int skip_x = 1, skip_y = 1;
        if(flag == 1 || flag == 49){//倍宽
            skip_x = 2;
        }else if(flag == 2 || flag == 50){//倍高
            skip_y = 2;
        }else if(flag == 3 || flag == 51){//倍高倍宽
            skip_x = 2;
            skip_y = 2;
        }
        Bitmap bitmap = Bitmap.createBitmap(w*8*skip_x, h*skip_y, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        canvas.drawRect(0, 0, bitmap.getWidth(), bitmap.getHeight(), paint);
        paint.setColor(Color.BLACK);
        for(int j  = 0; j < h; j++){
            for(int i = 0; i < w; i++){
                int ret = data[j*w+i];
                for(int n = 0; n < 8; n++){
                    if((ret&(1<<(7-n))) > 0){
                        canvas.drawRect(x, y, x + skip_x, y + skip_y, paint);
                    }
                    x+=skip_x;
                }
            }
            y+=skip_y;
            x = 0;
        }
        addBitmap(bitmap, true);
    }

    /**
     * 打表格
     *
     * @param colsTextArr  每列文本
     * @param colsWidthArr 每列宽度 字符为单位
     * @param colsAlign    对齐格式	0左对齐 1 居中  2右对齐
     * @return true 成功 false 失败
     */
     boolean addTableString(String[] colsTextArr, int[] colsWidthArr, int[] colsAlign) {
        boolean needAddRow = false;                            //需要增加一行
        int rows = 1;                                        //可能总行数 根据情况增加，必然为1行
        int size = colsTextArr.length;                        //总列数
        int[] cols_num = new int[size];                        //每列文本下标缓存
        int weight = 0;                                        //权重
        float unit_width;                                    //单位宽度

        for (int unit : colsWidthArr) {
            weight += unit;
        }
        unit_width = printerSet.drawSpace / weight;

        for (int row = 0; row < rows; row++) {                //处理每行
            for (int i = 0; i < size; i++) {                    //处理每列
                char[] cols_chars = colsTextArr[i].toCharArray();
                int length = cols_chars.length;
                int current_num = cols_num[i];
                StringBuilder cols_sb = new StringBuilder();
                float width = unit_width * colsWidthArr[i];

                while (current_num < length) {
                    if (colsOverflow(cols_sb, cols_chars[current_num], width)) {
                        printCols(cols_sb.toString(), colsWidthArr, colsAlign, i, unit_width);
                        cols_num[i] = current_num;
                        if (!needAddRow) {
                            needAddRow = true;
                        }
                        break;
                    } else {
                        cols_sb.append(cols_chars[current_num]);
                    }
                    current_num++;
                }
                if (current_num == length) {
                    printCols(cols_sb.toString(), colsWidthArr, colsAlign, i, unit_width);
                    cols_num[i] = current_num;
                }
            }
            addString("\n");
            if (needAddRow) {
                ++rows;
                needAddRow = false;
            }
        }
        return true;
    }


    //列文本是否溢出
    private boolean colsOverflow(StringBuilder text, char a, float width) {
        StringBuilder sb = new StringBuilder(text);
        float size = printerSet.textPaint.measureText(sb.append(a).toString());
        return (size >= width);
    }

    //计算总共所占权重
    private int calculate(int[] width, int num) {
        int size = 0;
        for (int i = 0; i <= num; i++) {
            size += width[i];
        }
        return size;
    }

    /**
     * 打印每列文字，打印的肯定是没有溢出列的
     *
     * @param text         //打印的文本
     * @param colsWidthArr //权重数组
     * @param colsAlign    //对齐模式数组	0、1、2
     * @param col          //第几列
     * @param unit_width   //权重单位宽度
     */
    private void printCols(String text, int[] colsWidthArr, int[] colsAlign, int col, float unit_width) {
        float pos;    //列开始结束位置
        float width = unit_width * colsWidthArr[col];
        float size = printerSet.textPaint.measureText(text);
        float corrected_value = printerSet.textSize * printerSet.TextTimesWidth / 4;    //计算字符串长度时可能会出现误差，故增加此修正值

        pos = calculate(colsWidthArr, col) * unit_width + printerSet.xLinePadding;
        switch (colsAlign[col]) {
            case 0://文本居左
                break;
            case 1://文本居中
                printerSet.posX = (width - size) / 2 + pos - width - corrected_value;
                break;
            case 2://文本居右
                printerSet.posX = pos - size - corrected_value;
                break;
        }
        addString(text);
        printerSet.posX = pos;
    }

    public byte[] showLcdString(String text){
        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(32);
        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.WHITE);
        Bitmap bitmap_line = Bitmap.createBitmap(128, 40, Config.RGB_565);
        Canvas canvas_line = new Canvas(bitmap_line);
        canvas_line.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG));
        canvas_line.drawRect(0, 0, 128, 40, backgroundPaint);
        canvas_line.drawText(text, 0, 40 - Math.abs(textPaint.getFontMetrics().bottom), textPaint);
        return printerSet.clearLcdToByte(bitmap_line);
    }

    public byte[] showLcdString(String text1, String text2){
        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.WHITE);
        Bitmap bitmap_line = Bitmap.createBitmap(128, 40, Config.RGB_565);
        Canvas canvas_line = new Canvas(bitmap_line);
        canvas_line.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG));
        canvas_line.drawRect(0, 0, 128, 40, backgroundPaint);
        Paint textPaint = new Paint();
        if(text1 != null && !text1.equals("")){
            textPaint.setColor(Color.BLACK);
            textPaint.setTextSize(16);
            canvas_line.drawText(text1, 0, 20 - Math.abs(textPaint.getFontMetrics().bottom), textPaint);
        }
        if(text2 != null && !text2.equals("")){
            textPaint.setColor(Color.BLACK);
            textPaint.setTextSize(16);
            canvas_line.drawText(text2, 0, 40 - Math.abs(textPaint.getFontMetrics().bottom), textPaint);
        }
        return printerSet.clearLcdToByte(bitmap_line);
    }

    public byte[] showLcdBitmap(Bitmap bitmap){
        Bitmap bitmap_line = Bitmap.createBitmap(128, 40, Config.RGB_565);
        Canvas canvas_line = new Canvas(bitmap_line);
        canvas_line.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG));
        Paint paint = new Paint();
        canvas_line.drawBitmap(bitmap, 0, 0, paint);
        return printerSet.clearLcdToByte(bitmap_line);
    }

}
