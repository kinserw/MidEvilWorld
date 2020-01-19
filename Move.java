package com.kinser.midevilworld;

public class Move
{
    private Cell myFrom = null;
    private Cell myTo = null;
    private int myWeight = 0;
    private String myDescription = new String("?");

    public Move()
    {
    }

    public Move(Move m)
    {
        myFrom = m.myFrom;
        myTo = m.myTo;
        myWeight = m.myWeight;
        myDescription = m.myDescription;
    }

    public Move(Cell from, Cell to, int weight)
    {
        myFrom = from;
        myTo = to;
        myWeight = weight;
    }

    public Move(Cell from, Cell to, int weight, String desc)
    {
        myFrom = from;
        myTo = to;
        myWeight = weight;
        myDescription = desc;
    }

    public Cell getTo()
    {
        return myTo;
    }

    public void setTo(Cell c)
    {
        myTo = c;
    }

    public Cell getFrom()
    {
        return myFrom;
    }

    public void setFrom(Cell c)
    {
        myFrom = c;
    }

    public int getWeight()
    {
        return myWeight;
    }

    public void setWeight(int w)
    {
        myWeight = w;
    }

    public String getDescription()
    {
        return myDescription;
    }

    public void setDescription(String desc)
    {
        myDescription = desc;
    }

    public void addWeight()
    {
        myWeight++;
    }

    public void loseWeight()
    {
        myWeight--;
    }
    public boolean equals(Object obj)
    {
        if (obj == null)
            return false;
        else if (obj.getClass() != this.getClass())
            return false;
        Move move = (Move)obj;
        return this.myTo == move.myTo && this.myFrom == move.myFrom;
    }

} // end class Move