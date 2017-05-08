package spim.fiji.plugin;

import ij.ImageJ;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.ViewId;
import mpicbg.spim.io.IOFunctions;
import spim.fiji.plugin.interestpointdetection.DifferenceOfGaussianGUI;
import spim.fiji.plugin.interestpointdetection.DifferenceOfMeanGUI;
import spim.fiji.plugin.interestpointdetection.InterestPointDetectionGUI;
import spim.fiji.plugin.queryXML.LoadParseQueryXML;
import spim.fiji.plugin.util.GUIHelper;
import spim.fiji.spimdata.SpimData2;
import spim.fiji.spimdata.imgloaders.AbstractImgLoader;
import spim.fiji.spimdata.interestpoints.InterestPoint;
import spim.process.interestpointdetection.InterestPointTools;

/**
 * Plugin to detect interest points, store them on disk, and link them into the XML
 * 
 * Different plugins to detect interest points are supported, needs to implement the
 * {@link InterestPointDetectionGUI} interface
 * 
 * @author Stephan Preibisch (stephan.preibisch@gmx.de)
 *
 */
public class Interest_Point_Detection implements PlugIn
{
	public static ArrayList< InterestPointDetectionGUI > staticAlgorithms = new ArrayList< InterestPointDetectionGUI >();
	public static int defaultAlgorithm = 1;
	public static boolean defaultDefineAnisotropy = false;
	public static boolean defaultSetMinMax = false;
	public static boolean defaultLimitDetections = false;
	public static String defaultLabel = "beads";
	
	static
	{
		IOFunctions.printIJLog = true;
		staticAlgorithms.add( new DifferenceOfMeanGUI( null, null ) );
		staticAlgorithms.add( new DifferenceOfGaussianGUI( null, null ) );
	}
	
	@Override
	public void run( final String arg )
	{
		// ask for everything but the channels
		final LoadParseQueryXML result = new LoadParseQueryXML();
		
		if ( !result.queryXML( "perfoming interest point detection", true, true, true, true ) )
			return;

		detectInterestPoints(
				result.getData(),
				SpimData2.getAllViewIdsSorted( result.getData(), result.getViewSetupsToProcess(), result.getTimePointsToProcess() ),
				result.getClusterExtension(),
				result.getXMLFileName(),
				true );
	}

	/**
	 * Does just the detection, no saving
	 * 
	 * @param data
	 * @param viewIds
	 * @return
	 */
	public boolean detectInterestPoints(
			final SpimData2 data,
			final List< ViewId > viewIds )
	{
		return detectInterestPoints( data, viewIds, "", null, false );
	}

	public boolean detectInterestPoints(
			final SpimData2 data,
			final List< ViewId > viewIds,
			final String xmlFileName,
			final boolean saveXML )
	{
		return detectInterestPoints( data, viewIds, "", xmlFileName, saveXML );
	}

	public boolean detectInterestPoints(
			final SpimData2 data,
			final List< ViewId > viewIds,
			final String clusterExtension,
			final String xmlFileName,
			final boolean saveXML )
	{
		// filter not present ViewIds
		final List< ViewId > removed = SpimData2.filterMissingViews( data, viewIds );
		IOFunctions.println( new Date( System.currentTimeMillis() ) + ": Removed " +  removed.size() + " views because they are not present." );

		// the GenericDialog needs a list[] of String
		final String[] descriptions = new String[ staticAlgorithms.size() ];
		
		for ( int i = 0; i < staticAlgorithms.size(); ++i )
			descriptions[ i ] = staticAlgorithms.get( i ).getDescription();
		
		if ( defaultAlgorithm >= descriptions.length )
			defaultAlgorithm = 0;
		
		final GenericDialog gd = new GenericDialog( "Detect Interest Points" );
		
		gd.addChoice( "Type_of_interest_point_detection", descriptions, descriptions[ defaultAlgorithm ] );
		gd.addStringField( "Label_interest_points", defaultLabel );

		gd.addCheckbox( "Define_anisotropy for segmentation", defaultDefineAnisotropy );
		gd.addCheckbox( "Set_minimal_and_maximal_intensity", defaultSetMinMax );
		gd.addCheckbox( "Limit_amount_of_detections" , defaultLimitDetections );
		
		gd.addMessage( "" );
		GUIHelper.addWebsite( gd );
		
		gd.showDialog();

		if ( gd.wasCanceled() )
			return false;

		final int algorithm = defaultAlgorithm = gd.getNextChoiceIndex();

		// how are the detections called (e.g. beads, nuclei, ...)
		final String label = defaultLabel = gd.getNextString();
		final boolean defineAnisotropy = defaultDefineAnisotropy = gd.getNextBoolean();
		final boolean setMinMax = defaultSetMinMax = gd.getNextBoolean();
		final boolean limitDetections = defaultLimitDetections = gd.getNextBoolean();

		final InterestPointDetectionGUI ipd = staticAlgorithms.get( algorithm ).newInstance(
				data,
				viewIds );

		// the interest point detection should query its parameters
		if ( !ipd.queryParameters( defineAnisotropy, setMinMax, limitDetections ) )
			return false;
		
		// now extract all the detections
		for ( final TimePoint tp : SpimData2.getAllTimePointsSorted( data, viewIds ) )
		{
			final HashMap< ViewId, List< InterestPoint > > points = ipd.findInterestPoints( tp );

			InterestPointTools.addInterestPoints( data, label, points, ipd.getParameters() );

			// update metadata if necessary
			if ( data.getSequenceDescription().getImgLoader() instanceof AbstractImgLoader )
			{
				IOFunctions.println( "(" + new Date( System.currentTimeMillis() ) + "): Updating metadata ... " );
				try
				{
					( (AbstractImgLoader)data.getSequenceDescription().getImgLoader() ).updateXMLMetaData( data, false );
				}
				catch( Exception e )
				{
					IOFunctions.println( "Failed to update metadata, this should not happen: " + e );
				}
			}

			// save the xml
			if ( saveXML )
				SpimData2.saveXML( data, xmlFileName, clusterExtension );
		}

		return true;
	}

	public static void main( final String[] args )
	{
		LoadParseQueryXML.defaultXMLfilename = "/Users/spreibi/Documents/Microscopy/SPIM/HisYFP-SPIM/dataset.xml";

		new ImageJ();
		new Interest_Point_Detection().run( null );
	}
}
