package com.kinser.midevilworld;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

public class City extends ArrayList<Cell> implements Serializable
{
    /**
     *
     */
    private static final long serialVersionUID = 3534671180721689277L;
    //private ArrayList<Cell> myLand = new ArrayList<Cell>();
    private transient ArrayList<Cell> myNeighbors = new ArrayList<Cell>();
    private ArrayList<Occupiers> myArmy = new ArrayList<Occupiers>(); // army
    private int myGoldReserve = 0;
    private int myID = -1;
    private transient Player myPlayer = null;
    //private transient Region myArea = new Region();
    private transient ArrayList<Cell> multiMoveCells = null;
    private transient ArrayList<Cell> emptyMultiMoveCells = null;
    private transient Occupiers multiMovePiece = Occupiers.NONE;


    public City()
    {
        myID = (int)(Math.random()*100000);
    }
    public City(ArrayList<Cell> city)
    {
        myID = (int)(Math.random()*100000);

        //myLand = city;
        for (Cell cell : city)
            this.add(cell);
    }

    public int currentGoldValue()
    {
        return myGoldReserve;
    }
    public void addGold(int gold)
    {
        myGoldReserve += gold;
    }

    public int getID()
    {
        return myID;
    }
    public Player getPlayer()
    {
        return myPlayer;
    }
    public void setPlayer(Player p)
    {
        myPlayer = p;
    }

    public int goldGeneratedEachTurn()
    {
        //return myLand.
        return size();
    }

    public int goldConsumptionEachTurn()
    {
        int total = 0;
        for (int i = 0; i < myArmy.size(); i++)
            total += myArmy.get(i).getConsumption();
        return total;
    }

    public void processGold()
    {
        myGoldReserve += goldGeneratedEachTurn() - goldConsumptionEachTurn();
        if (myGoldReserve < 0) // lose my army if I'm out of gold
        {
            makeArmyAvailable(); // pull them from cells
            myArmy = new ArrayList<Occupiers>();
        }

    }

    public boolean armyPieceAffordable(Occupiers piece)
    {
        boolean canAffordIt = false;
        // if I have enough gold to buy the piece
        // AND I'll have enough to cover all costs next turn
        if (( myGoldReserve >= piece.getCost()) &&
                ((piece.getConsumption() + goldConsumptionEachTurn()) <
                        (goldGeneratedEachTurn() + (myGoldReserve-piece.getCost()))))
        {
            canAffordIt = true;
        }

        return canAffordIt;
    }

    public boolean buyAnArmyPieceForCity(Occupiers piece, Cell c)
    {
        boolean canAffordIt = armyPieceAffordable(piece);
        if (canAffordIt && (c.getOccupiers() == Occupiers.NONE))
        {
            myGoldReserve -= piece.getCost();
            c.setOccupiers(piece);
            myArmy.add(piece);
            // don't need to do anything with the old piece???
        }
        else
            canAffordIt = false;

        return canAffordIt;
    }

    public Cell getCurrentVillage()
    {
        Cell village = null;
        for (Cell cell : this)
            if (cell.getOccupiers() == Occupiers.VILLAGE)
                village = cell;

        return village;
    }

    public ArrayList<Cell> getNeighbors()
    {
        findMyNeighbors();
        return myNeighbors;
    }

    public ArrayList<Cell> getEnemyNeighbors()
    {
        ArrayList<Cell> n = new ArrayList<Cell>();
        for (Cell cell : /* myLand */this)
        {
            // some cells have the same neighbors so don't add the same one twice
            // also, don't add allied cells (cells owned by the same player)

            ArrayList<Cell> cn = cell.getEnemyNeighbors();
            for (Cell neighbor : cn)
            {
                // need to check that I haven't added this neighbor already
                if (!n.contains(neighbor))
                    n.add(neighbor);
            }
        }
        return n;
    }

    // find all the cells adjacent to this city's cells (may or may not
    // belong to the same player)
    private void findMyNeighbors()
    {
        ArrayList<Cell> n = new ArrayList<Cell>();
        for (Cell cell : /* myLand */this)
        {
            // some cells have the same neighbors so don't add the same one twice
            // also, don't add allied cells (cells owned by the same player)
            // NOTE: getNeighbors at the cell level returns all 8 neighbors
            // regardless of color or city affiliation
            ArrayList<Cell> cn = cell.getNeighbors();
            for (Cell neighbor : cn)
            {
                // every city belongs to a player so I belong to a player but I
                // need to check to see if the neighbor belongs to a city and
                // if that city isn't me.
                // and that I haven't added this neighbor already
                // NOTE: this mean I could return neighbors who belong to the
                // same player but just in a different city
                if ((neighbor.getCity() != null) &&
                        (neighbor.getCity() != this) &&
                        (!n.contains(neighbor)))
                    n.add(neighbor);
                    // if neighbor isn't in a city and I don't have it in the list
                    // already, add it
                else if ((neighbor.getCity() == null) &&
                        (!n.contains(neighbor)))
                    n.add(neighbor);
            }
        }
        myNeighbors = n;
    }

    // returns only the neighbor cells that are the same color but not in
    // this city (may or may not be part of a city)
    public ArrayList<Cell> findMyAlliedNeighbors()
    {
        ArrayList<Cell> n = new ArrayList<Cell>();
        for (Cell cell : /* myLand */this)
        {
            // getAlliedNeighbors returns neighbors of cell that are
            // the same color (but NOT the same city)
            ArrayList<Cell> cn = cell.getAlliedNeighbors();
            for (Cell neighbor : cn)
            {
                if (!n.contains(neighbor))
                    n.add(neighbor);
            }
        }
        return n;
    }

    public ArrayList<Occupiers> getArmy()
    {
        return myArmy;
    }
    public ArrayList<Occupiers> makeArmyAvailable()
    {
        ArrayList<Occupiers> moveablePieces = new ArrayList<Occupiers>();
        for (Cell cell : /* myLand */this)
        {
            Occupiers piece = cell.getOccupiers();
            if ((piece.ordinal() != Occupiers.CASTLE.ordinal()) &&
                    (piece.ordinal() != Occupiers.VILLAGE.ordinal()))
            {
                cell.setOccupiers(Occupiers.NONE);
                moveablePieces.add(piece);
            }
        }
        return moveablePieces;
    }

    public void cancelMultiMovePiece()
    {
        if (this.multiMoveCells == null)
            return;
        for (Cell cell : this.multiMoveCells)
        {
            // is there a conflict (one of the cells we took the multi select
            // piece from has something on it now so we can't return the original piece)
            if (cell.getOccupiers() != Occupiers.NONE)
            {
                // if so, then use one of the empty ones
                // note: the empty list has to have something or we wouldn't be in
                // this situation.
                cell = this.emptyMultiMoveCells.get(0);
            }
            cell.setOccupiers(this.multiMovePiece);
        }
        this.multiMoveCells = null;
        this.multiMovePiece = Occupiers.NONE;
        this.emptyMultiMoveCells = null;

    }

    public boolean setMultiMovePiece(Occupiers piece)
    {
        boolean piecesAvailable = true;
        int count = 0;
        if (multiMoveCells != null)
            cancelMultiMovePiece();
        multiMoveCells = new ArrayList<Cell>();
        this.emptyMultiMoveCells = new ArrayList<Cell>();
        multiMovePiece = piece;
        for (Cell cell : /* myLand */this)
        {
            Occupiers cellPiece = cell.getOccupiers();
            if (cellPiece.ordinal() == piece.ordinal())
            {
                if (cell.ableToAttack())
                {
                    cell.setOccupiers(Occupiers.NONE);
                    multiMoveCells.add(cell);
                    count++;
                }
            }
        }
        if (count <2)
        {
            piecesAvailable = false;
            cancelMultiMovePiece();
        }

        return piecesAvailable;
    }

    // returns true if more pieces available after decrement
    public boolean decrementMultiMovePieces()
    {
        boolean status = false;
        if (this.multiMoveCells != null)
        {
            if (this.multiMoveCells.size() != 0)
            {
                // hang on to the cells that used to have multi select pieces on it
                // as we may need these if there is a conflict during cancellation
                this.emptyMultiMoveCells.add(this.multiMoveCells.remove(0));
            }

            if (this.multiMoveCells.size() == 0)
                this.multiMoveCells = null;
            else
                status = true;
        }

        return status;
    }


    public boolean equals(Object obj)
    {
        if (obj == null)
            return false;
        else if (obj.getClass() != this.getClass())
            return false;
        City city = (City)obj;
        //return (this.myGoldReserve == city.myGoldReserve) && (this.myPlayer == city.myPlayer) && (this.myArmy == city.myArmy);
        return this.myID == city.myID;
    }

    public void calculateEdges(int height, int width)
    {
		/*
		 * if (myArea == null) myArea = new Region(); myArea.setEmpty(); for (Cell c :
		 * this) myArea.op(c.getArea(),Region.Op.UNION);
		 */
    }

	/*
	 * public Region getArea() { if (myArea == null) myArea = new Region(); return
	 * myArea; }
	 */

    @SuppressWarnings("unchecked")
    public void loadCity(ObjectInputStream in, Cell[][] map) throws IOException
    {
        myGoldReserve = in.readInt();
        Object obj = null;
        try {
            obj = in.readObject();
            if (obj.getClass() != myArmy.getClass())
                throw new ClassNotFoundException();
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        myArmy = (ArrayList<Occupiers>)obj;
        myID = in.readInt();

        int count = in.readInt();
        for (int i = 0; i < count; i++)
        {
            Cell cell = map[in.readInt()][in.readInt()];
            this.add(cell);
            cell.setCity(this);
        }
        this.multiMoveCells = null;
        this.multiMovePiece = Occupiers.NONE;

    }

    @Override
    public String toString()
    {
        String str = super.toString();
        return myID + str;
    }

    public void saveCity(ObjectOutputStream out) throws IOException
    {
        out.writeInt(myGoldReserve);
        out.writeObject(myArmy);
        out.writeInt(myID);
        int count = size();
        out.writeInt(count);
        for (int i = 0; i < count; i++)
        {
            Cell cell = get(i);
            out.writeInt(cell.getRow());
            out.writeInt(cell.getCol());
        }
    }

}