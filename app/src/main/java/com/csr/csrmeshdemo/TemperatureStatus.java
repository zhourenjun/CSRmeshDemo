package com.csr.csrmeshdemo;


/******************************************************************************
 Copyright Cambridge Silicon Radio Limited 2014 - 2015.
 ******************************************************************************/

/**
 * Class which helps to store the current temperature status of a device/group. (Storing for instance: desired temperature, current temperature,...)
 */
public class TemperatureStatus {

    private double desiredTemperature= Double.MIN_VALUE;
    private boolean desiredTemperatureConfirmed = false;
    private double currentTemperature = Double.MIN_VALUE;

    public TemperatureStatus () {}

    public TemperatureStatus (double currentTemperature, boolean desiredTemperatureConfirmed, double desiredTemperature) {
        this.currentTemperature = currentTemperature;
        this.desiredTemperatureConfirmed = desiredTemperatureConfirmed;
        this.desiredTemperature = desiredTemperature;
    }

    public double getCurrentTemperature() {
        return currentTemperature;
    }

    public void setCurrentTemperature(double currentTemperature) {
        this.currentTemperature = currentTemperature;
    }

    public boolean isDesiredTemperatureConfirmed() {
        return desiredTemperatureConfirmed;
    }

    public void setDesiredTemperatureConfirmed(boolean desiredTemperatureConfirmed) {
        this.desiredTemperatureConfirmed = desiredTemperatureConfirmed;
    }

    public double getDesiredTemperature() {
        return desiredTemperature;
    }

    public void setDesiredTemperature(double desiredTemperature) {
        this.desiredTemperature = desiredTemperature;
    }


}
