package org.mapyrus.function;

import java.util.ArrayList;

import org.mapyrus.Argument;
import org.mapyrus.ContextStack;
import org.mapyrus.MapyrusException;
import org.mapyrus.MapyrusMessages;

/**
 * Function returning a a GeoJSON format feature.
 * For example, geojson(geom) = {"type": "Feature", "geometry":
 * {"type": "Point", "coordinates": [100, 0]}, "properties": {}}
 */
public class Geojson implements Function
{
	@Override
	public Argument evaluate(ContextStack context, ArrayList<Argument> args)
		throws MapyrusException
	{
		Argument geometry;
		Argument identifier;
		Argument properties;

		geometry = args.get(0);

		if (args.size() >= 2)
		{
			properties = args.get(1);
			if (properties.getType() != Argument.HASHMAP)
			{
				throw new MapyrusException(MapyrusMessages.get(MapyrusMessages.INVALID_ARRAY));
			}
		}
		else
		{
			properties = new Argument();			
		}

		if (args.size() >= 3)
			identifier = args.get(2);
		else
			identifier = Argument.emptyString;

		String geojson = geometry.getGeoJSONValue();
		String props = properties.toString();
		String id = identifier.toString();
		if (properties.getHashMapSize() == 0)
			props = "null";
		StringBuffer feature = new StringBuffer();
		feature.append("{\"type\": \"Feature\", \"geometry\": ");
		feature.append(geojson);
		feature.append(", \"properties\": ");
		feature.append(props);
		if (id.length() > 0)
			feature.append(", \"id\": \"").append(id).append("\"");
		feature.append("}");
		return(new Argument(Argument.STRING, feature.toString()));
	}

	@Override
	public int getMaxArgumentCount()
	{
		return(3);
	}

	@Override
	public int getMinArgumentCount()
	{
		return(1);
	}

	@Override
	public String getName()
	{
		return("geojson");
	}
}
