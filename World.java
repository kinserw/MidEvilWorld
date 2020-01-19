package com.kinser.midevilworld;

import java.io.Serializable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;

public class World implements Serializable, Runnable
{
    /**
     *
     */
    private static final long serialVersionUID = 2112960996668811817L;
    public static World theWorld = null;
    public static int max_rows = 50;
    public static int max_cols = 25;
    public static int rows = 50;
    public static int cols = 25;
    public static int density = 85;
    public static int numPlayers = 7;
    public static Cell currentCell = null;

    public static final int rowOffset = 20;
    public static final int colOffset = 20;

    public static int difficulty = 8;

    public Cell[][] myMap = null;

    public transient boolean myKeepRunning = false;
    private int whosTurnItIs = 0;
    public  transient HumanPlayer human = null;
    private int humanIndex = 0;
    public transient City citySelected = null;
    private transient Cell movingPieceFrom = null;
    private transient Occupiers movingPiece = Occupiers.NONE;
    private boolean multiPieceMove = false;
    private transient boolean buyingAPiece = false;
    private transient ArrayList<Cell> myPlayableMap;
    private ArrayList<Player> myPlayers;

    private transient UIRefreshInterface myUIRefresh = null;
    private transient NotifyGameWon myNotifyWhoWon = null;



    // World default constructor
    public World() {
        World.theWorld = this;

    } // end World constructor

    public Player currentPlayer()
    {
        if (myPlayers != null)
            return myPlayers.get(whosTurnItIs);
        return null;
    }


    private Cell findCellAt(int relativeX, int relativeY)
    {
        if (!myKeepRunning || myMap == null)
            return null;

        Cell cell = null;
        int row = relativeX/rowOffset;
        int col = relativeY/colOffset;
        if ((row >= 0) && (row < World.rows) & (col >= 0) && (col < World.cols ))
            cell = myMap[row][col];

        return cell;
    }


    public void setUIRefresh(UIRefreshInterface uir)
    {
        this.myUIRefresh = uir;
    }

    public void setNotifyWhoWon(NotifyGameWon ngw)
    {
        this.myNotifyWhoWon = ngw;
    }

    public void mouseClicked(int x, int y)
    {

        Cell cell = findCellAt(x,y);
        if ((cell != null) &&
                ((cell.getCity() != null )
                        && (cell.getCity().getPlayer() == human)
                ))
        {
            citySelected = cell.getCity();
        }
    }

    public void buyingArmyUnit(Occupiers piece)
    {
        buyingAPiece = true;
        movingPiece = piece;
        movingPieceFrom = null;
    }


    public void mousePressed(boolean btn1, int x, int y)
    {
        Cell cell = findCellAt(x,y);
        // only process if not btn 1 and not already in a multi piece move action
        if (!btn1 && !multiPieceMove && (cell != null))
        {

            // if cell is the human's then enable moving the pieces
            if ((cell.ableToAttack() &&
                    ((cell.getCity() != null ) &&
                            (cell.getCity().getPlayer() == human))))
            {
                citySelected = cell.getCity();
                for (Occupiers piece : Occupiers.moveablePieces)
                {
                    // find the piece they clicked on
                    if (cell.getOccupiers().ordinal() == piece.ordinal())
                    {
                        // make sure they have more than one
                        if (citySelected.setMultiMovePiece(piece))
                        {
                            movingPieceFrom = cell;
                            movingPiece = piece;
                            this.multiPieceMove = true;
                        }
                    }

                }
            } // end of moving human pieces on the board
        } // end multiple piece move

        // see if the player is moving an existing piece or buying one
        if (btn1 && (cell != null))
        {
            // if btn 1 pressed then cancel multiPieceMove if active
            if (this.multiPieceMove)
            {
                citySelected.cancelMultiMovePiece();
                multiPieceMove = false;
                movingPieceFrom = null;
            }

            // if cell is the human's then enable moving the pieces
            if ((cell.ableToAttack() &&
                    ((cell.getCity() != null ) &&
                            (cell.getCity().getPlayer() == human))))
            {
                citySelected = cell.getCity();
                for (Occupiers piece : Occupiers.moveablePieces)
                {
                    if (cell.getOccupiers().ordinal() == piece.ordinal())
                    {
                        movingPiece = piece;
                        movingPieceFrom = cell;

                    }

                }
            } // end of moving human pieces on the board
            // now check if human is buying a piece

        } // if btn one and cell not null

    } // mouse pressed

    public void endTurn()
    {
        if (human != null)
        {
            this.citySelected = null;
            this.buyingAPiece = false;
            this.movingPiece = Occupiers.NONE;
            this.movingPieceFrom = null;
            this.multiPieceMove = false;

            human.turnIsOver();
        }
        validateWorld();
    }


    public void mouseReleased(int x, int y)
    {
        // this logic is for placing the selected army piece in the same
        // city as selected (either moving an existing piece or buying one)
        // TODO consider moving a lot if not all this logic to the City class
        Cell cell = findCellAt(x,y);
        if ((cell != null) && (cell.getCity() != null) &&
                (cell.getCity().equals(citySelected)))
        {
            currentCell = cell;
            // can't move it to a cell with an piece already there
            if (cell.getOccupiers() != Occupiers.NONE)
            {

            }
            // target cell is available
            else
            {
                // if human is buying a piece, go ahead and move it
                if (buyingAPiece)
                {
                    citySelected.buyAnArmyPieceForCity(movingPiece, cell);
                }
                // else if human is moving it within the same city,...
                else if (movingPieceFrom != null)
                {
                    if (this.multiPieceMove)
                    {
                        cell.setOccupiers(movingPiece);
                        if (!citySelected.decrementMultiMovePieces())
                        {
                            movingPieceFrom = null;
                            multiPieceMove = false;
                        }
                    }
                    else
                    {
                        cell.setOccupiers(movingPiece);
                        movingPieceFrom.setOccupiers(Occupiers.NONE);
                    }
                }
            }
        }
        else
            currentCell = null;

        // check to see if human is attacking someone (movingPieceFrom !null)
        // cell needs to be not null and in the selected cities neighbor list
        // NOTE: if a piece can't attack, it can't be "picked up" so we don't need
        // to check for able to attack
        if ((cell != null) &&
                ((movingPieceFrom != null) ||
                        (buyingAPiece && (movingPiece != Occupiers.CASTLE)))
                && (citySelected != null)
                && (!citySelected.equals(cell.getCity())) && (citySelected.getNeighbors().contains(cell)))
        {
            // get defense value, compare to moving piece attack value
            if (cell.getDefense() < movingPiece.getValue())
            {

                // tell enemy player their city lost a cell.
                // player will adjust the status of it's city accordingly.

                City city = cell.getCity();
                if (city != null)
                {
                    city.getPlayer().cityLostCell(city,cell);
                    city.getPlayer().determineCityConnectivity(city);
                }

                cell.setCity(citySelected);

                cell.setBackground(citySelected.getPlayer().getColor());
                citySelected.add(cell);
                if (movingPieceFrom != null)
                    movingPieceFrom.setOccupiers(Occupiers.NONE);

                if (buyingAPiece)
                {
                    // buyAnArmyPieceForCity expects the cell to be empty
                    cell.setOccupiers(Occupiers.NONE);
                    citySelected.buyAnArmyPieceForCity(movingPiece, cell);
                }
                else // buyAnArmyPieceForCity handles this so do it if not buying
                {
                    cell.setOccupiers(movingPiece);
                    if (this.multiPieceMove)
                    {
                        if (!citySelected.decrementMultiMovePieces())
                        {
                            movingPieceFrom = null;
                            multiPieceMove = false;
                        }
                    }

                }

                // can't move this piece again this turn since I attacked with it
                human.addToAttackingPieces(cell);


                // check if new cell is near another allied city
                if (cell.getAlliedNeighbors().size() > 0)
                {
                    // cancel multi piece move in case city gets eliminated
                    if (this.multiPieceMove)
                        citySelected.cancelMultiMovePiece();

                    human.combineAdjacentCities();
                    // just in case the selected city was combined and lost...
                    // use the city the taken cell is associated with cause/
                    // it will either be the citySelected or the city it was combined with

                    citySelected = cell.getCity();
                    if (this.multiPieceMove)
                    {
                        citySelected.setMultiMovePiece(this.movingPiece);
                    }

                }

                citySelected.calculateEdges(colOffset, rowOffset);
            } // can human take enemy occupier
        }

        if (!this.multiPieceMove)
        {
            movingPiece = Occupiers.NONE;
            movingPieceFrom = null;
        }
        buyingAPiece = false;

    } // end mouseReleased

    public void newGame()
    {
        myMap = null;
        myKeepRunning = true;
    }

    public void resetWorld()
    {

        myKeepRunning = false;
        for (int i = 0; myMap != null && i < rows; i++)
        {
            for (int j = 0; j< cols; j++)
            {
                myMap[i][j].setGeology(Geology.WATER);
                myMap[i][j].setBackground(Cell.BLUE); // blue is bits 0-7
                myMap[i][j].setCity(null);
                myMap[i][j].setOccupiers(Occupiers.NONE);
                citySelected = null;
                currentCell = null;
            }
        }
    }

    public void saveGame(ObjectOutputStream out) throws IOException
    {
        // Method for serialization of object
        out.writeInt(rows);
        out.writeInt(cols);
        out.writeInt(density);
        out.writeInt(difficulty);

        out.writeInt(myPlayers.size());
        out.writeInt(humanIndex);
        out.writeInt(human.currentTurnCount());

        // can't save the whole world because it is
        // runnable and it is too complicated to
        // drop existing world and reconnect.
        // so we only save the map (all cells)
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
            {
                out.writeObject(myMap[i][j]);
            }


        for (int i = 0; i < myPlayers.size(); i++)
        {
            Player player = myPlayers.get(i);
            out.writeInt(player.getColor());
            out.writeObject(player.getColorName());

            // rebuild player's cities
            int count = player.getCities().size(); // city count
            out.writeInt(count);
            for (int a = 0; a < count; a++)
            {
                City city = player.getCities().get(a);
                //out.writeObject(city); // save non-reference items

                city.saveCity(out); // save reference items
            }

        } // end loop through players to save their cities
    } // end saveGame

    public void loadGame(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        myKeepRunning = false; // stop game if not already stopped
        citySelected = null;
        currentCell = null;
        movingPieceFrom = null;
        buyingAPiece = false;
        movingPiece = Occupiers.NONE;
        multiPieceMove = false;


        rows = in.readInt();
        cols = in.readInt();
        density = in.readInt();
        difficulty = in.readInt();

        int playerCount = in.readInt();
        humanIndex = in.readInt();
        whosTurnItIs = humanIndex;
        int turn = in.readInt(); // don't have a human yet so store here temporarily

        myMap = new Cell[rows][cols];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
            {
                myMap[r][c] = (Cell)in.readObject();
                myMap[r][c].computeArea();
            }

        // now World has everything it needs to rebuild

        myPlayers = new ArrayList<Player>();
        for (int i = 0; i < playerCount; i++)
        {
            Player player = null;
            if (i == humanIndex)
            {
                human = new HumanPlayer(false,false);
                player = human;
                human.currentTurnCount(turn);
            }
            else
                player = new ComputerPlayer(difficulty);
            myPlayers.add(player);
            int color = in.readInt();
            player.setColor(color);
            String colorName = (String)in.readObject();
            player.setColorName(colorName);


            // rebuild player's cities
            int count = in.readInt(); // city count
            for (int a = 0; a < count; a++)
            {
                City city = new City();
                city.loadCity(in, myMap); // loads reference items
                city.calculateEdges(colOffset,rowOffset);

                // associate city with this player
                player.getCities().add(city);
                city.setPlayer(player);
            }

        }

        // rebuild everything that depends on Cells
        myPlayableMap = new ArrayList<Cell>();
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
            {
                Cell cell = myMap[r][c];
                if (cell.getGeology() != Geology.WATER)
                    myPlayableMap.add(cell);

                // rebuild reference data for each cell
                setNeighbors(cell);
            }

    } // end loadGame

    public void run()
    {
        while (true)
        {
            if (myKeepRunning)
            {
                if (myMap == null) // if we haven't created a world yet, do so now.
                {
                    myPlayableMap = new ArrayList<Cell>();
                    buildMap();
                    humanIndex = (int)(Math.random()*numPlayers);
                    assignPlayers(numPlayers, humanIndex);
                }

                do
                {
                    takeTurn();
                    myUIRefresh.refreshUI();

                } while (myKeepRunning);
            }
            else
                synchronized(this)
                {
                    try {
                        wait(100);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
        }
    } // end run


    public void buildMap()
    {

        // first pass builds an oval shaped island
        myMap = new Cell[rows][cols];

        for (int i = 0; i < rows; i++)
        {
            for (int j = 0; j< cols; j++)
            {
                myMap[i][j] = createCell(i,j,density);

                if (myMap[i][j].getGeology() != Geology.WATER)
                {
                    myPlayableMap.add(myMap[i][j]);
                }
                else
                    myMap[i][j].setBackground(Cell.BLUE); // blue is bits 0-7
            }
        }


        // TODO: second pass will build outward any areas of 4 or more cells
        // adjacent to each other on the coast line but nothing on the outer
        // edge of the map (0,?) or (?,0) or (max,0) or (0,max)

        // third pass will tell each cell who its neighbors are .
        // note that all playable cells will have 8 neighbors
        for (Cell cell : myPlayableMap)
        {
            setNeighbors(cell);
        }

        // remove any "islands" not connected to the main land so that no city
        // can accidently be placed there thus eliminating the ability of
        // someone from eliminating all cities.
        determinePlayableMapConnectivity();


        // TODO this pass will also convert any land cells to be coastal cells


    }

    private void determinePlayableMapConnectivity()
    {
        // remove any cells that are all by themselves.
        int index = 0;
        for (index = 0; index < myPlayableMap.size(); index++)
        {
            Cell cell = myPlayableMap.get(index);
            ArrayList<Cell> cells = cell.getNeighbors();
            if (cells.size() == 0)
            { // then this cell is isolated and should be removed
                myPlayableMap.remove(index);
                cell.setBackground(Cell.BLUE);
                cell.setGeology(Geology.WATER);
                index--; // redo this index now that a different cell is there
            }
        }

        // if the map is disjointed, then picking any cell at random and building
        // a list of all cells connected to that cell will result in the new list
        // being of a different size than the original map list.


        // pick any cell in the map
        // add it to a new list
        ArrayList<Cell> newMap = new ArrayList<Cell>();
        newMap.add(myPlayableMap.get(0));
        int newMapIndex = 0;

        // find all its allied neighbors (that aren't already in the new list)
        // if 1 or more,
        // add them to the list,
        // take next cell in new list
        // and try again
        //NOTE: at this point, all cells think they are part of the original
        // map even if disjointed (and that's okay for now)
        while (newMapIndex < newMap.size())
        {
            ArrayList<Cell> cells = newMap.get(newMapIndex).getNeighbors();

            for(Cell cell : cells)
            {
                if (!newMap.contains(cell))
                    newMap.add(cell);
            }
            newMapIndex++;
        }

        //once no more  neighbors can be found and all the cells in the new
        //          list have been checked, compare #in new list to #in original city
        // if the # is same then it is not disjointed.
        // if # is different, set playable map to the larger of the 2 maps
        int diff = (newMap.size() - myPlayableMap.size());
        if (diff == 0)
            return;

        // make myPlayableMap the larger of the two
        if (diff > 0) // only positive if newMap was bigger
        {
            ArrayList<Cell> temp = myPlayableMap;
            myPlayableMap = newMap;
            newMap = temp;
        }

        diff = Math.abs(diff);

        // now at this point, newMap is smaller than the playable map
        // but we don't know if it contains the island or the larger
        // continent. If the difference in size is less than the size of
        // the new map then the new map represents the continent instead
        // of the isolated island so switch it around.
        if (diff < newMap.size())
        {
            @SuppressWarnings("unchecked")
            ArrayList<Cell> dup = (ArrayList<Cell>)myPlayableMap.clone();// shallow copy
            dup.removeAll(newMap);
            newMap = dup;
        }

        // remove all cells in newMap from myPlayableMap
        for (Cell cell : newMap)
        {
            myPlayableMap.remove(cell);
            cell.setBackground(Cell.BLUE);
            cell.setGeology(Geology.WATER);
        }


        // now, recursively call this method again to check for
        // multiple islands . this call will return once no islands found
        determinePlayableMapConnectivity();

    }
    private Cell createCell(int row, int col, int landDensity)
    {
        int maxRow = rows;
        int maxCol = cols;
        int centerX = maxRow/2;
        int centerY = maxCol/2;

        // if cell location is beyond land mass, it is a water cell
        Geology landType = Geology.WATER;
        Random numGen = new Random();
        int maxRowDistanceFromCenter = (int)((landDensity/100.0) * (maxRow/2.0)) -6;
        int maxColDistanceFromCenter = (int)((landDensity/100.0) * (maxCol/2.0))-2;

        int offset = (int)(Math.min(maxRow, maxCol)*0.10);

        int densityOfWater = 10;
        int densityOfLand = 50;
        int densityOfForest = 20;

        if (((int)Math.pow((row - centerX), 2) / (int)Math.pow(maxRowDistanceFromCenter, 2))
            + ((int)Math.pow((col - centerY), 2) / (int)Math.pow(maxColDistanceFromCenter, 2)) <= 1)
        {
            if (((int)Math.pow((row - centerX), 2) / (int)Math.pow(maxRowDistanceFromCenter-offset, 2))
                    + ((int)Math.pow((col - centerY), 2) / (int)Math.pow(maxColDistanceFromCenter-offset, 2)) > 1) // along outer edge
            {
                densityOfWater = 50;
                densityOfLand = 50;
                densityOfForest = 0;
            }
            int randomValue = numGen.nextInt(100);
            if (randomValue < densityOfWater)
                landType = Geology.WATER;
            else if (randomValue < densityOfLand+densityOfWater)
                landType = Geology.LAND;
            else if (randomValue < densityOfLand+densityOfWater+densityOfForest)
                landType = Geology.FOREST;
            else
                landType = Geology.MTN;
        }

        return new Cell(row,col,landType);
    }

    public void assignPlayers(int numberOfPlayers, int theHuman)
    {
        myPlayers = new ArrayList<Player>();

        @SuppressWarnings("unchecked")
        ArrayList<Cell> availableCells = (ArrayList<Cell>)myPlayableMap.clone();
        Player newPlayer;
//int avgNumCellsPerPlayer = 12-myDifficulty;
//int variance = 5-myDifficulty/2;

        for (int i = 0; i < numberOfPlayers; i++)
        {
            if (theHuman == (i))
            {
                human = new HumanPlayer();
                newPlayer = human;
            }
            else
            {
                newPlayer = new ComputerPlayer(World.difficulty);
            }
            myPlayers.add(newPlayer);

            newPlayer.setColor(Player.playerColors[i+1]);
            newPlayer.setColorName(Player.playerColorNames[i+1]);

            // assign territory
            for (int j=0;j < 10; j++) // each player gets 10 cities
            {
                City city = buildThisCity(i, availableCells);
                newPlayer.addCity(city);
                city.addGold(city.size());
            }
            newPlayer.combineAdjacentCities();

        } // create each player
    } // end assign player

    private City buildThisCity(int player, ArrayList<Cell> availableCells)
    {
/*
redesign:
find 10 random cells from available list
if it has < 2 cells in its neighbors list that are available, pick a new cell
make cell the capital and add 1 or 2 of its avail neighbors to the same city
once all cities are made,

*/
        City city = new City();
        Cell branchCell = null;
        Random numGen = new Random();

        // establish capitol cell
        branchCell = findCapitol(availableCells);
        city.add(branchCell);
        branchCell.setCity(city);
        branchCell.setOccupiers(Occupiers.VILLAGE);
        branchCell.setBackground(Player.playerColors[player+1]);
        availableCells.remove(branchCell);

        // add 1 or 2 more cells to the city from capitol's neighbors
        int count = numGen.nextInt(2) + 1;
        for (Cell c : branchCell.getNeighbors())
        {
            if (-1 != (availableCells.indexOf(c)))
            {
                count -= 1;
                city.add(c);
                c.setCity(city);
                availableCells.remove(c);
                c.setBackground(Player.playerColors[player+1]);
            }
            if (count <= 0)
                break;
        }

        return city;
    }

    private int countAvailNeighbors(Cell cell, ArrayList<Cell> availableCells)
    {
        // loop through cells around the cell ensure it isn't all
        // alone (i.e. surrounded by water or unavailable cells
        int count = 0;
        for (Cell c : cell.getNeighbors())
        {
            if (-1 != (availableCells.indexOf(c)))
                count += 1;
        }

        return count;
    }

    private Cell findCapitol(ArrayList<Cell> availableCells)
    {
        Cell capitol = null;
        while (capitol == null)
        {
            int index = (int)(availableCells.size()*Math.random());
            capitol = availableCells.get(index);
            int count = countAvailNeighbors(capitol, availableCells);
            if (count <2)
                capitol = null;
        }
        return capitol;
    }



    public void takeTurn()
    {
        // if numberOfPlayers is 1, then there is a winner
        if (myPlayers.size() == 1)
        {
            if (myKeepRunning)
            {
                myKeepRunning = false;
                this.multiPieceMove = false;
                this.movingPiece = Occupiers.NONE;
                this.movingPieceFrom = null;
                this.citySelected = null;
                this.myNotifyWhoWon.gameWasWonBy(myPlayers.get(0));
            }
            return;

        }
        if (whosTurnItIs >= myPlayers.size()) // start over with first player
            whosTurnItIs = 0;

        // only increments to the next player if current player returns true
        if (myPlayers.get(whosTurnItIs).takeTurn())
        {
            whosTurnItIs++;
            citySelected = null;

            int count = 0;

            // remove any players that have zero cities left
            boolean keepGoing = (myPlayers.size()> count);
            while (keepGoing)
            {
                Player player = myPlayers.get(count);

                if (player.getCities().size() == 0)
                {
                    myPlayers.remove(player);
                }
                else
                    count+= 1;
                keepGoing = (myPlayers.size()> count);
            }
        }

        // if total defense of all computer is < 25% of human's then surrender
    }

    private void setNeighbors(Cell cell)
    {
        ArrayList<Cell> neighbors = new ArrayList<Cell>();
        int row = cell.getRow();
        int col = cell.getCol();

        for (int a = 0; a < 3; a++) {
            for (int b = 0; b < 3; b++) {
                // catch and ignore any out of bounds exceptions as this
                // indicates we are looking for neighbors outside the mapped area
                try {
                    // don't add the cell as its own neighbor
                    if (!(a == 1 && b == 1))
                    {
                        // don't add water cells as neighbors either
                        Cell n = myMap[row-1+a][col-1+b];
                        if (n.getGeology() != Geology.WATER)
                            neighbors.add(n);
                    }
                }
                catch (Exception e)
                {}
            }
        }
        cell.setNeighbors(neighbors);

        // Find far neighbors (cells one past neighbors)
        ArrayList<Cell> farNeighbors = new ArrayList<Cell>();

        for (int a = 0; a < 5; a++) {
            for (int b = 0; b < 5; b++) {
                // catch and ignore any out of bounds exceptions as this
                // indicates we are looking for neighbors outside the mapped area
                try {
                    // don't add the cell as its own neighbor
                    if (!(a == 1 && b == 1))
                    {
                        // don't add water cells or neighbors either
                        Cell n = myMap[row-2+a][col-2+b];
                        if ((n.getGeology() != Geology.WATER) &&
                                (!neighbors.contains(n)))
                            farNeighbors.add(n);
                    }
                }
                catch (Exception e)
                {}
            }
        }
        cell.setFarNeighbors(farNeighbors);
    }

    public void validateWorld()
    {
        /*
         * start with cells and cities since that is where we're seeing problems...
         * iterate through all the players
         * 	iterate thru each player's cities
         * 		iterate thru each city's cells
         * 			make sure each cell has the city registered....
         */
        for (Player p : myPlayers)
        {
            for (City city : p.getCities())
                for(Cell c : city)
                    if (!city.equals(c.getCity()))
                        System.out.println("out of synch");
        }

    } // end validate world

    public Occupiers getMovingPiece()
    {
        return movingPiece;
    }
} // end world


