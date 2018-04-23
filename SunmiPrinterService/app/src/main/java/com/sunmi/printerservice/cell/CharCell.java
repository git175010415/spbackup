package com.sunmi.printerservice.cell;

import android.graphics.Bitmap;
import android.graphics.Paint;

public class CharCell {
    public char c;
    public int offset;
    public Paint paint;
    public byte isRtl;
    public float baseLine;
    public float textHeight;
    public  boolean isBitmap;
    public Bitmap bitmap;
    public float PosX;

    public CharCell(Bitmap bitmap,int offset,byte isRtl,float baseLine,float textHeight,float PosX)
    {
        isBitmap=true;
        this.bitmap=bitmap;
        this.offset=offset;
        this.isRtl=isRtl;
        this.baseLine=baseLine;
        this.textHeight=textHeight;
        this.PosX=PosX;
    }

    public CharCell(char c,Paint paint,int offset,byte isRtl,float baseLine,float textHeight,float PosX)
    {
        isBitmap=false;
        this.offset=offset;
        this.paint=paint;
        this.c=c;
        this.isRtl=isRtl;
        this.baseLine=baseLine;
        this.textHeight=textHeight;
        this.PosX=PosX;
    }
}
