package com.sunmi.printerservice.commandbean;

import android.os.RemoteException;

import com.sunmi.printerservice.exception.ExceptionConst;
import com.sunmi.printerservice.exception.PrinterException;
import com.sunmi.printerservice.render.BitmapCreator;
import com.sunmi.printerservice.utils.C;

import java.io.ByteArrayOutputStream;

import woyou.aidlservice.jiuiv5.ICallback;

public class TableTask extends ITask {
    private String[] colsTextArr;
    private int[] colsWidthArr;
    private int[] colsAlign;
    private int flag;    //1 原始表格 2特殊表格
    private int tmp;

    public TableTask(BitmapCreator bitmapCreator, String[] colsTextArr, int[] colsWidthArr, int[] colsAlign, int flag,
                     ICallback callback) throws PrinterException {
        this.bitmapCreator = bitmapCreator;
        this.createTime = System.currentTimeMillis();
        this.callback = callback;
        this.colsTextArr = colsTextArr;
        this.colsWidthArr = colsWidthArr;
        this.colsAlign = colsAlign;
        this.flag = flag;

        try{
            if(colsTextArr.length != colsWidthArr.length || colsWidthArr.length != colsAlign.length){
                throw new PrinterException(ExceptionConst.CODEFAILED, ExceptionConst.CODEFAILED_MSG);
            }
            for (String i : colsTextArr) {
                tmp += i.length() * 2;
            }
        }catch (NullPointerException e){
            throw new PrinterException(ExceptionConst.NullPointer, ExceptionConst.NullPointer_MSG);
        }

        if (bitmapCreator.getCurrentMemory() + tmp > C.BC_MAXMEMORY) {
            throw new PrinterException(ExceptionConst.ADDTASKFAILED, ExceptionConst.ADDTASKFAILED_MSG);
        } else {
            bitmapCreator.addCurrentMemory(tmp);
        }
    }

    @Override
    public void run() {
        bitmapCreator.runtime(createTime);
        boolean res = false;
        if (flag == 1) {
            byte[] data;
            try {
                data = getColumnsText(colsTextArr, colsWidthArr, colsAlign, (int) bitmapCreator.getFontSize() / 2, (int) bitmapCreator.getFontSize() / 4);
                if (data != null) {
                    res = bitmapCreator.sendRAWData(data, callback);
                }
            } catch (Exception e) {
                res = false;
                try {
                    if (callback != null) {
                        callback.onRaiseException(ExceptionConst.CODEFAILED, ExceptionConst.CODEFAILED_MSG);
                    }
                } catch (RemoteException e1) {
                    e1.printStackTrace();
                }

            }
        } else if (flag == 2) {
            res = bitmapCreator.printColumnsText(colsTextArr, colsWidthArr, colsAlign);
        }
        bitmapCreator.delCurrentMemory(tmp);
        try {
            if (callback != null) {
                callback.onRunResult(res);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    private byte[] getColumnsText(String[] colsText, int[] colsWidth, int[] colsAlign, int f, int g) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int size = colsText.length;
        LinkQueue<Byte>[] link = new LinkQueue[size];

        int[] s = new int[size];
        int min = 1; // 单元格中长文本打印行数

        byte[] t;
        for (int i = 0; i < size; i++) {
            link[i] = new LinkQueue<Byte>();
            t = colsText[i].getBytes("GB18030");
            for (int j = 0; j < t.length; j++) {
                link[i].r_push_queue(t[j]);
            }
            s[i] = (t.length + colsWidth[i] - 1) / colsWidth[i];
            if (min < s[i])
                min = s[i];
        }
        for (int k = 1; k <= min; k++) {
            int pos = 0;
            byte[] tmp2;
            for (int i = 0; i < size; i++) {
                if (link[i].size() > 0) {
                    int tmp1 = 1;
                    if (link[i].size() <= colsWidth[i]) {
                        tmp1 = link[i].size();
                    } else {
                        tmp1 = colsWidth[i];
                    }
                    tmp2 = new byte[tmp1];

                    for (int j = 0; j < tmp1; j++) {
                        tmp2[j] = link[i].l_pop_queue();
                        if (tmp2[j] < 0) { // 汉字成对出现
                            ++j;
                            if (j < tmp1) {
                                tmp2[j] = link[i].l_pop_queue();
                            } else { // 最后如果出现半个汉字，回压，放在下一行
                                link[i].l_push_queue(tmp2[tmp1 - 1]);
                                tmp2[tmp1 - 1] = 0x20;
                            }
                        }
                    }
                } else {
                    tmp2 = new byte[]{0x20};
                }
                if (k == min) {
                    buffer.write(new byte[]{0x1B, 0x33, 0x28});
                }
                switch (colsAlign[i]) {
                    case 1: // 居中对齐
                        buffer.write(setCusorPosition((pos + (colsWidth[i] - tmp2.length) / 2) * f + g * i));
                        break;
                    case 2: // 右对齐
                        buffer.write(setCusorPosition((pos + colsWidth[i] - tmp2.length) * f + g * i));
                        break;
                    default: // 左对齐
                        buffer.write(setCusorPosition(pos * f + g * i));
                        break;
                }
                buffer.write(tmp2);
                pos += colsWidth[i];
            }
            buffer.write((byte) 0x0A);
            buffer.write(new byte[]{0x1B, 0x32});

        }
        return buffer.toByteArray();
    }

    private byte[] setCusorPosition(int f) {
        byte[] returnText = new byte[4]; // 当前行，设置绝对打印位置 ESC $ bL bH
        returnText[0] = 0x1B;
        returnText[1] = 0x24;
        returnText[2] = (byte) f;
        returnText[3] = (byte) (f >> 8);
        return returnText;
    }

    class LinkQueue<T> {
        private class Node {
            public T data;
            public Node next;

            public Node() {
            }

            public Node(T data, Node next) {
                this.data = data;
                this.next = next;
            }
        }

        private Node front;
        // 队列尾指针
        private Node rear;
        // 队列长度
        private int size = 0;

        public LinkQueue() {
            Node n = new Node(null, null);
            n.next = null;
            front = rear = n;
        }

        /**
         * 队列入队尾算法
         *
         * @param data
         */
        public void r_push_queue(T data) {
            // 创建一个节点
            Node s = new Node(data, null);
            // 将队尾指针指向新加入的节点，将s节点插入队尾
            rear.next = s;
            rear = s;
            size++;
        }

        /**
         * 队列入队头算法
         *
         * @param data
         */
        public void l_push_queue(T data) {
            Node s = new Node(data, null);
            if (front.next == null) {
                front.next = s;
                rear = s;
            } else {
                // 暂存队头元素
                Node p = front.next;
                front.next = s;
                s.next = p;
            }
            size++;
        }

        /**
         * 队列出队算法
         *
         * @return
         */
        public T l_pop_queue() {
            if (rear == front) {
                /*
				 * try { throw new Exception("堆栈为空"); } catch (Exception e) {
				 * e.printStackTrace(); }
				 */
                return null;
            } else {
                // 暂存队头元素
                Node p = front.next;
                T x = p.data;
                // 将队头元素所在节点摘链
                front.next = p.next;
                // 判断出队列长度是否为1
                if (p.next == null)
                    rear = front;
                // 删除节点
                p = null;
                size--;
                return x;
            }
        }

        public void clear() {
            while (l_pop_queue() != null)
                ;
        }

        /**
         * 队列长度
         *
         * @return
         */
        public int size() {
            if (size == 0)
                return -1;
            return size;
        }

        /**
         * 判断队列是否为空
         *
         * @return
         */
        public boolean isEmpty() {
            return size == 0;
        }

        public String toString() {
            if (isEmpty()) {
                return "[]";
            } else {
                StringBuilder sb = new StringBuilder("[");
                for (Node current = front.next; current != null; current = current.next) {
                    sb.append(current.data.toString() + ", ");
                }
                int len = sb.length();
                return sb.delete(len - 2, len).append("]").toString();
            }

        }
    }
}
