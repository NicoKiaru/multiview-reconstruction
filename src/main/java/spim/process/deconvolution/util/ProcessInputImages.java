package spim.process.deconvolution.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import bdv.util.ConstantRandomAccessible;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicImgLoader;
import mpicbg.spim.data.generic.sequence.BasicViewDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.sequence.ViewId;
import mpicbg.spim.io.IOFunctions;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import spim.fiji.spimdata.SpimData2;
import spim.process.deconvolution.MultiViewDeconvolution;
import spim.process.deconvolution.normalization.NormalizingRandomAccessibleInterval;
import spim.process.fusion.FusionTools;
import spim.process.fusion.transformed.FusedRandomAccessibleInterval;
import spim.process.fusion.transformed.TransformView;
import spim.process.fusion.transformed.TransformVirtual;
import spim.process.fusion.transformed.TransformWeight;
import spim.process.fusion.transformed.weightcombination.CombineWeightsRandomAccessibleInterval;
import spim.process.fusion.transformed.weightcombination.CombineWeightsRandomAccessibleInterval.CombineType;
import spim.process.interestpointdetection.methods.downsampling.DownsampleTools;
import spim.process.interestpointregistration.pairwise.constellation.grouping.Group;

public class ProcessInputImages< V extends ViewId >
{
	final AbstractSpimData< ? extends AbstractSequenceDescription< ? extends BasicViewSetup, ? extends BasicViewDescription< ? >, ? extends BasicImgLoader > > spimData;
	final ArrayList< Group< V > > groups;
	final Interval bb;
	Interval downsampledBB;
	final double downsampling;
	final boolean useWeightsFusion, useWeightsDecon;
	final float blendingRangeFusion, blendingBorderFusion, blendingRangeDeconvolution, blendingBorderDeconvolution;

	final HashMap< Group< V >, RandomAccessibleInterval< FloatType > > images;
	final HashMap< Group< V >, RandomAccessibleInterval< FloatType > > unnormalizedWeights, normalizedWeights;
	final HashMap< V, AffineTransform3D > models;

	public ProcessInputImages(
			final AbstractSpimData< ? extends AbstractSequenceDescription< ? extends BasicViewSetup, ? extends BasicViewDescription< ? >, ? extends BasicImgLoader > > spimData,
			final Collection< Group< V > > groups,
			final Interval bb,
			final double downsampling,
			final boolean useWeightsFusion,
			final float blendingRangeFusion,
			final float blendingBorderFusion,
			final boolean useWeightsDecon,
			final float blendingRangeDeconvolution,
			final float blendingBorderDeconvolution )
	{
		this.spimData = spimData;
		this.groups = SpimData2.filterGroupsForMissingViews( spimData, groups );
		this.bb = bb;
		this.downsampling = downsampling;
		this.useWeightsFusion = useWeightsFusion;
		this.blendingRangeFusion = blendingRangeFusion;
		this.blendingBorderFusion = blendingBorderFusion;
		this.useWeightsDecon = useWeightsDecon;
		this.blendingRangeDeconvolution = blendingRangeDeconvolution;
		this.blendingBorderDeconvolution = blendingBorderDeconvolution;

		this.images = new HashMap<>();
		this.unnormalizedWeights = new HashMap<>();
		this.normalizedWeights = new HashMap<>();
		this.models = new HashMap<>();

		IOFunctions.println( "(" + new Date(System.currentTimeMillis()) + "): Remaining groups: " + this.groups.size() );
	}

	public ProcessInputImages(
			final AbstractSpimData< ? extends AbstractSequenceDescription< ? extends BasicViewSetup, ? extends BasicViewDescription< ? >, ? extends BasicImgLoader > > spimData,
			final Collection< Group< V > > groups,
			final Interval bb,
			final double downsampling )
	{
		this(
				spimData, groups, bb, Double.NaN,
				true, FusionTools.defaultBlendingRange, FusionTools.defaultBlendingBorder,
				true, MultiViewDeconvolution.defaultBlendingRange, MultiViewDeconvolution.defaultBlendingBorder );
	}

	public ProcessInputImages(
			final AbstractSpimData< ? extends AbstractSequenceDescription< ? extends BasicViewSetup, ? extends BasicViewDescription< ? >, ? extends BasicImgLoader > > spimData,
			final Collection< Group< V > > groups,
			final Interval bb )
	{
		this( spimData, groups, bb, Double.NaN );
	}

	public Collection< Group< V > > getGroups() { return groups; }
	public Interval getBoundingBox() { return bb; }
	public Interval getDownsampledBoundingBox() { return downsampledBB; }
	public HashMap< V, AffineTransform3D > getDownsampledModels() { return models; }
	public HashMap< Group< V >, RandomAccessibleInterval< FloatType > > getImages() { return images; }
	public HashMap< Group< V >, RandomAccessibleInterval< FloatType > > getNormalizedWeights() { return normalizedWeights; }
	public HashMap< Group< V >, RandomAccessibleInterval< FloatType > > getUnnormalizedWeights() { return unnormalizedWeights; }

	public void fuseGroups()
	{
		this.downsampledBB = fuseGroups(
				spimData,
				images,
				unnormalizedWeights,
				models,
				groups,
				bb,
				downsampling,
				useWeightsFusion ? Util.getArrayFromValue( blendingRangeFusion, 3 ) : null,
				useWeightsFusion ? Util.getArrayFromValue( blendingBorderFusion, 3 ) : null,
				useWeightsDecon ? Util.getArrayFromValue( blendingRangeDeconvolution, 3 ) : null,
				useWeightsDecon ? Util.getArrayFromValue( blendingBorderDeconvolution, 3 ) : null );
	}

	public void cacheImages( final int cellDim, final int maxCacheSize ) { cacheRandomAccessibleInterval( groups, cellDim, maxCacheSize, images ); }
	public void cacheImages() { cacheImages( MultiViewDeconvolution.cellDim, MultiViewDeconvolution.maxCacheSize ); }

	public void copyImages( final ImgFactory< FloatType > imgFactory ) { copyRandomAccessibleInterval( groups, imgFactory, images ); }
	public void copyImages() { copyImages( new CellImgFactory<>( MultiViewDeconvolution.cellDim ) ); }

	public void cacheUnnormalizedWeights( final int cellDim, final int maxCacheSize ) { cacheRandomAccessibleInterval( groups, cellDim, maxCacheSize, unnormalizedWeights ); }
	public void cacheUnnormalizedWeights() { cacheUnnormalizedWeights( MultiViewDeconvolution.cellDim, MultiViewDeconvolution.maxCacheSize ); }

	public void copyUnnormalizedWeights( final ImgFactory< FloatType > imgFactory ) { copyRandomAccessibleInterval( groups, imgFactory, unnormalizedWeights ); }
	public void copyUnnormalizedWeights() { copyUnnormalizedWeights( new CellImgFactory<>( MultiViewDeconvolution.cellDim ) ); }

	public void cacheNormalizedWeights( final int cellDim, final int maxCacheSize ) { cacheRandomAccessibleInterval( groups, cellDim, maxCacheSize, normalizedWeights ); }
	public void cacheNormalizedWeights() { cacheNormalizedWeights( MultiViewDeconvolution.cellDim, MultiViewDeconvolution.maxCacheSize ); }

	public void copyNormalizedWeights( final ImgFactory< FloatType > imgFactory ) { copyRandomAccessibleInterval( groups, imgFactory, normalizedWeights ); }
	public void copyNormalizedWeights() { copyNormalizedWeights( new CellImgFactory<>( MultiViewDeconvolution.cellDim ) ); }

	public void normalizeWeights() { normalizeWeights( 1.0 ); }
	public void normalizeWeights( final double osemspeedup )
	{
		normalizeWeights(
				osemspeedup,
				MultiViewDeconvolution.additionalSmoothBlending,
				MultiViewDeconvolution.maxDiffRange,
				MultiViewDeconvolution.scalingRange );
	}

	public void normalizeWeights(
			final double osemspeedup,
			final boolean additionalSmoothBlending,
			final float maxDiffRange,
			final float scalingRange )
	{
		normalizedWeights.putAll(
			normalizeWeights(
				unnormalizedWeights,
				groups,
				osemspeedup,
				additionalSmoothBlending,
				maxDiffRange,
				scalingRange ) );
	}

	public static < V extends ViewId > HashMap< Group< V >, RandomAccessibleInterval< FloatType > > normalizeWeights(
			final HashMap< Group< V >, RandomAccessibleInterval< FloatType > > unnormalizedWeights,
			final List< Group< V > > groups,
			final double osemspeedup,
			final boolean additionalSmoothBlending,
			final float maxDiffRange,
			final float scalingRange )
	{
		final HashMap< Group< V >, RandomAccessibleInterval< FloatType > > normalizedWeights = new HashMap<>();
		final ArrayList< RandomAccessibleInterval< FloatType > > originalWeights = new ArrayList<>();

		// ordered as in the group list
		for ( int i = 0; i < groups.size(); ++i )
			originalWeights.add( unnormalizedWeights.get( groups.get( i ) ) );

		// normalizes the weights (sum == 1) and applies osem-speedup if wanted
		for ( int i = 0; i < groups.size(); ++i )
		{
			final Group< V > group = groups.get( i );

			normalizedWeights.put( group,
				new NormalizingRandomAccessibleInterval< FloatType >(
					unnormalizedWeights.get( group ),
					i,
					originalWeights,
					osemspeedup,
					additionalSmoothBlending,
					maxDiffRange,
					scalingRange,
					new FloatType() ) );
		}

		return normalizedWeights;
	}

	public static < V extends ViewId > void cacheRandomAccessibleInterval(
			final Collection< Group< V > > groups,
			final int cellDim,
			final int maxCacheSize,
			final HashMap< Group< V >, RandomAccessibleInterval< FloatType > > images )
	{
		for ( final Group< V > group : groups )
		{
			if ( !images.containsKey( group ) )
				continue;

			images.put( group, FusionTools.cacheRandomAccessibleInterval( images.get( group ), maxCacheSize, new FloatType(), cellDim ) );
		}
	}

	public static < V extends ViewId > void copyRandomAccessibleInterval(
			final Collection< Group< V > > groups,
			final ImgFactory< FloatType > factory,
			final HashMap< Group< V >, RandomAccessibleInterval< FloatType > > images )
	{
		for ( final Group< V > group : groups )
		{
			if ( !images.containsKey( group ) )
				continue;

			images.put( group, FusionTools.copyImg( images.get( group ), factory, new FloatType() ) );
		}
	}

	public static < V extends ViewId > Interval fuseGroups(
			final AbstractSpimData< ? extends AbstractSequenceDescription< ? extends BasicViewSetup, ? extends BasicViewDescription< ? >, ? extends BasicImgLoader > > spimData,
			final HashMap< Group< V >, RandomAccessibleInterval< FloatType > > tImgs,
			final HashMap< Group< V >, RandomAccessibleInterval< FloatType > > tWeights,
			final HashMap< V, AffineTransform3D > models,
			final Collection< Group< V > > groups,
			final Interval boundingBox,
			final double downsampling,
			final float[] blendingRangeFusion,
			final float[] blendingBorderFusion,
			final float[] blendingRangeDecon,
			final float[] blendingBorderDecon )
	{
		int i = 0;

		// scale the bounding box if necessary
		final Interval bb;
		if ( !Double.isNaN( downsampling ) )
			bb = TransformVirtual.scaleBoundingBox( boundingBox, 1.0 / downsampling );
		else
			bb = boundingBox;

		final long[] dim = new long[ bb.numDimensions() ];
		bb.dimensions( dim );

		for ( final Group< V > group : groups )
		{
			IOFunctions.println( "(" + new Date(System.currentTimeMillis()) + "): Transforming group " + (++i) + " of " + groups.size() + " (group=" + group + ")" );

			final ArrayList< RandomAccessibleInterval< FloatType > > images = new ArrayList<>();
			final ArrayList< RandomAccessibleInterval< FloatType > > weightsFusion = new ArrayList<>();
			final ArrayList< RandomAccessibleInterval< FloatType > > weightsDecon = new ArrayList<>();

			for ( final V viewId : group.getViews() )
			{
				final BasicImgLoader imgloader = spimData.getSequenceDescription().getImgLoader();
				final ViewRegistration vr = spimData.getViewRegistrations().getViewRegistration( viewId );
				vr.updateModel();
				AffineTransform3D model = vr.getModel();

				// adjust the model for downsampling
				if ( !Double.isNaN( downsampling ) )
				{
					model = model.copy();
					TransformVirtual.scaleTransform( model, 1.0 / downsampling );
				}

				// we need to add a copy here since below the model might be modified for downsampled opening
				models.put( viewId, model.copy() );

				// this modifies the model so it maps from a smaller image to the global coordinate space,
				// which applies for the image itself as well as the weights since they also use the smaller
				// input image as reference
				final RandomAccessibleInterval inputImg = DownsampleTools.openDownsampled( imgloader, viewId, model );
				images.add( TransformView.transformView( inputImg, model, bb, MultiViewDeconvolution.minValueImg, MultiViewDeconvolution.outsideValueImg, 1 ) );

				if ( blendingRangeFusion != null && blendingBorderFusion != null )
				{
					// we need a different blending when virtually fusing the images since a negative
					// value would actually lead to artifacts there
					final float[] rangeFusion = blendingRangeFusion.clone();
					final float[] borderFusion = blendingBorderFusion.clone();

					// adjust both for z-scaling (anisotropy)
					FusionTools.adjustBlending( spimData.getSequenceDescription().getViewDescriptions().get( viewId ), rangeFusion, borderFusion );

					weightsFusion.add( TransformWeight.transformBlending( inputImg, borderFusion, rangeFusion, model, bb ) );
				}
				else
				{
					weightsFusion.add( Views.interval( new ConstantRandomAccessible< FloatType >( new FloatType( 1 ), bb.numDimensions() ), new FinalInterval( dim ) ) );
				}

				if ( blendingRangeDecon != null && blendingBorderDecon != null )
				{
					// however, to then run the deconvolution with this data, we want negative values
					// to maximize the usage of image data
					final float[] rangeDecon = blendingRangeDecon.clone();
					final float[] borderDecon = blendingBorderDecon.clone();

					// adjust both for z-scaling (anisotropy)
					FusionTools.adjustBlending( spimData.getSequenceDescription().getViewDescriptions().get( viewId ), rangeDecon, borderDecon );

					weightsDecon.add( TransformWeight.transformBlending( inputImg, borderDecon, rangeDecon, model, bb ) );
				}
				else
				{
					weightsDecon.add( Views.interval( new ConstantRandomAccessible< FloatType >( new FloatType( 1 ), bb.numDimensions() ), new FinalInterval( dim ) ) );
				}
			}

			// the fused image per group
			final RandomAccessibleInterval< FloatType > img = new FusedRandomAccessibleInterval( new FinalInterval( dim ), images, weightsFusion );

			// the weights used for deconvolution per group
			final RandomAccessibleInterval< FloatType > weight = new CombineWeightsRandomAccessibleInterval( new FinalInterval( dim ), weightsDecon, CombineType.SUM );

			tImgs.put( group, img );
			tWeights.put( group, weight );
		}

		return bb;
	}
}
