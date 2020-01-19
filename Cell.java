package com.kinser.midevilworld;

import java.io.Serializable;
import java.util.ArrayList;

public class Cell implements Serializable 
{
    /**
     *
     */
    private static final long serialVersionUID = -1597484864744817236L;
    public static final int GREEN = 255*256;
    public static final int BLUE = 255;
    private int myBG = GREEN;
    private Geology myGeology;
    private transient ArrayList<Cell> myNeighbors = new ArrayList<Cell>();
    private transient ArrayList<Cell> myFarNeighbors = new ArrayList<Cell>();
    private Occupiers myDefense;
    private int myRow;
    private int myCol;
    //private transient Region myArea = new Region();
    private boolean myAttackAbility = true;
    private transient City myCity = null; // not all cells are in a city so may be null
    //private Drawable myScaledIcon = null;
    //private int myScaledWidth = 0;
    //private int myScaledHeight = 0;

    public Cell()
    {
        myGeology = Geology.UNKNOWN;
        myDefense = Occupiers.NONE;
    }

    public Cell(int row, int col, Geology geo)
    {
        myRow = row;
        myCol = col;
        myGeology = geo;
        myDefense = Occupiers.NONE;
        computeArea();
    }

    public Cell(int row, int col, Geology geo, Occupiers defense)
    {
        myRow = row;
        myCol = col;
        myGeology = geo;
        myDefense = defense;
        computeArea();
    }

    public City getCity()
    {
        return myCity;
    }
    public void setCity(City c)
    {
        myCity = c;

    }

    public void computeArea()
    {
//        myArea = new Region(new Rect(getRow()*World.rowOffset, getCol()*World.colOffset,
//                getRow()*World.rowOffset+World.rowOffset, getCol()*World.colOffset+World.colOffset));
    }

	/*
	 * public Region getArea() { if (myArea.isEmpty()) computeArea(); return myArea;
	 * }
	 */
    public int getRow()
    {
        return myRow;
    }
    public void setRow(int row)
    {
        myRow = row;
        computeArea();
    }

    public int getCol()
    {
        return myCol;
    }
    public void setCol(int col)
    {
        myCol = col;
        computeArea();
    }


    public void setGeology(Geology geo)
    {
        myGeology = geo;
    }
    public Geology getGeology()
    {
        return myGeology;
    }

    /* @returns current defense level is returned */
    public Occupiers setOccupiers(Occupiers defense)
    {
        Occupiers oldDefense = myDefense;
        myDefense = defense;
//        myScaledIcon = null;
        return oldDefense;
    }
    public Occupiers getOccupiers()
    {
        return myDefense;
    }

    public void setFarNeighbors(ArrayList<Cell> neighbors)
    {
        myFarNeighbors = neighbors;
    }
    public ArrayList<Cell> getFarNeighbors()
    {
        return myFarNeighbors;
    }


    public void setNeighbors(ArrayList<Cell> neighbors)
    {
        myNeighbors = neighbors;
    }
    public ArrayList<Cell> getNeighbors()
    {

        return myNeighbors;
    }

    // returns all the neighbors of this cell with the same
    // color (but NOT in the same city)
    public ArrayList<Cell> getAlliedNeighbors()
    {
        ArrayList<Cell> an = new ArrayList<Cell>();
        for (Cell cell : myNeighbors)
        {
            if ((cell.getBackground() == this.getBackground())
                    && (!this.getCity().equals(cell.getCity())))
                an.add(cell);
        }
        return an;
    }
    // returns all the neighbors of this cell with the different color
    public ArrayList<Cell> getEnemyNeighbors()
    {
        ArrayList<Cell> an = new ArrayList<Cell>();
        for (Cell cell : myNeighbors)
        {
            if (cell.getBackground() != this.getBackground())
                an.add(cell);
        }
        return an;
    }
    // returns all the neighbors of this cell that are part of the SAME city
    public ArrayList<Cell> getCityNeighbors()
    {
        ArrayList<Cell> an = new ArrayList<Cell>();
        if (this.getCity() != null)
        {
            for (Cell cell : myNeighbors)
            {
                if (this.getCity().equals(cell.getCity()))
                    an.add(cell);
            }
        }
        return an;
    }

    // default is to show geology but army pieces override
    public char getChar()
    {
        char c = myGeology.getChar();
        if (myDefense.ordinal() > Occupiers.NONE.ordinal())
            c = Occupiers.ourChar[myDefense.ordinal()];
        return c;
    }
	/*
	 * public Drawable getImage(int width, int height) { Drawable i =
	 * myGeology.getImage(width,height); if (myDefense.ordinal() >
	 * Occupiers.NONE.ordinal()) { i = Occupiers.ourImage[myDefense.ordinal()]; if
	 * (this.myScaledIcon == null) { this.myScaledHeight = height;
	 * this.myScaledWidth = width; this.myScaledIcon =
	 * getImage();//i.getScaledInstance(width, height, Image.SCALE_SMOOTH); } else
	 * if (myScaledHeight != height || myScaledWidth != width) { this.myScaledHeight
	 * = height; this.myScaledWidth = width; this.myScaledIcon =
	 * getImage();//i.getScaledInstance(width, height, Image.SCALE_SMOOTH); } i =
	 * myScaledIcon; } return i; }
	 * 
	 * 
	 * // default is to show geology but army pieces override public Drawable
	 * getImage() { Drawable i = myGeology.getImage(); if (myDefense.ordinal() >
	 * Occupiers.NONE.ordinal()) i = Occupiers.ourImage[myDefense.ordinal()]; return
	 * i; }
	 */

    public void setBackground(int rgb)
    {
        myBG = rgb;

    }

    public int getBackground()
    {
        return myBG;
    }

    public boolean equals(Object obj)
    {
        if (obj == null)
            return false;
        else if (obj.getClass() != this.getClass())
            return false;
        Cell cell = (Cell)obj;
        return this.myRow == cell.myRow && this.myCol == cell.myCol;
    }

    public int getDefense()
    {
        int defense = getOccupiers().getValue();
        ArrayList<Cell> allies = getCityNeighbors();
        for (Cell cell : allies)
        {
            defense = (defense > cell.getOccupiers().getValue() ?
                    defense : cell.getOccupiers().getValue());
        }
        return defense;
    }

    public boolean ableToAttack()
    {
        return myAttackAbility;
    }
    public void ableToAttack(boolean ableTo)
    {
        myAttackAbility = ableTo;
    }

    public String toString()
    {
        return " " + myRow + " " + myCol + " " + myDefense.ordinal() + " ";
    }


}
