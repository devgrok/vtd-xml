/* 
 * Copyright (C) 2002-2010 XimpleWare, info@ximpleware.com
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
package com.ximpleware;
import java.io.*;

import com.ximpleware.transcode.*;
/**
 * XMLModifier offers an easy-to-use interface for users to
 * take advantage of the incremental update of VTD-XML
 * The XML modifier assumes there is a master document on which
 * the modification is applied: users can remove an element, update
 * a token, replace an element name, or insert new content anywhere in the document
 * transcoding methods are built-in
 * 
 * The process:
 * * The modification operations are recorded first
 * * The output() is called to generate output document
 *
 */

public class XMLModifier {
    protected VTDNav md; // master document
    
    private static final long MASK_DELETE = 0x00000000000000000L; //0000
    private static final long MASK_INSERT_SEGMENT_BYTE = 0x2000000000000000L; //0010
    private static final long MASK_INSERT_BYTE = 0x4000000000000000L;//0100
    //private static final long MASK_INSERT_SEGMENT_STRING = 0x6000000000000000L; //0110
    //private static final long MASK_INSERT_STRING = 0x8000000000000000L; //1000
    private static final long MASK_INSERT_FRAGMENT_NS = 0xa000000000000000L; //1010
    
    protected FastObjectBuffer fob;
    protected FastLongBuffer flb;
    protected intHash deleteHash; // one deletion per offset val
    protected intHash insertHash;   // one insert per offset val
    protected String charSet;
    int encoding;
    
    public class ByteSegment{
        byte[] ba;
        int offset;
        int len;
    }
    /**
     * Constructor for XMLModifier that takes VTDNav object as the master document
     * @param masterDocument is the document on which the modification is applied
     */
    public XMLModifier(VTDNav masterDocument) throws ModifyException{
        bind(masterDocument);
    }
    

    /**
     * Argument-less constructor for XMLModifier,
     * needs to call bind to attach the master document to an instance
     * of XMLModifier
     *
     */
    public XMLModifier(){
        md = null;
    }
    /**
     * Attach master document to this instance of XMLModifier
     * so all the operations occuring aftewards are based on this instance
     * of VTDNav
     * @param masterDocument
     *
     */
    public void bind(VTDNav masterDocument) throws ModifyException{
        if (masterDocument == null)
            throw new IllegalArgumentException("MasterDocument can't be null");
        md = masterDocument;
        flb = new FastLongBuffer();
        fob = new FastObjectBuffer();
        int i = intHash.determineHashWidth(md.vtdSize);
        insertHash = new intHash(i);
        deleteHash = new intHash(i);    
        //determine encoding charset string here
        encoding = md.getEncoding();
        switch(encoding){
        	case VTDNav.FORMAT_ASCII:
        	    charSet = "ASCII";
        	    break;
            case VTDNav.FORMAT_ISO_8859_1:
                charSet = "ISO8859_1";
            	break;
            case VTDNav.FORMAT_UTF8:
                charSet = "UTF8";
            	break;
            case VTDNav.FORMAT_UTF_16BE:
                charSet = "UnicodeBigUnmarked";
            	break;
            case VTDNav.FORMAT_UTF_16LE:
                charSet = "UnicodeLittleUnmarked";
            	break;
            case VTDNav.FORMAT_ISO_8859_2:
                charSet = "ISO8859_2";
            	break;
        	case VTDNav.FORMAT_ISO_8859_3:
        	    charSet = "ISO8859_3";
        		break;
        	case VTDNav.FORMAT_ISO_8859_4:
        	    charSet = "ISO8859_4";
        	    break;
        	case VTDNav.FORMAT_ISO_8859_5:
        	    charSet = "ISO8859_5";
        	    break;
        	case VTDNav.FORMAT_ISO_8859_6:
        	    charSet = "ISO8859_6";
        		break;
        	case VTDNav.FORMAT_ISO_8859_7:
        	    charSet = "ISO8859_7";
        		break;
        	case VTDNav.FORMAT_ISO_8859_8:
        	    charSet = "ISO8859_8";
        		break;
        	case VTDNav.FORMAT_ISO_8859_9:
        	    charSet = "ISO8859_9";
    			break;
        	case VTDNav.FORMAT_ISO_8859_10:
        	    charSet = "ISO8859_10";
    			break;
        	case VTDNav.FORMAT_ISO_8859_11:
        	    charSet = "x-iso-8859-11";
    			break;
        	case VTDNav.FORMAT_ISO_8859_12:
        	    charSet = "ISO8859_12";
    			break;
        	case VTDNav.FORMAT_ISO_8859_13:
        	    charSet = "ISO8859_13";
				break;
        	case VTDNav.FORMAT_ISO_8859_14:
        	    charSet = "ISO8859_14";
				break;
        	case VTDNav.FORMAT_ISO_8859_15:
        	    charSet = "ISO8859_15";
				break;
        	
        	
        	case VTDNav.FORMAT_WIN_1250:
        	    charSet = "Cp1250";
				break;
        	case VTDNav.FORMAT_WIN_1251:
        	    charSet = "Cp1251";
				break;
        	case VTDNav.FORMAT_WIN_1252:
        	    charSet = "Cp1252";
				break;
        	case VTDNav.FORMAT_WIN_1253:
        	    charSet = "Cp1253";
				break;
        	case VTDNav.FORMAT_WIN_1254:
        	    charSet = "Cp1254";
				break;
        	case VTDNav.FORMAT_WIN_1255:
        	    charSet = "Cp1255";
				break;
        	case VTDNav.FORMAT_WIN_1256:
        	    charSet = "Cp1256";
				break;
        	case VTDNav.FORMAT_WIN_1257:
        	    charSet = "Cp1257";
				break;
        	case VTDNav.FORMAT_WIN_1258:
        	    charSet = "Cp1258";
				break;
            default:
                throw new ModifyException
                ("Master document encoding not yet supported by XML modifier");
        }
    }
    /**
     * Removes content from the master XML document 
     * It first calls getCurrentIndex() if the result is 
     * a starting tag, then the entire element referred to
     * by the starting tag is removed
     * If the result is an attribute name or ns node, then 
     * the corresponding attribute name/value pair is removed
     * If the token type is one of text, CDATA or commment,
     * then the entire node, including the starting and ending 
     * delimiting text surrounding the content, is removed
     *
     */
    public void remove() throws NavException,ModifyException{
        
        int i = md.getCurrentIndex();
        int type = md.getTokenType(i);
        if (type==VTDNav.TOKEN_STARTING_TAG){
            long l = md.getElementFragment();
            removeContent((int)l, (int)(l>>32));            
        } else if (type == VTDNav.TOKEN_ATTR_NAME 
                || type==VTDNav.TOKEN_ATTR_NS){
            removeAttribute(i);
        } else {
            removeToken(i);
        }
    }
    
    /**
     * Remove a byte segment from XML.
     * l's upper 32 bits is length in # of bytes
     * l's lower 32 bits is byte offset 
     * @param l
     * @throws NavException
     * @throws ModifyException
     *
     */
    public void remove(long l) throws NavException,ModifyException{
        removeContent((int)l, (int)(l>>32));
    }
    
    /**
     * Remove the token content. If the token type is text, CDATA
     * or comment, then the entire node, including the starting and 
     * ending delimiting text, will be removed as well
     * @param i the index for the content
     *
     */
    public void removeToken(int i) throws ModifyException{        
        int type = md.getTokenType(i);
        int os = md.getTokenOffset(i);
		//int len = md.getTokenLength(i)&0xffff;
		int len =
			(type == VTDNav.TOKEN_STARTING_TAG
				|| type == VTDNav.TOKEN_ATTR_NAME
				|| type == VTDNav.TOKEN_ATTR_NS)
				? md.getTokenLength(i) & 0xffff
				: md.getTokenLength(i);
        switch(type){
        	case VTDNav.TOKEN_CDATA_VAL:        	   
        		if (encoding < VTDNav.FORMAT_UTF_16BE)
        		    removeContent(os - 9, len + 12 );
        		else
        		    removeContent((os - 9)<<1,(len+12)<<1);
        		return;
        		 
        	case VTDNav.TOKEN_COMMENT:
           	    if (encoding < VTDNav.FORMAT_UTF_16BE)
           	        removeContent(os-4, len+7);
           	    else
           	        removeContent((os-4) << 1, (len+7) << 1);
           	    return;
        		
        	default:
    			if (encoding < VTDNav.FORMAT_UTF_16BE)
        	        removeContent(os, len);
        	    else
        	        removeContent((os) << 1, (len) << 1);
        	    return;        	    
        }
    }
    /**
     * Remove an attribute name value pair from the master document.
     * @param attrNameIndex
     *
     */
    public void removeAttribute(int attrNameIndex) throws ModifyException{
         int type = md.getTokenType(attrNameIndex);
        if (type != VTDNav.TOKEN_ATTR_NAME&& type != VTDNav.TOKEN_ATTR_NS)
            throw new ModifyException("token type should be attribute name");
        int os1 = md.getTokenOffset(attrNameIndex);
        int os2 = md.getTokenOffset(attrNameIndex+1);
        int len2 = md.getTokenLength(attrNameIndex+1);
   	    if (encoding < VTDNav.FORMAT_UTF_16BE)
   	        removeContent(os1,os2+len2-os1+1); 
	    else 
	        removeContent(os1<<1,(os2+len2-os1+1)<<1); 
		    
    }
    
    /**
     * Remove a segment of byte content from master XML doc.
     * The segment is denoted by its offset and len. 
     * @param offset
     * @param len
     *
     */
    public void removeContent(int offset, int len) throws ModifyException{

        if (offset < md.docOffset || len > md.docLen 
                || offset + len > md.docOffset + md.docLen){
            throw new ModifyException("Invalid offset or length for removeContent");
        }
        if (deleteHash.isUnique(offset)==false)
            throw new ModifyException("There can be only one deletion per offset value");
            
        flb.append(((long)len)<<32 | offset | MASK_DELETE);
        fob.append((Object)null);
    }
    
    /**
     * insert the byte content into XML
     * @param offset
     * @param content
     *
     */
    public void insertBytesAt(int offset, byte[] content) throws ModifyException{

        if (insertHash.isUnique(offset)==false){
            throw new ModifyException("There can be only one insert per offset");
        }
        flb.append( (long)offset | MASK_INSERT_BYTE);
        fob.append(content);
    }
    /**
     * Insert ns compensated element fragment into the document
     * @param ef
     *
     */
    private void insertElementFragmentNsAt(int offset, ElementFragmentNs ef) throws ModifyException{
        if (insertHash.isUnique(offset)==false){
            throw new ModifyException("There can be only one insert per offset");
        }
        flb.append((long)offset | MASK_INSERT_FRAGMENT_NS);
        fob.append(ef);
    }
    
    /**
     * Insert a segment of the byte content into XML
     * @param offset
     * @param content
     * @param contentOffset
     * @param contentLen
     * @throws ModifyException
     *
     */
    public  void insertBytesAt(int offset, byte[] content, int contentOffset, int contentLen) 
    throws ModifyException {
        if (insertHash.isUnique(offset)==false){
            throw new ModifyException("There can be only one insert per offset");
        }
        if (contentOffset < 0 
                || contentLen <0 
                || contentOffset+contentLen >= content.length){
            throw new ModifyException("Invalid contentOffset and/or contentLen");
        }
        flb.append( (long)offset | MASK_INSERT_SEGMENT_BYTE);
        ByteSegment bs = new ByteSegment();
        bs.ba = content;
        bs.len = contentLen;
        bs.offset = contentOffset;
       
        fob.append(bs);
    }
    /**
     * Insert a segment of content into XML
     * l (a long)'s upper 32 bit is length, lower 32 bit is offset
     * @param offset
     * @param content
     * @param l
     * @throws ModifyException
     *
     */
    private void insertBytesAt(int offset, byte[] content, long l)
    throws ModifyException {
        if (insertHash.isUnique(offset)==false){
            throw new ModifyException("There can be only one insert per offset");
        }
        int contentOffset = (int)l;
        int contentLen = (int)(l>>32); 
        if (contentOffset < 0 
                || contentLen <0 
                || contentOffset+contentLen >= content.length){
            throw new ModifyException("Invalid contentOffset and/or contentLen");
        }
        flb.append( (long)offset | MASK_INSERT_SEGMENT_BYTE);
        ByteSegment bs = new ByteSegment();
        bs.ba = content;
        bs.len = contentLen;
        bs.offset = contentOffset;
        fob.append(bs);
    }
   
   /**
    * Update the token with the given byte array content,
    * @param index
    * @param newContentBytes
    * @throws ModifyException
    * @throws UnsupportedEncodingException
    *
    */
    public void updateToken(int index, byte[] newContentBytes) 
    	throws ModifyException,UnsupportedEncodingException{
        if (newContentBytes==null)
            throw new IllegalArgumentException
            ("newContentBytes can't be null");
        int offset = md.getTokenOffset(index);
        
        int type = md.getTokenType(index);
        /*int len =
			(type == VTDNav.TOKEN_STARTING_TAG
				|| type == VTDNav.TOKEN_ATTR_NAME
				|| type == VTDNav.TOKEN_ATTR_NS)
				? md.getTokenLength(index) & 0xffff
				: md.getTokenLength(index);*/
        // one insert
        switch(type){
        	case VTDNav.TOKEN_CDATA_VAL:
        	    if (encoding < VTDNav.FORMAT_UTF_16BE)
        	        insertBytesAt(offset-9,newContentBytes);
        	    else 
        	        insertBytesAt((offset-9)>>1,newContentBytes);
        		break;
        	case VTDNav.TOKEN_COMMENT:
           	    if (encoding < VTDNav.FORMAT_UTF_16BE)
        	        insertBytesAt(offset-4,newContentBytes);
        	    else 
        	        insertBytesAt((offset-4)>>1,newContentBytes);
        		break;
        	    
        	default: 
        	    if (encoding < VTDNav.FORMAT_UTF_16BE)
        	        insertBytesAt(offset,newContentBytes);
        	    else
        	        insertBytesAt(offset<<1,newContentBytes);
        }
        // one delete
        removeToken(index);        	
    }
    
    /**
     * Update the token with the transcoded representation of 
     * given byte array content,
     * @param index
     * @param newContentBytes
     * @throws ModifyException
     * @throws UnsupportedEncodingException
     * @throws TranscodeException
     *
     */
     public void updateToken(int index, byte[] newContentBytes, int src_encoding) 
     	throws ModifyException,UnsupportedEncodingException,TranscodeException{
         if (src_encoding == encoding){
             updateToken(index,newContentBytes);
             return;
         }
         if (newContentBytes==null)
             throw new IllegalArgumentException
             ("newContentBytes can't be null");
         int offset = md.getTokenOffset(index);
         
         int type = md.getTokenType(index);
         /*int len =
 			(type == VTDNav.TOKEN_STARTING_TAG
 				|| type == VTDNav.TOKEN_ATTR_NAME
 				|| type == VTDNav.TOKEN_ATTR_NS)
 				? md.getTokenLength(index) & 0xffff
 				: md.getTokenLength(index);*/
         // one insert
         byte[] bo = Transcoder.transcode(newContentBytes,0,
                 newContentBytes.length,src_encoding,encoding);
         switch(type){
         	case VTDNav.TOKEN_CDATA_VAL:
         	    if (encoding < VTDNav.FORMAT_UTF_16BE)
         	        insertBytesAt(offset-9, bo);
         	    else 
         	        insertBytesAt((offset-9)>>1, bo);
         		break;
         	case VTDNav.TOKEN_COMMENT:
            	    if (encoding < VTDNav.FORMAT_UTF_16BE)
         	        insertBytesAt(offset-4, bo);
         	    else 
         	        insertBytesAt((offset-4)>>1, bo);
         		break;
         	    
         	default: 
         	    if (encoding < VTDNav.FORMAT_UTF_16BE)
         	        insertBytesAt(offset, bo);
         	    else
         	        insertBytesAt(offset<<1, bo);
         }
         // one delete
         removeToken(index);        	
     }
    /**
     * Update token with a segment of byte array (in terms of offset and length)
     * @param index
     * @param newContentBytes
     * @param contentOffset
     * @param contentLen
     * @throws ModifyException
     * @throws UnsupportedEncodingException
     *
     */
    public void updateToken(int index, byte[] newContentBytes, int contentOffset, int contentLen) 
	throws ModifyException,UnsupportedEncodingException{
    if (newContentBytes==null)
        throw new IllegalArgumentException
        ("newContentBytes can't be null");

    int offset = md.getTokenOffset(index);
    //int len = md.getTokenLength(index);
    int type = md.getTokenType(index);
    /*int len =
		(type == VTDNav.TOKEN_STARTING_TAG
			|| type == VTDNav.TOKEN_ATTR_NAME
			|| type == VTDNav.TOKEN_ATTR_NS)
			? md.getTokenLength(index) & 0xffff
			: md.getTokenLength(index);*/
    // one insert
    switch(type){
    	case VTDNav.TOKEN_CDATA_VAL:
    	    if (encoding < VTDNav.FORMAT_UTF_16BE)
    	        insertBytesAt(offset-9,newContentBytes,contentOffset, contentLen);
    	    else 
    	        insertBytesAt((offset-9)<<1,newContentBytes,contentOffset, contentLen);
    		break;
    	case VTDNav.TOKEN_COMMENT:
       	    if (encoding < VTDNav.FORMAT_UTF_16BE)
    	        insertBytesAt(offset-4,newContentBytes,contentOffset, contentLen);
    	    else 
    	        insertBytesAt((offset-4)<<1,newContentBytes,contentOffset, contentLen);
    		break;
    	    
    	default: 
    	    if (encoding < VTDNav.FORMAT_UTF_16BE)
    	        insertBytesAt(offset,newContentBytes,contentOffset, contentLen);
    	    else 
    	        insertBytesAt(offset<<1,newContentBytes,contentOffset, contentLen);
    }
    // one delete
    removeToken(index);        	
}
    
    /**
     * Update token with the transcoded representation of 
     * a segment of byte array (in terms of offset and length)
     * @param index
     * @param newContentBytes
     * @param contentOffset
     * @param contentLen
     * @throws ModifyException
     * @throws UnsupportedEncodingException
     * @throws TranscodeException
     *
     */
    public void updateToken(int index, byte[] newContentBytes, 
            int contentOffset, int contentLen, int src_encoding) 
	throws ModifyException,UnsupportedEncodingException, TranscodeException{
        
        if (src_encoding == encoding) {
            updateToken(index, newContentBytes, contentOffset, contentLen);
            return;
        }
        if (newContentBytes == null)
            throw new IllegalArgumentException("newContentBytes can't be null");

        int offset = md.getTokenOffset(index);
        //int len = md.getTokenLength(index);
        int type = md.getTokenType(index);
        /*int len = (type == VTDNav.TOKEN_STARTING_TAG
                || type == VTDNav.TOKEN_ATTR_NAME || type == VTDNav.TOKEN_ATTR_NS) ? md
                .getTokenLength(index) & 0xffff
                : md.getTokenLength(index);*/
        
        // one insert
        byte[] bo = Transcoder.transcode(newContentBytes,contentOffset,
                contentLen, src_encoding, encoding);
        
        switch (type) {
        case VTDNav.TOKEN_CDATA_VAL:
            if (encoding < VTDNav.FORMAT_UTF_16BE)
                insertBytesAt(offset - 9, bo);
            else
                insertBytesAt((offset - 9) << 1, bo);
            break;
        case VTDNav.TOKEN_COMMENT:
            if (encoding < VTDNav.FORMAT_UTF_16BE)
                insertBytesAt(offset - 4, bo);
            else
                insertBytesAt((offset - 4) << 1, bo);
            break;

        default:
            if (encoding < VTDNav.FORMAT_UTF_16BE)
                insertBytesAt(offset, bo);
            else
                insertBytesAt(offset << 1, bo);
        }
        // one delete
        removeToken(index);        	
    }
    
    /**
     * Update token with the transcoded representation of 
     * a segment of byte array contained in vn (in terms of offset and length)
     * @param index
     * @param vn
     * @param contentOffset
     * @param contentLen
     * @throws ModifyException
     * @throws UnsupportedEncodingException
     * @throws TranscodeException
     *
     */
    
    public void updateToken(int index, VTDNav vn, 
            int contentOffset, int contentLen) 
	throws ModifyException,UnsupportedEncodingException, TranscodeException{
        updateToken(index, vn.XMLDoc.getBytes(), contentOffset, contentLen, vn.encoding);
    }
    
    
    
    
    /**
    * Update the token with the given string value,
    * notice that string will be converted into byte array
    * according to the encoding of the master document
    * @param index
    * @param newContent
    * @throws ModifyException
    * @throws UnsupportedEncodingException
    *
    */
    
    public void updateToken(int index, String newContent) 
    	throws ModifyException,UnsupportedEncodingException{
        if (newContent==null)
            throw new IllegalArgumentException
            ("String newContent can't be null");
        int offset = md.getTokenOffset(index);
        //int len = md.getTokenLength(index);
        int type = md.getTokenType(index);
        /*int len =
			(type == VTDNav.TOKEN_STARTING_TAG
				|| type == VTDNav.TOKEN_ATTR_NAME
				|| type == VTDNav.TOKEN_ATTR_NS)
				? md.getTokenLength(index) & 0xffff
				: md.getTokenLength(index);*/
        // one insert
        switch(type){
        	case VTDNav.TOKEN_CDATA_VAL:
        	    if (encoding < VTDNav.FORMAT_UTF_16BE)
        	        insertBytesAt(offset-9,newContent.getBytes(charSet));
        	    else 
        	        insertBytesAt((offset-9)<<1,newContent.getBytes(charSet));
        		break;
        	case VTDNav.TOKEN_COMMENT:
           	    if (encoding < VTDNav.FORMAT_UTF_16BE)
        	        insertBytesAt(offset-4,newContent.getBytes(charSet));
        	    else 
        	        insertBytesAt((offset-4)<<1,newContent.getBytes(charSet));
        		break;
        	    
        	default: 
        	    if (encoding < VTDNav.FORMAT_UTF_16BE)
        	        insertBytesAt(offset,newContent.getBytes(charSet));
        	    else 
        	        insertBytesAt(offset<<1,newContent.getBytes(charSet));
        }
        // one delete
        removeToken(index);        	
    }
    
    
  
    
    /**
     * 
     * 
     *
     */
    protected void sort(){
        if (flb.size>0)
            quickSort(0,flb.size-1);        
    }
    
    /**
     * 
     * This function will do the range checking and make
     * sure there is no overlaping or invalid deletion 
     * There can be only one deletion at one offset value
     * Delete can't overlap with, nor contains, another delete
     *
     */
    protected void check()  throws ModifyException{
        int os1, os2, temp;
        int size = flb.size;
        
        for (int i=0;i<size;i++){
            os1 = flb.lower32At(i);
            os2 = flb.lower32At(i)+ (flb.upper32At(i)& 0x1fffffff)-1;
            if (i+1<size){
                temp = flb.lower32At(i+1);
                if (temp!= os1 && temp<=os2)
                    throw new ModifyException
                    ("Invalid insertion/deletion condition detected between offset "
                            +os1 + " and offset "+os2);
            }
        }
    }
    /**
     * Compute the size of the updated XML document without composing it
     * @return updated document size
     *
     */
    public int getUpdatedDocumentSize() throws TranscodeException{
        int size = flb.size;
        int docSize = md.getXML().getBytes().length;
        long l;
        for (int i=0;i<size;i++){
            l= flb.longAt(i);
            if ((l & (~0x1fffffffffffffffL)) == MASK_DELETE) {
                docSize -= (int) ((l & (0x1fffffffffffffffL))>> 32);
            } else if ((l & (~0x1fffffffffffffffL)) == MASK_INSERT_BYTE){
                docSize += ((byte[])fob.objectAt(i)).length;
            } else if ((l & (~0x1fffffffffffffffL)) == MASK_INSERT_SEGMENT_BYTE){ 
                // MASK_INSERT_SEGMENT_BYTE
                docSize += ((ByteSegment)fob.objectAt(i)).len;
            } else{
                docSize += ((ElementFragmentNs)fob.objectAt(i)).getSize(md.encoding);
            }
        }
        return docSize;
    }
    /**
     * This method will first call getCurrentIndex() to get the cursor index value
     * then insert the byte array b after the element
     * @param b
     * @throws ModifyException
     * @throws NavException
     *
     */
    public void insertAfterElement(byte[] b)
		throws ModifyException,NavException{
        int startTagIndex =md.getCurrentIndex();
        int type = md.getTokenType(startTagIndex);
        if (type!=VTDNav.TOKEN_STARTING_TAG)
            throw new ModifyException("Token type is not a starting tag");
        long l = md.getElementFragment();
        int offset = (int)l;
        int len = (int)(l>>32);
        insertBytesAt(offset+len,b);
    }
    
    /**
     * This method will insert byte array b after the head of cursor element, 
     * @param b
     * @return
     * @throws ModifyException
     * @throws NavException
     *
     */
     
    public void insertAfterHead(byte[] b)
        throws ModifyException,NavException{
        int i = md.getOffsetAfterHead();
        if (i==-1)
            throw new ModifyException("Insertion failed");
        insertBytesAt(i,b);
    }
    
    /**
     * This method will insert the transcoded representation of 
     * byte array b after the head of cursor element, 
     * @param src_encoding
     * @param b
     * @return
     * @throws ModifyException
     * @throws NavException
     * @throws TranscodeException
     *
     */
    public void insertAfterHead(int src_encoding, byte[] b)
    throws ModifyException, NavException,TranscodeException {
        if(src_encoding == encoding){
            insertAfterHead(b);
        }else{
            int i = md.getOffsetAfterHead();
            if (i==-1)
                throw new ModifyException("Insertion failed");
            byte[] bo = Transcoder.transcode(b, 0, b.length, src_encoding, encoding);
            insertBytesAt(i,bo);            
        }
    }
    /**
     * This method will insert the transcoded representation of 
     * a segment of the byte array b after the head of cursor element, 
     * @param src_encoding
     * @param b
     * @param offset
     * @param length
     * @return
     * @throws ModifyException
     * @throws NavException
     * @throws TranscodeException
     *
     */
    public void insertAfterHead(int src_encoding, byte[] b, int offset, int length)
    throws ModifyException, NavException,TranscodeException {
        if(src_encoding == encoding){
            insertAfterHead(b,offset,length);
        }else{
            int i = md.getOffsetAfterHead();
            if (i==-1)
                throw new ModifyException("Insertion failed");
            byte[] bo = Transcoder.transcode(b, offset, length, src_encoding, encoding);
            insertBytesAt(i,bo,offset, length);            
        }
    }
    /**
     * This method will insert the transcoded representation of 
     * a segment of the byte array b  after the head of cursor element, 
     * @param src_encoding
     * @param b
     * @param l
     * @return
     * @throws ModifyException
     * @throws NavException
     * @throws TranscodeException
     *
     */
    public void insertAfterHead(int src_encoding, byte[] b, long l) 
    throws ModifyException, NavException,TranscodeException {
        if(src_encoding == encoding){
            insertAfterHead(b,l);
        }else{
            int i = md.getOffsetAfterHead();
            if (i==-1)
                throw new ModifyException("Insertion failed");
            byte[] bo = Transcoder.transcode(b, (int)l, (int)l>>32, src_encoding, encoding);
            insertBytesAt(i,bo,l);            
        }
    }
    
    
    /**
     * This method will insert s' byte array representation 
     * of the string after the head of cursor element, 
     * @param s
     * @return
     * @throws ModifyException
     * @throws NavException
     * @throws TranscodeException
     *
     */
    public void insertAfterHead(String s)
    throws ModifyException, UnsupportedEncodingException, NavException{
        int i = md.getOffsetAfterHead();
        if (i==-1)
            throw new ModifyException("Insertion failed");
        insertBytesAt(i,s.getBytes(charSet));
    }
    
   
    /**
     * This method will insert a segment of the byte array b (contained in vn, and 
     * transcode into a byte array) after the head of cursor element, 
     * @param vn
     * @param contentOffset
     * @param contentLen
     * @throws ModifyException
     * @throws NavException
     * @throws TranscodeException
     *
     */
    public void insertAfterHead(VTDNav vn, int contentOffset, int contentLen)
    throws ModifyException, NavException,TranscodeException {
        insertAfterHead(vn.encoding,vn.XMLDoc.getBytes(),contentOffset, contentLen);       
    }
    
   /**
    * This method will insert a segment of the byte array b after the head of cursor element, 
    * @param b
    * @param offset
    * @param len
    * @throws ModifyException
    * @throws NavException
    *
    */
    public void insertAfterHead(byte[] b, int offset, int len)
    throws ModifyException,NavException{
        int i = md.getOffsetAfterHead();
        if (i==-1)
            throw new ModifyException("Insertion failed");
        insertBytesAt(i,b,offset, len);
    }
    
   /**
    * This method will insert a segment of the byte array b after the head of cursor element
    * @param b
    * @param l
    * @throws ModifyException
    * @throws NavException
    *
    */
    public void insertAfterHead(byte[] b, long l)
    throws ModifyException,NavException{
        int i = md.getOffsetAfterHead();
        if (i==-1)
            throw new ModifyException("Insertion failed");
        insertBytesAt(i,b,l);
    }
    
    /**
     * This method will insert an ElementFragmentNs instance 
     * after the head of cursor element, 
     * @param efn
     * @throws ModifyException
     * @throws NavException
     *
     */
    public void insertAfterHead(ElementFragmentNs ef) 
    throws ModifyException, NavException{
        int i = md.getOffsetAfterHead();
        if (i==-1)
            throw new ModifyException("Insertion failed");
        insertElementFragmentNsAt(i, ef);
    }
    

    
    /**
     * Insert a namespace compensated element after cursor element
     * @param ef (an ElementFragmentNs object)
     * @throws ModifyException
     * @throws NavException
     *
     */
    public void insertAfterElement(ElementFragmentNs ef)
            throws ModifyException, NavException {
        int startTagIndex = md.getCurrentIndex();
        int type = md.getTokenType(startTagIndex);
        if (type != VTDNav.TOKEN_STARTING_TAG)
            throw new ModifyException("Token type is not a starting tag");
        long l = md.getElementFragment();
        int offset = (int) l;
        int len = (int) (l >> 32);
        insertElementFragmentNsAt(offset + len, ef);
    }
    
 
    
    /**
     * This method will first call getCurrentIndex() to get the cursor index value
     * then insert a segment of the byte array b after the element
     * @param b
     * @param contentOffset
     * @param contentLen
     * @throws ModifyException
     * @throws NavException
     *
     */
    public void insertAfterElement(byte[] b, int contentOffset, int contentLen)
            throws ModifyException, NavException {
        
        int startTagIndex = md.getCurrentIndex();
        int type = md.getTokenType(startTagIndex);
        
        if (type != VTDNav.TOKEN_STARTING_TAG)
            throw new ModifyException("Token type is not a starting tag");
        long l = md.getElementFragment();
        int offset = (int) l;
        int len = (int) (l >> 32);
        insertBytesAt(offset + len, b, contentOffset, contentLen);
    }
    
    
 
    
    /**
     * This method will first call getCurrentIndex() to get the cursor index value
     * then insert the transcoded array of bytes of a segment of the byte array b after the element
     * @param b
     * @param contentOffset
     * @param contentLen
     * @throws ModifyException
     * @throws NavException
     * @throws TranscodeException
     *
     */
    public void insertAfterElement(int src_encoding, byte[] b, int contentOffset, int contentLen)
            throws ModifyException, NavException,TranscodeException {
        if (src_encoding == encoding) {
            insertAfterElement(b,contentOffset,contentLen);
        } else {
            int startTagIndex = md.getCurrentIndex();
            int type = md.getTokenType(startTagIndex);
            if (type != VTDNav.TOKEN_STARTING_TAG)
                throw new ModifyException("Token type is not a starting tag");
            long l = md.getElementFragment();
            int offset = (int) l;
            int len = (int) (l >> 32);
            // transcode in here
            byte[] bo = Transcoder.transcode(b, contentOffset, contentLen, src_encoding, encoding);
            insertBytesAt(offset + len, bo);
        }
    }
    
    /**
     * This method will first call getCurrentIndex() to get the cursor index value
     * then insert the transcoded array of bytes of a segment of the byte array b after the element
     * the VTDNav object is the container of the XML document in byte array
     * 
     * @param vn
     * @param contentOffset
     * @param contentLen
     * @throws ModifyException
     * @throws UnsupportedEncodingException
     * @throws NavException
     * @throws TranscodeException
     *
     */
    public void insertAfterElement(VTDNav vn, int contentOffset, int contentLen)
    throws ModifyException, UnsupportedEncodingException, NavException,TranscodeException {
        insertAfterElement(vn.encoding,vn.XMLDoc.getBytes(),contentOffset, contentLen);       
    }
    /**
     * This method will first call getCurrentIndex() to get the cursor index value
     * then insert a segment of the byte array b after the element,
     * l1 (a long)'s upper 32 bit is length, lower 32 bit is offset
     * @param b
     * @param contentOffset
     * @param contentLen
     * @throws ModifyException
     * @throws NavException
     *
     */
    public void insertAfterElement(byte[] b, long l1)
    throws ModifyException,NavException {
        int startTagIndex = md.getCurrentIndex();
        int type = md.getTokenType(startTagIndex);
        if (type != VTDNav.TOKEN_STARTING_TAG)
            throw new ModifyException("Token type is not a starting tag");
        long l = md.getElementFragment();
        int offset = (int) l;
        int len = (int) (l >> 32);
        insertBytesAt(offset + len, b, l1);
    }
    
    /**
     * This method will first call getCurrentIndex() to get the cursor index value
     * then insert a segment of the byte array b (transcode into a byte array) after the element,
     * l1 (a long)'s upper 32 bit is length, lower 32 bit is offset
     * @param b
     * @param l1
     * @throws ModifyException
     * @throws UnsupportedEncodingException
     * @throws NavException
     * @throws TranscodeException
     *
     */
    public void insertAfterElement(int src_encoding, byte[] b, long l1) throws ModifyException,
            UnsupportedEncodingException, NavException, TranscodeException {
        if (src_encoding == encoding){
            insertAfterElement(b,l1);
        } else {
            int startTagIndex = md.getCurrentIndex();
            int type = md.getTokenType(startTagIndex);
            if (type != VTDNav.TOKEN_STARTING_TAG)
                throw new ModifyException("Token type is not a starting tag");
            long l = md.getElementFragment();
            int offset = (int) l;
            int len = (int) (l >> 32);
            byte[] bo = Transcoder.transcode(b, (int)l, (int)l>>32, src_encoding, encoding);
            insertBytesAt(offset + len, bo, l1);
        }
    }
    
    /**
     * This method will first call getCurrentIndex() to get the cursor index value
     * then insert a segment of the byte array b (contained in vn, and 
     * transcode into a byte array) after the element, 
     * l1 (a long)'s upper 32 bit is length, lower 32 bit is offset
     * @param vn
     * @param l1
     * @throws ModifyException
     * @throws UnsupportedEncodingException
     * @throws NavException
     * @throws TranscodeException
     *
     */
    public void insertAfterElement(VTDNav vn, long l1) throws ModifyException,
    UnsupportedEncodingException, NavException, TranscodeException {
        insertAfterElement(vn.encoding, vn.XMLDoc.getBytes(), l1);
    }
    
    /**
     * This method will insert a segment of the byte array b (contained in vn, and 
     * transcode into a byte array) after the head of cursor element, 
     * @param vn
     * @param l1
     * @return
     * @throws ModifyException
     * @throws NavException
     * @throws TranscodeException
     *
     */
    public void insertAfterHead(VTDNav vn, long l1) throws ModifyException,
    NavException, TranscodeException {
        insertAfterHead(vn.encoding, vn.XMLDoc.getBytes(), l1);
    }
    
    /**
     * Insert a byte array of given encoding into the master document
     * transcoding is done underneath to ensure the correctness of output
     * @param encoding The encoding format of the byte array 
     * @param b
     * @throws ModifyException
     * @throws NavException
     * @throws TranscodeException
     *
     */
    public void insertAfterElement(int src_encoding, byte[] b)
            throws ModifyException, NavException,TranscodeException {
        if(src_encoding == encoding){
            insertAfterElement(b);
        }
        else {    
            int startTagIndex =md.getCurrentIndex();
            int type = md.getTokenType(startTagIndex);
            if (type!=VTDNav.TOKEN_STARTING_TAG)
                throw new ModifyException("Token type is not a starting tag");
            long l = md.getElementFragment();
            int offset = (int)l;
            int len = (int)(l>>32);
            // transcoding logic
            byte[] bo = Transcoder.transcode(b, 0, b.length, src_encoding, encoding);
            insertBytesAt(offset+len,bo);
        }
    }
    /**
     * This method will first call getCurrentIndex() to get the cursor index value
     * then insert the byte value of s after the element
     * @param s  the string whose byte content will be inserted into the master document
     *
     */
    public void insertAfterElement(String s)
    	throws ModifyException,UnsupportedEncodingException,NavException{
        int startTagIndex =md.getCurrentIndex();
        int type = md.getTokenType(startTagIndex);
        if (type!=VTDNav.TOKEN_STARTING_TAG)
            throw new ModifyException("Token type is not a starting tag");
        long l = md.getElementFragment();
        int offset = (int)l;
        int len = (int)(l>>32);
        insertBytesAt(offset+len,s.getBytes(charSet));       
    }
    
    /**
     * This method will first call getCurrentIndex() to get the cursor index value
     * then insert the byte array b before the element
     * @param b the byte array to be inserted into the master document
     * @throws ModifyException
     *
     */
    public void insertBeforeElement(byte[] b)
    	throws ModifyException{
        int startTagIndex =md.getCurrentIndex();
        int type = md.getTokenType(startTagIndex);
        if (type!=VTDNav.TOKEN_STARTING_TAG)
            throw new ModifyException("Token type is not a starting tag");
        
        int offset = md.getTokenOffset(startTagIndex)-1;
        
        if (encoding < VTDNav.FORMAT_UTF_16BE)
            insertBytesAt(offset,b);
        else
            insertBytesAt((offset)<<1,b);        
    }
    
    /**
     * This method will first call getCurrentIndex() to get the cursor index value
     * then insert the transcoded representatin of the byte array b  before the element
     * @param b the byte array to be inserted into the master document
     * @throws ModifyException
     * @throws TranscodeException
     *
     */
    public void insertBeforeElement(int src_encoding, byte[] b)
    	throws ModifyException,TranscodeException{
        if (src_encoding == md.encoding) {
            insertBeforeElement(b);
        } else {
            int startTagIndex = md.getCurrentIndex();
            int type = md.getTokenType(startTagIndex);
            if (type != VTDNav.TOKEN_STARTING_TAG)
                throw new ModifyException("Token type is not a starting tag");

            int offset = md.getTokenOffset(startTagIndex) - 1;
            byte[] bo = Transcoder.transcode(b,0,b.length,src_encoding, encoding); 
            if (encoding < VTDNav.FORMAT_UTF_16BE)
                insertBytesAt(offset, bo);
            else
                insertBytesAt((offset) << 1, bo);
        }
    }
    
   /**
    * Insert a namespace compensated fragment before the cursor element
    * @param ef
    * @throws ModifyException
    * 
    *
    */
    public void insertBeforeElement(ElementFragmentNs ef)
    	throws ModifyException{
        int startTagIndex =md.getCurrentIndex();
        int type = md.getTokenType(startTagIndex);
        if (type!=VTDNav.TOKEN_STARTING_TAG)
            throw new ModifyException("Token type is not a starting tag");
        
        int offset = md.getTokenOffset(startTagIndex)-1;
        
        if (encoding < VTDNav.FORMAT_UTF_16BE)
            insertElementFragmentNsAt(offset,ef);
        else
            insertElementFragmentNsAt((offset)<<1,ef);        
    }
    
    
    /**
     * This method will first call getCurrentIndex() to get the cursor index value
     * then insert a segment of the byte array b before the element
     * @param b
     * @param contentOffset
     * @param contentLen
     * @throws ModifyException
     * @throws UnsupportedEncodingException
     *
     */
    public void insertBeforeElement(byte[] b,int contentOffset, int contentLen) throws ModifyException,
            UnsupportedEncodingException {
        int startTagIndex = md.getCurrentIndex();
        int type = md.getTokenType(startTagIndex);
        if (type != VTDNav.TOKEN_STARTING_TAG)
            throw new ModifyException("Token type is not a starting tag");

        int offset = md.getTokenOffset(startTagIndex) - 1;

        if (encoding < VTDNav.FORMAT_UTF_16BE)
            insertBytesAt(offset, b, contentOffset, contentLen);
        else
            insertBytesAt((offset) << 1, b, contentOffset, contentLen);
    }
    
    
    /**
     * This method will first call getCurrentIndex() to get the cursor index value
     * then insert the transcoded representation of a segment of the byte array b 
     * before the element
     * @param b
     * @param contentOffset
     * @param contentLen
     * @throws ModifyException
     * @throws UnsupportedEncodingException
     * @throws TranscodeException
     *
     */
    public void insertBeforeElement(int src_encoding, byte[] b,int contentOffset, int contentLen) throws ModifyException,
            UnsupportedEncodingException, TranscodeException {
        if (src_encoding == encoding) {
            insertBeforeElement(b,contentOffset, contentLen);
        } else {
            int startTagIndex = md.getCurrentIndex();
            int type = md.getTokenType(startTagIndex);
            if (type != VTDNav.TOKEN_STARTING_TAG)
                throw new ModifyException("Token type is not a starting tag");

            int offset = md.getTokenOffset(startTagIndex) - 1;
            // do transcoding here
            byte[] bo = Transcoder.transcode(b,contentOffset,contentLen,src_encoding, encoding);
            if (encoding < VTDNav.FORMAT_UTF_16BE)
                insertBytesAt(offset, bo);
            else
                insertBytesAt((offset) << 1, bo);
        }
    }
    

    
    /**
     * This method will first call getCurrentIndex() to get the cursor index value
     * then insert the transcoded representation of a segment of the byte array contained
     * in vn before the element
     * @param vn
     * @param contentOffset
     * @param contentLen
     * @throws ModifyException
     * @throws UnsupportedEncodingException
     * @throws TranscodeException
     *
     */
    public void insertBeforeElement(VTDNav vn,int contentOffset, int contentLen) throws ModifyException,
    UnsupportedEncodingException, TranscodeException {
        insertBeforeElement(vn.encoding, vn.XMLDoc.getBytes(),contentOffset, contentLen);
    }
    /**
     * This method will first call getCurrentIndex() to get the cursor index value
     * then insert a segment of the byte array b before the element
     * l1 (a long)'s upper 32 bit is length, lower 32 bit is offset
     * @param b
     * @param l1
     * @throws ModifyException
     * @throws UnsupportedEncodingException
     *
     */
    public void insertBeforeElement(byte[] b, long l1) throws ModifyException,
            UnsupportedEncodingException {
        int startTagIndex = md.getCurrentIndex();
        int type = md.getTokenType(startTagIndex);
        if (type != VTDNav.TOKEN_STARTING_TAG)
            throw new ModifyException("Token type is not a starting tag");

        int offset = md.getTokenOffset(startTagIndex) - 1;

        if (encoding < VTDNav.FORMAT_UTF_16BE)
            insertBytesAt(offset, b, l1);
        else
            insertBytesAt((offset) << 1, b, l1);
    }
    
    /**
     * This method will first call getCurrentIndex() to get the cursor index value
     * then insert the transcoded representation of a segment of the byte array b before the element
     * l1 (a long)'s upper 32 bit is length, lower 32 bit is offset
     * @param b
     * @param l1
     * @throws ModifyException
     * @throws UnsupportedEncodingException
     * @throws TranscodeException
     *
     */
    public void insertBeforeElement(int src_encoding, byte[] b, long l1) throws ModifyException,
            UnsupportedEncodingException, TranscodeException {
        if (src_encoding == md.encoding) {
            insertBeforeElement(b, l1);
        } else {
            int startTagIndex = md.getCurrentIndex();
            int type = md.getTokenType(startTagIndex);
            if (type != VTDNav.TOKEN_STARTING_TAG)
                throw new ModifyException("Token type is not a starting tag");

            int offset = md.getTokenOffset(startTagIndex) - 1;
            byte[] bo = Transcoder.transcode(b, (int)l1, (int)(l1>>32),src_encoding, encoding);
            if (encoding < VTDNav.FORMAT_UTF_16BE)
                insertBytesAt(offset, bo );
            else
                insertBytesAt((offset) << 1, bo);
        }
    }
    
    /**
     * This method will first call getCurrentIndex() to get the cursor index value
     * then insert the transcoded representation of a segment of the byte array contained in
     * vn before the element
     * l1 (a long)'s upper 32 bit is length, lower 32 bit is offset
     * @param vn
     * @param l
     * @throws ModifyException
     * @throws UnsupportedEncodingException
     * @throws TranscodeException
     *
     */
    
    public void insertBeforeElement(VTDNav vn, long l) throws ModifyException,
    UnsupportedEncodingException, TranscodeException {
        insertBeforeElement(vn.encoding, vn.XMLDoc.getBytes(),l);
    }
    /**
     * This method will first call getCurrentIndex() to get the cursor index value
     * then insert the byte value of s before the element
     * @param s
     *
     */
    public void insertBeforeElement(String s)
    	throws ModifyException,UnsupportedEncodingException{
        int startTagIndex =md.getCurrentIndex();
        int type = md.getTokenType(startTagIndex);
        if (type!=VTDNav.TOKEN_STARTING_TAG)
            throw new ModifyException("Token type is not a starting tag");
        
        int offset = md.getTokenOffset(startTagIndex)-1;
        
        if (encoding < VTDNav.FORMAT_UTF_16BE)
            insertBytesAt(offset,s.getBytes(charSet));
        else
            insertBytesAt((offset)<<1,s.getBytes(charSet));        
    }
    
    /**
     * Insert an attribute after the starting tag
     * This method will first call getCurrentIndex() to get the cursor index value
     * if the index is of type "starting tag", then the attribute is inserted
     * after the starting tag
     * @param attr e.g. " attrName='attrVal' ",notice the starting and ending 
     * white space
     *
     */
    public void insertAttribute(String attr) 
    	throws ModifyException,UnsupportedEncodingException{
        int startTagIndex =md.getCurrentIndex();
        int type = md.getTokenType(startTagIndex);
        if (type!=VTDNav.TOKEN_STARTING_TAG)
            throw new ModifyException("Token type is not a starting tag");
        int offset = md.getTokenOffset(startTagIndex);
        int len = md.getTokenLength(startTagIndex);
        
        if (encoding < VTDNav.FORMAT_UTF_16BE)
            insertBytesAt(offset+len,attr.getBytes(charSet));
        else
            insertBytesAt((offset+len)<<1,attr.getBytes(charSet));
        //insertBytesAt()
    }
    
    /**
     * Insert a byte arry of an attribute after the starting tag
     * This method will first call getCurrentIndex() to get the cursor index value
     * if the index is of type "starting tag", then teh attribute is inserted
     * after the starting tag
     * @param b the byte content of e.g. " attrName='attrVal' ",notice the starting and ending 
     * white space
     *
     */
    public void insertAttribute(byte[] b) 
    	throws ModifyException,UnsupportedEncodingException{
        int startTagIndex =md.getCurrentIndex();
        int type = md.getTokenType(startTagIndex);
        if (type!=VTDNav.TOKEN_STARTING_TAG)
            throw new ModifyException("Token type is not a starting tag");
        int offset = md.getTokenOffset(startTagIndex);
        int len = md.getTokenLength(startTagIndex);
        
        if (encoding < VTDNav.FORMAT_UTF_16BE)
            insertBytesAt(offset+len,b);
        else
            insertBytesAt((offset+len)<<1,b);
        //insertBytesAt()
    }
    
    /**
     * Insert the transcoded representation of a byte arry of an attribute 
     * after the starting tag This method will first call getCurrentIndex() 
     * to get the cursor index value if the index is of type "starting tag", 
     * then teh attribute is inserted after the starting tag
     * @param b the byte content of e.g. " attrName='attrVal' ",notice the 
     * starting and ending white space
     *
     */
    public void insertAttribute(int src_encoding, byte[] b) 
    	throws ModifyException,UnsupportedEncodingException,TranscodeException{
        int startTagIndex =md.getCurrentIndex();
        int type = md.getTokenType(startTagIndex);
        if (type!=VTDNav.TOKEN_STARTING_TAG)
            throw new ModifyException("Token type is not a starting tag");
        int offset = md.getTokenOffset(startTagIndex);
        int len = md.getTokenLength(startTagIndex);
        
        if (encoding < VTDNav.FORMAT_UTF_16BE)
            insertBytesAt(offset+len,b);
        else
            insertBytesAt((offset+len)<<1,b);
        //insertBytesAt()
    }
    /**
     * This method applies the modification to the XML document
     * and writes the output byte content accordingly to an outputStream
     * Notice that output is not guaranteed to be well-formed 
     * @param os
     *
     */
    public void output(OutputStream os) throws IOException, 
    ModifyException,TranscodeException{
        if (os == null)
            throw new IllegalArgumentException("OutputStream can't be null");
        sort();
        check();
        long l;
        byte[] ba = md.getXML().getBytes();
        //for(int i=0;i<flb.size();i++){
        //    System.out.println(" offset value is ==>"+flb.lower32At(i));
        //}
        int t = md.vtdBuffer.lower32At(0);
        int start = (t==0)?
                md.docOffset:32;
        int len = (t==0)?
                md.docLen:(md.docLen-32);
                    
        if (flb.size==0){
            os.write(ba,start,len);
        }else{
            int offset = start;
            int inc=1;
            for(int i=0;i<flb.size;i=i+inc){
                if (flb.lower32At(i)==flb.lower32At(i+1)){
                    inc  = 2;
                } else 
                    inc = 1;
                l = flb.longAt(i);
                if (inc == 1){                    
                    if ((l & (~0x1fffffffffffffffL)) == MASK_DELETE){
                        os.write(ba,offset, flb.lower32At(i)-offset);
                        offset = flb.lower32At(i) + (flb.upper32At(i) & 0x1fffffff);
                    }else if ((l & (~0x1fffffffffffffffL)) == MASK_INSERT_BYTE ) { // insert
                        os.write(ba,offset, flb.lower32At(i)-offset);
                        os.write((byte[])fob.objectAt(i));                       
                        offset=flb.lower32At(i);
                    } else if ((l & (~0x1fffffffffffffffL)) == MASK_INSERT_SEGMENT_BYTE) { 
                        // XML_INSERT_SEGMENT_BYTE
                        os.write(ba,offset, flb.lower32At(i)-offset);
                        ByteSegment bs = (ByteSegment) fob.objectAt(i);
                        os.write(bs.ba,bs.offset,bs.len);
                        offset=flb.lower32At(i);
                    } else {
                        //ElementFragmentNs
                        os.write(ba,offset, flb.lower32At(i)-offset);
                        ElementFragmentNs ef = (ElementFragmentNs)fob.objectAt(i);
                        ef.writeToOutputStream(os,md.encoding);
                        offset=flb.lower32At(i);
                    }
                } else { // share the same offset value one insert, one delete
                    // to make sure that l's offset val is >= k's
                    // also to make sure that the first token is a delete
                    long k = flb.longAt(i+1),temp;
                    int i1 = i,temp2;
                    int i2 = i+1;
                    if ((l & (~0x1fffffffffffffffL)) != MASK_DELETE){
                        temp = l;
                        l= k;
                        k = temp;
                        temp2 = i1;
                        i1 = i2;
                        i2 = temp2;
                    }
                    // first is definitely
                    os.write(ba,offset, flb.lower32At(i1)-offset);
                    //os.write((byte[])fob.objectAt(i2));
                    //offset = flb.lower32At(i1) + (flb.upper32At(i1) & 0x1fffffff);
                    
                    
                    if ((k & (~0x1fffffffffffffffL)) == MASK_INSERT_BYTE ){ // insert
                        //os.write(ba,offset, flb.lower32At(i2)-offset);
                        os.write((byte[])fob.objectAt(i2));
                        offset = flb.lower32At(i1) + (flb.upper32At(i1) & 0x1fffffff);
                    } else if ((k & (~0x1fffffffffffffffL)) == MASK_INSERT_SEGMENT_BYTE){
                        // XML_INSERT_SEGMENT_BYTE
                        //os.write(ba,offset, flb.lower32At(i2)-offset);
                        ByteSegment bs = (ByteSegment) fob.objectAt(i2);
                        os.write(bs.ba,bs.offset,bs.len);
                        offset = flb.lower32At(i1) + (flb.upper32At(i1) & 0x1fffffff);
                    } else {
                        //ElementFragmentNs
                        //os.write(ba,offset, flb.lower32At(i2)-offset);
                        ElementFragmentNs ef = (ElementFragmentNs)fob.objectAt(i2);
                        ef.writeToOutputStream(os,md.encoding);
                        offset = flb.lower32At(i1) + (flb.upper32At(i1) & 0x1fffffff);
                    }
                }
            }  
            os.write(ba,offset,start+len-offset);
        } 
    }
    
    /**
     * Generate the updated output XML document and write it into 
     * a file of given name
     * @param fileName
     * @throws IOException
     * @throws ModifyException
     *
     */
    public void output(String fileName) throws IOException, ModifyException,TranscodeException{
        FileOutputStream fos = new FileOutputStream(fileName);
        output(fos);
        fos.close();
    }
    
    void quickSort (int lo, int hi)
    {
//      lo is the lower index, hi is the upper index
//      of the region of array a that is to be sorted
        //System.out.println("lo ==>"+lo);
        //System.out.println("hi ==>"+hi);
        int i=lo, j=hi; 
        long h;
        Object o;
        int x=flb.lower32At((lo+hi)/2);

        //  partition
        do
        {    
            while (flb.lower32At(i)<x) i++; 
            while (flb.lower32At(j)>x) j--;
            if (i<=j)
            {
                h=flb.longAt(i); 
                o = fob.objectAt(i);
                flb.modifyEntry(i,flb.longAt(j)); 
                fob.modifyEntry(i,fob.objectAt(j));
                flb.modifyEntry(j,h);
                fob.modifyEntry(j,o);
                i++; 
                j--;
            }
        } while (i<=j);

        //  recursion
        if (lo<j) quickSort(lo, j);
        if (i<hi) quickSort(i, hi);
    }

    /**
     * This method resets the internal state of XMLModify instance so 
     * it can be reused
     * 
     *
     */
    public void reset(){
        if (flb!=null)
            flb.size=0;
        if (fob!=null)
            fob.size=0;
        if (insertHash!=null)
            insertHash.reset();
        if (deleteHash!=null)
            deleteHash.reset();
    }
    
     /**
      * Replace the cursor element's name with a new name
      * @param newElementName
      * @throws ModifyException
      * @throws NavException
      *
      */
    public void updateElementName(String newElementName) throws ModifyException,
    NavException,UnsupportedEncodingException{
        int i = md.getCurrentIndex();
        int type = md.getTokenType(i);
        if (type!=VTDNav.TOKEN_STARTING_TAG){
            throw new ModifyException("You can only update an element name");
        }
        //int offset = md.getTokenOffset(i);
        int len = md.getTokenLength(i)& 0xffff;
        updateToken(i,newElementName);
        long l = md.getElementFragment();
        int encoding = md.getEncoding();
        byte[] xml = md.getXML().getBytes();
        int temp = (int)l+(int)(l>>32);
        if (encoding < VTDNav.FORMAT_UTF_16BE) {
            //scan backwards for />
            //int temp = (int)l+(int)(l>>32);
            if (xml[temp - 2] == (byte) '/')
                return;
            //look for </
            temp--;
            while (xml[temp] != (byte) '/') {
                temp--;
            }
            insertBytesAt(temp + 1, newElementName.getBytes(charSet));
            removeContent(temp + 1, len);
            return;
            //
        } else if (encoding == VTDNav.FORMAT_UTF_16BE) {
            
            //scan backwards for />
            if (xml[temp - 3] == (byte) '/' && xml[temp - 4] == 0)
                return;
            
            temp-=2;
            while (!(xml[temp+1] == (byte) '/' && xml[temp ] == 0)) {
                temp-=2;
            }
            insertBytesAt(temp+2, newElementName.getBytes(charSet));
            removeContent(temp+2, len<<1);            
        } else {
            //scan backwards for />
            if (xml[temp - 3] == 0 && xml[temp - 4] == '/')
                return;
            
            temp-=2;
            while (!(xml[temp] == (byte) '/' && xml[temp+1 ] == 0) ) {
                temp-=2;
            }
            insertBytesAt(temp+2 , newElementName.getBytes(charSet));
            removeContent(temp+2 , len<<1);
        }
    }

    
    /**
     * outAndReparse writes updated XML content into a new byte
     * array, then parse and return a new VTDNav object 
     * @return VTDNav encapsulating update XML documents
     * @throws ParseException
     * @throws IOException
     * @throws TranscodeException
     * @throws ModifyException
     */
    public VTDNav outputAndReparse() throws ParseException, IOException,TranscodeException,ModifyException{
    	XMLByteOutputStream xbos = new XMLByteOutputStream(getUpdatedDocumentSize());
    	output(xbos);
    	VTDGen vg = new VTDGen();
    	vg.setDoc(xbos.getXML());
    	vg.parse(this.md.ns);
    	return vg.getNav();
    }
    
    
}
