package com.kinser.midevilworld;

import java.io.Serializable;

import java.util.*;

public class Player implements Serializable
{
    public static final String[] playerColorNames =
            {"LIGHT GRAY",
                    "LIGHT BLUE",
                    "PINK",
                    "RED",
                    "ORANGE",
                    "MAGENTA",
                    "YELLOW",
                    "LIGHT GREEN"
            };

    public static final int rgb(int r, int g, int b)
    {
        return r*256*256 + g*256 + b;
    }

    public static final int[] playerColors =
            {   rgb(204, 204, 204),
                    rgb(0, 255, 255),
                    rgb(255, 192, 203),
                    rgb(255, 0, 0),
                    rgb(255, 165, 0),
                    rgb(255, 0, 255),
                    rgb(255, 255, 0),
                    rgb(153, 255, 51)};

    /**
     *
     */
    private static final long serialVersionUID = 6139076431935707192L;
    private transient ArrayList<Cell> myAttackingPieces = new ArrayList<Cell>();
    protected transient ArrayList<City> myCities;
    protected int myColor;
    protected String myColorDisplayName;

    public Player()
    {
        myCities = new ArrayList<City>();
    }
    public ArrayList<City> getCities()
    {
        return myCities;
    }
    public void addCity(City city)
    {
        if (!myCities.contains(city))
        {
            myCities.add(city);
            city.setPlayer(this);
        }
    }
    public void addCity(ArrayList<Cell> city)
    {
        addCity(new City(city));
    }
    public City findCityWithCell (Cell cell)
    {
        City c = cell.getCity();
        if (c != null)
        {
            if (c.getPlayer() != this)
                c = null; // not my city
        }
        return c;
    }
    public void removeCity(City city)
    {
        myCities.remove(city);
    }

    public void setColor(int rgb)
    {
        myColor = rgb;
    }

    public int getColor()
    {
        return myColor;
    }

    public void setColorName(String clr)
    {
        myColorDisplayName = clr;
    }
    public String getColorName()
    {
        return myColorDisplayName;
    }


    // returns true if done with turn (this is needed for the human players)
    public boolean takeTurn()
    {
        return true;
    }


    public boolean equals(Object obj)
    {
        if (obj == null)
            return false;
        else if (obj.getClass() != this.getClass())
            return false;
        Player p = (Player)obj;
        return this.myColor == p.myColor;
    }

    public void combineAdjacentCities()
    {
/*
	iterate through each city
		iterate through the cities neighbors
		if a neighbor is a member of another city, merge them together (removing village that appears later in the collection in order to avoid messing up the loop through the collection)
*/
        int index = 0;
        boolean keepGoing = (getCities().size() > index);
        while (keepGoing)
        {
            City city = getCities().get(index);

            // findMyAlliedNeighbors only returns cells that are the same color
            // but belong to a different city or no city at all
            for(Cell cell : city.findMyAlliedNeighbors())
            {
                City otherCity = findCityWithCell(cell);
                //Note: if this cell is part of another city, that city
                // must come later in the collection otherwise that city
                // would have identified this city as a neighbor and already
                // merged
                // Note: city might have multiple cells touching another
                // city which can lead to a situation where findCityWithCell
                // returns this city instead of another one because it already/
                // merged so check that otherCity isn't me

                if ((null != otherCity) &&
                        (otherCity != city))
                {
                    city.addGold(otherCity.currentGoldValue());
                    while (otherCity.size() >= 1)
                    {
                        // move each cell from otherCity to this city
                        Cell c = otherCity.remove(0);
                        city.add(c);

                        // no need to keep otherCity village
                        if (c.getOccupiers() == Occupiers.VILLAGE)
                            c.setOccupiers(Occupiers.NONE);
                            // transfer otherCity army units to city army
                        else if ((c.getOccupiers() != Occupiers.NONE) &&
                                (c.getOccupiers() != Occupiers.CASTLE))
                            city.getArmy().add(c.getOccupiers());

                        // tell cell that it belongs to different city now
                        c.setCity(city);
                        // note: cell's background color already set

                    }
                    // other city should be empty now so remove it
                    getCities().remove(otherCity);

                    // now that cities are merged, redraw perimeter
                    city.calculateEdges(World.colOffset, World.rowOffset);
                }
                else if (null == otherCity) // then this is just an unaffiliated cell
                {
                    city.add(cell);
                    cell.setCity(city);
                    cell.setOccupiers(Occupiers.NONE);
                    // cell's background color already set
                    city.calculateEdges(World.colOffset, World.rowOffset);
                }
                // else otherCity must be this city so do nothing
            }// loop through neighbors
            index += 1;
            keepGoing = (getCities().size() > index);
        }// loop through cities
    }// merge cities

    public void cityLostCell(City city, Cell cell)
    {
        // quick validation of parameters in to ensure they are mine
        if (city == null || cell == null)
            return;
        // this is not my city
        if (!myCities.contains(city))
            return;

        if (!city.contains(cell)) //city.equals(cell.getCity()))
        {
            return;
        }

        // remove cell from city right away so we don't double count
        city.remove(cell);

        // remember lostPiece for later processing if city continues to exist
        Occupiers lostPiece = cell.getOccupiers();
        cell.setOccupiers(Occupiers.NONE);
        cell.setBackground(Cell.GREEN);// green is bits 8-15
        cell.setCity(null);

        // does city only have one cell or less?
        if (city.size() < 2)
        {
            if (city.size() == 1)
            {
                city.get(0).setCity(null);
                city.get(0).setOccupiers(Occupiers.NONE);
                city.get(0).setBackground(Cell.GREEN);// green is bits 8-15
                city.remove(city.get(0));
            }
            myCities.remove(city);
            return;
        } // city can't exist with only one cell

        // don't need to do the rest of this if city doesn't exist

        // relocate village if taken
        if (lostPiece == Occupiers.VILLAGE)
        {
            // lose all gold when village taken
            city.addGold(-1 * city.currentGoldValue());
            findNewVillage(city);
        }
        // if lost cell had an army piece on it, remove this from city's army
        else if (lostPiece != Occupiers.NONE)
        {
            city.getArmy().remove(cell.getOccupiers());
        }
    } // city lost cell

    public void determineCityConnectivity(City city)
    {

        if (city == null || city.size() < 2)
            return;

        // remove any cells that are all by themselves.
        int index = 0;
        for (index = 0; index < city.size(); index++)
        {
            Cell cell = city.get(index);
            ArrayList<Cell> cells = cell.getCityNeighbors();
            if (cells.size() == 0)
            { // then this cell is isolated and should be removed
                cityLostCell(city,cell);
                index--; // redo this index now that a different cell is there
            }
        }

        // if this left the city with zero cells, remove it from player list
        if (city.size() == 0)
        {
            myCities.remove(city);
            return;
        }
        if (city.size() == 1)
        {
            cityLostCell(city,city.get(0));
            return;
        }

        // if the city is disjointed, then picking any cell at random and building
        // a list of all cells connected to that cell will result in the new list
        // being of a different size than the original city list.


        // pick any cell in the city
        // add it to a new list
        City newCity = new City();
        newCity.add(city.get(0));
        int newCityIndex = 0;

        // find all its allied neighbors (that aren't already in the new list)
        // if 1 or more,
        // add them to the list,
        // take next cell in new list
        // and try again
        //NOTE: at this point, all cells think they are part of the original
        // city even if disjointed (and that's okay for now)
        while (newCityIndex < newCity.size())
        {
            ArrayList<Cell> cells = newCity.get(newCityIndex).getCityNeighbors();

            for(Cell cell : cells)
            {
                if (!newCity.contains(cell))
                    newCity.add(cell);
            }
            newCityIndex++;
        }

        //once no more allied neighbors can be found and all the cells in the new
        //          list have been checked, compare #in new list to #in original city
        // if the # is same then it is not disjointed.
        // if # is different, take new list and make it its own city (add village, etc)
        // Take all cells in new list out of original city list
        // start over at the beginning (recursive call?)
        if (newCity.size() == city.size())
            return;

        if (newCity.size() == 1)
        {
            cityLostCell(city,newCity.get(0));
            newCity.remove(0);
        }

        // only gets here if city was disjointed
        boolean villageFound = false;
        for (Cell cell : newCity)
        {
            city.remove(cell);
            cell.setCity(newCity);

            // if cell has village keep it for newCity but remember to find a
            // new village for the original city later
            if (cell.getOccupiers() == Occupiers.VILLAGE)
                villageFound = true;

                // transfer army units to newCity
            else if ((cell.getOccupiers() != Occupiers.NONE) &&
                    (cell.getOccupiers() != Occupiers.CASTLE))
            {
                newCity.getArmy().add(cell.getOccupiers());
                city.getArmy().remove(cell.getOccupiers());
            }
        }

        // if resulting original city has only 1 cell, remove it from player list
        if (city.size() == 1)
        {
            cityLostCell(city,city.get(0));
        }

        if (newCity.size() > 0)
        {
            newCity.setPlayer(this);
            myCities.add(newCity);
        }

        // NOTE: original city gets to keep all the gold accumulated thus far
        if (villageFound && (city.size() > 0))  // village is in newCity, not here so add one here
        {
            findNewVillage(city);
        }

        // newcity is either empty or complete at this point.
        // now, recursively call this method again to check for
        // multiple disjoint cities (i.e. it is possible that a single move
        // could cause the original city to break up into 3 or more parts
        // especially if it was large.
        if (city.size() > 1)
            determineCityConnectivity(city);
    } // end determineCityConnectivity

    private Cell findNewVillage(City city)
    {
        Cell location = city.getCurrentVillage();
        boolean villagePlaced = (location != null);
        for (int index = 0;!villagePlaced && index < city.size(); index++)
        {
            if (city.get(index).getOccupiers() == Occupiers.NONE)
            {
                villagePlaced = true;
                city.get(index).setOccupiers(Occupiers.VILLAGE);
                location = city.get(index);
            }
        }
        // if village not placed then all cells occupied, delete occupier
        // in first cell and place village there
        if (!villagePlaced)
        {
            city.getArmy().remove(city.get(0).getOccupiers());
            city.get(0).setOccupiers(Occupiers.VILLAGE);
            location = city.get(0);
        }
        return location;
    }


    // takes cell, and places piece on it
    public void cityGainsCell(City city, Cell cell, Occupiers piece)
    {
        // end processing if params are null or if this isn't my city or the city already
        // has this cell in it.
        if (city == null || cell == null || !myCities.contains(city) || city.contains(cell))
            return;

        // tell cell's current player that their city lost this cell
        if (cell.getCity() != null)
        {
            City cellCity = cell.getCity();
            cellCity.getPlayer().cityLostCell(cellCity, cell);
            cellCity.getPlayer().determineCityConnectivity(cellCity);
        }

        cell.setCity(city);
        cell.setBackground(this.getColor());
        cell.setOccupiers(piece);
        addToAttackingPieces(cell);
        city.add(cell);
    }

    // buys necessary army piece to take cell if possible. return false if can't.
    public boolean cityGainsCell(City city, Cell cell)
    {
        // end processing if params are null or if this isn't my city or the city already
        // has this cell in it.
        if (city == null || cell == null || !myCities.contains(city) || city.contains(cell))
            return false;

        // what is the weakest army piece able to take this cell?
        for (Occupiers piece : Occupiers.moveablePieces)
        {
            if ((piece.getValue() > cell.getDefense()) &&
                    (piece.getCost() < city.currentGoldValue()))
            {
                // tell cell's current player that their city lost this cell
                if (cell.getCity() != null)
                {
                    City cellCity = cell.getCity();
                    cellCity.getPlayer().cityLostCell(cellCity, cell);
                    cellCity.getPlayer().determineCityConnectivity(cellCity);				}

                // make the cell mine
                cell.setCity(city);
                cell.setBackground(this.getColor());
                addToAttackingPieces(cell);
                city.add(cell);

                city.buyAnArmyPieceForCity(piece, cell);
                return true;
            }
        }
        return false;
    }

    public City findNearestAlly(City city)
    {
        // for now, just do distance from village to village
        City nearestCity = city;
        double distance = 1000000.0;
        Cell cell = findNewVillage(city);
        Cell allyCell;
        for (City ally : myCities)
        {
            if (ally != city)
            {
                allyCell = findNewVillage(ally);
                double allyDistance = Math.sqrt(Math.pow(allyCell.getCol()-cell.getCol(), 2) +
                        Math.pow(allyCell.getRow()-cell.getRow(), 2));
                if (allyDistance < distance)
                {
                    nearestCity = ally;
                    distance = allyDistance;
                }
            }
        }
        return nearestCity;
    }

    public void resetAttackingPieces()
    {
        for (Cell cell : myAttackingPieces)
        {
            cell.ableToAttack(true);
        }
        myAttackingPieces.clear();
    }

    public void addToAttackingPieces (Cell cell)
    {
        cell.ableToAttack(false);
        myAttackingPieces.add(cell);
    }

    public void debugDump()
    {
        System.out.println("My background color is " + myColor +  " and I have " + myCities.size() + " cities ");
        int count = 0;
        for (City city : myCities)
        {
            System.out.println("city # " + count + " has " + city.size() + " cells and " + city.getArmy().size() + " armies");
            count++;
            for (Cell cell : city)
            {
                if (cell.getBackground() != myColor) {
                    System.out.println("cell at " + cell.getRow() + ", " + cell.getCol() + " has the wrong color " + cell.getBackground());
                    System.out.println(" cell thinks its in the right city : " + (cell.getCity().equals(city)));
                }

            }
        }
    }

}

