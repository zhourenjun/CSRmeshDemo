
/******************************************************************************
 Copyright Cambridge Silicon Radio Limited 2014 - 2015.
 ******************************************************************************/

package com.csr.csrmeshdemo.listeners;

/**
 * Call backs implemented by TemperatureFragment, and called by MainActivity.
 */
public interface TemperatureListener {

    /**
     * Method used to notify to TemperatureFragment about the desired temperature request has been acknowledged.
     */
    public void confirmDesiredTemperature();

    /**
     * Method used to notify to TemperatureFragment about the current temperature (in celsius).
     * @param celsius
     */
    public void setCurrentTemperature(double celsius);


    /**
     * Method used to notify to TemperatureFragment about the current desired temperature (in celsius).
     * @param celsius
     */
    public void setDesiredTemperature(double celsius);
}
