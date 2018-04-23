package com.sunmi.printerservice.kernel;

import com.sunmi.printerservice.entity.ServiceValue;

/**
 * 描述 :
 * 作者 : kaltin
 * 日期 : 2017/11/1 15:07
 */

public interface RequestInterface {
    void updataLocalDCB();

    ServiceValue getServiceValue();

    void setSettingStyle(String style);
}
