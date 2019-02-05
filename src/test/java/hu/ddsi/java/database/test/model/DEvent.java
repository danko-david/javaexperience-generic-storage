package hu.ddsi.java.database.test.model;

import java.util.Date;

import hu.ddsi.java.database.GsdbModel;

public class DEvent extends GsdbModel
{
	public DEventHeader owner;
	public DType type;
	public Date at = new Date();
}