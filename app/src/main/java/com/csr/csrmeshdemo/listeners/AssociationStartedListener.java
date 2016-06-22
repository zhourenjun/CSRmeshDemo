
/******************************************************************************
 Copyright Cambridge Silicon Radio Limited 2014 - 2015.
 ******************************************************************************/

package com.csr.csrmeshdemo.listeners;

public interface AssociationStartedListener {
    /**
     * Used to let AssociationFragment know that association has been started by the main Acitivity after a QR code has
     * been scanned.
     */
    public void associationStarted();
}