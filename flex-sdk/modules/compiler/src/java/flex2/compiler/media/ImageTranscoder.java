////////////////////////////////////////////////////////////////////////////////
//
//  ADOBE SYSTEMS INCORPORATED
//  Copyright 2004-2007 Adobe Systems Incorporated
//  All Rights Reserved.
//
//  NOTICE: Adobe permits you to use, modify, and distribute this file
//  in accordance with the terms of the license agreement accompanying it.
//
////////////////////////////////////////////////////////////////////////////////

package flex2.compiler.media;

import flex2.compiler.SymbolTable;
import flex2.compiler.Transcoder;
import flex2.compiler.TranscoderException;
import flex2.compiler.common.PathResolver;
import flex2.compiler.io.VirtualFile;
import flash.swf.TagValues;
import flash.swf.builder.tags.ImageShapeBuilder;
import flash.swf.tags.*;
import flash.swf.types.*;

import java.util.ArrayList;
import java.util.Map;

/**
 * Transcodes images into DefineBits for embedding
 *
 * @author Paul Reilly
 * @author Clement Wong
 */
public abstract class ImageTranscoder extends AbstractTranscoder
{
    public ImageTranscoder(String[] mimeTypes, Class defineTag, boolean cacheTags)
    {
        super( mimeTypes, defineTag, cacheTags );
    }

    public boolean isSupportedAttribute( String attr )
    {
        return (AbstractTranscoder.SCALE9BOTTOM.equals( attr )
                || AbstractTranscoder.SCALE9LEFT.equals( attr )
                || AbstractTranscoder.SCALE9RIGHT.equals( attr )
                || AbstractTranscoder.SCALE9TOP.equals( attr ));
    }

    public abstract ImageInfo getImage( VirtualFile source, Map args ) throws TranscoderException;

	public TranscodingResults doTranscode( PathResolver context, SymbolTable symbolTable,
                                           Map args, String className, boolean generateSource )
        throws TranscoderException
	{
        TranscodingResults results = new TranscodingResults( resolveSource( context, args ) );

        // We don't need to export in FP9 movies, but hey, here's a top secret loophole 'til we're positive
        String newName = (String) args.get( Transcoder.NEWNAME );

        ImageInfo info = getImage( results.assetSource, args );
        if (args.containsKey(SCALE9LEFT) || args.containsKey(SCALE9RIGHT) || args.containsKey(SCALE9TOP) || args.containsKey(SCALE9BOTTOM))
        {
            if (args.get(SCALE9LEFT)==null || args.get(SCALE9RIGHT)==null || args.get(SCALE9TOP)==null || args.get(SCALE9BOTTOM)==null)
            {
                throw new ScalingGridException();
            }
            results.defineTag = buildSlicedSprite( newName, info, args );
        }
        else
        {
            //results.defineTag = buildSprite( newName, info );
            results.defineTag = buildBitmap( newName, info );
        }
        if (generateSource)
            generateSource( results, className, args );

        return results;
	}

    private static DefineShape generateSlicedShape( DefineBits refBitmap, Rect r, int width, int height )
    {
        int slt = r.xMin;
        int srt = r.xMax;
        int stt = r.yMin;
        int sbt = r.yMax;

        DefineShape shape = new DefineShape( TagValues.stagDefineShape6 );
        shape.bounds = new Rect( 0, width, 0, height );
        shape.edgeBounds = shape.bounds;
        shape.shapeWithStyle = new ShapeWithStyle();
        shape.shapeWithStyle.shapeRecords = new ArrayList();
        shape.shapeWithStyle.fillstyles = new ArrayList();
        shape.shapeWithStyle.linestyles = new ArrayList();
        Matrix tsm = new Matrix();    // translate into source bitmap
        tsm.setScale(20,20); // unity in twips

        for (int i = 0; i < 9; ++i) // 9 identical fillstyles to fool things
        {
            FillStyle fs = new FillStyle( FillStyle.FILL_BITS|FillStyle.FILL_BITS_NOSMOOTH, tsm, refBitmap);
            shape.shapeWithStyle.fillstyles.add( fs );
        }
        int dxa = slt;
        int dxb = srt-slt;
        int dxc = width-srt;

        int dya = stt;
        int dyb = sbt-stt;
        int dyc = height-sbt;

        StyleChangeRecord startStyle = new StyleChangeRecord();
        startStyle.setMove( 0, dya );
        shape.shapeWithStyle.shapeRecords.add( startStyle );

        // border
        addEdgesWithFill( shape, new int[][]{{0, -dya}, {dxa, 0}}, 0, 1 );
        addEdgesWithFill( shape, new int[][]{{dxb, 0}}, 0, 2 );
        addEdgesWithFill( shape, new int[][]{{dxc, 0}, {0, dya}}, 0, 3 );
        addEdgesWithFill( shape, new int[][]{{0, dyb}}, 0, 6 );
        addEdgesWithFill( shape, new int[][]{{0, dyc}, {-dxc, 0}}, 0, 9 );
        addEdgesWithFill( shape, new int[][]{{-dxb, 0}}, 0, 8 );
        addEdgesWithFill( shape, new int[][]{{-dxa, 0}, {0, -dyc}}, 0, 7 );
        addEdgesWithFill( shape, new int[][]{{0, -dyb}}, 0, 4 );

        // down 1
        StyleChangeRecord down1Style = new StyleChangeRecord();
        down1Style.setMove( dxa, 0 );
        shape.shapeWithStyle.shapeRecords.add( down1Style );
        addEdgesWithFill( shape, new int[][]{{0, dya}}, 2, 1 );
        addEdgesWithFill( shape, new int[][]{{0, dyb}}, 5, 4 );
        addEdgesWithFill( shape, new int[][]{{0, dyc}}, 8, 7 );

        // down 2
        StyleChangeRecord down2Style = new StyleChangeRecord();
        down2Style.setMove( dxa+dxb, 0 );
        shape.shapeWithStyle.shapeRecords.add( down2Style );
        addEdgesWithFill( shape, new int[][]{{0, dya}}, 3, 2 );
        addEdgesWithFill( shape, new int[][]{{0, dyb}}, 6, 5 );
        addEdgesWithFill( shape, new int[][]{{0, dyc}}, 9, 8 );

        // right 1
        StyleChangeRecord right1Style = new StyleChangeRecord();
        right1Style.setMove( 0, dya );
        shape.shapeWithStyle.shapeRecords.add( right1Style );
        addEdgesWithFill( shape, new int[][]{{dxa, 0}}, 1, 4 );
        addEdgesWithFill( shape, new int[][]{{dxb, 0}}, 2, 5 );
        addEdgesWithFill( shape, new int[][]{{dxc, 0}}, 3, 6 );

        // right 2
        StyleChangeRecord right2Style = new StyleChangeRecord();
        right2Style.setMove( 0, dya+dyb);
        shape.shapeWithStyle.shapeRecords.add( right2Style );
        addEdgesWithFill( shape, new int[][]{{dxa, 0}}, 4, 7 );
        addEdgesWithFill( shape, new int[][]{{dxb, 0}}, 5, 8 );
        addEdgesWithFill( shape, new int[][]{{dxc, 0}}, 6, 9 );
        return shape;
    }


    private static void addEdgesWithFill( DefineShape shape, int[][] coords, int left, int right )
    {
        StyleChangeRecord scr = new StyleChangeRecord();
        if ((left != 0) || (right != 0))
        {
            scr.setFillStyle0( left );
            scr.setFillStyle1( right );
        }
        shape.shapeWithStyle.shapeRecords.add( scr );

        for (int i = 0; i < coords.length; ++i)
        {
            shape.shapeWithStyle.shapeRecords.add( new StraightEdgeRecord(coords[i][0], coords[i][1]));
        }
    }

    public static DefineBits buildBitmap(String name, ImageInfo imageInfo)
    {
        return imageInfo.defineBits;
    }

    public static DefineSprite buildSprite(String name, ImageInfo imageInfo)
	{
        DefineSprite sprite = new DefineSprite( name );
        sprite.tagList.tags.add( imageInfo.defineBits );

		DefineShape ds3 = ImageShapeBuilder.buildImage(imageInfo.defineBits, imageInfo.width, imageInfo.height);
		sprite.tagList.defineShape3(ds3);

		PlaceObject po2 = new PlaceObject(ds3, 1);
		po2.setMatrix(new Matrix());
		// po2.setName(name);

		sprite.tagList.placeObject2(po2);
        return sprite;
	}

    public static DefineSprite buildSlicedSprite(String name, ImageInfo imageInfo, Map args) throws TranscoderException
    {
        DefineSprite sprite = new DefineSprite( name );
        MovieTranscoder.defineScalingGrid( sprite, args );
        DefineShape shape = generateSlicedShape( imageInfo.defineBits, sprite.scalingGrid.rect, imageInfo.width*20, imageInfo.height*20);

        PlaceObject po = new PlaceObject( shape, 10 );
        Matrix tm = new Matrix();
        tm.setScale(1,1);
        po.setMatrix(tm);
        sprite.tagList.placeObject( po );
        return sprite;

    }


    public String getAssociatedClass(DefineTag tag)
    {
        if (tag instanceof DefineBits)
        {
            return "mx.core.BitmapAsset";
        }
        return super.getAssociatedClass(tag);
    }
    static public class ImageInfo
    {
        public DefineBits defineBits;
        public int width;
        public int height;
    }

    public static final class ScalingGridException extends TranscoderException
    {

    }
}
