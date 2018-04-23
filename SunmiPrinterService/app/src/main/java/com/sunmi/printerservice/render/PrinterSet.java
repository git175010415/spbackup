package com.sunmi.printerservice.render;

import java.text.Bidi;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.sunmi.printerservice.cell.CharCell;
import com.sunmi.printerservice.cell.PaintCell;
import com.sunmi.printerservice.entity.ServiceValue;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

public class PrinterSet {

    public static final float default_textSize = 24;
    public static final float default_lineHeight = 32;
    public static final int default_BarLRpadding = 10;

    public float textSize;
    public float xLinePadding;
    public float drawSpace;
    public int canvasWidth;
    public int canvasHeight;
    public float xWorldPadding;
    public float lineHeight;
    public float baseLine;
    public Paint textPaint;
    public Paint backgroundPaint;
    public float textHeight;
    public int justificationMode;
    public boolean isFakeBoldText;
    public boolean isUnderlineText;
    public int TextTimesHeight;
    public int TextTimesWidth;
    public boolean reversePrintingMode;
    public boolean isSingleByteChar;
    public String codeSystem;
    public boolean isTextTimes;
    public Bitmap bitmap_line;
    public Canvas canvas_line;
    public int isRtl;
    public float scaleX;
    public float x;
    public float y;
    public float posX;
    public int BarHRIPos;
    public int BarWidth;
    public int BarHeight;
    public int QrSize;
    public ErrorCorrectionLevel errorCorrectionLevel;
    public byte[] qr_data;
    public float tabWidth;
    public ArrayList<Byte> tabPos;
    private ServiceValue mServiceValue;
    private int mServiceValueId;

    public PrinterSet(ServiceValue serviceValue) {
        mServiceValue = serviceValue;
        mServiceValueId = 0;
        xLinePadding = 0;
        canvasWidth = mServiceValue.getPaper();
        drawSpace = canvasWidth - xLinePadding;
        canvasHeight = canvasWidth * 3;
        xWorldPadding = 0;
        scaleX = 1f;
        textPaint = new Paint();
        //textPaint.setAntiAlias(true);
        textPaint.setColor(Color.BLACK);
        textPaint.setTypeface(mServiceValue.getTypeface());
        textSize = default_textSize;
        textPaint.setTextSize(textSize);
        textPaint.setLetterSpacing(xWorldPadding);
        textPaint.setTextScaleX(scaleX);
        justificationMode = -1;
        lineHeight = default_lineHeight;
        textHeight = Math.abs(textPaint.getFontMetrics().bottom) + Math.abs(textPaint.getFontMetrics().top);
        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.WHITE);
        isFakeBoldText = false;
        isUnderlineText = false;
        TextTimesHeight = 1;
        TextTimesWidth = 1;
        reversePrintingMode = false;
        isSingleByteChar = false;
        codeSystem = "GB18030";
        isTextTimes = true;
        bitmap_line = Bitmap.createBitmap(canvasWidth, canvasHeight, Config.RGB_565);
        canvas_line = new Canvas(bitmap_line);
        //canvas_line.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG| Paint.FILTER_BITMAP_FLAG));
        canvas_line.drawRect(0, 0, canvasWidth, canvasHeight, backgroundPaint);
        baseLine = canvasHeight - Math.abs(textPaint.getFontMetrics().bottom);
        isRtl = 0;
        x = 0;
        y = 0;
        posX = -1;
        BarHRIPos = 0;
        BarWidth = 2;
        BarHeight = 162;
        QrSize = 4;
        errorCorrectionLevel = ErrorCorrectionLevel.L;
        tabWidth = textSize * TextTimesWidth;
        tabPos = null;
    }

    public void reset() {
        xLinePadding = 0;
        xWorldPadding = 0;
        scaleX = 1f;
        isRtl = 0;
        x = 0;
        drawSpace = canvasWidth - xLinePadding;
        textSize = default_textSize;
        justificationMode = -1;
        y = 0;
        backgroundPaint.setColor(Color.WHITE);
        isSingleByteChar = false;
        codeSystem = "GB18030";
        canvas_line.drawRect(0, 0, canvasWidth, canvasHeight, backgroundPaint);
        posX = -1;
        BarHRIPos = 0;
        BarWidth = 2;
        BarHeight = 162;
        QrSize = 4;
        errorCorrectionLevel = ErrorCorrectionLevel.L;
        tabWidth = textSize * TextTimesWidth;
        tabPos = null;
        isTextTimes = true;
        TextTimesHeight = 1;
        TextTimesWidth = 1;
        isFakeBoldText = false;
        isUnderlineText = false;
        lineHeight = default_lineHeight;
        reversePrintingMode = false;
        //画笔复位设置
        refresh();
    }

    void refresh() {
        textPaint = new Paint();
        //textPaint.setAntiAlias(true);
        textPaint.setColor(Color.BLACK);
        textPaint.setLetterSpacing(xWorldPadding / textSize);
        textPaint.setTypeface(mServiceValue.getTypeface());
        if (mServiceValue.isTextTimes()) {
            int realTimesHeight = mServiceValue.getTextTimesHeight() == 1?TextTimesHeight:mServiceValue.getTextTimesHeight();
            int realTimesWidth = mServiceValue.getTextTimesWidth() == 1?TextTimesWidth:mServiceValue.getTextTimesWidth();
            textPaint.setTextSize(textSize * realTimesHeight);
            textPaint.setTextScaleX(((float) realTimesWidth) / realTimesHeight);
        } else {
            if (isTextTimes) {
                textPaint.setTextSize(textSize * TextTimesHeight);
                textPaint.setTextScaleX(((float) TextTimesWidth) / TextTimesHeight);
            } else {
                textPaint.setTextSize(textSize);
                textPaint.setTextScaleX(1);
            }
        }
        if (mServiceValue.isUnderlineText()) {
            textPaint.setUnderlineText(mServiceValue.isUnderlineText());
        } else {
            textPaint.setUnderlineText(isUnderlineText);
        }
        if (mServiceValue.isFakeBoldText()) {
            textPaint.setFakeBoldText(mServiceValue.isFakeBoldText());
        } else {
            textPaint.setFakeBoldText(isFakeBoldText);
        }
        textHeight = Math.abs(textPaint.getFontMetrics().bottom) + Math.abs(textPaint.getFontMetrics().top);
        baseLine = canvasHeight - Math.abs(textPaint.getFontMetrics().bottom);
    }

    //切换打印纸
    private void setCanvasWidth() {
        canvasWidth = mServiceValue.getPaper();
        canvasHeight = canvasWidth * 3;
        xLinePadding = 0;
        x = 0;
        drawSpace = canvasWidth - xLinePadding;
        bitmap_line = Bitmap.createBitmap(canvasWidth, canvasHeight, Config.RGB_565);
        canvas_line = new Canvas(bitmap_line);
        canvas_line.drawRect(0, 0, canvasWidth, canvasHeight, backgroundPaint);
        baseLine = canvasHeight - Math.abs(textPaint.getFontMetrics().bottom);
    }

    void draw(Queue<CharCell> queue) {
        StringBuilder stringBuilder = null;
        Queue<PaintCell> arrayList = new LinkedBlockingQueue<>();
        while (!queue.isEmpty()) {
            CharCell cc = queue.poll();
            if (cc.isBitmap) {
                arrayList.offer(new PaintCell(cc.bitmap, cc.paint, cc.PosX));
                stringBuilder = null;
            } else {
                if (stringBuilder == null || cc.PosX >= 0) {
                    stringBuilder = new StringBuilder();
                    arrayList.offer(new PaintCell(stringBuilder, cc.paint, cc.textHeight, cc.baseLine, cc.PosX));
                }
                stringBuilder.append(cc.c);
            }
        }
        while (!arrayList.isEmpty()) {
            PaintCell o = arrayList.poll();
            if (o.isBitmap) {
                if (o.PosX >= 0) {
                    x = o.PosX;
                }
                if (x < canvasWidth) {
                    if (o.bitmap.getHeight() > canvasHeight) {
                        Bitmap temp_b = Bitmap.createBitmap(canvasWidth, o.bitmap.getHeight(), Config.RGB_565);
                        Canvas temp_c = new Canvas(temp_b);
                        temp_c.drawRect(0, 0, temp_b.getWidth(), temp_b.getHeight(), backgroundPaint);
                        temp_c.drawBitmap(bitmap_line, 0, temp_b.getHeight() - canvasHeight, null);
                        canvasHeight = temp_b.getHeight();
                        bitmap_line = temp_b;
                        canvas_line = temp_c;
                        baseLine = canvasHeight - Math.abs(textPaint.getFontMetrics().bottom);
                    }
                    canvas_line.drawBitmap(o.bitmap, new Rect(0, 0, o.bitmap.getWidth(), o.bitmap.getHeight()),
                            new Rect((int) Math.ceil(x), canvasHeight - o.bitmap.getHeight(),
                                    (int) Math.ceil(x) + o.bitmap.getWidth(), canvasHeight),
                            o.paint);
                    x += o.bitmap.getWidth();
                }
                if (x > canvasWidth) {
                    x = canvasWidth;
                }
                if (y == 0) {
                    if (mServiceValue.isSetLineHeight() && mServiceValue.compareId(mServiceValueId)) {
                        y = mServiceValue.getLineHeight();
                    } else {
                        y = lineHeight;
                    }
                }
                if (o.finallyHeight > y) {
                    y = o.finallyHeight;
                }
            } else {
                float l = o.paint.measureText(o.stringBuilder.toString());
                if (o.PosX >= 0) {
                    x = o.PosX;
                }
                if (x < canvasWidth) {
                    canvas_line.drawText(o.stringBuilder.toString(), x, o.finallyBaseLine, o.paint);
                    x += l;
                }
                if (x > canvasWidth) {
                    x = canvasWidth;
                }
                if (y == 0) {
                    if (mServiceValue.isSetLineHeight() && mServiceValue.compareId(mServiceValueId)) {
                        y = mServiceValue.getLineHeight();
                    } else {
                        y = lineHeight;
                    }
                }
                if (l != 0 && o.finallyHeight > y) {
                    y = o.finallyHeight;
                }
            }
        }
    }

    byte[] clearLcdToByte(Bitmap lcdBitmap) {
        int[] temp = new int[128 * 40];
        lcdBitmap.getPixels(temp, 0, 128, 0, 0, 128, 40);
        byte[] data = new byte[128 * 5];
        for (int x = 0; x < 128; x++) {
            for (int y = 0; y < 40; y++) {
                int clr = temp[128 * y + x];
                int red = (clr & 0x00ff0000) >> 16;
                int green = (clr & 0x0000ff00) >> 8;
                int blue = clr & 0x000000ff;
                data[128 * (y / 8) + x] = (byte) ((RGB2Gray(red, green, blue) << (y % 8)) | data[128 * (y / 8) + x]);
            }
        }
        return data;
    }

    byte[] clearLineToByte() {
        if ((int) Math.ceil(y) > 0) {
            int ww = canvasWidth;
            int h = (int) Math.ceil(y);
            int www = (int) Math.ceil(x);
            int w = (ww - 1) / 8 + 1;
            byte[] data = new byte[h * w + 8];
            data[0] = 0x1D;
            data[1] = 0x76;
            data[2] = 0x30;
            data[3] = 0x00;
            data[4] = (byte) w;
            data[5] = (byte) (w >> 8);
            data[6] = (byte) h;
            data[7] = (byte) (h >> 8);
            int[] temp = new int[h * canvasWidth];
            bitmap_line.getPixels(temp, 0, canvasWidth, 0, canvasHeight - h, canvasWidth, h);
            float xx = 0;
            if (justificationMode == 1) {
                xx = (canvasWidth - x) / 2;
            }
            if (justificationMode == 2) {
                xx = canvasWidth - x;
            }
            int temp_ = (int) Math.floor(xx);
            for (int i = 0; i < h; i++) {
                for (int j = temp_; j < www + temp_; j++) {
                    int clr = temp[canvasWidth * i + j - temp_];
                    int red = (clr & 0x00ff0000) >> 16;
                    int green = (clr & 0x0000ff00) >> 8;
                    int blue = clr & 0x000000ff;
                    data[(canvasWidth * i + j) / 8 + 8] = (byte) (data[(canvasWidth * i + j) / 8 + 8]
                            | (RGB2Gray(red, green, blue) << (7 - ((canvasWidth * i + j) % 8))));
                }
            }
            canvas_line.drawRect(0, 0, canvasWidth, canvasHeight, backgroundPaint);
            x = xLinePadding;
            y = 0;
            return data;
        }
        return null;
    }

    //阈值设置为200，若小于200emoji表情中的颜色将丢失
    private byte RGB2Gray(int r, int g, int b) {
        boolean reverse = mServiceValue.isReversePrintingMode() && mServiceValue.compareId(mServiceValueId) || reversePrintingMode;
        return (reverse ? ((int) (0.29900 * r + 0.58700 * g + 0.11400 * b) > 200)
                : ((int) (0.29900 * r + 0.58700 * g + 0.11400 * b) < 200)) ? (byte) 1 : (byte) 0;
    }

    Bitmap clearLineToBitmap(int offset) {
        if ((int) Math.ceil(y) + offset > 0) {
            Bitmap bitmap = Bitmap.createBitmap(canvasWidth, (int) Math.ceil(y) + offset, Config.RGB_565);
            Canvas canvas = new Canvas(bitmap);
            float xx = 0;
            int yy = (int) Math.ceil(y);
            if (justificationMode == 1) {
                xx = (canvasWidth - x) / 2;
            }
            if (justificationMode == 2) {
                xx = canvasWidth - x;
            }
            canvas.drawRect(0, 0, canvasWidth, (int) Math.ceil(y), backgroundPaint);
            canvas.drawBitmap(bitmap_line,
                    new Rect(0, canvasHeight - (int) Math.ceil(y), (int) Math.ceil(x), canvasHeight),
                    new Rect((int) Math.ceil(xx), 0, (int) Math.ceil(xx) + (int) Math.ceil(x), yy), textPaint);
            canvas_line.drawRect(0, 0, canvasWidth, canvasHeight, backgroundPaint);
            x = xLinePadding;
            y = 0;
            return bitmap;
        }
        return null;
    }

    void setxLinePadding(float a) {
        if (a < canvasWidth) {
            xLinePadding = a;
            drawSpace = canvasWidth - a;
        } else {
            xLinePadding = canvasWidth;
            drawSpace = 0;
        }
        x = xLinePadding;
    }

    void setDrawSpace(float a) {
        if (a < canvasWidth - xLinePadding) {
            drawSpace = a;
        } else {
            drawSpace = canvasWidth - xLinePadding;
        }
    }

    float getLineSpaceWidth() {
        return drawSpace;
    }

    boolean isRtl(char[] s) {
        boolean b = Bidi.requiresBidi(s, 0, 1);
        if (isRtl == 0) {
            isRtl = (b ? 2 : 1);
            if (justificationMode == -1) {
                justificationMode = (isRtl == 1 ? 0 : 2);
            }
        }
        return b;
    }

    public void runtime() {
        if (mServiceValue.getMyId() != mServiceValueId) {
            mServiceValueId = mServiceValue.getMyId();
            setCanvasWidth();
            refresh();
        }
    }

}
