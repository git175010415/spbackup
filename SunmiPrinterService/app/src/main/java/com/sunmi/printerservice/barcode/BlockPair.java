/** 
 * @Title:  BlockPair.java 
 * @author:  郭晗
 * @data:  2016年12月21日 下午4:36:57 <创建时间>
 * 
 * @history：<以下是历史记录>
 *
 * @modifier: <修改人>
 * @modify date: 2016年12月21日 下午4:36:57 <修改时间>
 * @log: <修改内容>
 *
 * @modifier: <修改人>
 * @modify date: 2016年12月21日 下午4:36:57 <修改时间>
 * @log: <修改内容>
 */
package com.sunmi.printerservice.barcode;

/** 
 * TODO<请描述这个类是干什么的> 
 * @author 郭晗 
 * @versionCode 1 <每次修改提交前+1>
 */
final class BlockPair {

	  private final byte[] dataBytes;
	  private final byte[] errorCorrectionBytes;

	  BlockPair(byte[] data, byte[] errorCorrection) {
	    dataBytes = data;
	    errorCorrectionBytes = errorCorrection;
	  }

	  public byte[] getDataBytes() {
	    return dataBytes;
	  }

	  public byte[] getErrorCorrectionBytes() {
	    return errorCorrectionBytes;
	  }

	}