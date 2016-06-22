
/******************************************************************************
 Copyright Cambridge Silicon Radio Limited 2014 - 2015.
 ******************************************************************************/

package com.csr.csrmeshdemo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class Utils {
	
	private static final int AD_DATA_HEADER_LENGTH = 2;
	private static final int AD_TYPE_COMPLETE_LIST_OF_16_BIT_UUIDS = 0x03;
	private static final byte UUID_1 = (byte)0xF1;
    private static final byte UUID_2 = (byte)0xFE;
    
    
	/**
     * Parse a scan record and check if the advert was from a CSRmesh bridge.
     * @param scanRecord The scan data returned from the LeScanCallback.
     * @return True if the scan data contains the UUID for CSRmesh bridge.
     */
    public static boolean isBridgeAdvert(byte [] scanRecord) {
    	int i = 0;
    	while (i < scanRecord.length) {
    		byte length = scanRecord[i];
    		if (length > AD_DATA_HEADER_LENGTH &&
    		    scanRecord[i+1] == AD_TYPE_COMPLETE_LIST_OF_16_BIT_UUIDS &&
    		    scanRecord[i+2] == UUID_1 &&
    		    scanRecord[i+3] == UUID_2) {
    	        return true;
    		}
    		else if (length == 0) {
    			return false;
    		}
    		else {
    			i += scanRecord[i] + 1;
    		}
    	}
    	return false;
    }
	
	public static String hexString(byte [] value) {
        if (value == null) return "null";        
        String out = "";        
        for (byte b : value) {
            out += String.format("%02x", b);
        }
        return out;
    }
	
 /**
 * Method to write text characters to file on SD card. 
 * @param filename
 * @param text
 * @return
 */
static public File writeToSDFile(String filename, String text) {

     File root = android.os.Environment.getExternalStorageDirectory(); 

     File dir = new File (root.getAbsolutePath());
     dir.mkdirs();
     File file = new File(dir, filename);

     try {
         FileOutputStream f = new FileOutputStream(file);
         PrintWriter pw = new PrintWriter(f);
         pw.println(text);
         pw.flush();
         pw.close();
         f.close();
     }
     catch (Exception e) {
         e.printStackTrace();
         return null;
     }
     
     return file;
 }
    /**
     * Convert celsius value to kelvin.
     * @param celsius Temperature in celsius.
     * @return Temperature in kelvin.
     */
    static public double convertCelsiusToKelvin(double celsius) {
        return (273.15 + celsius);
    }

    /**
     * Convert kelvin value to celsius.
     * @param kelvin Temperature in celsius.
     * @return Temperature in celsius.
     */
    static public double convertKelvinToCelsius(double kelvin) {
        return (kelvin - 273.15);
    }


    /**
     * Get the extension of a file.
     * @param file
     * @return extension.
     */
    static public String getFileExtension(File file) {
        String name = file.getName();
        try {
            return name.substring(name.lastIndexOf("."));

        } catch (Exception e) {
            return "";
        }

    }

}
