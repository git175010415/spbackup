package com.sunmi.printerservice.kernel;

import com.sunmi.printerservice.entity.ServiceValue;


public interface RequestInterface {
    void setSettingStyle(String style);

    void updataLocalDCB();

    ServiceValue getServiceValue();
}
