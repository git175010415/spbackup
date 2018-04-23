package com.sunmi.printerservice.render;

import com.sunmi.printerservice.cell.DataCell;

/**
 * Created by Administrator on 2017/8/10.
 */

public interface RawPrintInterface {
    void sendDataCell(DataCell bytes);
    void openBox();
    void addOneOrder();
}
