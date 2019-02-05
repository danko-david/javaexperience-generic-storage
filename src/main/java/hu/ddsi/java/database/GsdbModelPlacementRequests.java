package hu.ddsi.java.database;

import java.util.ArrayList;

import eu.javaexperience.interfaces.simple.publish.SimplePublish1;

public class GsdbModelPlacementRequests
{
	public final Class<? extends GenericStorable> cls;
	public final long id;
	public final ArrayList<SimplePublish1<GenericStorable>> placers = new ArrayList<>();
	
	public GsdbModelPlacementRequests(Class cls, long id)
	{
		this.cls = cls;
		this.id = id;
	}
}
