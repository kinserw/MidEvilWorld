package com.kinser.midevilworld;

import java.io.Serializable;

public enum Geology implements Serializable
{
    UNKNOWN,
    WATER,
    COAST,
    LAND,
    FOREST,
    MTN;

	/*
	 * // private Drawable myIcon; // private Drawable myScaledIcon = null; private
	 * int myScaledWidth = 0; private int myScaledHeight = 0;
	 */

    static private char[] ourChar =
            {'?', // unknown
                    '~', // water
                    '-', // coast
                    '_', // land
                    '#', // forest
                    '^' // mtn
            };
	/*
	 * static private Drawable[] ourImage = {null, // unknown null, // water null,
	 * // coast null, // land null, // forest null // mtn };
	 */
	/*
	 * static private int[] ourIconFiles = {R.drawable.water, // unknown
	 * R.drawable.water, // water R.drawable.water, // coast R.drawable.land, //
	 * land R.drawable.forest, // forest R.drawable.mountain // mtn };
	 * 
	 * static public void getIcons(Context context) {
	 * 
	 * Resources res = context.getResources(); for (int i= 0; i < ourImage.length ;
	 * i++) { ourImage[i] = ResourcesCompat.getDrawable(res, ourIconFiles[i], null);
	 * } }
	 */

	/*
	 * public Drawable getImage(int width, int height) { if (this.myScaledIcon ==
	 * null) { this.myScaledHeight = height; this.myScaledWidth = width;
	 * this.myScaledIcon = getImage();// .getScaledInstance(width, height,
	 * Image.SCALE_SMOOTH); } else if (myScaledHeight != height || myScaledWidth !=
	 * width) { this.myScaledHeight = height; this.myScaledWidth = width;
	 * this.myScaledIcon = getImage();//.getScaledInstance(width, height,
	 * Image.SCALE_SMOOTH); } return myScaledIcon; }
	 * 
	 * public Drawable getImage() { if (myIcon == null) { myIcon =
	 * ourImage[this.ordinal()]; } return myIcon; }
	 */

    public char getChar()
    {
        return ourChar[this.ordinal()];
    }

    // enum constructor - cannot be public or protected
    private Geology()
    {
    }

    public String toString()
    {
        return " " + ourChar[this.ordinal()] + " " ;
    }

} // end class Geology
