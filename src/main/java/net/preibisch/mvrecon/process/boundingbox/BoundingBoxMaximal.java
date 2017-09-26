package net.preibisch.mvrecon.process.boundingbox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewDescription;
import mpicbg.spim.data.sequence.ImgLoader;
import mpicbg.spim.data.sequence.ViewId;
import mpicbg.spim.io.IOFunctions;
import net.imglib2.Dimensions;
import net.imglib2.FinalRealInterval;
import net.imglib2.realtransform.AffineTransform3D;
import net.preibisch.mvrecon.fiji.spimdata.SpimData2;
import net.preibisch.mvrecon.fiji.spimdata.ViewSetupUtils;
import net.preibisch.mvrecon.fiji.spimdata.boundingbox.BoundingBox;
import net.preibisch.mvrecon.process.interestpointregistration.pairwise.constellation.grouping.Group;

public class BoundingBoxMaximal implements BoundingBoxEstimation
{
	final Collection< ViewId > views;
	final HashMap< ViewId, Dimensions > dimensions;
	final HashMap< ViewId, AffineTransform3D > registrations;

	public BoundingBoxMaximal(
			final Collection< ? extends ViewId > views,
			final AbstractSpimData< ? extends AbstractSequenceDescription<?,? extends BasicViewDescription< ? >, ? extends ImgLoader > > data )
	{
		this.views = new ArrayList<>();
		this.dimensions = new HashMap<>();
		this.registrations = new HashMap<>();

		this.views.addAll( views );
		SpimData2.filterMissingViews( data, this.views );

		for ( final ViewId viewId : this.views )
		{
			final BasicViewDescription< ? > vd = data.getSequenceDescription().getViewDescriptions().get( viewId );
			dimensions.put( viewId, ViewSetupUtils.getSizeOrLoad( vd.getViewSetup(), vd.getTimePoint(), data.getSequenceDescription().getImgLoader() ) );

			data.getViewRegistrations().getViewRegistration( vd ).updateModel();
			registrations.put( viewId, data.getViewRegistrations().getViewRegistration( vd ).getModel() );
		}
	}

	public BoundingBoxMaximal(
			final Collection< ? extends ViewId > views,
			final HashMap< ? extends ViewId, Dimensions > dimensions,
			final HashMap< ? extends ViewId, AffineTransform3D > registrations )
	{
		this.dimensions = new HashMap<>();
		this.dimensions.putAll( dimensions );

		this.registrations = new HashMap<>();
		this.registrations.putAll( registrations );

		this.views = new ArrayList<>();
		this.views.addAll( views );
	}

	@Override
	public BoundingBox estimate( final String title )
	{
		final double[] minBB = new double[ 3 ];
		final double[] maxBB = new double[ 3 ];

		for ( int d = 0; d < minBB.length; ++d )
		{
			minBB[ d ] = Double.MAX_VALUE;
			maxBB[ d ] = -Double.MAX_VALUE;
		}

		computeMaxBoundingBoxDimensions( views, dimensions, registrations, minBB, maxBB );

		final BoundingBox maxsized = new BoundingBox( title, approximateLowerBound( minBB ), approximateUpperBound( maxBB ) );

		return maxsized;
	}

	public static int[] approximateLowerBound( final double[] min )
	{
		final int[] lowerBound = new int[ min.length ];

		for ( int d = 0; d < min.length; ++d )
			lowerBound[ d ] = (int)Math.round( Math.floor( min[ d ] ) );

		return lowerBound;
	}

	public static int[] approximateUpperBound( final double[] max )
	{
		final int[] upperBound = new int[ max.length ];

		for ( int d = 0; d < max.length; ++d )
			upperBound[ d ] = (int)Math.round( Math.ceil( max[ d ] ) );

		return upperBound;
	}

	/**
	 * @param viewIds - view ids to process
	 * @param dimensions - map vid to dimensions
	 * @param registrations - map vid to registrations
	 * @param minBB - lower bounds to fill
	 * @param maxBB - upper bounds to fill
	 */
	public static void computeMaxBoundingBoxDimensions(
			final Collection< ViewId > viewIds,
			final HashMap< ViewId, Dimensions > dimensions,
			final HashMap< ViewId, AffineTransform3D > registrations,
			final double[] minBB, final double[] maxBB )
	{
		for ( int d = 0; d < minBB.length; ++d )
		{
			minBB[ d ] = Double.MAX_VALUE;
			maxBB[ d ] = -Double.MAX_VALUE;
		}

		for ( final ViewId viewId : viewIds )
		{
			if ( !dimensions.containsKey( viewId ) )
			{
				IOFunctions.println( "ERROR: ViewID"  + Group.pvid( viewId ) + " is not present in dimensions for MaximumBoundingBox.computeMaxBoundingBoxDimensions()" );
				continue;
			}

			if ( !registrations.containsKey( viewId ) )
			{
				IOFunctions.println( "ERROR: ViewID"  + Group.pvid( viewId ) + " is not present in registrations for MaximumBoundingBox.computeMaxBoundingBoxDimensions()" );
				continue;
			}

			final Dimensions size = dimensions.get( viewId );
			final double[] min = new double[]{ 0, 0, 0 };
			final double[] max = new double[]{
					size.dimension( 0 ) - 1,
					size.dimension( 1 ) - 1,
					size.dimension( 2 ) - 1 };

			final FinalRealInterval interval = registrations.get( viewId ).estimateBounds( new FinalRealInterval( min, max ) );
			
			for ( int d = 0; d < minBB.length; ++d )
			{
				minBB[ d ] = Math.min( minBB[ d ], interval.realMin( d ) );
				maxBB[ d ] = Math.max( maxBB[ d ], interval.realMax( d ) );
			}
		}
	}
}