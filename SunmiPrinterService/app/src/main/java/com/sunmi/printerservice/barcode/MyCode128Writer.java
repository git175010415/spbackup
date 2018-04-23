/** 
 * @Title:  MyCode128Writer.java 
 * @author:  郭晗
 * @data:  2016年12月20日 下午4:35:53 <创建时间>
 * 
 * @history：<以下是历史记录>
 *
 * @modifier: <修改人>
 * @modify date: 2016年12月20日 下午4:35:53 <修改时间>
 * @log: <修改内容>
 *
 * @modifier: <修改人>
 * @modify date: 2016年12月20日 下午4:35:53 <修改时间>
 * @log: <修改内容>
 */
package com.sunmi.printerservice.barcode;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.OneDimensionalCodeWriter;

/**
 * TODO<请描述这个类是干什么的>
 * 
 * @author 郭晗
 * @versionCode 1 <每次修改提交前+1>
 */
public class MyCode128Writer extends OneDimensionalCodeWriter {
	 static final int[][] CODE_PATTERNS = {
		      {2, 1, 2, 2, 2, 2}, // 0
		      {2, 2, 2, 1, 2, 2},
		      {2, 2, 2, 2, 2, 1},
		      {1, 2, 1, 2, 2, 3},
		      {1, 2, 1, 3, 2, 2},
		      {1, 3, 1, 2, 2, 2}, // 5
		      {1, 2, 2, 2, 1, 3},
		      {1, 2, 2, 3, 1, 2},
		      {1, 3, 2, 2, 1, 2},
		      {2, 2, 1, 2, 1, 3},
		      {2, 2, 1, 3, 1, 2}, // 10
		      {2, 3, 1, 2, 1, 2},
		      {1, 1, 2, 2, 3, 2},
		      {1, 2, 2, 1, 3, 2},
		      {1, 2, 2, 2, 3, 1},
		      {1, 1, 3, 2, 2, 2}, // 15
		      {1, 2, 3, 1, 2, 2},
		      {1, 2, 3, 2, 2, 1},
		      {2, 2, 3, 2, 1, 1},
		      {2, 2, 1, 1, 3, 2},
		      {2, 2, 1, 2, 3, 1}, // 20
		      {2, 1, 3, 2, 1, 2},
		      {2, 2, 3, 1, 1, 2},
		      {3, 1, 2, 1, 3, 1},
		      {3, 1, 1, 2, 2, 2},
		      {3, 2, 1, 1, 2, 2}, // 25
		      {3, 2, 1, 2, 2, 1},
		      {3, 1, 2, 2, 1, 2},
		      {3, 2, 2, 1, 1, 2},
		      {3, 2, 2, 2, 1, 1},
		      {2, 1, 2, 1, 2, 3}, // 30
		      {2, 1, 2, 3, 2, 1},
		      {2, 3, 2, 1, 2, 1},
		      {1, 1, 1, 3, 2, 3},
		      {1, 3, 1, 1, 2, 3},
		      {1, 3, 1, 3, 2, 1}, // 35
		      {1, 1, 2, 3, 1, 3},
		      {1, 3, 2, 1, 1, 3},
		      {1, 3, 2, 3, 1, 1},
		      {2, 1, 1, 3, 1, 3},
		      {2, 3, 1, 1, 1, 3}, // 40
		      {2, 3, 1, 3, 1, 1},
		      {1, 1, 2, 1, 3, 3},
		      {1, 1, 2, 3, 3, 1},
		      {1, 3, 2, 1, 3, 1},
		      {1, 1, 3, 1, 2, 3}, // 45
		      {1, 1, 3, 3, 2, 1},
		      {1, 3, 3, 1, 2, 1},
		      {3, 1, 3, 1, 2, 1},
		      {2, 1, 1, 3, 3, 1},
		      {2, 3, 1, 1, 3, 1}, // 50
		      {2, 1, 3, 1, 1, 3},
		      {2, 1, 3, 3, 1, 1},
		      {2, 1, 3, 1, 3, 1},
		      {3, 1, 1, 1, 2, 3},
		      {3, 1, 1, 3, 2, 1}, // 55
		      {3, 3, 1, 1, 2, 1},
		      {3, 1, 2, 1, 1, 3},
		      {3, 1, 2, 3, 1, 1},
		      {3, 3, 2, 1, 1, 1},
		      {3, 1, 4, 1, 1, 1}, // 60
		      {2, 2, 1, 4, 1, 1},
		      {4, 3, 1, 1, 1, 1},
		      {1, 1, 1, 2, 2, 4},
		      {1, 1, 1, 4, 2, 2},
		      {1, 2, 1, 1, 2, 4}, // 65
		      {1, 2, 1, 4, 2, 1},
		      {1, 4, 1, 1, 2, 2},
		      {1, 4, 1, 2, 2, 1},
		      {1, 1, 2, 2, 1, 4},
		      {1, 1, 2, 4, 1, 2}, // 70
		      {1, 2, 2, 1, 1, 4},
		      {1, 2, 2, 4, 1, 1},
		      {1, 4, 2, 1, 1, 2},
		      {1, 4, 2, 2, 1, 1},
		      {2, 4, 1, 2, 1, 1}, // 75
		      {2, 2, 1, 1, 1, 4},
		      {4, 1, 3, 1, 1, 1},
		      {2, 4, 1, 1, 1, 2},
		      {1, 3, 4, 1, 1, 1},
		      {1, 1, 1, 2, 4, 2}, // 80
		      {1, 2, 1, 1, 4, 2},
		      {1, 2, 1, 2, 4, 1},
		      {1, 1, 4, 2, 1, 2},
		      {1, 2, 4, 1, 1, 2},
		      {1, 2, 4, 2, 1, 1}, // 85
		      {4, 1, 1, 2, 1, 2},
		      {4, 2, 1, 1, 1, 2},
		      {4, 2, 1, 2, 1, 1},
		      {2, 1, 2, 1, 4, 1},
		      {2, 1, 4, 1, 2, 1}, // 90
		      {4, 1, 2, 1, 2, 1},
		      {1, 1, 1, 1, 4, 3},
		      {1, 1, 1, 3, 4, 1},
		      {1, 3, 1, 1, 4, 1},
		      {1, 1, 4, 1, 1, 3}, // 95
		      {1, 1, 4, 3, 1, 1},
		      {4, 1, 1, 1, 1, 3},
		      {4, 1, 1, 3, 1, 1},
		      {1, 1, 3, 1, 4, 1},
		      {1, 1, 4, 1, 3, 1}, // 100
		      {3, 1, 1, 1, 4, 1},
		      {4, 1, 1, 1, 3, 1},
		      {2, 1, 1, 4, 1, 2},
		      {2, 1, 1, 2, 1, 4},
		      {2, 1, 1, 2, 3, 2}, // 105
		      {2, 3, 3, 1, 1, 1, 2}
		  };

	private static final int CODE_SHIFT = 98;

	private static final int CODE_CODE_C = 99;
	private static final int CODE_CODE_B = 100;
	private static final int CODE_CODE_A = 101;

	private static final int CODE_FNC_1 = 102;
	private static final int CODE_FNC_2 = 97;
	private static final int CODE_FNC_3 = 96;
	private static final int CODE_FNC_4_B = 100;

	private static final int CODE_START_A = 103;
	private static final int CODE_START_B = 104;
	private static final int CODE_START_C = 105;
	private static final int CODE_STOP = 106;



	public boolean[] encode(String contents,StringBuilder stringBuilder) {
		byte[] content_b;
		try {
			content_b = contents.getBytes("US-ASCII");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
		int length = content_b.length;
		// Check length
		if (length < 1 || length > 80) {
			throw new IllegalArgumentException(
					"Contents length should be between 1 and 80 characters, but got " + length);
		}

		Collection<int[]> patterns = new ArrayList<>(); // temporary storage for
														// patterns
		int checkSum = 0;
		int checkWeight = 1;
		int codeSet = 0; // selected code (CODE_CODE_B or CODE_CODE_C)
		int position = 0; // position in contents
		int newCodeSet = 0;
		while (position < length) {
			if (content_b[position] == '{' && position + 1 < length) {
				if (content_b[position+1] == 'A') {
					newCodeSet = CODE_CODE_A;
					position += 2;
				} else if (content_b[position+1] == 'B') {
					newCodeSet = CODE_CODE_B;
					position += 2;
				} else if (content_b[position+1] == 'C') {
					newCodeSet = CODE_CODE_C;
					position += 2;
				} else {
					throw new IllegalArgumentException("Bad character in input: {" + contents.charAt(position + 1));
				}
			}

			// Get the pattern index
			int patternIndex;
			if (newCodeSet == codeSet) {
				// Encode the current character
				// First handle escapes
				switch (content_b[position]) {
				case '{':
					if (content_b[position+1] == '1') {
						patternIndex = CODE_FNC_1;
						position++;
					} else if (content_b[position+1] == '2') {
						patternIndex = CODE_FNC_2;
						position++;
					} else if (content_b[position+1] == '3') {
						patternIndex = CODE_FNC_3;
						position++;
					} else if (content_b[position+1] == '4') {
						patternIndex = CODE_FNC_4_B;
						position++;
					} else if (content_b[position+1] == 'S') {
						patternIndex = CODE_SHIFT;
						position++;
					} else if (content_b[position+1] == '{') {
						patternIndex = '{' - ' ';
						position++;
					} else {
						throw new IllegalArgumentException("Bad character in input: {" + contents.charAt(position + 1));
					}
					break;
				default:
					// Then handle normal characters otherwise
					if (codeSet == CODE_CODE_B) {
						patternIndex = content_b[position] - ' ';
						try {
							stringBuilder.append(new String(content_b,position,1,"US-ASCII"));
						} catch (UnsupportedEncodingException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}else if (codeSet == CODE_CODE_A) {
						patternIndex = content_b[position] - ' ';
						try {
							stringBuilder.append(new String(content_b,position,1,"US-ASCII"));
						} catch (UnsupportedEncodingException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} else { // CODE_CODE_C
						patternIndex = content_b[position];
						stringBuilder.append(String.format("%02d", patternIndex));
					}
				}
				position++;
			} else {
				// Should we change the current code?
				// Do we have a code set?
				if (codeSet == 0) {
					// No, we don't have a code set
					if(newCodeSet==0)
					{
						throw new IllegalArgumentException("No Code Setting (Code A or Code B or Code C)");
					}
					if (newCodeSet == CODE_CODE_B) {
						patternIndex = CODE_START_B;
					} else if (newCodeSet == CODE_CODE_A) {
						patternIndex = CODE_START_A;
					} else {
						// CODE_CODE_C
						patternIndex = CODE_START_C;
					}
				} else {
					// Yes, we have a code set
					patternIndex = newCodeSet;
				}
				codeSet = newCodeSet;
			}

			// Get the pattern
			patterns.add(CODE_PATTERNS[patternIndex]);

			// Compute checksum
			checkSum += patternIndex * checkWeight;
			if (position != 2) {
				checkWeight++;
			}
		}

		// Compute and append checksum
		checkSum %= 103;
		patterns.add(CODE_PATTERNS[checkSum]);

		// Append stop code
		patterns.add(CODE_PATTERNS[CODE_STOP]);

		// Compute code width
		int codeWidth = 0;
		for (int[] pattern : patterns) {
			for (int width : pattern) {
				codeWidth += width;
			}
		}

		// Compute result
		boolean[] result = new boolean[codeWidth];
		int pos = 0;
		for (int[] pattern : patterns) {
			pos += appendPattern(result, pos, pattern, true);
		}

		return result;
	}


	/* (non-Javadoc)
	 * @see com.google.zxing.oned.OneDimensionalCodeWriter#encode(java.lang.String)
	 */
	@Override
	public boolean[] encode(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}
