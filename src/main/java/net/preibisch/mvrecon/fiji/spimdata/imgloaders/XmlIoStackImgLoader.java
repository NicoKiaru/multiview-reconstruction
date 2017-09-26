package net.preibisch.mvrecon.fiji.spimdata.imgloaders;

import static mpicbg.spim.data.XmlHelpers.loadPath;
import static mpicbg.spim.data.XmlKeys.IMGLOADER_FORMAT_ATTRIBUTE_NAME;

import java.io.File;

import mpicbg.spim.data.XmlHelpers;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.ImgLoaderIo;
import mpicbg.spim.data.generic.sequence.XmlIoBasicImgLoader;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.img.planar.PlanarImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.real.FloatType;

import org.jdom2.Element;

public abstract class XmlIoStackImgLoader< T extends StackImgLoader< ? > > implements XmlIoBasicImgLoader< T >
{
	public static final String DIRECTORY_TAG = "imagedirectory";
	public static final String FILE_PATTERN_TAG = "filePattern";
	public static final String IMGLIB2CONTAINER_PATTERN_TAG = "imglib2container";

	public static final String LAYOUT_TP_TAG = "layoutTimepoints";
	public static final String LAYOUT_CHANNEL_TAG = "layoutChannels";
	public static final String LAYOUT_ILLUMINATION_TAG = "layoutIlluminations";
	public static final String LAYOUT_ANGLE_TAG = "layoutAngles";
	public static final String LAYOUT_TILE_TAG = "layoutTiles";

	@Override
	public Element toXml( final T imgLoader, final File basePath )
	{
		final Element elem = new Element( "ImageLoader" );
		elem.setAttribute( IMGLOADER_FORMAT_ATTRIBUTE_NAME, this.getClass().getAnnotation( ImgLoaderIo.class ).format() );
		elem.addContent( XmlHelpers.pathElement( DIRECTORY_TAG, imgLoader.getPath(), basePath ) );

		elem.addContent( XmlHelpers.textElement( FILE_PATTERN_TAG, imgLoader.getFileNamePattern() ) );
		elem.addContent( XmlHelpers.intElement( LAYOUT_TP_TAG, imgLoader.getLayoutTimePoints() ) );
		elem.addContent( XmlHelpers.intElement( LAYOUT_CHANNEL_TAG, imgLoader.getLayoutChannels() ) );
		elem.addContent( XmlHelpers.intElement( LAYOUT_ILLUMINATION_TAG, imgLoader.getLayoutIlluminations() ) );
		elem.addContent( XmlHelpers.intElement( LAYOUT_ANGLE_TAG, imgLoader.getLayoutAngles() ) );
		elem.addContent( XmlHelpers.intElement( LAYOUT_TILE_TAG, imgLoader.getLayoutTiles() ) );
		elem.addContent( XmlHelpers.textElement( IMGLIB2CONTAINER_PATTERN_TAG, imgLoader.getImgFactory().getClass().getSimpleName() ) );
		
		return elem;
	}

	@Override
	public T fromXml( final Element elem, final File basePath, final AbstractSequenceDescription< ?, ?, ? > sequenceDescription )
	{
		try
		{
			File path = loadPath( elem, DIRECTORY_TAG, basePath );
			String fileNamePattern = XmlHelpers.getText( elem, FILE_PATTERN_TAG );

			int layoutTP = XmlHelpers.getInt( elem, LAYOUT_TP_TAG );
			int layoutChannels = XmlHelpers.getInt( elem, LAYOUT_CHANNEL_TAG );
			int layoutIllum = XmlHelpers.getInt( elem, LAYOUT_ILLUMINATION_TAG );
			int layoutAngles = XmlHelpers.getInt( elem, LAYOUT_ANGLE_TAG );
			int layoutTiles = 0;

			try { layoutTiles = XmlHelpers.getInt( elem, LAYOUT_TILE_TAG ); } catch (Exception e) {}

			final String container = XmlHelpers.getText( elem, IMGLIB2CONTAINER_PATTERN_TAG );
			ImgFactory< FloatType > imgFactory;
			if ( container == null )
			{
				System.out.println( "WARNING: No Img implementation defined, using CellImg." );

				// if no factory is defined we define an CellImgFactory
				imgFactory = new CellImgFactory< FloatType >( 256 );
			}
			else
			{
				if ( container.toLowerCase().contains( "cellimg" ) )
				{
					imgFactory = new CellImgFactory< FloatType >( 256 );
				}
				else if ( container.toLowerCase().contains( "arrayimg" ) )
				{
					imgFactory = new ArrayImgFactory< FloatType >();
				}
				else if ( container.toLowerCase().contains( "planarimg" ) )
				{
					imgFactory = new PlanarImgFactory< FloatType >();
				}
				else
				{
					// if factory is unknown we define an ArrayImgFactory
					imgFactory = new ArrayImgFactory< FloatType >();
					
					System.out.println( "WARNING: Unknown Img implementation '" + container + "', using ArrayImg." );
				}
			}
			
			return createImgLoader( path, fileNamePattern, imgFactory, layoutTP, layoutChannels, layoutIllum, layoutAngles, layoutTiles, sequenceDescription );
		}
		catch ( final Exception e )
		{
			e.printStackTrace();
			throw new RuntimeException( e );
		}
	}

	protected abstract T createImgLoader(
			final File path, final String fileNamePattern, final ImgFactory< ? extends NativeType< ? > > imgFactory,
			final int layoutTP, final int layoutChannels, final int layoutIllum, final int layoutAngles, final int layoutTiles,
			final AbstractSequenceDescription< ?, ?, ? > sequenceDescription );
}