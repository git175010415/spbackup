package com.sunmi.printerservice.cell;

import android.graphics.Bitmap;
import android.graphics.Paint;

/**
 * TODO<请描述这个类是干什么的>
 * @author 郭晗
 * @versionCode 1 <每次修改提交前+1>
 */
public class PaintCell {
    public StringBuilder stringBuilder;
    public Paint paint;
    public float measureLength;
    public float finallyHeight;
    public float finallyBaseLine;
    public  boolean isBitmap;
    public Bitmap bitmap;
    public float PosX =-1;


    public PaintCell(char c,Paint paint)
    {
        stringBuilder=new StringBuilder();
        stringBuilder.append(c);
        this.paint=paint;
        isBitmap=false;
        measureLength=paint.measureText(stringBuilder.toString());
    }
    public PaintCell(Bitmap bitmap,Paint paint,float PosX)
    {
        isBitmap=true;
        this.bitmap=bitmap;
        measureLength=bitmap.getWidth();
        finallyHeight=bitmap.getHeight();
        this.paint=paint;
        this.PosX=PosX;
    }
    public PaintCell(StringBuilder stringBuilder,Paint paint,float finallyHeight,float finallyBaseLine,float PosX)
    {
        isBitmap=false;
        this.paint=paint;
        this.finallyHeight=finallyHeight;
        this.finallyBaseLine=finallyBaseLine;
        this.stringBuilder=stringBuilder;
        this.PosX=PosX;
    }

    public boolean addChar(Paint paint,char c)
    {
        if(this.paint==paint&&isBitmap==false)
        {
            stringBuilder.append(c);
            measureLength=paint.measureText(stringBuilder.toString());
            return true;
        }
        return false;
    }
}
