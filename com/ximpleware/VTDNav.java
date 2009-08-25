/* 
 * Copyright (C) 2002-2009 XimpleWare, info@ximpleware.com
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

/*
 *
 * This class is created to update VTDNav's implementation with 
 * a more thread safe version
 */
package com.ximpleware;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import com.ximpleware.parser.ISO8859_15;
import com.ximpleware.parser.ISO8859_14;
import com.ximpleware.parser.ISO8859_13;
import com.ximpleware.parser.ISO8859_11;
import com.ximpleware.parser.ISO8859_10;
import com.ximpleware.parser.ISO8859_2;
import com.ximpleware.parser.ISO8859_3;
import com.ximpleware.parser.ISO8859_4;
import com.ximpleware.parser.ISO8859_5;
import com.ximpleware.parser.ISO8859_6;
import com.ximpleware.parser.ISO8859_7;
import com.ximpleware.parser.ISO8859_8;
import com.ximpleware.parser.ISO8859_9;
import com.ximpleware.parser.UTF8Char;
import com.ximpleware.parser.WIN1250;
import com.ximpleware.parser.WIN1251;
import com.ximpleware.parser.WIN1252;
import com.ximpleware.parser.WIN1253;
import com.ximpleware.parser.WIN1254;
import com.ximpleware.parser.WIN1255;
import com.ximpleware.parser.WIN1256;
import com.ximpleware.parser.WIN1257;
import com.ximpleware.parser.WIN1258;
/**
 * The VTD Navigator allows one to navigate XML document represented in VTD
 * records and Location caches. There is one and only one cursor that you can
 * navigate to any part of the tree. If a method operating on a node doesn't
 * accept the node as input, by default it refers to the cursor element. The
 * hierarchy consists entirely of elements.
 */
public class VTDNav {
	// Navigation directions
	public final static int ROOT = 0;
	public final static int PARENT = 1;
	public final static int FIRST_CHILD = 2;
	public final static int LAST_CHILD = 3;
	public final static int NEXT_SIBLING = 4;
	public final static int PREV_SIBLING = 5;

	// Navigation directions
	public final static int R = 0;
	public final static int P = 1;
	public final static int FC = 2;
	public final static int LC = 3;
	public final static int NS = 4;
	public final static int PS = 5;

	// token type definitions
	public final static int TOKEN_STARTING_TAG = 0;
	public final static int TOKEN_ENDING_TAG = 1;
	public final static int TOKEN_ATTR_NAME = 2;
	public final static int TOKEN_ATTR_NS = 3;
	public final static int TOKEN_ATTR_VAL = 4;
	public final static int TOKEN_CHARACTER_DATA = 5;
	public final static int TOKEN_COMMENT = 6;
	public final static int TOKEN_PI_NAME = 7;
	public final static int TOKEN_PI_VAL = 8;
	public final static int TOKEN_DEC_ATTR_NAME = 9;
	public final static int TOKEN_DEC_ATTR_VAL = 10;
	public final static int TOKEN_CDATA_VAL = 11;
	public final static int TOKEN_DTD_VAL = 12;
	public final static int TOKEN_DOCUMENT =13;

	// encoding format definition here
	public final static int FORMAT_UTF8 = 2;
	public final static int FORMAT_ASCII = 0;
	
	public final static int FORMAT_ISO_8859_1 = 1;
	public final static int FORMAT_ISO_8859_2 = 3;
	public final static int FORMAT_ISO_8859_3 = 4;
	public final static int FORMAT_ISO_8859_4 = 5;
	public final static int FORMAT_ISO_8859_5 = 6;
	public final static int FORMAT_ISO_8859_6 = 7;
	public final static int FORMAT_ISO_8859_7 = 8;
	public final static int FORMAT_ISO_8859_8 = 9;
	public final static int FORMAT_ISO_8859_9 = 10;
	public final static int FORMAT_ISO_8859_10 = 11;
	public final static int FORMAT_ISO_8859_11 = 12;
	public final static int FORMAT_ISO_8859_12 = 13;
	public final static int FORMAT_ISO_8859_13 = 14;
	public final static int FORMAT_ISO_8859_14 = 15;
	public final static int FORMAT_ISO_8859_15 = 16;
	public final static int FORMAT_ISO_8859_16 = 17;
	
	public final static int FORMAT_WIN_1250 = 18;
	public final static int FORMAT_WIN_1251 = 19;
	public final static int FORMAT_WIN_1252 = 20;
	public final static int FORMAT_WIN_1253 = 21;
	public final static int FORMAT_WIN_1254 = 22;
	public final static int FORMAT_WIN_1255 = 23;
	public final static int FORMAT_WIN_1256 = 24;
	public final static int FORMAT_WIN_1257 = 25;
	public final static int FORMAT_WIN_1258 = 26;
	
	
	public final static int FORMAT_UTF_16LE = 64;
	public final static int FORMAT_UTF_16BE = 63;
	
	// String style
	public final static int STRING_RAW =0;
	public final static int STRING_REGULAR = 1;
	public final static int STRING_NORMALIZED = 2;
	
	// masks for obtaining various fields from a VTD token
	protected final static long MASK_TOKEN_FULL_LEN = 0x000fffff00000000L;
	private final static long MASK_TOKEN_PRE_LEN = 0x000ff80000000000L;
	private final static long MASK_TOKEN_QN_LEN = 0x000007ff00000000L;
	long MASK_TOKEN_OFFSET = 0x000000003fffffffL;
	private final static long MASK_TOKEN_TYPE = 0xf000000000000000L;
	private final static long MASK_TOKEN_DEPTH = 0x0ff0000000000000L;

	// tri-state variable for namespace lookup
	private final static long MASK_TOKEN_NS_MARK = 0x00000000c0000000L;

	protected int rootIndex; // where the root element is at
	protected int nestingLevel;
	protected int[] context; // main navigation tracker aka context object
    protected boolean atTerminal; // this variable is to make vn compatible with
    								// xpath's data model
	
	
	// location cache part
	protected int l2upper;
	protected int l2lower;
	protected int l3upper;
	protected int l3lower;
	protected int l2index;
	protected int l3index;
	protected int l1index;

	// containers
	protected ILongBuffer vtdBuffer;
	protected ILongBuffer l1Buffer;
	protected ILongBuffer l2Buffer;
	protected IIntBuffer l3Buffer;
	protected IByteBuffer XMLDoc;

	//private int recentNS; // most recently visited NS node, experiment for
    // now
	// Hierarchical representation is an array of integers addressing elements
    // tokens
	private ContextBuffer contextStack;
	protected ContextBuffer contextStack2;// this is reserved for XPath

	protected int LN; // record txt and attrbute for XPath eval purposes
	// the document encoding
	protected int encoding;
	//protected boolean writeOffsetAdjustment;
	// for string to token comparison
	//protected int currentOffset;
	//protected int currentOffset2;

	// whether the navigation is namespace enabled or not.
	protected boolean ns;

	// intermediate buffer for push and pop purposes
	protected int[] stackTemp;
	protected int docOffset;
	// length of the document
	protected int docLen;
	protected int vtdSize; //vtd record count
	/**
     * Initialize the VTD navigation object.
     * 
     * @param RootIndex
     *            int
     * @param maxDepth
     *            int
     * @param encoding
     *            int
     * @param NS
     *            boolean
     * @param x
     *            byte[]
     * @param vtd
     *            com.ximpleware.ILongBuffer
     * @param l1
     *            com.ximpleware.ILongBuffer
     * @param l2
     *            com.ximpleware.ILongBuffer
     * @param l3
     *            com.ximpleware.IIntBuffer
     * @param so
     *            int starting offset of the document(in byte)
     * @param length
     *            int length of the document (in byte)
     */
	protected VTDNav(
		int RootIndex,
		int enc,
		boolean NS,
		int depth,
		IByteBuffer x,
		ILongBuffer vtd,
		ILongBuffer l1,
		ILongBuffer l2,
		IIntBuffer l3,
		int so, // start offset of the starting offset(in byte)
	int length) // lengnth of the XML document (in byte))
	{
		// initialize all buffers
		if (l1 == null
			|| l2 == null
			|| l3 == null
			|| vtd == null
			|| x == null
			|| depth < 0
			|| RootIndex < 0 //|| encoding <= FORMAT_UTF8
			//|| encoding >= FORMAT_ISO_8859_1
			|| so < 0
			|| length < 0) {
			throw new IllegalArgumentException();
		}

		l1Buffer = l1;
		l2Buffer = l2;
		l3Buffer = l3;
		vtdBuffer = vtd;
		XMLDoc = x;

		encoding = enc;
		//System.out.println("encoding " + encoding);
		rootIndex = RootIndex;
		nestingLevel = depth + 1;
		ns = NS; // namespace aware or not
		if (ns == false)
		    MASK_TOKEN_OFFSET = 0x000000007fffffffL; // this allows xml size to
                                                     // be 2GB
		else // if there is no namespace
		    MASK_TOKEN_OFFSET = 0x000000003fffffffL;
		
		
		atTerminal = false; //this variable will only change value during XPath
                            // eval

		// initialize the context object
		this.context = new int[nestingLevel];
		//depth value is the first entry in the context because root is
        // singular.
		context[0] = 0;
		//set the value to zero
		for (int i = 1; i < nestingLevel; i++) {
			context[i] = -1;
		}
		//currentOffset = 0;
		//contextStack = new ContextBuffer(1024, nestingLevel + 7);
		contextStack = new ContextBuffer(10, nestingLevel + 9);
		contextStack2 = new ContextBuffer(10, nestingLevel+9);
		stackTemp = new int[nestingLevel + 9];

		// initial state of LC variables
		l1index = l2index = l3index = -1;
		l2lower = l3lower = -1;
		l2upper = l3upper = -1;
		docOffset = so;
		docLen = length;
		//System.out.println("offset " + offset + " length " + length);
		//printL2Buffer();
		vtdSize = vtd.size();
		//writeOffsetAdjustment = false;
		//recentNS = -1;
	}
	/**
     * Return the attribute count of the element at the cursor position. when ns
     * is false, attr_ns tokens are considered attributes; otherwise, ns tokens
     * are not considered attributes
     * 
     * @return int
     */
	public int getAttrCount() {
	    if (context[0]==-1)return 0;
		int count = 0;
		int index = getCurrentIndex() + 1;
		while (index < vtdSize) {
			int type = getTokenType(index);
			if (type == TOKEN_ATTR_NAME
				|| type == TOKEN_ATTR_VAL
				|| type == TOKEN_ATTR_NS) {
				if (type == TOKEN_ATTR_NAME
					|| (!ns && (type == TOKEN_ATTR_NS))) {
					count++;
				}
			} else
				break;
			index++;
		}
		return count;
	}
	/**
     * Get the token index of the attribute value given an attribute name.
     * 
     * @return int (-1 if no such attribute name exists)
     * @param an
     *            java.lang.String
     * @exception com.ximpleware.NavException
     *                The exception if the underlying byte content contains
     *                various errors. Notice that we are being conservative in
     *                making little assumption on the correctness of underlying
     *                byte content. This is because the VTD etc can be generated
     *                by another machine from a load-balancer.
     * @exception IllegalArguementException
     *                if an is null
     */
	public int getAttrVal(String an) throws NavException {
		//int size = vtdBuffer.size();
		if (context[0]==-1)
			return -1;
		int index = (context[0] != 0) ? context[context[0]] + 1 : rootIndex + 1;
		
		int type;
		if (index<vtdSize)
		   type= getTokenType(index);
		else
			return -1;
		if (ns == false) {
			while ((type == TOKEN_ATTR_NAME || type == TOKEN_ATTR_NS)) {
				if (matchRawTokenString(index,
					an)) { // ns node visible only ns is disabled
					return index + 1;
				}
				index += 2;
				if (index >= vtdSize)
					break;
				type = getTokenType(index);
			}
		} else {
			while ((type == TOKEN_ATTR_NAME || type == TOKEN_ATTR_NS)) {
				if (type == TOKEN_ATTR_NAME
					&& matchRawTokenString(
						index,
						an)) { // ns node visible only ns is disabled
					return index + 1;
				}
				index += 2;
				if (index>=vtdSize)
					break;
				type = getTokenType(index);
			}
		}
		return -1;
	}
	/**
     * Get the token index of the attribute value of given URL and local name.
     * If ns is not enabled, the lookup will return -1, indicating a no-found.
     * Also namespace nodes are invisible using this method. One can't use * to
     * indicate any name space because * is ambiguous!!
     * 
     * @return int (-1 if no matching attribute found)
     * @param URL
     *            java.lang.String (Name space URL)
     * @param ln
     *            java.lang.String (local name)
     * @exception com.ximpleware.NavException
     *                The exception if the underlying byte content contains
     *                various errors. Notice that we are being conservative in
     *                making little assumption on the correctness of underlying
     *                byte content. This is because the VTD etc can be generated
     *                by another machine from a load-balancer.
     * @exception IllegalArguementException
     *                if s is null
     */
    public int getAttrValNS(String URL, String ln) throws NavException {
    	if (ns == false)
    		return -1;
    	if (URL == null)
    		return getAttrVal(ln);
    	int size = vtdBuffer.size();
    	int index = (context[0] != 0) ? context[context[0]] + 1 : rootIndex + 1;
    	// point to the token next to the element tag
    	int type;
    	if (index<vtdSize)
    		type = getTokenType(index);
    	else 
    		return -1;
    	while (index < size
    		&& (type == TOKEN_ATTR_NAME || type == TOKEN_ATTR_NS)) {
    		int i = getTokenLength(index);
    		int offset = getTokenOffset(index);
    		int preLen = (i >> 16) & 0xffff;
    		int fullLen = i & 0xffff;
    		if (preLen != 0
    			// attribute name without a prefix is not bound to any
                // namespaces
    			&& matchRawTokenString(
    				offset + preLen + 1,
    				fullLen - preLen - 1,
    				ln)
    			&& resolveNS(URL, offset, preLen)) {
    			return index + 1;
    		}
    		index += 2;
    		if (index>=vtdSize)
    			break;
    		type = getTokenType(index);
    	}
    	return -1;
    }
	private long handle_utf8(long temp, int offset) throws NavException {
        int c, d, a; 
        
        long val;
        switch (UTF8Char.byteCount((int)temp & 0xff)) {
        case 2:
            c = 0x1f;
            d = 6;
            a = 1;
            break;
        case 3:
            c = 0x0f;
            d = 12;
            a = 2;
            break;
        case 4:
            c = 0x07;
            d = 18;
            a = 3;
            break;
        case 5:
            c = 0x03;
            d = 24;
            a = 4;
            break;
        case 6:
            c = 0x01;
            d = 30;
            a = 5;
            break;
        default:
            throw new NavException("UTF 8 encoding error: should never happen");
        }

        val = (temp & c) << d;
        int i = a - 1;
        while (i >= 0) {
            temp = XMLDoc.byteAt(offset + a - i);
            if ((temp & 0xc0) != 0x80)
                throw new NavException(
                        "UTF 8 encoding error: should never happen");
            val = val | ((temp & 0x3f) << ((i << 2) + (i << 1)));
            i--;
        }
        //currentOffset += a + 1;
        return val | (((long)(a+1))<<32);
    }


	private long handle_utf16le(int offset) throws NavException {
		// implement UTF-16LE to UCS4 conversion
		int val, temp =
			(XMLDoc.byteAt((offset << 1) + 1 ) & 0xff)
				<< 8 | (XMLDoc.byteAt(offset << 1) & 0xff);
		if (temp < 0xdc00 || temp > 0xdfff) { // check for low surrogate
			if (temp == '\r') {
				if (XMLDoc.byteAt((offset << 1) + 2) == '\n'
					&& XMLDoc.byteAt((offset << 1) + 3) == 0) {
					return '\n' | (2L<<32) ;
				} else {
					return '\n' | (1L<<32);
				}
			}
			return temp | (1L<<32);
		} else {
			if (temp<0xd800 || temp>0xdbff)				
				throw new NavException("UTF 16 LE encoding error: should never happen");
			val = temp;
			temp =
				(XMLDoc.byteAt((offset << 1) + 3)&0xff)
					<< 8 | (XMLDoc.byteAt((offset << 1) + 2) & 0xff);
			if (temp < 0xdc00 || temp > 0xdfff) {
				// has to be high surrogate
				throw new NavException("UTF 16 LE encoding error: should never happen");
			}
			val = ((temp - 0xd800)<<10) + (val - 0xdc00) + 0x10000;
			
			return val | (2L<<32);
		}
		//System.out.println("UTF 16 LE unimplemented for now");
	}

	private long handle_utf16be(int offset) throws NavException{
		long val; 
		
		int temp =
			((XMLDoc.byteAt(offset << 1) & 0xff)	<< 8) 
					|(XMLDoc.byteAt((offset << 1) + 1)& 0xff);
		if ((temp < 0xd800)
			|| (temp > 0xdfff)) { // not a high surrogate
			if (temp == '\r') {
				if (XMLDoc.byteAt((offset << 1) + 3) == '\n'
					&& XMLDoc.byteAt((offset << 1) + 2) == 0) {
					
					return '\n'|(2L<<32);
				} else {
					return '\n'|(1L<<32);
				}
			}
			//currentOffset++;
			return temp| (1L<<32);
		} else {
			if (temp<0xd800 || temp>0xdbff)				
				throw new NavException("UTF 16 BE encoding error: should never happen");
			val = temp;
			temp =
				((XMLDoc.byteAt((offset << 1) + 2) & 0xff)
					<< 8) | (XMLDoc.byteAt((offset << 1 )+ 3) & 0xff);
			if (temp < 0xdc00 || temp > 0xdfff) {
				// has to be a low surrogate here
				throw new NavException("UTF 16 BE encoding error: should never happen");
			}
			val = ((temp - 0xd800) << 10) + (val - 0xdc00) + 0x10000;
			//currentOffset += 2;
			return val | (2L<<32);
		}
	}

	private long getChar4OtherEncoding(int offset) throws NavException{
	    if (encoding <= FORMAT_WIN_1258){
	        int	temp = decode(offset);
	        if (temp == '\r') {
	            if (XMLDoc.byteAt(offset + 1) == '\n') {
	                return '\n'|(2L<<32);
	            } else {
				return '\n'|(1L<<32);
	            }
	        }
	        return temp|(1L<<32);
	    }
	    throw new NavException("Unknown Encoding");
	}
	/**
     * This method decodes the underlying byte array into corresponding UCS2
     * char representation . It doesn't resolves built-in entity and character
     * references. Length will never be zero Creation date: (11/21/03 6:26:17
     * PM)
     * 
     * @return int
     * @exception com.ximpleware.NavException
     *                The exception is thrown if the underlying byte content
     *                contains various errors. Notice that we are being
     *                conservative in making little assumption on the
     *                correctness of underlying byte content. This is because
     *                the VTD can be generated by another machine, e.g. from a
     *                load-balancer.
     */
	private long getChar(int offset) throws NavException {
		long temp = 0;
		//int a, c, d;
		//int val;
		//int ch;
		//int inc;
		//a = c = d = val = 0;

		switch (encoding) {
			case FORMAT_ASCII : // ascii is compatible with UTF-8, the offset
                                // value is bytes
				temp = XMLDoc.byteAt(offset);
				if (temp == '\r') {
					if (XMLDoc.byteAt(offset + 1) == '\n') {
						return '\n'|(2L<<32);
					} else {
						return '\n'|(1L<<32);
					}
				}
				
				return temp|(1L<<32);
				
			case FORMAT_ISO_8859_1 :
				temp = XMLDoc.byteAt(offset);
				if (temp == '\r') {
					if (XMLDoc.byteAt(offset + 1) == '\n') {
						return '\n'|(2L<<32);
					} else {
						return '\n'|(1L<<32);
					}
				}
				
				return (temp & 0xff)|(1L<<32);
				
			case FORMAT_UTF8 :
				temp = XMLDoc.byteAt(offset);
				if (temp>=0){
					if (temp == '\r') {
						if (XMLDoc.byteAt(offset + 1) == '\n') {
							return '\n'|(2L<<32);
						} else {
							return '\n'|(1L<<32);
						}
					}
					//currentOffset++;
					return temp|(1L<<32);
				}				
				return handle_utf8(temp,offset);

			case FORMAT_UTF_16BE :
			    return handle_utf16be(offset);

			case FORMAT_UTF_16LE :
			    return handle_utf16le(offset);

			default :
			    return getChar4OtherEncoding(offset);
				//throw new NavException("Unknown Encoding");
		}
	}
	/*
     * the exact same copy of getChar except it operates on currentOffset2 this
     * is needed to compare VTD tokens directly
     */
	

	/**
     * This method decodes the underlying byte array into corresponding UCS2
     * char representation . Also it resolves built-in entity and character
     * references.
     * 
     * @return int
     * @exception com.ximpleware.NavException
     *                The exception is thrown if the underlying byte content
     *                contains various errors. Notice that we are being
     *                conservative in making little assumption on the
     *                correctness of underlying byte content. This is because
     *                the VTD can be generated by another machine from a
     *                load-balancer.
     */
	private long getCharResolved(int offset) throws NavException {
		int ch = 0;
		int val = 0;
		long inc =2;
		long l = getChar(offset);
		
		ch = (int)l;
		
		if (ch != '&')
			return l;
		
		// let us handle references here
		//currentOffset++;
		offset++;
		ch = getCharUnit(offset);
		offset++;
		switch (ch) {
			case '#' :
			    	
				ch = getCharUnit(offset);

				if (ch == 'x') {
					while (true) {
						offset++;
						inc++;
						ch = getCharUnit(offset);

						if (ch >= '0' && ch <= '9') {
							val = (val << 4) + (ch - '0');
						} else if (ch >= 'a' && ch <= 'f') {
							val = (val << 4) + (ch - 'a' + 10);
						} else if (ch >= 'A' && ch <= 'F') {
							val = (val << 4) + (ch - 'A' + 10);
						} else if (ch == ';') {
							inc++;
							break;
						} else
							throw new NavException("Illegal char in a char reference");
					}
				} else {
					while (true) {

						ch = getCharUnit(offset);
						offset++;
						inc++;
						if (ch >= '0' && ch <= '9') {
							val = val * 10 + (ch - '0');
						} else if (ch == ';') {
							break;
						} else
							throw new NavException("Illegal char in char reference");
					
					}
				}
				break;

			case 'a' :
				ch = getCharUnit(offset);
				if (ch == 'm') {
					if (getCharUnit(offset + 1) == 'p'
						&& getCharUnit(offset + 2) == ';') {
						inc = 5;
						val = '&';
					} else
						throw new NavException("illegal builtin reference");
				} else if (ch == 'p') {
					if (getCharUnit(offset + 1) == 'o'
						&& getCharUnit(offset + 2) == 's'
						&& getCharUnit(offset + 3) == ';') {
						inc = 6;
						val = '\'';
					} else
						throw new NavException("illegal builtin reference");
				} else
					throw new NavException("illegal builtin reference");
				break;

			case 'q' :

				if (getCharUnit(offset) == 'u'
					&& getCharUnit(offset + 1) == 'o'
					&& getCharUnit(offset + 2) == 't'
					&& getCharUnit(offset + 3) ==';') {
					inc = 6;
					val = '\"';
				} else
					throw new NavException("illegal builtin reference");
				break;
			case 'l' :
				if (getCharUnit(offset) == 't'
					&& getCharUnit(offset + 1) == ';') {
					//offset += 2;
					inc = 4;
					val = '<';
				} else
					throw new NavException("illegal builtin reference");
				break;
			case 'g' :
				if (getCharUnit(offset) == 't'
					&& getCharUnit(offset + 1) == ';') {
					inc = 4;
					val = '>';
				} else
					throw new NavException("illegal builtin reference");
				break;

			default :
				throw new NavException("Invalid entity char");

		}

		//currentOffset++;
		return val | (inc << 32);
	}
	

	
	private int decode(int offset){
	    byte ch = XMLDoc.byteAt(offset);
	    switch(encoding){
        case FORMAT_ISO_8859_2:
            return ISO8859_2.decode(ch);
        case FORMAT_ISO_8859_3:
            return ISO8859_3.decode(ch);
        case FORMAT_ISO_8859_4:
            return ISO8859_4.decode(ch);
        case FORMAT_ISO_8859_5:
            return ISO8859_5.decode(ch);
        case FORMAT_ISO_8859_6:
            return ISO8859_6.decode(ch);
        case FORMAT_ISO_8859_7:
            return ISO8859_7.decode(ch);
        case FORMAT_ISO_8859_8:
            return ISO8859_8.decode(ch);
        case FORMAT_ISO_8859_9:
            return ISO8859_9.decode(ch);
        case FORMAT_ISO_8859_10:
            return ISO8859_10.decode(ch);
        case FORMAT_ISO_8859_11:
            return ISO8859_11.decode(ch);
        case FORMAT_ISO_8859_13:
            return ISO8859_13.decode(ch);
        case FORMAT_ISO_8859_14:
            return ISO8859_14.decode(ch);
        case FORMAT_ISO_8859_15:
            return ISO8859_15.decode(ch);
        case FORMAT_WIN_1250:
            return WIN1250.decode(ch);
        case FORMAT_WIN_1251:
            return WIN1251.decode(ch);
        case FORMAT_WIN_1252:
            return WIN1252.decode(ch);
        case FORMAT_WIN_1253:
            return WIN1253.decode(ch);
        case FORMAT_WIN_1254:
            return WIN1254.decode(ch);
        case FORMAT_WIN_1255:
            return WIN1255.decode(ch);
        case FORMAT_WIN_1256:
            return WIN1256.decode(ch);
        case FORMAT_WIN_1257:
            return WIN1257.decode(ch);
		default:
		    return WIN1258.decode(ch);
	    }
	}
	
	/**
     * Dump the in memory XML text into output stream
     * 
     * @param os
     * @throws java.io.IOException
     *  
     */
	public void dumpXML(OutputStream os) throws java.io.IOException{
	    os.write(this.XMLDoc.getBytes(),this.docOffset,this.docLen);
	}
	
	/**
     * Dump the in-memory copy of XML text into a file
     * 
     * @param fileName
     * @throws java.io.IOException
     *  
     */
	public void dumpXML(String fileName) throws java.io.IOException{
	    FileOutputStream fos = new FileOutputStream(fileName);
	    try{
	        dumpXML(fos);
	    }
	    finally{
	        fos.close();
	    }
	}
	/**
     * Get the next char unit which gets decoded automatically
     * 
     * @return int
     */
	private int getCharUnit(int offset) {
		return (encoding <= 2)
			? XMLDoc.byteAt(offset) & 0xff
			: (encoding <= FORMAT_WIN_1258)
			? decode(offset):(encoding == FORMAT_UTF_16BE)
			? (((int)XMLDoc.byteAt(offset << 1))
				<< 8 | XMLDoc.byteAt((offset << 1) + 1))
			: (((int)XMLDoc.byteAt((offset << 1) + 1))
				<< 8 | XMLDoc.byteAt(offset << 1));
	}
	/**
     * Get the depth (>=0) of the current element. Creation date: (11/16/03
     * 6:58:22 PM)
     * 
     * @return int
     */
	final public int getCurrentDepth() {
		return context[0];
	}
	/**
     * Get the index value of the current element. Creation date: (11/16/03
     * 6:40:25 PM)
     * 
     * @return int
     */
	final public int getCurrentIndex() {
	    if (atTerminal)
	        return LN;
		return getCurrentIndex2();
		//return (context[0] == 0) ? rootIndex : context[context[0]];
	}
	
	// this one is used in iterAttr() in autoPilot
	final protected int getCurrentIndex2(){
		switch(context[0]){
		case -1: return 0;
		case 0: return rootIndex;
		default: return context[context[0]];
	}
	}
	/**
     * getElementFragmentNS returns a ns aware version of the element fragment
     * encapsulated in an ElementFragment object
     * 
     * @return an ElementFragment object
     * @throws NavException
     *  
     */
	public ElementFragmentNs getElementFragmentNs() throws NavException{
	     if (this.ns == false)
	        throw new NavException("getElementFragmentNS can only be called when parsing is ns enabled");
	     
	     FastIntBuffer fib = new FastIntBuffer(3); // init size 8
	     
	     //fill the fib with integer
	     // first get the list of name space nodes
	     int[] ia = context;
	     int d =ia[0]; // -1 for document node, 0 for root element;
	     int c = getCurrentIndex2();
	     
	     
	     int len = (c == 0 || c == rootIndex )? 0: 
	         (getTokenLength(c) & 0xffff); // get the length of qualified node
	     
	     // put the neighboring ATTR_NS nodes into the array
	     // and record the total # of them
	     int i = 0;	    
	     int count=0;
	     if (d > 0){ // depth > 0 every node except document and root element
	         int k=getCurrentIndex2()+1;
	         if (k<this.vtdSize){
	             
	             while(k<this.vtdSize){
	                 int type = this.getTokenType(k);
	                 if (type==VTDNav.TOKEN_ATTR_NAME || type==VTDNav.TOKEN_ATTR_NS)
	                 if (type == VTDNav.TOKEN_ATTR_NS){    
	                     fib.append(k);
	                     //System.out.println(" ns name ==>" + toString(k));
	                 }
	                 k+=2;
	                 //type = this.getTokenType(k);
	             }
	         }
	         count = fib.size();
	        d--; 
            while (d >= 0) {                
                // then search for ns node in the vinicity of the ancestor nodes
                if (d > 0) {
                    // starting point
                    k = ia[d]+1;
                } else {
                    // starting point
                    k = this.rootIndex+1;
                }
                if (k<this.vtdSize){
                   
                    while (k < this.vtdSize) {
                        int type = this.getTokenType(k);
                        if (type == VTDNav.TOKEN_ATTR_NAME
                                || type == VTDNav.TOKEN_ATTR_NS) {
                            boolean unique = true;
                            if (type == VTDNav.TOKEN_ATTR_NS) {
                                for (int z = 0; z < fib.size(); z++) {
                                    //System.out.println("fib size ==>
                                    // "+fib.size());
                                    //if (fib.size()==4);
                                    if (matchTokens(fib.intAt(z), this, k)) {
                                        unique = false;
                                        break;
                                    }
                                }
                                if (unique)
                                    fib.append(k);
                            }
                        }
                        k += 2;
                       // type = this.getTokenType(k);
                    }
                }
                d--;
            }
           // System.out.println("count ===> "+count);
            // then restore the name space node by shifting the array
            int newSz= fib.size()-count;
            for (i= 0; i<newSz; i++ ){
                fib.modifyEntry(i,fib.intAt(i+count));                
            }
            fib.resize(newSz);
	     }
	     
	     long l = getElementFragment();
	     return new ElementFragmentNs(this,l,fib,len);
	}
	
	
	
	/**
     * Get the starting offset and length of an element encoded in a long, upper
     * 32 bits is length; lower 32 bits is offset Unit is in byte. Creation
     * date: (3/15/04 1:47:55 PM)
     */
	public long getElementFragment() throws NavException {
		// a little scanning is needed
		// has next sibling case
		// if not
		
		int depth = getCurrentDepth();
//		 document length and offset returned if depth == -1
		if (depth == -1){
		    int i=vtdBuffer.lower32At(0);
		    if (i==0)
		        return ((long)docLen)<<32| docOffset;
		    else
		        return ((long)(docLen-32))| 32;
		}
		int so = getTokenOffset(getCurrentIndex2()) - 1;
		int length = 0;
		

		// for an element with next sibling
		if (toElement(NEXT_SIBLING)) {

			int temp = getCurrentIndex();
			// rewind
			while (getTokenDepth(temp) < depth) {
				temp--;
			}
			//temp++;
			int so2 = getTokenOffset(temp) - 1;
			// look for the first '>'
			while (getCharUnit(so2) != '>') {
				so2--;
			}
			length = so2 - so + 1;
			toElement(PREV_SIBLING);
			if (encoding <= FORMAT_WIN_1258)
				return ((long) length) << 32 | so;
			else
				return ((long) length) << 33 | (so << 1);
		}

		// for root element
		if (depth == 0) {
			int temp = vtdBuffer.size() - 1;
			boolean b = false;
			int so2 = 0;
			while (getTokenDepth(temp) == -1) {
				temp--; // backward scan
				b = true;
			}
			if (b == false)
				so2 =
					(encoding <= FORMAT_WIN_1258 )
						? (docOffset + docLen - 1)
						: ((docOffset + docLen) >> 1) - 1;
			else
				so2 = getTokenOffset(temp + 1);
			while (getCharUnit(so2) != '>') {
				so2--;
			}
			length = so2 - so + 1;
			if (encoding <= FORMAT_WIN_1258)
				return ((long) length) << 32 | so;
			else
				return ((long) length) << 33 | (so << 1);
		}
		// for a non-root element with no next sibling
		int temp = getCurrentIndex() + 1;
		int size = vtdBuffer.size();
		// temp is not the last entry in VTD buffer
		if (temp < size) {
			while (temp < size && getTokenDepth(temp) >= depth) {
				temp++;
			}
			if (temp != size) {
				int d =
					depth
						- getTokenDepth(temp)
						+ ((getTokenType(temp) == TOKEN_STARTING_TAG) ? 1 : 0);
				int so2 = getTokenOffset(temp) - 1;
				int i = 0;
				// scan backward
				while (i < d) {
					if (getCharUnit(so2) == '>')
						i++;
					so2--;
				}
				length = so2 - so + 2;
				if (encoding <= FORMAT_WIN_1258)
					return ((long) length) << 32 | so;
				else
					return ((long) length) << 33 | (so << 1);
			}
			/*
             * int so2 = getTokenOffset(temp - 1) - 1; int d = depth -
             * getTokenDepth(temp - 1); int i = 0; while (i < d) { if
             * (getCharUnit(so2) == '>') { i++; } so2--; } length = so2 - so +
             * 2; if (encoding < 3) return ((long) length) < < 32 | so; else
             * return ((long) length) < < 33 | (so < < 1);
             */
		}
		// temp is the last entry
		// scan forward search for /> or </cc>
		
		int so2 =
			(encoding <= FORMAT_WIN_1258)
				? (docOffset + docLen - 1)
				: ((docOffset + docLen) >> 1) - 1;
		int d;
	   
	    d = depth + 1;
	    
	    int i = 0;
        while (i < d) {
            if (getCharUnit(so2) == '>') {
                i++;
            }
            so2--;
        }
	  

		length = so2 - so + 2;

		if (encoding <= FORMAT_WIN_1258)
			return ((long) length) << 32 | so;
		else
			return ((long) length) << 33 | (so << 1);
	}
	/**
     * Get the encoding of the XML document.
     * 
     * @return int
     */
	final public int getEncoding() {
		return encoding;
	}
	/**
     * Get the maximum nesting depth of the XML document (>0). max depth is
     * nestingLevel -1
     * 
     * @return int
     */
	final public int getNestingLevel() {
		return nestingLevel;
	}
	
	/**
	 * Return the offset after head (the ending bracket of the starting tag, 
	 * not including an empty element)
	 * @return
	 *
	 */
	final public int getOffsetAfterHead(){
	    
	    int i = getCurrentIndex();
	    if (getTokenType(i)!=VTDNav.TOKEN_STARTING_TAG){
	        return -1;
	    }
	    int j=i+1;
	    while (j<vtdSize && (getTokenType(j)==VTDNav.TOKEN_ATTR_NAME 
	            || getTokenType(j)==VTDNav.TOKEN_ATTR_NS)){
	        j += 2;
	    }
	    
	    int enc = encoding;
	    int offset; // this is character offset
	    if (i+1==j)
	    {
	        offset = getTokenOffset(i)+getTokenLength(i);	                   
	    }else {
	        offset = getTokenOffset(j-1)+getTokenLength(j-1)+1;	                    
	    }
	    
	    while(getCharUnit(offset)!='>'){
	        offset++;	        
	    }
	    
	    if (getCharUnit(offset-1)=='/')
	        return -1;
	    else
	        return (encoding<= FORMAT_WIN_1258)? offset+1:((offset+1)<<1);
	}
	/**
     * Get root index value , which is the index val of root element
     * 
     * @return int
     */
	final public int getRootIndex() {
		return rootIndex;
	}
	/**
     * This method returns of the token index of the type character data or
     * CDATA. Notice that it is intended to support data orient XML (not
     * mixed-content XML). return the index of the text token, or -1 if none
     * exists.
     * 
     * @return int
     */
	public int getText() {
		if (context[0]==-1) return -1;
		int index = (context[0] != 0) ? context[context[0]] + 1 : rootIndex + 1;
		int depth = getCurrentDepth();
		int type; 
		if (index<vtdSize)
			type = getTokenType(index);
		else 
			return -1;

		while (true) {
			if (type == TOKEN_CHARACTER_DATA || type == TOKEN_CDATA_VAL) {
				if (depth == getTokenDepth(index))
					return index;
				else
					return -1;
			} else if (type == TOKEN_ATTR_NS || type == TOKEN_ATTR_NAME) {
				index += 2; // assuming a single token for attr val
			} else if (
				type == TOKEN_PI_NAME
					|| type == TOKEN_PI_VAL
					|| type == TOKEN_COMMENT) {
				if (depth == getTokenDepth(index)) {
					index += 1;
				} else
					return -1;
			} else
				return -1;
			if (index >= vtdSize)
				break;
			type = getTokenType(index);
		}
		return -1;
	}
	/**
     * Get total number of VTD tokens for the current XML document.
     * 
     * @return int
     */
	final public int getTokenCount() {
		return vtdSize;
	}
	/**
     * Get the depth value of a token (>=0).
     * 
     * @return int
     * @param index
     *            int
     */
	final public int getTokenDepth(int index) {
		int i = (int) ((vtdBuffer.longAt(index) & MASK_TOKEN_DEPTH) >> 52);
		if (i != 255)
			return i;
		return -1;
	}
	/**
     * Get the token length at the given index value please refer to VTD spec
     * for more details Length is in terms of the UTF char unit For prefixed
     * tokens, it is the qualified name length. When ns is not enabled, return
     * the full name length for attribute name and element name When ns is
     * enabled, return an int with upper 16 bit for prefix length, lower 16 bit
     * for qname length
     * 
     * @return int
     * @param index
     *            int
     */
	public int getTokenLength(int index) {
		int type = getTokenType(index);
		int depth;
		int val;
		int len = 0;
		long l;
		int temp=0;
		switch (type) {
			case TOKEN_ATTR_NAME :
			case TOKEN_ATTR_NS :
			case TOKEN_STARTING_TAG :
				l = vtdBuffer.longAt(index);
				return (ns == false)
					? (int) ((l & MASK_TOKEN_QN_LEN) >> 32)
					: ((int) ((l & MASK_TOKEN_QN_LEN)
						>> 32)
						| ((int) ((l & MASK_TOKEN_PRE_LEN)
							>> 32)
							<< 5));
			case TOKEN_CHARACTER_DATA:
			case TOKEN_CDATA_VAL:
			case TOKEN_COMMENT: // make sure this is total length
				depth = getTokenDepth(index);
				do{
					len = len +  (int)
					((vtdBuffer.longAt(index)& MASK_TOKEN_FULL_LEN) >> 32);
					temp =  getTokenOffset(index)+(int)
					((vtdBuffer.longAt(index)& MASK_TOKEN_FULL_LEN) >> 32);
					index++;		
					}
				while(index < vtdSize && depth == getTokenDepth(index) 
						&& type == getTokenType(index) && temp == getTokenOffset(index));
				//if (int k=0)
				return len;
			default :
				return (int)
					((vtdBuffer.longAt(index) & MASK_TOKEN_FULL_LEN) >> 32);
		}

	}
	/**
     * Get the starting offset (unit in native char) of the token at the given
     * index.
     * 
     * @return int
     * @param index
     *            int
     */
	final public int getTokenOffset(int index) {
		//return (context[0] != 0)
		//    ? (int) (vtdBuffer.longAt(context[context[0]]) & MASK_TOKEN_OFFSET)
		//    : (int) (vtdBuffer.longAt(rootIndex) & MASK_TOKEN_OFFSET);
		return (int) (vtdBuffer.longAt(index) & MASK_TOKEN_OFFSET);
	}

	/**
     * Get the XML document
     * 
     * @return IByteBuffer
     */
	final public IByteBuffer getXML() {
		return XMLDoc;
	}
	/**
     * Get the token type of the token at the given index value. Creation date:
     * (11/16/03 6:41:51 PM)
     * 
     * @return int
     * @param index
     *            int
     */
	final public int getTokenType(int index) {
		return (int) ((vtdBuffer.longAt(index) & MASK_TOKEN_TYPE) >> 60) & 0xf;
	}
	/**
     * Test whether current element has an attribute with the matching name. "*"
     * will match any attribute name, therefore is a test whether there is any
     * attribute at all if namespace is disabled, this function will not
     * distinguish between ns declaration and attribute otherwise, ns tokens are
     * invisible Creation date: (11/16/03 5:50:26 PM)
     * 
     * @return boolean (true if such an attribute exists)
     * @param an
     *            java.lang.String
     * @exception com.ximpleware.NavException
     *                The exception if the underlying byte content contains
     *                various errors. Notice that we are being conservative in
     *                making little assumption on the correctness of underlying
     *                byte content. This is because the VTD etc can be generated
     *                by another machine from a load-balancer.
     * @exception IllegalArguementException
     *                if an is null
     */
	final public boolean hasAttr(String an) throws NavException {
	    return getAttrVal(an)!=-1;
	}
	/**
     * Test whether the current element has an attribute with matching namespace
     * URL and localname. If ns is false, return false immediately
     * 
     * @return boolean
     * @param URL
     *            java.lang.String (namespace URL)
     * @param ln
     *            java.lang.String (localname )
     * @exception com.ximpleware.NavException
     *                The exception if the underlying byte content contains
     *                various errors. Notice that we are being conservative in
     *                making little assumption on the correctness of underlying
     *                byte content. This is because the VTD can be generated by
     *                another machine such as a load-balancer.
     * @exception IllegalArguementException
     *                if ln is null
     */
	final public boolean hasAttrNS(String URL, String ln) throws NavException {
		return (getAttrValNS(URL, ln) != -1);
	}
	/**
     * Test the token type, to see if it is a starting tag.
     * 
     * @return boolean
     * @param index
     *            int
     */
	private final boolean isElement(int index) {
		return (((vtdBuffer.longAt(index) & MASK_TOKEN_TYPE) >> 60) & 0xf)
			== TOKEN_STARTING_TAG;
	}
	
	/**
     * Test the token type, to see if it is a starting tag or document token
     * (introduced in 1.0).
     * 
     * @return boolean
     * @param index
     *            int
     */
	private final boolean isElementOrDocument(int index){
		long i =(((vtdBuffer.longAt(index) & MASK_TOKEN_TYPE) >> 60) & 0xf);
		return (i==TOKEN_STARTING_TAG||i==TOKEN_DOCUMENT);
	}
	/**
     * Test whether ch is a white space character or not.
     * 
     * @return boolean
     * @param ch
     *            int
     */
	final private boolean isWS(int ch) {
		return (ch == ' ' || ch == '\n' || ch == '\t' || ch == '\r');
	}
	
	/**
     * This function is called by selectElement_P in autoPilot
     * 
     * @param en
     *            element Name
     * @param a
     *            context of current position
     * @param special
     *            whether the test type is node()
     * @return boolean
     * @throws NavException
     */
	protected boolean iterate_preceding(String en, int[] a, boolean special)
	throws NavException {
		int index = getCurrentIndex() - 1;
		int t,d;
		//int depth = getTokenDepth(index);
		//int size = vtdBuffer.size();
		while (index >  0) {
			if (isElementOrDocument(index)) {
				int depth = getTokenDepth(index);
				context[0] = depth;
				//context[depth]=index;
				if (depth>0){
					context[depth] = index;
					t = index -1;
					for (int i=depth-1;i>0;i--){
						if (context[i]>index || context[i] == -1){
							while(t>0){
								d = getTokenDepth(t);
								if ( d == i && isElement(t)){
									context[i] = t;
									break;
								}
								t--;
							}							
						}else
							break;
					}
				}
				//dumpContext();
				if (index!= a[depth] && (special || matchElement(en))) {					
					resolveLC();
					return true;
				}
			} 
			index--;
		}
		return false;	
	}
	/**
     * This function is called by selectElementNS_P in autoPilot
     * 
     * @param URL
     * @param ln
     * @return boolean
     * @throws NavException
     */
	protected boolean iterate_precedingNS(String URL, String ln, int[] a )
	throws NavException {
		int index = getCurrentIndex() - 1;
		int t,d;
		//int depth = getTokenDepth(index);
		//int size = vtdBuffer.size();
		while (index > 0 ) {
			if (isElementOrDocument(index)) {
				int depth = getTokenDepth(index);
				context[0] = depth;
				//context[depth]=index;
				if (depth>0){
					context[depth] = index;
					t = index -1;
					for (int i=depth-1;i>0;i--){
						if (context[i]>index || context[i]==-1){
							while(t>0){
								d = getTokenDepth(t);
								if ( d == i && isElement(t)){
									context[i] = t;
									break;
								}
								t--;
							}							
						}else
							break;
					}
				}
				//dumpContext();
				if (index != a[depth] && matchElementNS(URL,ln)) {					
					resolveLC();
					return true;
				}
			} 
			index--;
		}
		return false;	
	}
	/**
     * This function is called by selectElement_F in autoPilot
     * 
     * @param en
     *            ElementName
     * @param special
     *            whether it is a node()
     * @return boolean
     * @throws NavException
     */

	protected boolean iterate_following(String en, boolean special) 
	throws NavException{
		int index = getCurrentIndex() + 1;
		//int size = vtdBuffer.size();
		while (index < vtdSize) {
			if (isElementOrDocument(index)) {
				int depth = getTokenDepth(index);
				context[0] = depth;
				if (depth>0)
					context[depth] = index;
				if (special || matchElement(en)) {					
					resolveLC();
					return true;
				}
			} 
			index++;
		}
		return false;		
	}
	
	/**
     * This function is called by selectElementNS_F in autoPilot
     * 
     * @param URL
     * @param ln
     * @return boolean
     * @throws NavException
     */
	protected boolean iterate_followingNS(String URL, String ln) 
	throws NavException{
		int index = getCurrentIndex() + 1;
		//int size = vtdBuffer.size();
		while (index < vtdSize) {
			if (isElementOrDocument(index)) {
				int depth = getTokenDepth(index);
				context[0] = depth;
				if (depth>0)
					context[depth] = index;
				if (matchElementNS(URL,ln)) {					
					resolveLC();
					return true;
				}
			} 
			index++;
		}
		return false;
	}
	/**
     * This method is similar to getElementByName in DOM except it doesn't
     * return the nodeset, instead it iterates over those nodes. Notice that
     * this method is called by the "iterate" method in the Autopilot class. "*"
     * will match any element Creation date: (12/2/03 2:31:20 PM)
     * 
     * @return boolean
     * @param dp
     *            int (The depth of the starting position before iterating)
     * @param en
     *            java.lang.String
     * @exception com.ximpleware.NavException
     *                The exception is signaled if the underlying byte content
     *                contains various errors. Notice that we are being
     *                conservative in making little assumption on the
     *                correctness of underlying byte content. This is because
     *                VTD records can be generated by another machine from a
     *                load-balancer. null element name allowed represent
     *                node()in XPath;
     */
	protected boolean iterate(int dp, String en, boolean special)
		throws NavException { // the navigation doesn't rely on LC
		// get the current depth
		int index = getCurrentIndex() + 1;
		int tokenType;
		//int size = vtdBuffer.size();
		while (index < vtdSize) {
		    tokenType = getTokenType(index);
			if (tokenType==VTDNav.TOKEN_ATTR_NAME
			        || tokenType == VTDNav.TOKEN_ATTR_NS){			  
			    index = index+2;
			    continue;
			}
			if (isElementOrDocument(index)) {
				int depth = getTokenDepth(index);
				if (depth > dp) {
					context[0] = depth;
					if (depth>0)
						context[depth] = index;
					if (special || matchElement(en)) {
						if (dp< 4)
						resolveLC();
						return true;
					}
				} else {
					return false;
				}
			}
			index++;

		}
		return false;
	}
	/**
     * This method is similar to getElementByName in DOM except it doesn't
     * return the nodeset, instead it iterates over those nodes . When URL is
     * "*" it will match any namespace if ns is false, return false immediately
     * 
     * @return boolean
     * @param dp
     *            int (The depth of the starting position before iterating)
     * @param URL
     *            java.lang.String
     * @param ln
     *            java.lang.String
     * @exception com.ximpleware.NavException
     *                The exception if the underlying byte content contains
     *                various errors. Notice that we are being conservative in
     *                making little assumption on the correctness of underlying
     *                byte content. This is because VTD records can be generated
     *                by another machine from a load-balancer..
     * @exception IllegalArguementException
     *                if ln is null example
     * 
     * int depth = nv.getCurrentDepth() while(iterateNS(depth,
     * "www.url.com","node_name")){ push(); // store the current position //move
     * position safely pop(); // load the position }
     */
	final protected boolean iterateNS(int dp, String URL, String ln)
		throws NavException {
		if (ns == false)
			return false;
		int tokenType;
		int index = getCurrentIndex() + 1;
		while (index < vtdSize) {
		    tokenType = getTokenType(index);
			if(tokenType==VTDNav.TOKEN_ATTR_NAME
			        || tokenType == VTDNav.TOKEN_ATTR_NS){
			    index = index+2;
			    continue;
			}
			if (isElementOrDocument(index)) {
				int depth = getTokenDepth(index);
				if (depth > dp) {
					context[0] = depth;
					if (depth>0)
						context[depth] = index;
					if (matchElementNS(URL, ln)) {
						if (dp < 4)
							resolveLC();
						return true;
					}
				} else {
					return false;
				}
			}
			index++;
		}
		return false;
	}

	/**
     * Test if the current element matches the given name. Creation date:
     * (11/26/03 2:09:43 PM)
     * 
     * @return boolean
     * @param en
     *            java.lang.String
     * @exception com.ximpleware.NavException
     *                If the underlying raw char representation has errors.
     */
	final public boolean matchElement(String en) throws NavException {
		
		if (en.equals("*") && context[0]!=-1)
			return true;
		if (context[0]==-1)
			return false;
		return matchRawTokenString(
			(context[0] == 0) ? rootIndex : context[context[0]],
			en);
	}
	/**
     * Test whether the current element matches the given namespace URL and
     * localname. URL, when set to "*", matches any namespace (including null),
     * when set to null, defines a "always-no-match" ln is the localname that,
     * when set to *, matches any localname
     * 
     * @return boolean
     * @param URL
     *            java.lang.String
     * @param ln
     *            java.lang.String
     * @exception com.ximpleware.NavException
     *                When there is any encoding conversion error or unknown
     *                entity.
     * @exception java.lang.IllegalArgumentException
     *                if ln == null
     */
    public boolean matchElementNS(String URL, String ln) throws NavException {
    	if (context[0]==-1)
    		return false;
    	int i =
    		getTokenLength((context[0] != 0) ? context[context[0]] : rootIndex);
    	int offset =
    		getTokenOffset((context[0] != 0) ? context[context[0]] : rootIndex);
    	int preLen = (i >> 16) & 0xffff;
    	int fullLen = i & 0xffff;
    
    	if (ln.equals("*")
    		|| ((preLen != 0)
    			? matchRawTokenString(
    				offset + preLen + 1,
    				fullLen - preLen - 1,
    				ln)
    			: matchRawTokenString(
    				offset,
    				fullLen,
    				ln))) { // no prefix, search for xmlns
    		if (((URL != null) ? URL.equals("*") : false)
    			|| (resolveNS(URL, offset, preLen) == true))
    			return true;
    	}
    	return false;
    }
    final private boolean matchRawTokenString(int offset, int len, String s)
    throws NavException{
        return compareRawTokenString(offset, len, s)==0;
    }
    
	protected int compareTokenString(int offset, int len, String s)
            throws NavException {
        int i, l;
        long l1;
        //this.currentOffset = offset;
        int endOffset = offset + len;

        //       System.out.print("currentOffset :" + currentOffset);
        l = s.length();
        //System.out.println(s);
        for (i = 0; i < l && offset < endOffset; i++) {
            l1 = getCharResolved(offset);
            int i1 = s.charAt(i);
            if (i1 < (int) l1)
                return 1;
            if (i1 > (int) l1)
                return -1;
            offset += (int) (l1 >> 32);
        }

        if (i == l && offset < endOffset)
            return 1;
        if (i < l && offset == endOffset)
            return -1;
        return 0;
    }
	/**
     * <em>New in 2.0</em> This method compares two VTD tokens of VTDNav
     * objects The behavior of this method is like compare the strings
     * corresponds to i1 and i2, meaning for text or attribute val, entities
     * will be converted into the corresponding char
     * 
     * @param i1
     * @param vn2
     * @param i2
     * @return -1,0, or 1
     * @throws NavException
     *  
     */
	public int compareTokens(int i1, VTDNav vn2, int i2) 
	throws NavException{
	    int t1, t2;
	    int ch1, ch2;
	    int endOffset1, endOffset2;
	    long l;

		if ( i1 ==i2 && this == vn2)
			return 0;
		
		t1 = this.getTokenType(i1);
		t2 = vn2.getTokenType(i2);
		
		int offset1 = this.getTokenOffset(i1);
		int offset2 = vn2.getTokenOffset(i2);
		
		int len1 =
			(t1 == TOKEN_STARTING_TAG
				|| t1 == TOKEN_ATTR_NAME
				|| t1 == TOKEN_ATTR_NS)
				? getTokenLength(i1) & 0xffff
				: getTokenLength(i1);
		int len2 = 
		    (t2 == TOKEN_STARTING_TAG
				|| t2 == TOKEN_ATTR_NAME
				|| t2 == TOKEN_ATTR_NS)
				? vn2.getTokenLength(i2) & 0xffff
				: vn2.getTokenLength(i2);
		
		endOffset1 = len1+offset1;
		endOffset2 = len2+ offset2;

		for(;offset1<endOffset1&& offset2< endOffset2;){
		    if(t1 == VTDNav.TOKEN_CHARACTER_DATA
		            || t1== VTDNav.TOKEN_ATTR_VAL){
		        l = this.getCharResolved(offset1);
		    } else {
		        l = this.getChar(offset1);
		    }
	        ch1 = (int)l;
	        offset1 += (int)(l>>32);
		    
		    if(t2 == VTDNav.TOKEN_CHARACTER_DATA
		            || t2== VTDNav.TOKEN_ATTR_VAL){
		        l = vn2.getCharResolved(offset2);
		    } else {
		        l = vn2.getChar(offset2);
		    }
	        ch2 = (int)l;
	        offset2 += (int)(l>>32);
	        
		    if (ch1 > ch2)
		        return 1;
		    if (ch1 < ch2)
		        return -1;
		}
		
		if (offset1 == endOffset1 
		        && offset2 < endOffset2)
			return -1;
		else if (offset1 < endOffset1 
		        && offset2 == endOffset2)
		    return 1;
		else
			return 0;
	}
	/**
     * Lexicographically compare a string against a token with given offset and
     * len, entities doesn't get resolved.
     * 
     * @return int (0 if they are equal, 1 if greater, else -1)
     * 
     * @param offset
     *            int
     * @param len
     *            int
     * @param s
     *            java.lang.String
     * @exception com.ximpleware.NavException
     *                The exception if the underlying byte content contains
     *                various errors. Notice that we are being conservative in
     *                making little assumption on the correctness of underlying
     *                byte content. This is because the VTD can be generated by
     *                another machine such as a load-balancer.
     */
	protected int compareRawTokenString(int offset, int len, String s)
		throws NavException {
		int i, l;
		long l1;
		//this.currentOffset = offset;
		int endOffset = offset + len;

			
		//       System.out.print("currentOffset :" + currentOffset);
        l = s.length();
        //System.out.println(s);
        for (i = 0; i < l && offset < endOffset; i++) {
            l1 = getChar(offset);
            int i1 = s.charAt(i); 
            if (i1 < (int) l1)                 
                return 1;
            if (i1 > (int) l1)
                return -1;
            offset += (int) (l1 >> 32);
        }
		
		if (i == l && offset < endOffset)
			return 1;
		if (i<l && offset == endOffset)
		    return -1;
		return 0;		
	}
	/**
     * <em>New in 2.0</em> Compare the string against the token at the given
     * index value. When a token is an attribute name or starting tag, qualified
     * name is what gets compared against This method has to take care of the
     * underlying encoding conversion but it <em> doesn't </em> resolve entity
     * reference in the underlying document The behavior is the same as calling
     * toRawString on index, then compare to s
     * 
     * @param index
     * @param s
     * @return the result of lexical comparison
     * @exception NavException
     *  
     */
	final public int compareRawTokenString(int index, String s)
	throws NavException {
		int type = getTokenType(index);
		int len =
			(type == TOKEN_STARTING_TAG
				|| type == TOKEN_ATTR_NAME
				|| type == TOKEN_ATTR_NS)
				? getTokenLength(index) & 0xffff
				: getTokenLength(index);
		// upper 16 bit is zero or for prefix

		//currentOffset = getTokenOffset(index);
		// point currentOffset to the beginning of the token
		// for UTF 8 and ISO, the performance is a little better by avoid
        // calling getChar() everytime
		return compareRawTokenString(getTokenOffset(index), len, s);
	}
	/**
     * Match the string against the token at the given index value. When a token
     * is an attribute name or starting tag, qualified name is what gets matched
     * against This method has to take care of the underlying encoding
     * conversion but it <em> doesn't </em> resolve entity reference in the
     * underlying document
     * 
     * @return boolean
     * @param index
     *            int (index into the VTD token buffer)
     * @param s
     *            java.lang.String
     * @exception com.ximpleware.NavException
     *                When if the underlying byte content contains various
     *                errors. Notice that we are being conservative in making
     *                little assumption on the correctness of underlying byte
     *                content. This is because the VTD can be generated by
     *                another machine such as a load-balancer.
     */
	final public boolean matchRawTokenString(int index, String s)
		throws NavException {
		int type = getTokenType(index);
		int len =
			(type == TOKEN_STARTING_TAG
				|| type == TOKEN_ATTR_NAME
				|| type == TOKEN_ATTR_NS)
				? getTokenLength(index) & 0xffff
				: getTokenLength(index);
		// upper 16 bit is zero or for prefix

		//currentOffset = getTokenOffset(index);
		// point currentOffset to the beginning of the token
		// for UTF 8 and ISO, the performance is a little better by avoid
        // calling getChar() everytime
		return compareRawTokenString(getTokenOffset(index), len, s)==0;
	}
	/**
     * Match a string with a token represented by a long (upper 32 len, lower 32
     * offset).
     * 
     * @return boolean
     * @param l
     *            long
     * @param s
     *            java.lang.String
     * @exception com.ximpleware.NavException
     *                When if the underlying byte content contains various
     *                errors. Notice that we are being conservative in making
     *                little assumption on the correctness of underlying byte
     *                content. This is because the VTD can be generated by
     *                another machine such as a load-balancer.
     *  
     */
	final private boolean matchRawTokenString(long l, String s) throws NavException {
		int len = (int) ((l & MASK_TOKEN_FULL_LEN) >> 32);
		// a little hardcode is always bad
		//currentOffset = (int) l;
		return compareRawTokenString((int)l, len, s)==0;
	}
	/**
     * Match a string against a token with given offset and len, entities get
     * resolved properly. Creation date: (11/24/03 1:37:42 PM)
     * 
     * @return boolean
     * @param offset
     *            int
     * @param len
     *            int
     * @param s
     *            java.lang.String
     * @exception com.ximpleware.NavException
     *                The exception if the underlying byte content contains
     *                various errors. Notice that we are being conservative in
     *                making little assumption on the correctness of underlying
     *                byte content. This is because the VTD can be generated by
     *                another machine such as a load-balancer.
     * @exception IllegalArguementException
     *                if s is null
     */
	final private boolean matchTokenString(int offset, int len, String s)
		throws NavException {
	    return compareTokenString(offset,len,s)==0;
	}
	
	/**
     * <em>New in 2.0</em> Compare the string against the token at the given
     * index value. When a token is an attribute name or starting tag, qualified
     * name is what gets matched against This method has to take care of the
     * underlying encoding conversion as well as entity reference comparison
     * 
     * @param index
     * @param s
     * @return int
     * @throws NavException
     *  
     */
	public int compareTokenString(int index, String s)
	throws NavException{
		int type = getTokenType(index);
		int len =
			(type == TOKEN_STARTING_TAG
				|| type == TOKEN_ATTR_NAME
				|| type == TOKEN_ATTR_NS)
				? getTokenLength(index) & 0xffff
				: getTokenLength(index);
		// upper 16 bit is zero or for prefix

		//currentOffset = getTokenOffset(index);
		// point currentOffset to the beginning of the token
		// for UTF 8 and ISO, the performance is a little better by avoid
        // calling getChar() everytime
		return compareTokenString(getTokenOffset(index), len, s);
	}
	/**
     * Match the string against the token at the given index value. When a token
     * is an attribute name or starting tag, qualified name is what gets matched
     * against This method has to take care of the underlying encoding
     * conversion as well as entity reference comparison
     * 
     * @return boolean
     * @param index
     *            int
     * @param s
     *            java.lang.String
     * @exception com.ximpleware.NavException
     *                When if the underlying byte content contains various
     *                errors. Notice that we are being conservative in making
     *                little assumption on the correctness of underlying byte
     *                content. This is because the VTD can be generated by
     *                another machine such as a load-balancer.
     */
	final public boolean matchTokenString(int index, String s) throws NavException {
		int type = getTokenType(index);
		int len =
			(type == TOKEN_STARTING_TAG
				|| type == TOKEN_ATTR_NAME
				|| type == TOKEN_ATTR_NS)
				? getTokenLength(index) & 0xffff
				: getTokenLength(index);
		// upper 16 bit is zero or for prefix

		//currentOffset = getTokenOffset(index);
		// point currentOffset to the beginning of the token
		// for UTF 8 and ISO, the performance is a little better by avoid
        // calling getChar() everytime
		return compareTokenString(getTokenOffset(index), len, s)==0;
	}
	/**
     * Match a string against a "non-extractive" token represented by a long
     * (upper 32 len, lower 32 offset).
     * 
     * @return boolean
     * @param l
     *            long
     * @param s
     *            java.lang.String
     * @exception com.ximpleware.NavException
     *                When the underlying byte content contains various errors.
     *                Notice that we are being conservative in making little
     *                assumption on the correctness of underlying byte content.
     *                This is because the VTD can be generated by another
     *                machine such as a load-balancer.
     *  
     */

	final private boolean matchTokenString(long l, String s) throws NavException {
		int len = (int) (l >> 32) & 0xffff;
		//currentOffset = (int) l;
		return compareTokenString((int) l, len, s)==0;
	}


	/**
     * Evaluate the namespace indicator in bit 31 and bit 30. Creation date:
     * (11/27/03 5:38:51 PM)
     * 
     * @return int
     * @param i
     *            int
     */
	final private int NSval(int i) {

		return (int) (vtdBuffer.longAt(i) & MASK_TOKEN_NS_MARK);
	}
	
	/**
     * overWrite is introduced in version 2.0 that allows you to directly
     * overwrite the XML content if the token is long enough If the operation is
     * successful, the new content along with whitespaces will fill the
     * available token space, and there will be no need to regenerate the VTD
     * and LCs !!!
     * <em> The current version (2.0) only allows overwrites on attribute value,
	 * character data, and CDATA</em>
     * 
     * Consider the XML below: <a>good </a> After overwriting the token "good"
     * with "bad," the new XML looks like: <a>bad </a> as you can see, "goo" is
     * replaced with "bad" character-by-character, and the remaining "d" is
     * replace with a white space
     * 
     * @param index
     * @param ba
     *            the byte array contains the new content to be overwritten
     * @return boolean as the status of the overwrite operation
     *  
     */
	final public boolean overWrite(int index, byte[] ba){
	    return overWrite(index,ba,0,ba.length);
	}
	
	
	/**
     * overWrite is introduced in version 2.0 that allows you to directly
     * overwrite the XML content if the token is long enough If the operation is
     * successful, white spaces will be used to fill the available token space,
     * and there will be no need to regenerate the VTD and LCs
     * <em> The current version (2.0) only allows overwrites on attribute value,
	 * character data, and CDATA</em>
     * 
     * Consider the XML below: <a>good </a> After overwriting the token "good"
     * with "bad," the new XML looks like: <a>bad </a> as you can see, "goo" is
     * replaced with "bad", and the remaining "d" is replace with a white space
     * 
     * @param index
     *            the VTD record to which the change will be applied
     * @param ba
     *            the byte array contains the new content to be overwritten
     * @param offset
     * @param len
     * @return boolean as the status of the overwrite operation
     *  
     */
	public boolean overWrite(int index, byte[] ba, int offset, int len){
	    if ( ba == null 
	            || index >= this.vtdSize
	            || offset<0 
	            || offset + len > ba.length)
	        throw new IllegalArgumentException("Illegal argument for overwrite");
	    if (encoding >=VTDNav.FORMAT_UTF_16BE 
	            && (((len & 1)==1 )
	            || ((offset&1)==1))){
	        // for UTF 16, len and offset must be integer multiple
	        // of 2
	        return false;
	    }
	    int t = getTokenType(index);
	    if ( t== VTDNav.TOKEN_CHARACTER_DATA
	            || t==VTDNav.TOKEN_ATTR_VAL 
	            || t==VTDNav.TOKEN_CDATA_VAL){
	        int length = getTokenLength(index);
	        if ( length < len)
	            return false;
	        int os = getTokenOffset(index);
	        int temp = length - len;
	        //System.out.println("temp ==>"+temp);
	        // get XML doc
	        System.arraycopy(ba, offset, XMLDoc.getBytes(),os, len);
	        for (int k=0;k<temp;){
	            //System.out.println("encoding ==>"+encoding);
	            if (encoding < VTDNav.FORMAT_UTF_16BE){
	             // write white spaces
	               // System.out.println("replacing...");
	                (XMLDoc.getBytes())[os+len+k] = ' ';
	                k++;
	            }else{
	                if (encoding == VTDNav.FORMAT_UTF_16BE){
	                    XMLDoc.getBytes()[os+len+k] = 0;
	                    XMLDoc.getBytes()[os+len+k+1] = ' ';
	                }else{	                    
	                    XMLDoc.getBytes()[os+len+k] = ' ';
	                    XMLDoc.getBytes()[os+len+k+1] = 0;
	                }
	                k += 2;
	            }
	        }    
	        return true;
	    }	    
	    return false;
	}
	/**
     * Convert a vtd token into a double. Creation date: (12/8/03 2:28:31 PM)
     * 
     * @return double
     * @exception com.ximpleware.NavException
     *                The exception if the underlying byte content contains
     *                various errors. Notice that we are being conservative in
     *                making little assumption on the correctness of underlying
     *                byte content. This is because the VTD can be generated by
     *                another machine such as a load-balancer.
     */
	public double parseDouble(int index) throws NavException {
		//if (matchTokenString()
		int offset = getTokenOffset(index);
		long l=0;
		int end = offset + getTokenLength(index);
		int t = getTokenType(index);
		boolean b = (t==VTDNav.TOKEN_CHARACTER_DATA )|| (t==VTDNav.TOKEN_ATTR_VAL);
		boolean expneg = false;
		int ch;
		//past the last one by one

		{
		l = b? getCharResolved(offset):getChar(offset);
		ch = (int)l;
	    offset += (int)(l>>32);
		}		

		while (offset < end) { // trim leading whitespaces
			if (!isWS(ch))
				break;
			l = b? getCharResolved(offset):getChar(offset);
			ch = (int)l;
		    offset += (int)(l>>32);
		}

		if (offset > end) // all whitespace
			return Double.NaN;

		boolean neg = (ch == '-');

		if (ch == '-' || ch == '+'){
		    l = b? getCharResolved(offset):getChar(offset);
			ch = (int)l;
		    offset += (int)(l>>32); //get another one if it is sign.
		}
		//left part of decimal
		double left = 0;
		while (offset <= end) {
			//must be <= since we get the next one at last.

			int dig = Character.digit((char) ch, 10); //only consider decimal
			if (dig < 0)
				break;

			left = left * 10 + dig;

			l = b? getCharResolved(offset):getChar(offset);
			ch = (int)l;
		    offset += (int)(l>>32);;
		}

		//right part of decimal
		double right = 0;
		double scale = 1;
		if (ch == '.') {
		    l = b? getCharResolved(offset):getChar(offset);
			ch = (int)l;
		    offset += (int)(l>>32);

			while (offset <= end) {
				//must be <= since we get the next one at last.

				int dig = Character.digit((char) ch, 10);
				//only consider decimal
				if (dig < 0)
					break;

				right = right * 10 + dig;
				scale *= 10;

				l = b? getCharResolved(offset):getChar(offset);
				ch = (int)l;
			    offset += (int)(l>>32);
			}
		}

		//exponent
		long exp = 0;
		if (ch == 'E' || ch == 'e') {
		    l = b? getCharResolved(offset):getChar(offset);
			ch = (int)l;
		    offset += (int)(l>>32);
			expneg = (ch == '-'); //sign for exp
			if (ch == '+' || ch == '-'){
			    l = b? getCharResolved(offset):getChar(offset);
				ch = (int)l;
			    offset += (int)(l>>32); //skip the +/- sign
			}
			int cur = offset;
			//remember the indx, used to find a invalid number like 1.23E

			while (offset <= end) {
				//must be <= since we get the next one at last.

				int dig = Character.digit((char) ch, 10);
				//only consider decimal
				if (dig < 0)
					break;

				exp = exp * 10 + dig;

				l = b? getCharResolved(offset):getChar(offset);
				ch = (int)l;
			    offset += (int)(l>>32);
			}
			if (cur == offset)
			    return Double.NaN;
			//found a invalid number like 1.23E

			//if (expneg)
			//	exp = (-exp);
		}

		//anything left must be space
		while (offset <= end) {
			if (!isWS(ch))
				return Double.NaN;

			l = b? getCharResolved(offset):getChar(offset);
			ch = (int)l;
		    offset += (int)(l>>32);
		}

		double v = (double) left;
		if (right != 0)
			v += ((double) right) / (double) scale;

		if (exp != 0)
			v = (expneg)? v /(Math.pow(10,exp)): v*Math.pow(10,exp);

		return ((neg) ? (-v) : v);
	}

	/**
     * Convert a vtd token into a float. we assume token type to be attr val or
     * character data Creation date: (12/8/03 2:28:18 PM)
     * 
     * @return float
     * @exception com.ximpleware.NavException
     *                The exception if the underlying byte content contains
     *                various errors. Notice that we are being conservative in
     *                making little assumption on the correctness of underlying
     *                byte content. This is because the VTD can be generated by
     *                another machine such as a load-balancer.
     */
	public float parseFloat(int index) throws NavException {

		int offset = getTokenOffset(index);
		int end = offset + getTokenLength(index);
		long l;
		//past the last one by one
		int t = getTokenType(index);
		boolean b = (t==VTDNav.TOKEN_CHARACTER_DATA )|| (t==VTDNav.TOKEN_ATTR_VAL);
		int ch ;
		l = b? getCharResolved(offset):getChar(offset);
		ch = (int)l;
	    offset += (int)(l>>32);

		while (offset <= end) { // trim leading whitespaces
			if (!isWS(ch))
				break;
			l = b? getCharResolved(offset):getChar(offset);
			ch = (int)l;
		    offset += (int)(l>>32);
		}

		if (offset > end) // all whitespace
			throw new NavException("Empty string");

		boolean neg = (ch == '-');

		if (ch == '-' || ch == '+'){
		    l = b? getCharResolved(offset):getChar(offset);
			ch = (int)l;
		    offset += (int)(l>>32); //get another one if it is sign.
		}
		//left part of decimal
		long left = 0;
		while (offset <= end) {
			//must be <= since we get the next one at last.

			int dig = Character.digit((char) ch, 10); //only consider decimal
			if (dig < 0)
				break;

			left = left * 10 + dig;

			l = b? getCharResolved(offset):getChar(offset);
			ch = (int)l;
		    offset += (int)(l>>32);
		}

		//right part of decimal
		long right = 0;
		long scale = 1;
		if (ch == '.') {
		    l = b? getCharResolved(offset):getChar(offset);
			ch = (int)l;
		    offset += (int)(l>>32);

			while (offset <= end) {
				//must be <= since we get the next one at last.

				int dig = Character.digit((char) ch, 10);
				//only consider decimal
				if (dig < 0)
					break;

				right = right * 10 + dig;
				scale *= 10;

				l = b? getCharResolved(offset):getChar(offset);
				ch = (int)l;
			    offset += (int)(l>>32);
			}
		}

		//exponent
		long exp = 0;
		if (ch == 'E' || ch == 'e') {
		    l = b? getCharResolved(offset):getChar(offset);
			ch = (int)l;
		    offset += (int)(l>>32);
			boolean expneg = (ch == '-'); //sign for exp
			if (ch == '+' || ch == '-'){
			    l = b? getCharResolved(offset):getChar(offset);
				ch = (int)l;
			    offset += (int)(l>>32); //skip the +/- sign
			}
			int cur = offset;
			//remember the indx, used to find a invalid number like 1.23E

			while (offset <= end) {
				//must be <= since we get the next one at last.

				int dig = Character.digit((char) ch, 10);
				//only consider decimal
				if (dig < 0)
					break;

				exp = exp * 10 + dig;

				l = b? getCharResolved(offset):getChar(offset);
				ch = (int)l;
			    offset += (int)(l>>32);
			}

			if (cur == offset)
				return Float.NaN;
			//found a invalid number like 1.23E

			if (expneg)
				exp = (-exp);
		}

		//anything left must be space
		while (offset <= end) {
			if (!isWS(ch))
				throw new NavException(toString(index));

			l = b? getCharResolved(offset):getChar(offset);
			ch = (int)l;
		    offset += (int)(l>>32);
		}

		double v = (double) left;
		if (right != 0)
			v += ((double) right) / (double) scale;

		if (exp != 0)
			v = v * Math.pow(10, exp);
		

		float f = (float) v;

		//try to handle overflow/underflow
		if (v >= (double)Float.MAX_VALUE)
			f = Float.MAX_VALUE;
		else if (v <= (double)Float.MIN_VALUE)
			f = Float.MIN_VALUE;
		if (neg)
			f = -f;
		return f;
	}
	/**
     * Convert a vtd token into an int. This method will automatically strip off
     * the leading and trailing we assume token type to be attr val or character
     * data zero, unlike Integer.parseInt(int index)
     * 
     * Creation date: (12/8/03 2:32:22 PM)
     * 
     * @return int
     * @param index
     *            int
     * @exception com.ximpleware.NavException
     *                The exception if the underlying byte content contains
     *                various errors. Notice that we are being conservative in
     *                making little assumption on the correctness of underlying
     *                byte content. This is because the VTD can be generated by
     *                another machine such as a load-balancer.
     */
	public int parseInt(int index) throws NavException {
		return parseInt(index, 10);
	}
	/**
     * Convert a vtd token into an int, with the given radix. we assume token
     * type to be attr val or character data the first char can be either '+' or
     * '-' Creation date: (12/16/03 1:21:20 PM)
     * 
     * @return int
     * @param index
     *            int
     * @param radix
     *            int
     * @exception com.ximpleware.NavException
     *                The exception if the underlying byte content contains
     *                various errors. Notice that we are being conservative in
     *                making little assumption on the correctness of underlying
     *                byte content. This is because the VTD can be generated by
     *                another machine such as a load-balancer.
     */
	protected int parseInt(int index, int radix) throws NavException {
		if (radix < 2 || radix > 36)
			throw new NumberFormatException(
				"radix " + radix + " out of valid range");
		int t = getTokenType(index);
		boolean b = (t==VTDNav.TOKEN_CHARACTER_DATA )|| (t==VTDNav.TOKEN_ATTR_VAL);
		int offset = getTokenOffset(index);
		int endOffset = offset + getTokenLength(index);

		int c;
		long l = b? getCharResolved(offset):getChar(offset);
		c = (int)l;
	    offset += (int)(l>>32);

		// trim leading whitespaces
		while ((c == ' ' || c == '\n' || c == '\t' || c == '\r')
			&& (offset <= endOffset)){
		    l = b? getCharResolved(offset):getChar(offset);
			c = (int)l;
		    offset += (int)(l>>32);
		}
		if (offset > endOffset) // all whitespace
			throw new NumberFormatException(" empty string");

		boolean neg = (c == '-');
		if (neg || c == '+') {
		    l = b? getCharResolved(offset):getChar(offset);
			c = (int)l;
		    offset += (int)(l>>32); //skip sign
		}
		long result = 0;
		long pos = 1;
		while (offset <= endOffset) {
			int digit = Character.digit((char) c, radix);
			if (digit < 0)
				break;

			//Note: for binary we can simply shift to left to improve
            // performance
			result = result * radix + digit;
			//pos *= radix;

			l = b? getCharResolved(offset):getChar(offset);
			c = (int)l;
		    offset += (int)(l>>32);
		}

		if (result > Integer.MAX_VALUE)
			throw new NumberFormatException("Overflow: " + toString(index));

		// take care of the trailing
		while (offset <= endOffset && isWS(c)) {
		    l = b? getCharResolved(offset):getChar(offset);
			c = (int)l;
		    offset += (int)(l>>32);
		}
		if (offset == (endOffset + 1))
			return (int) ((neg) ? (-result) : result);
		else
			throw new NumberFormatException(toString(index));
	}
	/**
     * Convert a vtd token into a long. we assume token type to be attr val or
     * character data Creation date: (12/8/03 2:32:59 PM)
     * 
     * @return long
     * @param index
     *            int
     * @exception com.ximpleware.NavException
     *                The exception if the underlying byte content contains
     *                various errors. Notice that we are being conservative in
     *                making little assumption on the correctness of underlying
     *                byte content. This is because the VTD can be generated by
     *                another machine such as a load-balancer.
     */
	public long parseLong(int index) throws NavException {
		return parseLong(index, 10);
	}
	/**
     * Convert a vtd token into a long, with the given radix. the first char can
     * be either '+' or '-', leading and trailing will be stripped we assume
     * token type to be attr val or character data Creation date: (12/17/03
     * 1:51:06 PM)
     * 
     * @return long
     * @param index
     *            int
     * @param radix
     *            int
     * @exception com.ximpleware.NavException
     *                The exception if the underlying byte content contains
     *                various errors. Notice that we are being conservative in
     *                making little assumption on the correctness of underlying
     *                byte content. This is because the VTD can be generated by
     *                another machine such as a load-balancer.
     */
	protected long parseLong(int index, int radix) throws NavException {
		if (radix < 2 || radix > 36)
			throw new NumberFormatException(
				"radix " + radix + " out of valid range");
		
		int t = getTokenType(index);
		boolean b = (t==VTDNav.TOKEN_CHARACTER_DATA )|| (t==VTDNav.TOKEN_ATTR_VAL);
		
		int offset = getTokenOffset(index);
		int endOffset = offset + getTokenLength(index);

		int c;
		long l;
		l = b? getCharResolved(offset):getChar(offset);
		c = (int)l;
	    offset += (int)(l>>32);

		// trim leading whitespaces
		while ((c == ' ' || c == '\n' || c == '\t' || c == '\r')
			&& (offset <= endOffset)){
		    l = b? getCharResolved(offset):getChar(offset);
			c = (int)l;
		    offset += (int)(l>>32);
		}
		if (offset > endOffset) // all whitespace
			throw new NumberFormatException(" empty string");

		boolean neg = (c == '-');
		if (neg || c == '+'){
		    l = b? getCharResolved(offset):getChar(offset);
			c = (int)l;
		    offset += (int)(l>>32);//skip sign
		}
		long result = 0;
		long pos = 1;
		while (offset <= endOffset) {
			int digit = Character.digit((char) c, radix);
			if (digit < 0)
				break;

			//Note: for binary we can simply shift to left to improve
            // performance
			result = result * radix + digit;
			//pos *= radix;

			l = b? getCharResolved(offset):getChar(offset);
			c = (int)l;
		    offset += (int)(l>>32);;
		}

		if (result > Long.MAX_VALUE)
			throw new NumberFormatException("Overflow: " + toString(index));

		// take care of the trailing
		while (offset <= endOffset && isWS(c)) {
		    l = b? getCharResolved(offset):getChar(offset);
			c = (int)l;
		    offset += (int)(l>>32);
		}
		if (offset == (endOffset + 1))
			return (long) ((neg) ? (-result) : result);
		else
			throw new NumberFormatException(toString(index));
	}
	
	/**
     * Load the context info from ContextBuffer. Info saved including LC and
     * current state of the context
     * 
     * @return boolean
     *  
     */
	final public boolean pop() {
		boolean b = contextStack.load(stackTemp);
		if (b == false)
			return false;
		for (int i = 0; i < nestingLevel; i++) {
			context[i] = stackTemp[i];
		}

		l1index = stackTemp[nestingLevel];
		l2index = stackTemp[nestingLevel + 1];
		l3index = stackTemp[nestingLevel + 2];
		l2lower = stackTemp[nestingLevel + 3];
		l2upper = stackTemp[nestingLevel + 4];
		l3lower = stackTemp[nestingLevel + 5];
		l3upper = stackTemp[nestingLevel + 6];
		atTerminal = (stackTemp[nestingLevel + 7] == 1);
		LN = stackTemp[nestingLevel+8];
		return true;
	}
	/**
     * Load the context info from contextStack2. This method is dedicated for
     * XPath evaluation.
     * 
     * @return status of pop2
     */
	
	
	final protected boolean pop2(){

		boolean b = contextStack2.load(stackTemp);
		if (b == false)
			return false;
		for (int i = 0; i < nestingLevel; i++) {
			context[i] = stackTemp[i];
		}
		l1index = stackTemp[nestingLevel];
		l2index = stackTemp[nestingLevel + 1];
		l3index = stackTemp[nestingLevel + 2];
		l2lower = stackTemp[nestingLevel + 3];
		l2upper = stackTemp[nestingLevel + 4];
		l3lower = stackTemp[nestingLevel + 5];
		l3upper = stackTemp[nestingLevel + 6];
		atTerminal = (stackTemp[nestingLevel + 7] == 1);
		LN = stackTemp[nestingLevel+8];
		return true;
	}
	/**
     * Store the context info into the ContextBuffer. Info saved including LC
     * and current state of the context Creation date: (11/16/03 7:00:27 PM)
     */
	final public void push() {
		
		for (int i = 0; i < nestingLevel; i++) {
			stackTemp[i] = context[i];
		}
		stackTemp[nestingLevel] = l1index;
		stackTemp[nestingLevel + 1] = l2index;
		stackTemp[nestingLevel + 2] = l3index;
		stackTemp[nestingLevel + 3] = l2lower;
		stackTemp[nestingLevel + 4] = l2upper;
		stackTemp[nestingLevel + 5] = l3lower;
		stackTemp[nestingLevel + 6] = l3upper;
		if (atTerminal)
			stackTemp[nestingLevel + 7] =1;
		else
			stackTemp[nestingLevel + 7] =0;
		stackTemp[nestingLevel+8] = LN; 
		contextStack.store(stackTemp);
	}
	/**
     * Store the context info into the contextStack2. This method is reserved
     * for XPath Evaluation
     *  
     */
	
	final protected void push2() {
		
		for (int i = 0; i < nestingLevel; i++) {
			stackTemp[i] = context[i];
		}
		stackTemp[nestingLevel] = l1index;
		stackTemp[nestingLevel + 1] = l2index;
		stackTemp[nestingLevel + 2] = l3index;
		stackTemp[nestingLevel + 3] = l2lower;
		stackTemp[nestingLevel + 4] = l2upper;
		stackTemp[nestingLevel + 5] = l3lower;
		stackTemp[nestingLevel + 6] = l3upper;
		if (atTerminal)
			stackTemp[nestingLevel + 7] =1;
		else
			stackTemp[nestingLevel + 7] =0;
		stackTemp[nestingLevel+8] = LN; 
		contextStack2.store(stackTemp);
	}
	
	/**
     * clear the contextStack2 after XPath evaluation
     * 
     *  
     */
	final protected void clearStack2(){
		contextStack2.clear();
	}
	
	
	/**
     * Sync up the current context with location cache. This operation includes
     * finding out l1index, l2index, l3index and restores upper and lower bound
     * info To improve efficieny this method employs some heuristic search
     * algorithm. The result is that it is quite close to direct access.
     * Creation date: (11/16/03 7:44:53 PM)
     * 
     * @return int The index of the NS URL
     */
	private void resolveLC() {
		int temp;
		if (context[0]<=0)
			return;
		if (l1index < 0 || l1index >= l1Buffer.size()
				|| context[1] != l1Buffer.upper32At(l1index)) {
			if (l1index >= l1Buffer.size() || l1index < 0) {
				l1index = 0;
			}
			if (l1index+1<l1Buffer.size() && context[1] != l1Buffer.upper32At(l1index+1)) {
				int init_guess = (int) (l1Buffer.size() * ((float) context[1] / vtdBuffer
						.size()));
				if (l1Buffer.upper32At(init_guess) > context[1]) {
					while (l1Buffer.upper32At(init_guess) != context[1]) {
						init_guess--;
					}
				} else if (l1Buffer.upper32At(init_guess) < context[1]) {
					while (l1Buffer.upper32At(init_guess) != context[1]) {
						init_guess++;
					}
				}
				l1index = init_guess;
			} else{
				if (context[1]>=l1Buffer.upper32At(l1index)){
					while(context[1]!=l1Buffer.upper32At(l1index) 
						&& l1index < l1Buffer.size()){
						l1index++;
					}
				}
				else{
					while(context[1]!=l1Buffer.upper32At(l1index) 
							&& l1index >=0){
							l1index--;
						}
				}
			}
			//	l1index = l1index + 1;
			// for iterations, l1index+1 is the logical next value for l1index
		}
		if (context[0] == 1)
			return;

		temp = l1Buffer.lower32At(l1index);
		if (l2lower != temp) {
			l2lower = temp;
			// l2lower shouldn't be -1 !!!! l2lower and l2upper always get
			// resolved simultaneously
			l2index = l2lower;
			l2upper = l2Buffer.size() - 1;
			for (int i = l1index + 1; i < l1Buffer.size(); i++) {
				temp = l1Buffer.lower32At(i);
				if (temp != 0xffffffff) {
					l2upper = temp - 1;
					break;
				}
			}
		} // intelligent guess again ??

		if (l2index < 0 || l2index >= l2Buffer.size()
				|| context[2] != l2Buffer.upper32At(l2index)) {
			
			if (l2index >= l2Buffer.size() || l2index<0)
				l2index = l2lower;
			if (l2index+1< l2Buffer.size()&& context[2] == l2Buffer.upper32At(l2index + 1))
				l2index = l2index + 1;
			else if (l2upper - l2lower >= 16) {
				int init_guess = l2lower
						+ (int) ((l2upper - l2lower)
								* ((float) context[2] - l2Buffer
										.upper32At(l2lower)) / (l2Buffer
								.upper32At(l2upper) - l2Buffer
								.upper32At(l2lower)));
				if (l2Buffer.upper32At(init_guess) > context[2]) {
					while (context[2] != l2Buffer.upper32At(init_guess))
						init_guess--;
				} else if (l2Buffer.upper32At(init_guess) < context[2]) {
					while (context[2] != l2Buffer.upper32At(init_guess))
						init_guess++;
				}
				l2index = init_guess;
			} else if (context[2]<l2Buffer.upper32At(l2index)){
				
				while ( context[2] != l2Buffer.upper32At(l2index)) {
					l2index--;
				}
			}
			else { 
				while(context[2]!=l2Buffer.upper32At(l2index))
					l2index++;
			}	
		}

		if (context[0] == 2)
			return;
		
		temp = l2Buffer.lower32At(l2index);
		if (l3lower != temp) {
			//l3lower and l3upper are always together
			l3lower = temp;
			// l3lower shouldn't be -1
			l3index = l3lower;
			l3upper = l3Buffer.size() - 1;
			for (int i = l2index + 1; i < l2Buffer.size(); i++) {
				temp = l2Buffer.lower32At(i);
				if (temp != 0xffffffff) {
					l3upper = temp - 1;
					break;
				}
			}
		}

		if (l3index < 0 || l3index >= l3Buffer.size()
				|| context[3] != l3Buffer.intAt(l3index)) {
			if (l3index >= l3Buffer.size() || l3index <0)
				l3index = l3lower;
			if (l3index+1 < l3Buffer.size() &&
					context[3] == l3Buffer.intAt(l3index + 1))
				l3index = l3index + 1;
			else if (l3upper - l3lower >= 16) {
				int init_guess = l3lower
						+ (int) ((l3upper - l3lower) * ((float) (context[3] - l3Buffer
								.intAt(l3lower)) / (l3Buffer.intAt(l3upper) - l3Buffer
								.intAt(l3lower))));
				if (l3Buffer.intAt(init_guess) > context[3]) {
					while (context[3] != l3Buffer.intAt(init_guess))
						init_guess--;
				} else if (l3Buffer.intAt(init_guess) < context[3]) {
					while (context[3] != l3Buffer.intAt(init_guess))
						init_guess++;
				}
				l3index = init_guess;
			} else if (context[3]<l3Buffer.intAt(l3index)){
				while (context[3] != l3Buffer.intAt(l3index)) {
					l3index--;
				}
			} else {
				while (context[3] != l3Buffer.intAt(l3index)) {
					l3index++;
				}
			}
		}

	}
	/**
     * Test whether the URL is defined in the scope. Null is allowed to indicate
     * the name space is undefined. Creation date: (11/16/03 7:54:01 PM)
     * 
     * @param URL
     *            java.lang.String
     * @exception com.ximpleware.NavException
     *                When there is any encoding conversion error or unknown
     *                entity.
     */
    final protected int lookupNS() throws NavException {
    	if (context[0]==-1)
    	    throw new NavException("Can't lookup NS for document node");
    	int i =
    		getTokenLength((context[0] != 0) ? context[context[0]] : rootIndex);
    	int offset =
    		getTokenOffset((context[0] != 0) ? context[context[0]] : rootIndex);
    	int preLen = (i >> 16) & 0xffff;
    
    	return lookupNS(offset, preLen);
 
    	//return resolveNS(URL, offset, preLen);
    }
    
    /**
     * This function returns the VTD record index of the namespace that matches
     * the prefix of cursor element
     * 
     * @param URL
     * @return int
     *  
     */
    protected int lookupNS(int offset, int len){
    	long l;
    	boolean hasNS = false;
    	int size = vtdBuffer.size();
    	// look for a match in the current hiearchy and return true
    	for (int i = context[0]; i >= 0; i--) {
    		int s = (i != 0) ? context[i] : rootIndex;
    		switch (NSval(s)) { // checked the ns marking
    			case 0xc0000000 :
    				s = s + 1;
    				if (s>=size)
    					break;
    				int type = getTokenType(s);
    
    				while ((type == TOKEN_ATTR_NAME || type == TOKEN_ATTR_NS)) {
    					if (type == TOKEN_ATTR_NS) {
    						// Get the token length
    						int temp = getTokenLength(s);
    						int preLen = ((temp >> 16) & 0xffff);
    						int fullLen = temp & 0xffff;
    						int os = getTokenOffset(s);
    						// xmlns found
    						if (temp == 5 && len == 0) {
    							return s+1;
    						} else if ((fullLen - preLen - 1) == len) {
    							// prefix length identical to local part of ns
                                // declaration
    							boolean a = true;
    							for (int j = 0; j < len; j++) {
    								if (getCharUnit(os + preLen + 1 + j)
    									!= getCharUnit(offset + j)) {
    									a = false;
    									break;
    								}
    							}
    							if (a == true) {
    								return s+1;
    							}
    						}
    					}
    					//return (URL != null) ? true : false;
    					s += 2;
    					if (s>=size)
    						break;
    					type = getTokenType(s);
    				}
    				break;
    			case 0x80000000 :
    				break;
    			default : // check the ns existence, mark bit 31:30 to 11 or 10
    				int k = s + 1;
    			    if (k>=size)
    			    	break;
    				type = getTokenType(k);
    
    				while ( (type == TOKEN_ATTR_NAME || type == TOKEN_ATTR_NS)) {
    					if (type == TOKEN_ATTR_NS) {
    						// Get the token length
    						hasNS = true;
    						int temp = getTokenLength(k);
    						int preLen = ((temp >> 16) & 0xffff);
    						int fullLen = temp & 0xffff;
    						int os = getTokenOffset(k);
    						// xmlns found
    						if (temp == 5 && len == 0) {
    							l = vtdBuffer.longAt(s);
    							hasNS = false;
    							vtdBuffer.modifyEntry(
    								s,
    								l | 0x00000000c0000000L);
    							
    							return k+1;
    							
    						} else if ((fullLen - preLen - 1) == len) {
    							// prefix length identical to local part of ns
                                // declaration
    							boolean a = true;
    							for (int j = 0; j < len; j++) {
    								if (getCharUnit(os + preLen + 1 + j)
    									!= getCharUnit(offset + j)) {
    									a = false;
    									break;
    								}
    							}
    							if (a == true) {
    								l = vtdBuffer.longAt(s);
    								//hasNS = false;
    								vtdBuffer.modifyEntry(
    									s,
    									l | 0x00000000c0000000L);
    								return k+1;
    							}
    						}
    					}
    					//return (URL != null) ? true : false;
    					k += 2;
    					if (k>=size) 
    						break;
    					type = getTokenType(k);
    				}
    				l = vtdBuffer.longAt(s);
    				if (hasNS) {
    					hasNS = false;
    					vtdBuffer.modifyEntry(s, l | 0x00000000c0000000L);
    				} else {
    					vtdBuffer.modifyEntry(s, l | 0x0000000080000000L);
    				}
    				break;
    		}
    	}
    	return 0;
        //return -1;
    }
    private boolean resolveNS(String URL, int offset, int len)
	throws NavException {
    
        int result = lookupNS(offset, len);
        switch(result){
        case 0: 
            if (URL == null){ 
              return true;
            } else {
               return false;
            }
        default:
            if (URL == null)
		        return false;
		    else {
		        return matchTokenString(result, URL);
		    }
        	
        }
    }
	/**
     * A generic navigation method. Move the cursor to the element according to
     * the direction constants If no such element, no position change and return
     * false. Creation date: (12/2/03 1:43:50 PM) Legal direction constants are
     * 
     * <pre>
     *    			ROOT               0 
     * </pre>	
	 *<pre>
     *  		    PARENT  		   1 
     * </pre>
	 *<pre>
     *        	    FIRST_CHILD		   2 
     * </pre>  
	 *<pre>
     *  		    LAST_CHILD 		   3 
     * </pre>
	 *<pre>
     *     	  	    NEXT_SIBLING       4 
     * </pre>
	 *<pre>
     *       	    PREV_SIBLING       5 
     * </pre>
     * 
     * @return boolean
     * @param direction
     *            int
     * @exception com.ximpleware.NavException
     *                When direction value is illegal.
     */
	public boolean toElement(int direction) throws NavException {
		int size;
		switch (direction) {
			case ROOT : // to document element!
				if (context[0] != 0) {
					/*
                     * for (int i = 1; i <= context[0]; i++) { context[i] =
                     * 0xffffffff; }
                     */
					context[0] = 0;
				}
				atTerminal = false;
				l1index = l2index = l3index = -1;
				return true;
			case PARENT :
				if (atTerminal == true){
					atTerminal = false;
					return true;
				}
				if (context[0] > 0) {
					//context[context[0]] = context[context[0] + 1] =
                    // 0xffffffff;
					context[context[0]] = -1;
					context[0]--;
					return true;
				}else if (context[0]==0){
					context[0]=-1; //to be compatible with XPath Data model
					return true;
 				}
				else {
					return false;
				}
			case FIRST_CHILD :
			case LAST_CHILD :
				if (atTerminal) return false;
				switch (context[0]) {
				    case -1:
				    	context[0] = 0;
				    	return true;
					case 0 :
						if (l1Buffer.size() > 0) {
							context[0] = 1;
							l1index =
								(direction == FIRST_CHILD)
									? 0
									: (l1Buffer.size() - 1);
							context[1] = l1Buffer.upper32At(l1index);
							//(int) (vtdToken >> 32);
							return true;
						} else
							return false;
					case 1 :
						l2lower = l1Buffer.lower32At(l1index);
						if (l2lower == -1) {
							return false;
						}
						context[0] = 2;
						l2upper = l2Buffer.size() - 1;
						size = l1Buffer.size();
						for (int i = l1index + 1; i < size; i++) {
							int temp = l1Buffer.lower32At(i);
							if (temp != 0xffffffff) {
								l2upper = temp - 1;
								break;
							}
						}
						//System.out.println(" l2 upper: " + l2upper + " l2
                        // lower : " + l2lower);
						l2index =
							(direction == FIRST_CHILD) ? l2lower : l2upper;
						context[2] = l2Buffer.upper32At(l2index);
						return true;

					case 2 :
						l3lower = l2Buffer.lower32At(l2index);
						if (l3lower == -1) {
							return false;
						}
						context[0] = 3;

						l3upper = l3Buffer.size() - 1;
						size = l2Buffer.size();
						for (int i = l2index + 1; i < size; i++) {
							int temp = l2Buffer.lower32At(i);
							if (temp != 0xffffffff) {
								l3upper = temp - 1;
								break;
							}
						}
						//System.out.println(" l3 upper : " + l3upper + " l3
                        // lower : " + l3lower);
						l3index =
							(direction == FIRST_CHILD) ? l3lower : l3upper;
						context[3] = l3Buffer.intAt(l3index);

						return true;

					default :
						if (direction == FIRST_CHILD) {
							size = vtdBuffer.size();
							int index = context[context[0]] + 1;
							while (index < size) {
								long temp = vtdBuffer.longAt(index);
								int token_type =
									(int) ((MASK_TOKEN_TYPE & temp) >> 60)
										& 0xf;

								if (token_type == TOKEN_STARTING_TAG) {
									int depth =
										(int) ((MASK_TOKEN_DEPTH & temp) >> 52);
									if (depth <= context[0]) {
										return false;
									} else if (depth == (context[0] + 1)) {
										context[0] += 1;
										context[context[0]] = index;
										return true;
									}
								}

								index++;
							} // what condition
							return false;
						} else {
							int index = context[context[0]] + 1;
							int last_index = -1;
							size = vtdBuffer.size();
							while (index < size) {
								long temp = vtdBuffer.longAt(index);
								int depth =
									(int) ((MASK_TOKEN_DEPTH & temp) >> 52);
								int token_type =
									(int) ((MASK_TOKEN_TYPE & temp) >> 60)
										& 0xf;
								
								if (token_type == TOKEN_STARTING_TAG) {
									if (depth <= context[0]) {
										break;
									} else if (depth == (context[0] + 1)) {
										last_index = index;
									}
								}

								index++;
							}
							if (last_index == -1) {
								return false;
							} else {
								context[0] += 1;
								context[context[0]] = last_index;
								return true;
							}
						}
				}

			case NEXT_SIBLING :
			case PREV_SIBLING :
				if(atTerminal)return false;
				switch (context[0]) {
					case -1:
					case 0 :
						return false;
					case 1 :
						if (direction == NEXT_SIBLING) {
							if (l1index + 1 >= l1Buffer.size()) {
								return false;
							}

							l1index++; // global incremental
						} else {
							if (l1index - 1 < 0) {
								return false;
							}
							l1index--; // global incremental
						}
						context[1] = l1Buffer.upper32At(l1index);
						return true;
					case 2 :
						if (direction == NEXT_SIBLING) {
							if (l2index + 1 > l2upper) {
								return false;
							}
							l2index++;
						} else {
							if (l2index - 1 < l2lower) {
								return false;
							}
							l2index--;
						}
						context[2] = l2Buffer.upper32At(l2index);
						return true;
					case 3 :
						if (direction == NEXT_SIBLING) {
							if (l3index + 1 > l3upper) {
								return false;
							}
							l3index++;
						} else {
							if (l3index - 1 < l3lower) {
								return false;
							}
							l3index--;
						}
						context[3] = l3Buffer.intAt(l3index);
						return true;
					default :
						//int index = context[context[0]] + 1;

						if (direction == NEXT_SIBLING) {
							int index = context[context[0]] + 1;
							size = vtdBuffer.size();
							while (index < size) {
								long temp = vtdBuffer.longAt(index);
								int token_type =
									(int) ((MASK_TOKEN_TYPE & temp) >> 60)
										& 0xf;

								if (token_type == TOKEN_STARTING_TAG) {
									int depth =
										(int) ((MASK_TOKEN_DEPTH & temp) >> 52);
									if (depth < context[0]) {
										return false;
									} else if (depth == (context[0])) {
										context[context[0]] = index;
										return true;
									}
								}
								index++;
							}
							return false;
						} else {
							int index = context[context[0]] - 1;
							while (index > context[context[0] - 1]) {
								// scan backforward
								long temp = vtdBuffer.longAt(index);
								int token_type =
									(int) ((MASK_TOKEN_TYPE & temp) >> 60)
										& 0xf;

								if (token_type == TOKEN_STARTING_TAG) {
									int depth =
										(int) ((MASK_TOKEN_DEPTH & temp) >> 52);
									/*
                                     * if (depth < context[0]) { return false; }
                                     * else
                                     */
									if (depth == (context[0])) {
										context[context[0]] = index;
										return true;
									}
								}
								index--;
							} // what condition
							return false;
						}
				}

			default :
				throw new NavException("illegal navigation options");
		}

	}
	/**
     * A generic navigation method. Move the cursor to the element according to
     * the direction constants and the element name If no such element, no
     * position change and return false. "*" matches any element Creation date:
     * (12/2/03 1:43:50 PM) Legal direction constants are <br>
     * 
     * <pre>
     * 		ROOT            0  
     * </pre>
	 * <pre>
     * 		PARENT          1  
     * </pre>
	 * <pre>
     * 		FIRST_CHILD     2  
     * </pre>
	 * <pre>
     * 		LAST_CHILD      3  
     * </pre>
	 * <pre>
     * 		NEXT_SIBLING    4  
     * </pre>
	 * <pre>
     * 		PREV_SIBLING    5  
     * </pre>
     * 
     * <br>
     * for ROOT and PARENT, element name will be ignored.
     * 
     * @return boolean
     * @param direction
     *            int
     * @param en
     *            String
     * @exception com.ximpleware.NavException
     *                When direction value is illegal. Or there are errors in
     *                underlying byte representation of the document
     * @exception IllegalArguementException
     *                if en is null
     */
	public boolean toElement(int direction, String en) throws NavException {
		int size;
		int temp;
		int d;
		int val=0;
		if (en == null)
			throw new IllegalArgumentException(" Element name can't be null ");
		if (en.equals("*"))
			return toElement(direction);
		switch (direction) {
			case ROOT :
				return toElement(ROOT);

			case PARENT :
				return toElement(PARENT);

			case FIRST_CHILD :
				if (atTerminal)return false;
				if (toElement(FIRST_CHILD) == false)
					return false;
				// check current element name
				if (matchElement(en) == false) {
					if (toElement(NEXT_SIBLING, en) == true)
						return true;
					else {
						//toParentElement();
						//context[context[0]] = 0xffffffff;
						context[0]--;
						return false;
					}
				} else
					return true;

			case LAST_CHILD :
				if (atTerminal)return false;
				if (toElement(LAST_CHILD) == false)
					return false;
				if (matchElement(en) == false) {
					if (toElement(PREV_SIBLING, en) == true)
						return true;
					else {
						//context[context[0]] = 0xffffffff;
						context[0]--;
						//toParentElement();
						return false;
					}
				} else
					return true;

			case NEXT_SIBLING :
				if (atTerminal)return false;
				d = context[0];
				
				switch(d)
				{
				  case -1:
				  case 0: return false;
				  case 1: val = l1index; break;
				  case 2: val = l2index; break;
				  case 3: val = l3index; break;
				  	default:
				}
				temp = context[d]; // store the current position
				
				while (toElement(NEXT_SIBLING)) {
					if (matchElement(en)) {
						return true;
					}
				}
				switch(d)
				{
				  case 1: l1index = val; break;
				  case 2: l2index = val; break;
				  case 3: l3index = val; break;
				  	default:
				}
				context[d] = temp;
				return false;

			case PREV_SIBLING :
				if (atTerminal) return false;
				d = context[0];
				switch(d)
				{
				  case -1:
				  case 0: return false;
				  case 1: val = l1index; break;
				  case 2: val = l2index; break;
				  case 3: val = l3index; break;
				  	default:
				}
				temp = context[d]; // store the current position
				
				while (toElement(PREV_SIBLING)) {
					if (matchElement(en)) {
						return true;
					}
				}
				switch(d)
				{
				  case 1: l1index = val; break;
				  case 2: l2index = val; break;
				  case 3: l3index = val; break;
				  	default:
				}
				context[d] = temp;
				return false;

			default :
				throw new NavException("illegal navigation options");
		}
	}
	/**
     * A generic navigation method with namespace support. Move the cursor to
     * the element according to the direction constants and the prefix and local
     * names If no such element, no position change and return false. URL *
     * matches any namespace, including undefined namespaces a null URL means
     * hte namespace prefix is undefined for the element ln * matches any
     * localname Creation date: (12/2/03 1:43:50 PM) Legal direction constants
     * are <br>
     * 
     * <pre>
     * 		ROOT            0  
     * </pre>
	 * <pre>
     * 		PARENT          1  
     * </pre>
	 * <pre>
     * 		FIRST_CHILD     2  
     * </pre>
	 * <pre>
     * 		LAST_CHILD      3  
     * </pre>
	 * <pre>
     * 		NEXT_SIBLING    4  
     * </pre>
	 * <pre>
     * 		PREV_SIBLING    5  
     * </pre>
     * 
     * <br>
     * for ROOT and PARENT, element name will be ignored. If not ns enabled,
     * return false immediately with no position change.
     * 
     * @return boolean
     * @param direction
     *            int
     * @param URL
     *            String
     * @param ln
     *            String
     * @exception com.ximpleware.NavException
     *                When direction value is illegal. Or there are errors in
     *                underlying byte representation of the document
     */
	public boolean toElementNS(int direction, String URL, String ln)
		throws NavException {
		int size;
		int temp;
		int val=0;
		int d; // temp location
		if (ns == false)
			return false;
		switch (direction) {
			case ROOT :
				return toElement(ROOT);

			case PARENT :
				return toElement(PARENT);

			case FIRST_CHILD :
				if (atTerminal)return false;
				if (toElement(FIRST_CHILD) == false)
					return false;
				// check current element name
				if (matchElementNS(URL, ln) == false) {
					if (toElementNS(NEXT_SIBLING, URL, ln) == true)
						return true;
					else {
						//toParentElement();
						//context[context[0]] = 0xffffffff;
						context[0]--;
						return false;
					}
				} else
					return true;

			case LAST_CHILD :
				if (atTerminal)return false;
				if (toElement(LAST_CHILD) == false)
					return false;
				if (matchElementNS(URL, ln) == false) {
					if (toElementNS(PREV_SIBLING, URL, ln) == true)
						return true;
					else {
						//context[context[0]] = 0xffffffff;
						context[0]--;
						//toParentElement();
						return false;
					}
				} else
					return true;

			case NEXT_SIBLING :
				if (atTerminal)return false;
				d = context[0];
				temp = context[d]; // store the current position
				switch(d)
				{
				  case -1:
				  case 0: return false;
				  case 1: val = l1index; break;
				  case 2: val = l2index; break;
				  case 3: val = l3index; break;
				  	default:
				}
				//if (d == 0)
				//	return false;
				while (toElement(NEXT_SIBLING)) {
					if (matchElementNS(URL, ln)) {
						return true;
					}
				}
				switch(d)
				{
				  case 1: l1index = val; break;
				  case 2: l2index = val; break;
				  case 3: l3index = val; break;
				  	default:
				}
				context[d] = temp;
				return false;

			case PREV_SIBLING :
				if (atTerminal)return false;
				d = context[0];
				temp = context[d]; // store the current position
				switch(d)
				{
				  case -1:
				  case 0: return false;
				  case 1: val = l1index; break;
				  case 2: val = l2index; break;
				  case 3: val = l3index; break;
				  	default:
				}
				//if (d == 0)
				//	return false;
				while (toElement(PREV_SIBLING)) {
					if (matchElementNS(URL, ln)) {
						return true;
					}
				}
				switch(d)
				{
				  case 1: l1index = val; break;
				  case 2: l2index = val; break;
				  case 3: l3index = val; break;
				  	default:
				}
				context[d] = temp;
				return false;

			default :
				throw new NavException("illegal navigation options");
		}

	}
	/**
     * This method normalizes a token into a string in a way that resembles DOM.
     * The leading and trailing white space characters will be stripped. The
     * entity and character references will be resolved Multiple whitespaces
     * char will be collapsed into one. Whitespaces via entities will
     * nonetheless be preserved. Creation date: (12/8/03 1:57:10 PM)
     * 
     * @return java.lang.String
     * @param index
     *            int
     * @exception NavException
     *                When the encoding has errors
     */
	public String toNormalizedString(int index) throws NavException {
		int type = getTokenType(index);
		if (type!=TOKEN_CHARACTER_DATA &&
				type!= TOKEN_ATTR_VAL)
			return toRawString(index); 
		long l;
		int len;
		len = getTokenLength(index);
		if (len == 0)
			return "";
		int offset = getTokenOffset(index);
		int endOffset = len + offset - 1; // point to the last character
		StringBuffer sb = new StringBuffer(len);
		
		int ch;

		// trim off the leading whitespaces

		while (true) {
			int temp = offset;
			l = getChar(offset);
			
			ch = (int)l;
			offset += (int)(l>>32);

			if (!isWS(ch)) {
				offset = temp;
				break;
			}
		}

		boolean d = false;
		while (offset <= endOffset) {
			l = getCharResolved(offset);
			ch = (int)l;
			offset += (int)(l>>32);
			if (isWS(ch) && getCharUnit(offset - 1) != ';') {
				d = true;
			} else {
				if (d == false)
					sb.append((char) ch); // java only supports 16 bit unicode
				else {
					sb.append(' ');
					sb.append((char) ch);
					d = false;
				}
			}
		}

		return sb.toString();
	}
	
	
	/**
	 * Get the string length of a token as if it is converted into a normalized UCS string
	 * @param index
	 * @return the string length
	 * @throws NavException
	 *
	 */
	final public int getNormalizedStringLength(int index) throws NavException {
		int type = getTokenType(index);
		if (type!=TOKEN_CHARACTER_DATA &&
				type!= TOKEN_ATTR_VAL)
			return getRawStringLength(index); 
		long l;
		int len,len1=0;
		len = getTokenLength(index);
		if (len == 0)
			return 0;
		int offset = getTokenOffset(index);
		int endOffset = len + offset - 1; // point to the last character
		//StringBuffer sb = new StringBuffer(len);
		
		int ch;
		// trim off the leading whitespaces

		while (true) {
			int temp = offset;
			l = getChar(offset);
			
			ch = (int)l;
			offset += (int)(l>>32);

			if (!isWS(ch)) {
				offset = temp;
				break;
			}
		}

		boolean d = false;
		while (offset <= endOffset) {
			l = getCharResolved(offset);
			ch = (int)l;
			offset += (int)(l>>32);
			if (isWS(ch) && getCharUnit(offset - 1) != ';') {
				d = true;
			} else {
				if (d == false)
					len1++; // java only supports 16 bit unicode
				else {
					len1= len1+2;
					d = false;
				}
			}
		}

		return len1;
	}

	/**
     * Convert a token at the given index to a String, (built-in entity and char
     * references not resolved) (entities and char references not expanded).
     * Creation date: (11/16/03 7:28:49 PM)
     * 
     * @return java.lang.String
     * @param index   int
     * @exception NavException When the encoding has errors
     */
	final public String toRawString(int index) throws NavException {
		int type = getTokenType(index);
		int len;
		if (type == TOKEN_STARTING_TAG
			|| type == TOKEN_ATTR_NAME
			|| type == TOKEN_ATTR_NS)
			len = getTokenLength(index) & 0xffff;
		else
			len = getTokenLength(index);
		int offset = getTokenOffset(index);
		return toRawString(offset, len);
	}
	
	/**
	 * 
	 * @param os
	 * @param len
	 * @return
	 * @throws NavException
	 *
	 */
	final public String toRawString(int os, int len) throws NavException{
	    StringBuffer sb = new StringBuffer(len);	    
	    int offset = os;
	    int endOffset = os + len;
	    long l;
	    while (offset < endOffset) {
	        l = getChar(offset);
	        offset += (int)(l>>32);
	        sb.append((char)l);	                
	    }
	    return sb.toString();
	}
	
	/**
	 * getStringLength return the string length of a token as if the token is converted into 
	 * a string (entity resolved)
	 * @param index
	 * @return the string length as if the token is converted to a UCS string (entity resolved)
	 * @throws NavException
	 *
	 */
	final public int getStringLength(int index) throws NavException {
        int type = getTokenType(index);
        if (type != TOKEN_CHARACTER_DATA && type != TOKEN_ATTR_VAL)
            return getRawStringLength(index);
        int len = 0, len1 = 0;
        
        len = getTokenLength(index);
        int offset = getTokenOffset(index);
        int endOffset = offset + len;
        long l;

        while (offset < endOffset) {
            l = getCharResolved(offset);
            offset += (int) (l >> 32);
            len1++;
        }
        return len1;
    }


	/**
	 * Get the string length as if the token is converted into a UCS string (entity not resolved)
	 * @param index
	 * @return
	 * @throws NavException
	 *
	 */
	final public int getRawStringLength(int index) throws NavException {
        int type = getTokenType(index);
        int len = 0, len1 = 0;
        if (type == TOKEN_STARTING_TAG || type == TOKEN_ATTR_NAME
                || type == TOKEN_ATTR_NS)
            len = getTokenLength(index) & 0xffff;
        else
            len = getTokenLength(index);
        int offset = getTokenOffset(index);
        int endOffset = offset + len;
        long l;
        while (offset < endOffset) {
            l = getChar(offset);
            offset += (int) (l >> 32);
            len1++;
        }
        return len1;
    }
	
	/**
     * Convert a token at the given index to a String, (entities and char
     * references resolved). An attribute name or an element name will get the
     * UCS2 string of qualified name Creation date: (11/16/03 7:27:19 PM)
     * 
     * @return java.lang.String
     * @param index
     * @exception NavException
     */
	public String toString(int index) throws NavException {
		int type = getTokenType(index);
		if (type!=TOKEN_CHARACTER_DATA &&
				type!= TOKEN_ATTR_VAL)
			return toRawString(index); 
		int len;
		long l;
		len = getTokenLength(index);

		int offset = getTokenOffset(index);
		return toString(offset, len);
	}
	
	/**
     * Convert the byte content segment (in terms of offset and length) to
     * String
     * 
     * @param os
     *            the offset of the segment
     * @param len
     *            the length of the segment
     * @return the corresponding string value
     * @throws NavException
     *  
     */
	final public String toString(int os, int len) throws NavException{
	    StringBuffer sb = new StringBuffer(len);	    
	    int offset = os;
	    int endOffset = os + len;
	    long l;
	    while (offset < endOffset) {
	        l = getCharResolved(offset);
	        offset += (int)(l>>32);
	        sb.append((char)l);	                
	    }
	    return sb.toString();
	}
	
/**
 * This method matches two VTD tokens of VTDNav objects
 * 
 * @param i1
 *            index of the first token
 * @param vn2
 *            the second VTDNav instance
 * @param i2
 *            index of the second token
 * @return boolean true if two tokens are lexically identical
 *  
 */
	final public boolean matchTokens(int i1, VTDNav vn2, int i2) 
	throws NavException{
	    return compareTokens(i1,vn2,i2)==0;
	}
	


	
	/**
     * Set the value of atTerminal This function only gets called in XPath eval
     * when a step calls for @* or child::text()
     * @param b
     */
	final protected void setAtTerminal(boolean b){
		atTerminal = b;
	}
	
	/**
     * Get the value of atTerminal This function only gets called in XPath eval
     * 
     * @return boolean
     */
	final protected boolean getAtTerminal(){
		return atTerminal;
	}
	/**
     * This is for debugging purpose
     * 
     * @param fib
     */
	
	public void sampleState(FastIntBuffer fib){
//		for(int i=0;i<context.)
//			context[i] = -1;
//		fib.append(context);
		if (context[0]>=1)
			fib.append(l1index);
		
		
		if (context[0]>=2){
			fib.append(l2index);
			fib.append(l2lower);
			fib.append(l2upper);				
		}
		
		if (context[0]>=3){
		   fib.append(l3index);
		   fib.append(l3lower);
		   fib.append(l3upper);
		}
	}
	
	/**
     * Write VTDNav's internal structure into an OutputStream
     * 
     * @param os
     * @throws IndexWriteException
     * @throws IOException
     *  
     */
	public void writeIndex(OutputStream os) throws IndexWriteException, IOException{
	    IndexHandler.writeIndex((byte)1,
	            this.encoding,
	            this.ns,
	            true,
	            this.nestingLevel-1,
	            3,
	            this.rootIndex,
	            this.XMLDoc.getBytes(),
	            this.docOffset,
	            this.docLen,
	            (FastLongBuffer)this.vtdBuffer,
	            (FastLongBuffer)this.l1Buffer,
	            (FastLongBuffer)this.l2Buffer,
	            (FastIntBuffer)this.l3Buffer,
	            os);
	}
	
	/**
	 * Write VTDNav's VTD and LCs into an OutputStream (XML not written out)
	 * @param os
	 * @throws IndexWriteException
	 * @throws IOException
	 *
	 */
	public void writeSeparateIndex(OutputStream os) throws IndexWriteException, IOException{
	    IndexHandler.writeSeparateIndex((byte)2,
	            this.encoding,
	            this.ns,
	            true,
	            this.nestingLevel-1,
	            3,
	            this.rootIndex,
	           // this.XMLDoc.getBytes(),
	            this.docOffset,
	            this.docLen,
	            (FastLongBuffer)this.vtdBuffer,
	            (FastLongBuffer)this.l1Buffer,
	            (FastLongBuffer)this.l2Buffer,
	            (FastIntBuffer)this.l3Buffer,
	            os);
	}
	/**
     * Write VTDNav's internal structure into a VTD+XML file
     * 
     * @param fileName
     * @throws IOException
     * @throws IndexWriteException
     *  
     */
	public void writeIndex(String fileName) throws IOException,IndexWriteException{
	    FileOutputStream fos = new FileOutputStream(fileName);
	    writeIndex(fos);
	    fos.close();
	}
	/**
	 * Write VTDNav's internal structure (VTD and LCs, but not XML) into a file
	 * @param fileName
	 * @throws IOException
	 * @throws IndexWriteException
	 *
	 */
	public void writeSeparateIndex(String fileName) throws IOException,IndexWriteException{
	    FileOutputStream fos = new FileOutputStream(fileName);
	    writeSeparateIndex(fos);
	    fos.close();
	}
	
	/**
     * Precompute the size of VTD+XML index
     * 
     * @return size of the index
     *  
     */
	 
	public long getIndexSize(){
	    int size;
	    if ( (docLen & 7)==0)
	       size = docLen;
	    else
	       size = ((docLen >>3)+1)<<3;
	    
	    size += (vtdBuffer.size()<<3)+
	            (l1Buffer.size()<<3)+
	            (l2Buffer.size()<<3);
	    
	    if ((l3Buffer.size() & 1) == 0){ //even
	        size += l3Buffer.size()<<2;
	    } else {
	        size += (l3Buffer.size()+1)<<2; //odd
	    }
	    return size+64;
	}
	
	/**
	 * Duplicate the VTDNav instance with shared XML, VTD and LC buffers
	 * This method may be useful for parallel XPath evaluation
	 * @return a VTDNav instance
	 *
	 */
	public VTDNav duplicateNav(){
	    return new VTDNav(rootIndex,
	            encoding,
	            ns,
	            nestingLevel-1,
	            XMLDoc,
	            vtdBuffer,
	            l1Buffer,
	            l2Buffer,
	            l3Buffer,
	            docOffset,
	            docLen
	            );
	}
}
