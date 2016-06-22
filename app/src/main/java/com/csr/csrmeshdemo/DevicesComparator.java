
/******************************************************************************
 Copyright Cambridge Silicon Radio Limited 2014 - 2015.
 ******************************************************************************/
 
package com.csr.csrmeshdemo;

import java.util.Comparator;

import com.csr.csrmeshdemo.entities.SingleDevice;

public class DevicesComparator implements Comparator<Object>{

	@Override
	public int compare(Object object1, Object object2) {
		
		// Strings to compare
    	String s1 = null;
    	String s2 = null;
		if (object1 instanceof CheckedListItem && object2 instanceof CheckedListItem) {
			CheckedListItem checkedListItem1 = (CheckedListItem) object1;
			CheckedListItem checkedListItem2 = (CheckedListItem) object2;
			s1 = checkedListItem1.getDevice().getName();
			s2 = checkedListItem2.getDevice().getName();
		} else if (object1 instanceof SingleDevice && object2 instanceof SingleDevice) {
			SingleDevice singleDevice1 = (SingleDevice) object1;
			SingleDevice singleDevice2 = (SingleDevice) object2;
			s1 = singleDevice1.getName();
			s2 = singleDevice2.getName();
		}
		
		// check if s1 or s2 is null  before continue.
		if (s1 == null || s2 == null) {
			return 0;
		}
		
    	
    	// split strings in parts according to the pattern.
    	String[] s1Parts = s1.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");
        String[] s2Parts = s2.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");

         int i = 0;
         while(i < s1Parts.length && i < s2Parts.length){

             //if parts are the same
             if(s1Parts[i].compareTo(s2Parts[i]) == 0){
                 ++i;
             }else{
                 try{

                     int intS1 = Integer.parseInt(s1Parts[i]);
                     int intS2 = Integer.parseInt(s2Parts[i]);

                     //if the parse works, compare the numbers.

                     int diff = intS1 - intS2; 
                     if(diff == 0){
                         ++i;
                     }else{
                         return diff;
                     }
                 }catch(Exception ex){
                     return s1.compareTo(s2);
                 }
             }
         }

         //Handle if one string is a prefix of the other.
         if(s1.length() < s2.length()){
             return -1;
         }else if(s1.length() > s2.length()){
             return 1;
         }else{
             return 0;
         }
	}

	
}
