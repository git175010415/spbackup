# sunmi-uhf
## 插件介绍
本插件用于uniapp、uniapp-x开发Sunmi L2k、L2s等Android设备上的UHF电子标签功能  
当前插件支持对ISO18000-6C标签的盘存、读写等操作

## 插件使用
在`script`中引入插件  
```
import * as uhf from '../../uni_modules/sunmi-uhf'

```
定义要使用的方法  
```
methods:{
    //初始化sdk
    init() {
        uhf.initSdk({
            model(ret) {
                
            },
            sn(ret) {
               
            },
            softVersion(ret) {
                
            },
            hardwareVersion(ret) {
             
            }
        })
    }
    
    //标签盘寸
    inventory() {
        uhf.realTimeInventory(1, {
            success: (ret) => {
                getCacheInventoryNums({
                    success: (inventoryResult) => {},
                    tag: (tagInfo) => {},
                    fail:(error) => {}
                })
            }
        })
    }
    //释放sdk
    deInit() {
        uhf.deInitSdk()
    }
}
```

## API列表
| 方法名 | 简单说明 |
|---|---|
|[initSdk][a1]| 插件所有操作需要先初始化sdk，并可以获取当前设备的基础信息 |
|[deInitSdk][a2]| 如果不使用插件可以释放sdk |
|getDeviceStatus| 实时获取设备的各种状态参数 |
|reset| 复位设备（复位uhf模块非Android设备） |
|realTimeInventory| 常规盘存方法（推荐使用） |
|customInventory| 自定义参数盘存方法 |
|switchInventory| 快速切换天线盘存方法 |
|accessTag| 设置需要操作标签的EPC |
|cancelAccessTag| 取消正在操作的标签 |
|getAccessTag| 获取正在操作的标签EPC |
|readAccessTag| 读取当前标签的信息 |
|writeAccessTag| 写入当前标签的信息 |
|lockAccessTag| 锁定标签的读写区域 |
|killAccessag| 销毁标签 |
|cacheInventory| 缓存结果盘存方法 |
|getCacheInventory| 获取缓存盘存的标签结果 |
|getCacheInventoryNums| 获取缓存盘存的标签数量 |
|resetCacheInventory| 重置盘存的缓存 |
|setImpinjFastTid| 配置支持FastTID |
|setWorkAntenna| 指定当前工作的天线 |
|setOutputAllPower| 设置各个天线的工作功率 |
|setOutputPower| 设置单个天线的工作功率 |
|setTemporaryOutputPower| 暂时设置各个天线的工作功率，复位后恢复 |
|setFrequencyRegion| 区域频段设置 |
|setUserFrequencyRegion| 自定义区域频段设置 |
|setRfLinkProfile| 设置射频链路 |
|setTagMask| 设置标签过滤规则 |
|clearTagMask| 清除标签过滤规则 |

### initSdk(deviceInfo)
[a1]: 初始化SDK

#### 参数
|名称|类型|必填|默认值|描述|
|---|---|---|---|---|
|deviceInfo|DevceInfo|是|{}|初始化成功后返回的设备信息|

**DevceInfo的属性值**
|名称|类型|必填|默认值|描述|
|---|---|---|---|---|
|model|(v: number) => void|否|null|设备的型号代码：100 未识别 101 R2000 102 INNER 103 S700|
|sn|(v: string) => void|否|null|模块SN|
|softVersion|(v: number) => void|否|null|模块软件版本号|
|hardwareVersion|(v: number) => void|否|null|模块版本号|

#### 示例

### deInitSdk()
[a2]: 释放SDK，当不需要使用uhf时可以调用完全释放资源
