package com.sunmi.printerservice.kernel;

import com.sunmi.printerservice.entity.ServiceValue;

/**
 * Created by Administrator on 2017/9/1.
 */

public interface RequestInterface {
    ServiceValue getServiceValue();
    void updataLocalDCB();
}
