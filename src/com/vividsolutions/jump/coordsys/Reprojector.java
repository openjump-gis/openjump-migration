/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * Copyright (C) 2003 Vivid Solutions
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * For more information, contact:
 *
 * Vivid Solutions
 * Suite #1A
 * 2328 Government Street
 * Victoria BC  V8T 5G5
 * Canada
 *
 * (250)385-6040
 * www.vividsolutions.com
 */

package com.vividsolutions.jump.coordsys;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateFilter;
import org.locationtech.jts.geom.Geometry;


/**
 * The source and destination coordinate reference systems must have
 * the same datum (for example, WGS 84).
 * 
 * @deprecated
 */
@Deprecated
public class Reprojector {
    // [12/2017 ede] reprojection is not properly implemented as of right now
    // therefore use a dummy reprojector that does nothing
    private static Reprojector instance = new Reprojector(){

      @Override
      public boolean wouldChangeValues(CoordinateSystem source, CoordinateSystem destination) {
        return false;
      }

      @Override
      public void reproject(Coordinate coordinate, CoordinateSystem source, CoordinateSystem destination) {
      }

      @Override
      public void reproject(Geometry geometry, CoordinateSystem source, CoordinateSystem destination) {
      }
      
    };

    private Reprojector() {
    }

    public static Reprojector instance() {
        return instance;
    }

    public boolean wouldChangeValues(CoordinateSystem source,
        CoordinateSystem destination) {
        if (source == CoordinateSystem.UNSPECIFIED) {
            return false;
        }

        if (destination == CoordinateSystem.UNSPECIFIED) {
            return false;
        }

        if (source == destination) {
            return false;
        }

        return true;
    }

    public void reproject(Coordinate coordinate, CoordinateSystem source,
        CoordinateSystem destination) {
        if (!wouldChangeValues(source, destination)) {
            return;
        }

        Planar result = destination.getProjection().asPlanar(source.getProjection()
                                                                   .asGeographic(new Planar(
                        coordinate.x, coordinate.y), new Geographic()),
                new Planar());
        coordinate.x = result.x;
        coordinate.y = result.y;
    }

    public void reproject(Geometry geometry, final CoordinateSystem source,
        final CoordinateSystem destination) {
        if (!wouldChangeValues(source, destination)) {
            return;
        }

        geometry.apply(new CoordinateFilter() {
                public void filter(Coordinate coord) {
                    reproject(coord, source, destination);
                }
            });
        geometry.setSRID(destination.getEPSGCode());
        geometry.geometryChanged();
    }
}
