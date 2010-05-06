/*
 * Copyright (c) 1998 - 2010. University Corporation for Atmospheric Research/Unidata
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

package thredds.server.wms;

import org.joda.time.DateTime;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import uk.ac.rdg.resc.ncwms.cdm.AbstractScalarLayerBuilder;
import uk.ac.rdg.resc.ncwms.cdm.CdmUtils;
import uk.ac.rdg.resc.ncwms.cdm.DataReadingStrategy;
import uk.ac.rdg.resc.ncwms.cdm.LayerBuilder;
import uk.ac.rdg.resc.ncwms.exceptions.LayerNotDefinedException;
import uk.ac.rdg.resc.ncwms.util.WmsUtils;
import uk.ac.rdg.resc.ncwms.wms.Dataset;
import uk.ac.rdg.resc.ncwms.wms.Layer;
import uk.ac.rdg.resc.ncwms.wms.VectorLayer;

import java.io.IOException;
import java.util.*;

/**
 * A {@link uk.ac.rdg.resc.ncwms.wms.Dataset} that provides access to layers read from {@link ucar.nc2.dataset.NetcdfDataset}
 * objects.
 *
 * @author Jon
 */
class ThreddsDataset implements Dataset
{

  private final String id;
  private final String title;
  private final Map<String, ThreddsLayer> scalarLayers = new LinkedHashMap<String, ThreddsLayer>();
  private final Map<String, VectorLayer> vectorLayers = new LinkedHashMap<String, VectorLayer>();

  /**
   * LayerBuilder used to create ThreddsLayers in CdmUtils.findAndUpdateLayers
   */
  private static final LayerBuilder<ThreddsLayer> THREDDS_LAYER_BUILDER = new AbstractScalarLayerBuilder<ThreddsLayer>()
  {
    @Override
    public ThreddsLayer newLayer( String id )
    {
      return new ThreddsLayer( id );
    }

    @Override
    public void setTimeValues( ThreddsLayer layer, List<DateTime> times )
    {
      layer.setTimeValues( times );
    }

    @Override
    public void setGridDatatype( ThreddsLayer layer, GridDatatype grid )
    {
      layer.setGridDatatype( grid );
    }
  };

  /**
   * Creates a new ThreddsDataset with the given id from the given NetcdfDataset
   *
   * @throws java.io.IOException if there was an i/o error extracting a GridDataset
   *                             from the given NetcdfDataset
   */
  public ThreddsDataset( String id, GridDataset gridDataset ) throws IOException
  {
    this.id = id;
    this.title = gridDataset.getTitle();
    
    NetcdfDataset ncDataset = (NetcdfDataset) gridDataset.getNetcdfFile();

    // Get the most appropriate data-reading strategy for this dataset
    DataReadingStrategy drStrategy = CdmUtils.getOptimumDataReadingStrategy( ncDataset );

    // Find out if the NetcdfDataset has deferred the application of
    // scale-offset-missing (meaning that we have to apply these attributes
    // when the data are read)
    boolean scaleMissingDeferred = CdmUtils.isScaleMissingDeferred( ncDataset );

    // Now load the scalar layers
    //GridDataset gd = CdmUtils.getGridDataset(ncDataset);
    CdmUtils.findAndUpdateLayers( gridDataset, THREDDS_LAYER_BUILDER, this.scalarLayers );
    // Set the dataset property of each layer
    for ( ThreddsLayer layer : this.scalarLayers.values() )
    {
      layer.setDataReadingStrategy( drStrategy );
      layer.setScaleMissingDeferred( scaleMissingDeferred );
      layer.setDataset( this );
    }

    // Find the vector quantities
    Collection<VectorLayer> vectorLayersColl = WmsUtils.findVectorLayers( this.scalarLayers.values() );
    // Add the vector quantities to the map of layers
    for ( VectorLayer vecLayer : vectorLayersColl )
    {
      this.vectorLayers.put( vecLayer.getId(), vecLayer );
    }
  }

  /**
   * Returns the ID of this dataset, unique on the server.
   */
  @Override
  public String getId()
  {
    return this.id;
  }

  @Override
  public String getTitle()
  {
    return this.title;
  }

  /**
   * Returns the current time, since datasets could change at any time without
   * our knowledge.
   *
   * @see ThreddsServerConfig#getLastUpdateTime()
   */
  @Override
  public DateTime getLastUpdateTime()
  {
    return new DateTime();
  }

  /**
   * Gets the {@link uk.ac.rdg.resc.ncwms.wms.Layer} with the given {@link uk.ac.rdg.resc.ncwms.wms.Layer#getId() id}.  The id
   * is unique within the dataset, not necessarily on the whole server.
   *
   * @return The layer with the given id, or null if there is no layer with
   *         the given id.
   * @todo repetitive of code in ncwms.config.Dataset: any way to refactor?
   */
  @Override
  public Layer getLayerById( String layerId ) throws LayerNotDefinedException
  {
    Layer layer = this.scalarLayers.get( layerId );
    if ( layer == null )
      layer = this.vectorLayers.get( layerId );
    if ( layer == null )
      throw new LayerNotDefinedException( layerId );

    return layer;
  }

  /**
   * @todo repetitive of code in ncwms.config.Dataset: any way to refactor?
   */
  @Override
  public Set<Layer> getLayers()
  {
    Set<Layer> layerSet = new LinkedHashSet<Layer>();
    layerSet.addAll( this.scalarLayers.values() );
    layerSet.addAll( this.vectorLayers.values() );
    return layerSet;
  }

  /**
   * Returns an empty string
   */
  @Override
  public String getCopyrightStatement()
  {
    return "";
  }

  /**
   * Returns an empty string
   */
  @Override
  public String getMoreInfoUrl()
  {
    return "";
  }

  @Override
  public boolean isReady()
  {
    return true;
  }

  @Override
  public boolean isLoading()
  {
    return false;
  }

  @Override
  public boolean isError()
  {
    return false;
  }

  @Override
  public Exception getException()
  {
    return null;
  }

  @Override
  public boolean isDisabled()
  {
    return false;
  }

}