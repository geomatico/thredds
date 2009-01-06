/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 * 
 * Portions of this software were developed by the Unidata Program at the 
 * University Corporation for Atmospheric Research.
 * 
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 * 
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.ft.point.standard;

import ucar.nc2.ft.point.OneNestedPointCollectionImpl;
import ucar.nc2.ft.point.TrajectoryFeatureImpl;
import ucar.nc2.ft.TrajectoryFeatureCollection;
import ucar.nc2.ft.PointFeatureCollectionIterator;
import ucar.nc2.ft.TrajectoryFeature;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.units.DateUnit;
import ucar.ma2.StructureDataIterator;

import java.io.IOException;

/**
 * @author caron
 * @since Dec 31, 2008
 */
public class StandardTrajectoryCollectionImpl extends OneNestedPointCollectionImpl implements TrajectoryFeatureCollection {
  private DateUnit timeUnit;
  private FlattenedTable ft;

  StandardTrajectoryCollectionImpl(FlattenedTable ft, DateUnit timeUnit) {
    super(ft.getName(), FeatureType.TRAJECTORY);
    this.ft = ft;
    this.timeUnit = timeUnit;
  }

  public PointFeatureCollectionIterator getPointFeatureCollectionIterator(int bufferSize) throws IOException {
    return null; // ft.getTrajectoryObsDataIterator(null, bufferSize);
  }

  public boolean hasNext() throws IOException {
    return false;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public TrajectoryFeature next() throws IOException {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public void resetIteration() throws IOException {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  /* private class StandardTrajectoryFeature extends TrajectoryFeatureImpl {

    StandardTrajectoryFeature(String name) {
      super(name, -1);
    }

    public PointFeatureIterator getPointFeatureIterator(int bufferSize) throws IOException {
      StructureDataIterator siter = ft.getTrajectoryObsDataIterator( sdata, bufferSize);
      return new StandardPointFeatureIterator(ft, timeUnit, siter, sdataList, false);
    }
  } */
}