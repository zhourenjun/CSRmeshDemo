
/******************************************************************************
 Copyright Cambridge Silicon Radio Limited 2014 - 2015.
 ******************************************************************************/

package com.csr.csrmeshdemo;

public class DuplicateDeviceIdException extends RuntimeException {           
    private static final long serialVersionUID = 1L;

    public DuplicateDeviceIdException(String message) {
        super(message);
    }    
}
