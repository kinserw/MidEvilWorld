package com.kinser.midevilworld;

import java.util.ArrayList;
import java.util.Random;

public class ComputerPlayer extends Player
{
    /**
     *
     */
    private static final long serialVersionUID = 6797042090274204705L;
    Random myNumGen = new Random();
    private int myDifficulty = 0;

    public ComputerPlayer(int difficulty)
    {
        myDifficulty = difficulty;
    }


    public boolean takeTurn()
    {
        int count = 0;
        resetAttackingPieces();

        // get rid of any of my cities that are too small to exist
        boolean keepGoing = myCities.size() > 0;
        while (keepGoing)
        {
            City city = myCities.get(count);
            if (city.size() <2)
            {
                if (city.size() != 0)
                {
                    cityLostCell(city, city.get(0));
                }

            }
            else
                count +=1;

            keepGoing =  (myCities.size() > count);

        }

        combineAdjacentCities();

        // computer player difficulty rating is considered..
        //     easy 	if they only take defensive moves
        //     medium 	if they random switch between defense and offense
        //     hard		if they only take offensive moves
        switch (myDifficulty)
        {
            case 3 :
                takeDefensiveTurn();
                break;
            case 8 :
            case 10 :
                takeOffensiveTurn();
                break;
            default :
                int randomNum = myNumGen.nextInt(100);
                if (randomNum > 50)
                    takeOffensiveTurn();
                else
                    takeDefensiveTurn();
                break;
        }

        combineAdjacentCities();

        return true;
    }

    private void takeOffensiveTurn()
    {
        // TODO loop through each city (offensive approach)
        for (City city : myCities)
        {
            city.processGold();

            // take all moveable pieces off their cells for redistribution
            ArrayList<Occupiers> availableArmy = city.makeArmyAvailable();

            // get list of neighbors (any cell nearby not in the same city)
            ArrayList<Cell> enemies = city.getEnemyNeighbors();

            // find general direction of nearest allied city, if any
            City nearestAlly = findNearestAlly(city);
            Cell myVillage = city.getCurrentVillage();
            Cell allyVillage = nearestAlly.getCurrentVillage();
            double direction = 0.0;
            if (!(myVillage == null || allyVillage == null))
                direction = Math.atan2(allyVillage.getCol() - myVillage.getCol(), allyVillage.getRow() - myVillage.getRow());


            ArrayList<Move> possibleMoves = new ArrayList<Move>();

            // loop through enemy neighbors
            // if neighbor is a village of city with 2 cells, highest weight
            // if neighbor is a village, high weight
            // if neighbor is part of a city and in direction of nearest ally, medium-high weight
            // if neighbor in direction of nearest ally, medium weight
            // All remaining neighbors are priorities weakest to strongest
            while (enemies.size() > 0)
            {
                Cell target = enemies.remove(0);

                double cellDir = Math.atan2(target.getCol() - myVillage.getCol(),
                        target.getRow() - myVillage.getRow());
                boolean inDirection = (Math.abs(cellDir-direction) < (Math.PI / 100) ? true : false);

                // if target cell belongs to another player, weigh move accordingly
                // Note: getEnemyNeighbors already excluded cities for the
                // attacking player
                if (target.getCity() != null)
                {
                    // when difficulty set to really hard, add more weight if this
                    // move is against a non-computer player
                    int weight = 0;
                    if ((myDifficulty == 10) &&
                            !target.getCity().getPlayer().getClass().equals(this.getClass()))
                        weight = 2;

                    // if taking target will eliminate a city...
                    if (target.getCity().size() == 2)
                    {
                        possibleMoves.add(new Move(null, target, 10+weight, "Offensive: take 2 cell city")); // highest weight
                    }
                    // if I can take all the gold from an enemy city,...
                    else if (target.getOccupiers() == Occupiers.VILLAGE)
                    {
                        possibleMoves.add(new Move(null, target, 8+weight, "Offensive: take village"));
                    }
                    // enemy cell in a direction of my nearest ally
                    else if (inDirection)
                    {
                        possibleMoves.add(new Move(null, target, 6+weight, "Offensive: city cell in direction"));
                    }
                    // taking enemy cell is better than taking an empty cell
                    else
                    {
                        possibleMoves.add(new Move(null, target, 4+weight, "Offensive: city cell"));
                    }

                } // end target is part of a city
                else // target is not occupied so it has lowest defense
                {
                    if (inDirection)
                    {
                        possibleMoves.add(new Move(null, target, 2, "Offensive: in direction"));
                    }
                    else
                    {
                        possibleMoves.add(new Move(null, target, 1, "Offensive: generic"));
                    }

                }
            }// keep going through all enemies


            // need to sort possible moves, highly weighted first, 2nd criteria is weakest first.
            boolean movesSorted = false;
            while (!movesSorted)
            {
                movesSorted = true;
                for (int i = 1; i < possibleMoves.size(); i++ ) // won't enter loop if size < 2
                {
                    if (possibleMoves.get(i).getWeight() >
                            possibleMoves.get(i-1).getWeight())
                    {
                        // swap positions
                        Move temp = possibleMoves.get(i);
                        possibleMoves.set(i,possibleMoves.get(i-1));
                        possibleMoves.set(i-1,temp);
                        movesSorted = false; // keep sorting until no swaps occur
                    }
                    else if ((possibleMoves.get(i).getWeight() ==
                            possibleMoves.get(i-1).getWeight()) &&
                            (possibleMoves.get(i).getTo().getOccupiers().getValue() <
                                    possibleMoves.get(i-1).getTo().getOccupiers().getValue()))
                    {
                        // swap positions
                        Move temp = possibleMoves.get(i);
                        possibleMoves.set(i,possibleMoves.get(i-1));
                        possibleMoves.set(i-1,temp);
                        movesSorted = false; // keep sorting until no swaps occur
                    }

                }
            } // sort moves
            // make as many moves as possible with available army pieces, buy where needed
            // if possible
            while (possibleMoves.size() > 0)
            {
                Move makeMove = possibleMoves.remove(0);
                boolean moveMade = false;

                // find the weakest army piece available to take this cell
                for (Occupiers piece : Occupiers.moveablePieces)
                {
                    if (makeMove.getTo().getDefense() < piece.getValue())
                    {
                        // if this piece is in availableArmy use that, else buy it if able
                        if (availableArmy.contains(piece))
                        {
                            moveMade = true;
                            availableArmy.remove(piece);
                            cityGainsCell(city, makeMove.getTo(), piece);
                        }
                        else if (piece.getCost() < (city.currentGoldValue()))
                        {
                            // this call buys the piece needed and takes target cell
                            moveMade = cityGainsCell(city, makeMove.getTo());
                        }

                        if (moveMade)
                            break;// stop looping through army pieces
                    } // if piece can take enemy cell
                } // find the weakest army piece available to take this cell

                // if no piece found, couldn't buy it, etc., then just drop this move on the floor
            } // loop through possible moves


            // distribute any remaining army pieces throughout the city without priority
            boolean moreCells = true;
            while (availableArmy.size() > 0 && moreCells)
            {
                for(Cell c : city)
                {
                    if (c.getOccupiers() == Occupiers.NONE)
                    {
                        c.setOccupiers(availableArmy.get(0));
                        availableArmy.remove(0);
                        // if out of army pieces, end city loop
                        if (availableArmy.size() == 0)
                            break;
                    }
                }
                moreCells = false;
            }

        } // loop through cities

    }

    private void takeDefensiveTurn()
    {

        Random numGen = new Random();
        for (City city : myCities)
        {
            city.processGold();

            // take all moveable pieces off their cells for redistribution
            ArrayList<Occupiers> availableArmy = city.makeArmyAvailable();

            // get list of cells that have 5 or more allied neighbors
            ArrayList<Cell> centerCells = new ArrayList<Cell>();
            // processing for castles if I can afford at least 1
            if (city.currentGoldValue() > Occupiers.CASTLE.getValue())
            {
                for (Cell cell : city)
                {
                    ArrayList<Cell> allies = cell.getCityNeighbors();

                    // if several allies nearby and doesn't already have a castle
                    // and isn't guarded by a castle nearby
                    if ((allies.size() >= 4) && (cell.getDefense() < Occupiers.CASTLE.getValue()))
                    {
                        // add this as a center city but order them lowest defense first
                        int i = 0;
                        for (i = 0; i < centerCells.size(); i++)
                        {
                            if (cell.getDefense() < centerCells.get(i).getDefense())
                                break;
                        }
                        centerCells.add(i, cell);
                    } // is it a center city
                } // find all the center cities

                // now, add castles to the lowest defended cities at random until
                // out of money, or run out of center cities to defend (since
                // we only want to defend the weakest, we only pull from the
                // first 75% of the list which means, we'll always have at least 1 left)
                while ( city.currentGoldValue() > Occupiers.CASTLE.getCost()
                        && centerCells.size() > 1)
                {
                    int i = numGen.nextInt((int)(centerCells.size()*0.75));
                    city.buyAnArmyPieceForCity(Occupiers.CASTLE, centerCells.get(i));
                    centerCells.remove(i);
                }

            } // end processing for castles

            // expanding definition of centerCity so forget any left from
            // castle processing
            centerCells = new ArrayList<Cell>();

            // no need to adjust consumption #s because castles don't cost anything
            // to maintain

            // find general direction of nearest allied city, if any
            City nearestAlly = findNearestAlly(city);
            Cell myVillage = city.getCurrentVillage();
            Cell allyVillage = nearestAlly.getCurrentVillage();
            double direction = 0.0;
            if (!(myVillage == null || allyVillage == null))
                direction = Math.atan2(allyVillage.getCol() - myVillage.getCol(), allyVillage.getRow() - myVillage.getRow());

            ArrayList<Move> possibleMoves = new ArrayList<Move>();

            // if city is getting big, try to fill in the middle and near
            // the village (thus creating more center cells, i.e. thicker defenses)
            if (city.size() > 5)
            {
                for (Cell cell : city)
                {
                    // if this cell is in the general direction of my nearest ally,
                    // then give more weight to possible moves near this cell
                    double cellDir = 1.0;
                    if (myVillage != null)
                        cellDir = Math.atan2(cell.getCol() - myVillage.getCol(), cell.getRow() - myVillage.getRow());
                    boolean inDirection = (Math.abs(cellDir-direction) < (Math.PI / 100) ? true : false);

                    // build on cells with more than 3 allies or is a village
                    if ((cell.getCityNeighbors().size() > 3) ||
                            (cell.getOccupiers() == Occupiers.VILLAGE))
                    {
                        for (Cell c : cell.getEnemyNeighbors())
                        {
                            // give more weight to less defense
                            int weight = (c.getDefense() > Occupiers.PEON.getValue() ? 1 : 2);
                            if (inDirection)
                                weight++;
                            possibleMoves.add(new Move(cell, c, weight, "Defensive: bulk up"));
                        }
                    }
                    else if (inDirection)
                    {
                        for (Cell c : cell.getEnemyNeighbors())
                        {
                            double cDir = 1.0;
                            if ( myVillage != null)
                                cDir = Math.atan2(c.getCol() - myVillage.getCol(), c.getRow() - myVillage.getRow());
                            if (Math.abs(cDir-direction) < (Math.PI / 100) ? true : false)
                                possibleMoves.add(new Move(cell, c, 1, "Defensive: in direction"));

                        }
                    } // cell is in direction of nearest ally
                } // loop through cells in city
            } // city is getting larger

            // need to sort possible moves, least defended first. second sort criteria is weight.
            boolean movesSorted = false;
            while (!movesSorted)
            {
                movesSorted = true;
                for (int i = 1; i < possibleMoves.size(); i++ ) // won't enter loop if size < 2
                {
                    if (possibleMoves.get(i).getTo().getDefense() <
                            possibleMoves.get(i-1).getTo().getDefense())
                    {
                        // swap positions
                        Move temp = possibleMoves.get(i);
                        possibleMoves.set(i,possibleMoves.get(i-1));
                        possibleMoves.set(i-1,temp);
                        movesSorted = false; // keep sorting until no swaps occur
                    }
                    else if ((possibleMoves.get(i).getTo().getDefense() ==
                            possibleMoves.get(i-1).getTo().getDefense()) &&
                            (possibleMoves.get(i).getWeight() <
                                    possibleMoves.get(i-1).getWeight()))
                    {
                        // swap positions
                        Move temp = possibleMoves.get(i);
                        possibleMoves.set(i,possibleMoves.get(i-1));
                        possibleMoves.set(i-1,temp);
                        movesSorted = false; // keep sorting until no swaps occur
                    }

                }
            } // sort moves

            // make as many moves as possible with available army pieces, or buy it if able
            while (possibleMoves.size() > 0)
            {
                Move makeMove = possibleMoves.remove(0);
                boolean moveMade = false;

                // find the weakest army piece available to take this cell
                for (Occupiers piece : Occupiers.moveablePieces)
                {
                    if (makeMove.getTo().getDefense() < piece.getValue())
                    {
                        if (availableArmy.contains(piece))
                        {
                            moveMade = true;
                            availableArmy.remove(piece);
                            cityGainsCell(city, makeMove.getTo(), piece);
                        }
                        // if move not made, and we have more than enough gold, buy the piece that
                        // is needed to make the move
                        else if (piece.getCost() < (city.currentGoldValue()/2))
                        {
                            moveMade = cityGainsCell(city, makeMove.getTo());
                        }
                    }
                    if (moveMade)
                        break; // stop looping through army pieces
                } // find the weakest army piece available to take this cell

            } // loop through possible moves, do the ones I can in the order they appear


            // distribute any remaining army pieces throughout the city without priority
            boolean moreCells = true;
            while (availableArmy.size() > 0 && moreCells)
            {
                for(Cell c : city)
                {
                    if (c.getOccupiers() == Occupiers.NONE)
                    {
                        c.setOccupiers(availableArmy.get(0));
                        availableArmy.remove(0);
                        // if out of army pieces, end city loop
                        if (availableArmy.size() == 0)
                            break;
                    }
                }
                moreCells = false;
            }

            // if enough gold left to buy peons and enough being generated to support peons
            // then distribute them to the cells with the greatest number of enemy neighbors
            while (moreCells && city.currentGoldValue() > Occupiers.PEON.getCost()*2)
            {
                for(Cell c : city)
                {
                    if ((c.getOccupiers() == Occupiers.NONE) &&
                            (c.getEnemyNeighbors().size() > 3))
                    {
                        city.buyAnArmyPieceForCity(Occupiers.PEON, c);
                    }
                }
                moreCells = false;

            }

        } // for each city


    }




}

