package com.carto.gismap;

import java.io.IOException;

import android.util.DisplayMetrics;

import com.carto.core.MapBounds;
import com.carto.core.MapPos;
import com.carto.core.ScreenBounds;
import com.carto.core.ScreenPos;
import com.carto.core.StringVariantMap;
import com.carto.core.Variant;
import com.carto.datasources.OGRVectorDataBase;
import com.carto.datasources.OGRVectorDataSource;
import com.carto.gismap.base.EditableOverlayActivityBase;
import com.carto.gismap.utils.AssetCopy;
import com.carto.graphics.Color;
import com.carto.layers.EditableVectorLayer;
import com.carto.projections.EPSG3857;
import com.carto.projections.Projection;
import com.carto.styles.PolygonStyle;
import com.carto.styles.PolygonStyleBuilder;
import com.carto.styles.StyleSelector;
import com.carto.styles.StyleSelectorBuilder;
import com.carto.vectorelements.Polygon;
import com.carto.vectorelements.VectorElement;
import com.carto.core.MapPosVector;

public class EditableShapeFileOgrOverlayActivity extends EditableOverlayActivityBase {
	private OGRVectorDataSource editableDataSource;
    
    @Override
    protected EditableVectorLayer createEditableLayer() {
        String localDir = getFilesDir().toString();
        try {
            AssetCopy.copyAssetToSDCard(getAssets(), "sample.shp", localDir);
            AssetCopy.copyAssetToSDCard(getAssets(), "sample.prj", localDir);
            AssetCopy.copyAssetToSDCard(getAssets(), "sample.dbf", localDir);
            AssetCopy.copyAssetToSDCard(getAssets(), "sample.shx", localDir);
        } catch (IOException e) {
			e.printStackTrace();
        }
        // Create polygon style
        PolygonStyleBuilder polygonStyleBuilder = new PolygonStyleBuilder();
        polygonStyleBuilder.setColor(new Color(0xff00ff00));
        PolygonStyle polygonStyle = polygonStyleBuilder.buildStyle();

        StyleSelectorBuilder styleSelectorBuilder = new StyleSelectorBuilder();
        styleSelectorBuilder.addRule(polygonStyle);
        StyleSelector styleSelector = styleSelectorBuilder.buildSelector();
        
        Projection proj = new EPSG3857();
        OGRVectorDataBase db;
		try {
			db = new OGRVectorDataBase(localDir + "/sample.shp", true);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		editableDataSource = new OGRVectorDataSource(proj, styleSelector, db, 0);
        EditableVectorLayer editLayer = new EditableVectorLayer(editableDataSource);
		mapView.getLayers().add(editLayer);
        
        // Fit to bounds
        MapBounds bounds = editableDataSource.getDataExtent();
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        ScreenBounds screenBounds = new ScreenBounds(new ScreenPos(0, 0), new ScreenPos(displaymetrics.widthPixels, displaymetrics.heightPixels));
        mapView.moveToFitBounds(bounds, screenBounds, false, 0.5f);

        return editLayer;
    }
    
    @Override
    protected VectorElement createElement() {
    	ScreenPos[] screenPoses = new ScreenPos[] {
   			new ScreenPos(0.35f, 0.40f),
   			new ScreenPos(0.65f, 0.40f),
    		new ScreenPos(0.50f, 0.60f),
    	};

        MapPosVector mapPoses = new MapPosVector();
        for (ScreenPos pos : screenPoses) {
        	MapPos mapPos = mapView.screenToMap(new ScreenPos(pos.getX() * mapView.getWidth(), pos.getY() * mapView.getHeight()));
        	mapPoses.add(mapPos);
        }
        PolygonStyleBuilder polygonStyleBuilder = new PolygonStyleBuilder();
        polygonStyleBuilder.setColor(new Color(0xff00ff00));
        StringVariantMap metaData = new StringVariantMap();
        metaData.set("prop1", new Variant("XXX"));
        metaData.set("prop2", new Variant("YYY"));
        VectorElement element = new Polygon(mapPoses, polygonStyleBuilder.buildStyle());
        element.setMetaData(metaData);
        return element;
    }

    @Override
    protected void addElement(VectorElement element) {
        editableDataSource.add(element);    	
    }
    
    @Override
    protected void removeElement(VectorElement element) {
    	editableDataSource.remove(element);
    }
    
    @Override
    protected void saveEditableLayerChanges() {
    	editableDataSource.commit();
    }

    @Override
    protected void discardEditableLayerChanges() {
    	editableDataSource.rollback();
    }

    @Override
    protected boolean hasEditableLayerChanges() {
    	return !editableDataSource.isCommitted();
    }
}
