/*
 * This file is part of Mapyrus, software for plotting maps.
 * Copyright (C) 2003 Simon Chenery.
 *
 * Mapyrus is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Mapyrus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Mapyrus; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

/*
 * @(#) $Id$
 */
package org.mapyrus.geom;

import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;

import org.mapyrus.Constants;

/**
 * Implements the well-known Sutherland-Hodgman algorithm for clipping
 * a line or polygon against a rectangle by clipping against each of
 * the four edges of the rectangle in turn.
 */
public class SutherlandHodgman
{
	/*
	 * Directions for clipping lines.
	 */
	private static final int HORIZONTAL = 0;
	private static final int VERTICAL = 1;

	/**
	 * Clip a shape against a rectangle
	 * @param s shape to clip
	 * @param rect rectangle to clip against.
	 * @param flatness flattening factor for curves.
	 * @return clipped shape.
	 */
	public static GeneralPath clip(GeneralPath s, Rectangle2D.Double rect, double flatness)
	{
		int clipDirection;
		boolean isPositiveInside;
		float v;
		int segmentType;
		float xStart = 0.0f, yStart = 0.0f;
		float xEnd = 0.0f, yEnd = 0.0f;
		float xInt, yInt;
		float xMoveTo = 0.0f, yMoveTo = 0.0f;

		/*
		 * Clip against each of the four edges of the rectangle
		 * in turn. 
		 */
		float []coords = new float[6];
		GeneralPath s1 = s;
		GeneralPath s2 = new GeneralPath();
		GeneralPath s3 = new GeneralPath();

		for (int i = 0; i < 4; i++)
		{
			/*
			 * Set clipping boundary for this iteration.
			 */
			if (i == 0)
			{
				clipDirection = VERTICAL;
				v = (float)rect.getMinX();
				isPositiveInside = true;
			}
			else if (i == 1)
			{
				clipDirection = VERTICAL;
				v = (float)rect.getMaxX();
				isPositiveInside = false;
				
				/*
				 * Exchange paths so the path we just created on
				 * first iteration is read on second iteration,
				 * writing into a new path.
				 */
				s1 = s2;
				s2 = s3;
			}
			else if (i == 2)
			{
				clipDirection = HORIZONTAL;
				v = (float)rect.getMinY();
				isPositiveInside = true;

				GeneralPath swapper = s1;
				s1 = s2;
				s2 = swapper;
			}
			else
			{
				clipDirection = HORIZONTAL;
				v = (float)rect.getMaxY();
				isPositiveInside = false;
				
				GeneralPath swapper = s1;
				s1 = s2;
				s2 = swapper;
			}

			PathIterator pi = s1.getPathIterator(Constants.IDENTITY_MATRIX, flatness);
			s2.reset();
			boolean isFirstSegment = true;

			/*
			 * Walk through path, clipping line segments against a boundary.
			 */
			while (!pi.isDone())
			{
				segmentType = pi.currentSegment(coords);
				if (segmentType == PathIterator.SEG_MOVETO)
				{
					xStart = xMoveTo = coords[0];
					yStart = yMoveTo = coords[1];
					isFirstSegment = true;
				}
				else
				{
					if (segmentType == PathIterator.SEG_CLOSE)
					{
						xEnd = xMoveTo;
						yEnd = yMoveTo;
					}
					else
					{
						xEnd = coords[0];
						yEnd = coords[1];
					}

					if (clipDirection == HORIZONTAL)
					{
						/*
						 * If segment is wholly inside boundary line then accept it.
						 */
						if ((isPositiveInside && yStart >= v && yEnd >= v) ||
							((!isPositiveInside) && yStart <= v && yEnd <= v))
						{
							if (isFirstSegment)
								s2.moveTo(xStart, yStart);
							isFirstSegment = false;
							s2.lineTo(xEnd, yEnd);
						}
						else if ((isPositiveInside && yStart >= v) ||
							((!isPositiveInside) && yStart <= v))
						{
							/*
							 * Segment starts inside boundary and ends outside.
							 * Add segment from start to boundary.
							 */
							xInt = xStart + (xEnd - xStart) * (v - yStart) / (yEnd - yStart); 
							yInt = v;
							
							if (isFirstSegment)
								s2.moveTo(xStart, yStart);
							isFirstSegment = false;
							s2.lineTo(xInt, yInt);
						}
						else if ((isPositiveInside && yEnd >= v) ||
							((!isPositiveInside) && yEnd <= v))
						{
							/*
							 * Segment starts outside boundary and ends inside.
							 * Add segment from boundary to end.
							 */
							xInt = xStart + (xEnd - xStart) * (yStart - v) / (yStart - yEnd); 
							yInt = v;

							if (isFirstSegment)
								s2.moveTo(xInt, yInt);
							else
								s2.lineTo(xInt, yInt);
							isFirstSegment = false;
							s2.lineTo(xEnd, yEnd);
						}
					}
					else /* VERTICAL */
					{
						/*
						 * If segment is wholly inside boundary line then accept it.
						 */
						if ((isPositiveInside && xStart >= v && xEnd >= v) ||
							((!isPositiveInside) && xStart <= v && xEnd <= v))
						{
							if (isFirstSegment)
								s2.moveTo(xStart, yStart);
							isFirstSegment = false;
							s2.lineTo(xEnd, yEnd);
						}
						else if ((isPositiveInside && xStart >= v) ||
							((!isPositiveInside) && xStart <= v))
						{
							/*
							 * Segment starts inside boundary and ends outside.
							 * Add segment from start to boundary.
							 */
							xInt = v;
							yInt = yStart + (yEnd - yStart) * (v - xStart) / (xEnd - xStart); 
							
							if (isFirstSegment)
								s2.moveTo(xStart, yStart);
							isFirstSegment = false;
							s2.lineTo(xInt, yInt);
						}
						else if ((isPositiveInside && xEnd >= v) ||
							((!isPositiveInside) && xEnd <= v))
						{
							/*
							 * Segment starts outside boundary and ends inside.
							 * Add segment from boundary to end.
							 */
							xInt = v;
							yInt = yStart + (yEnd - yStart) * (xStart - v) / (xStart - xEnd);

							if (isFirstSegment)
								s2.moveTo(xInt, yInt);
							else
								s2.lineTo(xInt, yInt);
							isFirstSegment = false;
							s2.lineTo(xEnd, yEnd);
						}
					}

					xStart = xEnd;
					yStart = yEnd;
				}
				pi.next();
			}
		}
		return(s2);
	}
}
