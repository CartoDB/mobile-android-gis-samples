package com.carto.gismap;

import com.carto.core.MapPos;
import com.carto.core.ScreenPos;
import com.carto.core.StringVariantMap;
import com.carto.core.Variant;
import com.carto.datasources.LocalVectorDataSource;
import com.carto.geometry.LineGeometry;
import com.carto.geometry.MultiGeometry;
import com.carto.geometry.PointGeometry;
import com.carto.geometry.PolygonGeometry;
import com.carto.graphics.Color;
import com.carto.layers.EditableVectorLayer;
import com.carto.projections.EPSG3857;
import com.carto.projections.Projection;
import com.carto.styles.GeometryCollectionStyleBuilder;
import com.carto.styles.LineStyleBuilder;
import com.carto.styles.PointStyleBuilder;
import com.carto.styles.PolygonStyleBuilder;
import com.carto.vectorelements.GeometryCollection;
import com.carto.vectorelements.Polygon;
import com.carto.vectorelements.VectorElement;
import com.carto.geometry.GeometryVector;
import com.carto.core.MapPosVector;
import com.carto.core.StringMap;

public class EditableLocalOverlayActivity extends EditableOverlayActivityBase {
	private LocalVectorDataSource editableDataSource;
    
    @Override
    protected EditableVectorLayer createEditableLayer() {
		Projection proj = new EPSG3857();        
		editableDataSource = new LocalVectorDataSource(proj);
        EditableVectorLayer editLayer = new EditableVectorLayer(editableDataSource);
		mapView.getLayers().add(editLayer);
		return editLayer;
    }
    
    private MapPosVector fromScreenPoses(ScreenPos[] screenPoses) {
        MapPosVector mapPoses = new MapPosVector();
        for (ScreenPos pos : screenPoses) {
        	MapPos mapPos = mapView.screenToMap(new ScreenPos(pos.getX() * mapView.getWidth(), pos.getY() * mapView.getHeight()));
        	mapPoses.add(mapPos);
        }
    	return mapPoses;
    }
    
    @Override
    protected VectorElement createElement() {
    	ScreenPos[] screenPoses1 = new ScreenPos[] {
   			new ScreenPos(0.35f, 0.40f),
   			new ScreenPos(0.65f, 0.40f),
    		new ScreenPos(0.50f, 0.60f),
    		new ScreenPos(0.35f, 0.40f),
    	};
    	ScreenPos[] screenPoses2 = new ScreenPos[] {
       			new ScreenPos(0.15f, 0.40f),
       			new ScreenPos(0.25f, 0.40f),
        		new ScreenPos(0.20f, 0.60f),
        	};
    	ScreenPos[] screenPoses3 = new ScreenPos[] {
       			new ScreenPos(0.05f, 0.10f),
       			new ScreenPos(0.05f, 0.80f),
        	};
    	ScreenPos[] screenPoses4 = new ScreenPos[] {
       			new ScreenPos(0.75f, 0.60f),
    			
    	};

        PointStyleBuilder pointStyleBuilder = new PointStyleBuilder();
        pointStyleBuilder.setColor(new Color(0xff00ffbb));
        LineStyleBuilder lineStyleBuilder = new LineStyleBuilder();
        lineStyleBuilder.setColor(new Color(0xff00ff00));
        PolygonStyleBuilder polygonStyleBuilder = new PolygonStyleBuilder();
        polygonStyleBuilder.setColor(new Color(0xffccff00));
        GeometryCollectionStyleBuilder geomCollectionStyleBuilder = new GeometryCollectionStyleBuilder();
        geomCollectionStyleBuilder.setPointStyle(pointStyleBuilder.buildStyle());
        geomCollectionStyleBuilder.setLineStyle(lineStyleBuilder.buildStyle());
        geomCollectionStyleBuilder.setPolygonStyle(polygonStyleBuilder.buildStyle());
        StringVariantMap metaData = new StringVariantMap();
        metaData.set("prop1", new Variant("XXX"));
        metaData.set("prop2", new Variant("YYY"));
        PolygonGeometry geom1 = new PolygonGeometry(fromScreenPoses(screenPoses1));
        PolygonGeometry geom2 = new PolygonGeometry(fromScreenPoses(screenPoses2));
        LineGeometry geom3 = new LineGeometry(fromScreenPoses(screenPoses3));
        PointGeometry geom4 = new PointGeometry(fromScreenPoses(screenPoses4).get(0));
        GeometryVector geoms = new GeometryVector();
        GeometryVector geoms2 = new GeometryVector();
        geoms.add(geom1);
        geoms.add(geom3);
        MultiGeometry geom0 = new MultiGeometry(geoms);
        geoms2.add(geom4);
        geoms2.add(geom2);
        geoms2.add(geom0);
        MultiGeometry multiGeom = new MultiGeometry(geoms2);
        VectorElement element = new GeometryCollection(multiGeom, geomCollectionStyleBuilder.buildStyle());
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
    }

    @Override
    protected void discardEditableLayerChanges() {
    }

    @Override
    protected boolean hasEditableLayerChanges() {
    	return false;
    }
}
