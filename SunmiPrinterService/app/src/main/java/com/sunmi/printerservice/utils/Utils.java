package com.sunmi.printerservice.utils;

import android.graphics.Bitmap;
import android.graphics.Color;

public class Utils {

    //crc校验
    public static void CRC_16(byte[] bytes) {
        int crc = 0;
        for (int i = 3; i < bytes.length - 2; i++) {
            crc ^= (bytes[i] << 8);
            for (int j = 0; j < 8; j++) {
                if ((crc & (0x8000)) != 0) {
                    crc = crc << 1 ^ 0x1021;
                } else {
                    crc = crc << 1;
                }
            }
        }
        bytes[bytes.length - 2] = (byte) (crc >> 8);
        bytes[bytes.length - 1] = (byte) crc;
    }

    //crc校验
    public static int CRC_16(byte[] datas, int start, int length) {
        int crc = 0xffff;
        int unit;
        for (int i = start; i < length; i++) {
            unit = datas[i];
            crc ^= (unit << 8);
            for (int j = 0; j < 8; j++) {
                if ((crc & (0x8000)) != 0) {
                    crc = crc << 1 ^ 0x1021;
                } else {
                    crc = crc << 1;
                }
            }
        }
        return crc&0xffff;
    }

    public static String getHexStringFromBytes(byte[] data) {
        if (data == null || data.length <= 0) {
            return null;
        }
        String hexString = "0123456789ABCDEF";
        int size = data.length * 2;
        StringBuilder sb = new StringBuilder(size);
        for (int i = 0; i < data.length; i++) {
            sb.append(hexString.charAt((data[i] & 0xF0) >> 4));
            sb.append(hexString.charAt(data[i] & 0x0F));
        }
        return sb.toString();
    }

    public static byte[] BitmapToByte(Bitmap b) {
        int ww = b.getWidth();
        int h = b.getHeight();
        int w = (ww - 1) / 8 + 1;
        byte[] data = new byte[h * w + 8];
        data[0] = 0x1D;
        data[1] = 0x76;
        data[2] = 0x30;
        data[3] = 0x00;
        data[4] = (byte) w;// xL
        data[5] = (byte) (w >> 8);// xH
        data[6] = (byte) h;
        data[7] = (byte) (h >> 8);
        getAllPixels_gh(b, data);
        return data;
    }

    //bitmap彩图转抖动灰度
    public static Bitmap convertToDithering(Bitmap img) {
        int width = img.getWidth();         //获取位图的宽
        int height = img.getHeight();       //获取位图的高
        int[] pixels = new int[width * height]; //通过位图的大小创建像素点数组
        img.getPixels(pixels, 0, width, 0, 0, width, height);
        int[] gray=new int[height*width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int color = pixels[width * i + j];
                int a= Color.alpha(color);
                if(a == 0)
                {
                    color=0xFFFFFFFF;
                }
                int r = Color.red(color);
                int g = Color.green(color);
                int b = Color.blue(color);
                int red = (r*19595 + g*38469 +b*7472) >> 16;
                gray[width*i+j]=red;
            }
        }
        int e=0;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int g=gray[width*i+j];
                if (g>=128) {
                    pixels[width*i+j]=0xffffffff;
                    e=g-255;
                }else {
                    pixels[width*i+j]=0xff000000;
                    e=g-0;
                }
                if (j<width-1&&i<height-1) {
                    gray[width*i+j+1]+=3*e/8;
                    gray[width*(i+1)+j]+=3*e/8;
                    gray[width*(i+1)+j+1]+=e/4;
                }else if (j==width-1&&i<height-1) {
                    gray[width*(i+1)+j]+=3*e/8;
                }else if (j<width-1&&i==height-1) {
                    gray[width*(i)+j+1]+=e/4;
                }
            }
        }
        Bitmap mBitmap=Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        mBitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return mBitmap;
    }

    //bitmap彩图转黑白
    public static Bitmap convertToBlackWhite(Bitmap bmp, int value) {
        int width = bmp.getWidth(); // 获取位图的宽
        int height = bmp.getHeight(); // 获取位图的高
        int[] pixels = new int[width * height]; // 通过位图的大小创建像素点数组

        bmp.getPixels(pixels, 0, width, 0, 0, width, height);
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int grey = pixels[width * i + j];
                int red = ((grey & 0x00FF0000) >> 16);
                int green = ((grey & 0x0000FF00) >> 8);
                int blue = (grey & 0x000000FF);
                pixels[width * i + j] = RGB2Gray(red, green, blue, value) == 0 ? 0xffffffff : 0xff000000;
            }
        }
        Bitmap newBmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        newBmp.setPixels(pixels, 0, width, 0, 0, width, height);
        return newBmp;
    }

    private static void getAllPixels_gh(Bitmap bit, byte[] gh) {
        int k = bit.getWidth() * bit.getHeight();
        int[] pixels = new int[k];
        bit.getPixels(pixels, 0, bit.getWidth(), 0, 0, bit.getWidth(), bit.getHeight());
        int j = 7;
        int index = 8;
        for (int i = 0; i < pixels.length; i++) {
            int clr = pixels[i];
            int red = (clr & 0x00ff0000) >> 16;
            int green = (clr & 0x0000ff00) >> 8;
            int blue = clr & 0x000000ff;
            if (j == -1) {
                j = 7;
                index++;
            }
            gh[index] = (byte) (gh[index] | (RGB2Gray(red, green, blue, 150) << j));
            j--;
        }
    }

    private static byte RGB2Gray(int r, int g, int b, int level) {
        return (int) (0.29900 * r + 0.58700 * g + 0.11400 * b)< level ? (byte) 1 : (byte) 0;
    }
}
