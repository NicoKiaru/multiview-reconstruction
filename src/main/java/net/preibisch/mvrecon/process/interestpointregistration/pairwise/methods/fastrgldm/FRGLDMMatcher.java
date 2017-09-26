package net.preibisch.mvrecon.process.interestpointregistration.pairwise.methods.fastrgldm;

import java.util.ArrayList;
import java.util.HashSet;

import mpicbg.pointdescriptor.TranslationInvariantLocalCoordinateSystemPointDescriptor;
import mpicbg.pointdescriptor.exception.NoSuitablePointsException;
import mpicbg.pointdescriptor.matcher.SubsetMatcher;
import mpicbg.spim.mpicbg.PointMatchGeneric;
import net.imglib2.KDTree;
import net.imglib2.neighborsearch.KNearestNeighborSearchOnKDTree;
import net.imglib2.util.Pair;
import net.imglib2.util.Util;
import net.imglib2.util.ValuePair;
import net.preibisch.mvrecon.fiji.spimdata.interestpoints.InterestPoint;

public class FRGLDMMatcher< I extends InterestPoint >
{
	public ArrayList< PointMatchGeneric< I > > extractCorrespondenceCandidates( 
			final ArrayList< I > nodeListA,
			final ArrayList< I > nodeListB,
			final int redundancy,
			final double ratioOfDistance )
	{
		final KDTree< I > tree1 = new KDTree<>( nodeListA, nodeListA );
		final KDTree< I > tree2 = new KDTree<>( nodeListB, nodeListB );

		final ArrayList< TranslationInvariantLocalCoordinateSystemPointDescriptor< I > > descriptors1 =
			createLocalCoordinateSystemPointDescriptors( tree1, nodeListA, redundancy );
		
		final ArrayList< TranslationInvariantLocalCoordinateSystemPointDescriptor< I > > descriptors2 =
			createLocalCoordinateSystemPointDescriptors( tree2, nodeListB, redundancy );
		
		// create lookup tree for descriptors2
		final KDTree< TranslationInvariantLocalCoordinateSystemPointDescriptor< I > > lookUpTree2 = new KDTree<>( descriptors2, descriptors2 );
		final KNearestNeighborSearchOnKDTree< TranslationInvariantLocalCoordinateSystemPointDescriptor< I > > nnsearch = new KNearestNeighborSearchOnKDTree<>( lookUpTree2, 2 );

		// store the candidates for corresponding beads
		final ArrayList< PointMatchGeneric< I > > correspondences = new ArrayList<>();
		
		/* compute matching */
		computeMatching( descriptors1, nnsearch, correspondences, ratioOfDistance );
		
		return correspondences;
	}
	
	protected void computeMatching(
			final ArrayList< TranslationInvariantLocalCoordinateSystemPointDescriptor< I > > descriptors1,
			final KNearestNeighborSearchOnKDTree< TranslationInvariantLocalCoordinateSystemPointDescriptor< I > > nnsearch2,
			final ArrayList< PointMatchGeneric< I > > correspondences,
			final double ratioOfDistance )
	{
		final HashSet< Pair< I, I > > pairs = new HashSet<>();

		int count = 0;
		
		for ( final TranslationInvariantLocalCoordinateSystemPointDescriptor< I > descriptorA : descriptors1 )
		{
			nnsearch2.search( descriptorA );

			double best = descriptorA.descriptorDistance( nnsearch2.getSampler( 0 ).get() );
			double secondBest = descriptorA.descriptorDistance( nnsearch2.getSampler( 1 ).get() );

			if ( best * ratioOfDistance <= secondBest )
			{
				final I detectionA = descriptorA.getBasisPoint();
				final I detectionB = nnsearch2.getSampler( 0 ).get().getBasisPoint();

				// twice the same pair could potentially show up due to redundancy
				pairs.add( new ValuePair<>( detectionA, detectionB ) );
				++count;
			}
		}

		for ( final Pair< I, I > pair : pairs )
			correspondences.add( new PointMatchGeneric< I >( pair.getA(), pair.getB(), 1 ) );
		
		System.out.println( count +  " <> " + correspondences.size() );
	}

	public static < I extends InterestPoint > ArrayList< TranslationInvariantLocalCoordinateSystemPointDescriptor< I > > createLocalCoordinateSystemPointDescriptors( 
			final KDTree< I > tree,
			final ArrayList< I > basisPoints,
			final int redundancy )
	{
		final int[][] neighborIndicies = SubsetMatcher.computePD( 2 + redundancy, 2, 1 );

		final KNearestNeighborSearchOnKDTree< I > nnsearch = new KNearestNeighborSearchOnKDTree<>( tree, 2 + redundancy + 1 );
		final ArrayList< TranslationInvariantLocalCoordinateSystemPointDescriptor< I > > descriptors = new ArrayList<> ( );
		
		for ( final I p : basisPoints )
		{
			nnsearch.search( p );

			for ( final int[] neighbors : neighborIndicies )
			{
				final I point1 = nnsearch.getSampler( neighbors[ 0 ] ).get();
				final I point2 = nnsearch.getSampler( neighbors[ 1 ] ).get();

				try
				{
					descriptors.add( new TranslationInvariantLocalCoordinateSystemPointDescriptor< I >( p, point1, point2 ) );
				}
				catch ( NoSuitablePointsException e )
				{
					e.printStackTrace();
				}
			}
		}

		return descriptors;
	}

	public static void main( String[] args )
	{
		final int numNeighbors = 2;
		final int redundancy = 1;

		final int[][] neighborIndicies = SubsetMatcher.computePD( numNeighbors + redundancy, numNeighbors, 1 );

		for ( final int[] neighbors : neighborIndicies )
			System.out.println( Util.printCoordinates( neighbors ) );
	}
}