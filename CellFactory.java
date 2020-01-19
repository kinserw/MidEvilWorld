package com.kinser.midevilworld;

abstract public class CellFactory {

	protected static CellFactory ourCellFactory;
	
	public static CellFactory getCellFactory()
	{ 
		return ourCellFactory;
	
	}
	
	public CellFactory() 
	{
		ourCellFactory = this;
	}

	public abstract Cell createCell(int row, int col, Geology geo);
	public abstract Cell createCell(int row, int col, Geology geo, Occupiers piece);
}
/*
 * 
 *rather than creating a factory for cells, occupiers and geology... i should 
 *move all the image loading and storage out of this package and into the UI classes.
 *just add comments to the world classes that they need images for each enum value.
 *
 *area (in Cell and City) is also only used for UI tasks. this could also be pulled out 
 *to the UI classes since all the info needed to build the areas is available through
 *class interfaces now.
 *
 *with these two approaches, there would be no need to factory classes or additional sub 
 *classes 

*/