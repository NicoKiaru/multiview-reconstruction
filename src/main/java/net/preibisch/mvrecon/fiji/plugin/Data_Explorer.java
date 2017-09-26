package net.preibisch.mvrecon.fiji.plugin;

import java.awt.Button;
import java.awt.TextField;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import net.preibisch.mvrecon.fiji.plugin.queryXML.GenericLoadParseQueryXML;
import net.preibisch.mvrecon.fiji.plugin.queryXML.LoadParseQueryXML;
import net.preibisch.mvrecon.fiji.spimdata.SpimData2;
import net.preibisch.mvrecon.fiji.spimdata.XmlIoSpimData2;
import net.preibisch.mvrecon.fiji.spimdata.explorer.SimpleInfoBox;
import net.preibisch.mvrecon.fiji.spimdata.explorer.ViewSetupExplorer;

import ij.ImageJ;
import ij.plugin.PlugIn;

public class Data_Explorer implements PlugIn
{
	public static boolean showNote = true;

	@Override
	public void run( String arg )
	{
		if ( showNote )
		{
			showNote();
			showNote = false;
		}

		final LoadParseQueryXML result = new LoadParseQueryXML();

		result.addButton( "Define a new dataset", new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				((TextField)result.getGenericDialog().getStringFields().firstElement()).setText( "define" );
				Button ok = result.getGenericDialog().getButtons()[ 0 ];

				ActionEvent ae =  new ActionEvent( ok, ActionEvent.ACTION_PERFORMED, "");
				Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(ae);
			}
		});

		if ( !result.queryXML( "XML Explorer", "", false, false, false, false, false ) )
			return;

		final SpimData2 data = result.getData();
		final String xml = result.getXMLFileName();
		final XmlIoSpimData2 io = result.getIO();

		final ViewSetupExplorer< SpimData2, XmlIoSpimData2 > explorer = new ViewSetupExplorer<SpimData2, XmlIoSpimData2 >( data, xml, io );

		explorer.getFrame().toFront();
	}

	public static SimpleInfoBox showNote()
	{
		String text = "Welcome to the Multiview Reconstruction Software!\n\n";

		text += "Here are a few tipps & tricks that hopefully get you started. The first thing you should do is to\n";
		text += "have a look at the online documentation, which is growing (http://fiji.sc/Multiview-Reconstruction).\n\n";

		text += "For newcomers, the basic steps you need to do are the following:\n";
		text += "1) Define a new dataset in the open dialog, which will create the XML and open an explorer window\n";
		text += "2) Select one of the views and make sure it displays right in ImageJ\n";
		text += "3) Consider converting your dataset to HDF5, as it makes it possible to use the BigDataViewer\n" + 
				"to browse the entire dataset interactively\n";
		text += "4) Detect interest points in your views (could be beads, nuclei, ...)\n";
		text += "5) Register your data using those interest points (rotation invariant)\n";
		text += "6) Fuse or deconvolve the dataset\n";
		text += "\n";

		text += "Please note that the outlined steps above should work out of the box it you have fluoresecent beads\n";
		text += "sourrounding your sample. If you want to use sample features like nuclei, you need to apply approximate\n";
		text += "transformations first (known rotation axis & angles) and register using translation-invariant matching.\n";
		text += "\n";
		text += "Tipp: If you get too many detections inside the sample and you just want to find beads, you can remove\n";
		text += "them based on their distance to each other (Remove Interest Points > By Distance ...) - remove all that\n";
		text += "too close to each other (e.g. less than 5 pixels)\n";

		return new SimpleInfoBox( "Getting started", text );
	}

	public static void main( String[] args )
	{
		new ImageJ();

		if ( !System.getProperty("os.name").toLowerCase().contains( "mac" ) )
			GenericLoadParseQueryXML.defaultXMLfilename = "/home/preibisch/Documents/Microscopy/SPIM/HisYFP-SPIM//dataset_tp18.xml";
		else
			GenericLoadParseQueryXML.defaultXMLfilename = "/Volumes/SSD1/His-YFP Drosophila/dataset.xml";//"/Users/spreibi/Documents/Microscopy/SPIM/HisYFP-SPIM//dataset.xml";

		new Data_Explorer().run( null );
	}
}