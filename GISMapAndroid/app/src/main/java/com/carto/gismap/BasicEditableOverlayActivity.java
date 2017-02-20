package com.carto.gismap;

import android.os.Bundle;

import com.carto.core.MapPos;
import com.carto.geometry.Geometry;
import com.carto.geometry.LineGeometry;
import com.carto.geometry.PointGeometry;
import com.carto.geometry.PolygonGeometry;
import com.carto.datasources.LocalVectorDataSource;
import com.carto.gismap.android.R;
import com.carto.gismap.base.BaseActivity;
import com.carto.graphics.Color;
import com.carto.layers.CartoBaseMapStyle;
import com.carto.layers.EditableVectorLayer;
import com.carto.layers.CartoOnlineVectorTileLayer;
import com.carto.layers.VectorElementDragPointStyle;
import com.carto.layers.VectorElementDragResult;
import com.carto.layers.VectorEditEventListener;
import com.carto.layers.VectorElementEventListener;
import com.carto.layers.VectorTileLayer;
import com.carto.projections.EPSG3857;
import com.carto.projections.Projection;
import com.carto.styles.LineStyleBuilder;
import com.carto.styles.PointStyle;
import com.carto.styles.PointStyleBuilder;
import com.carto.styles.PolygonStyleBuilder;
import com.carto.ui.MapClickInfo;
import com.carto.ui.MapEventListener;
import com.carto.ui.MapView;
import com.carto.ui.VectorElementClickInfo;
import com.carto.ui.VectorElementDragInfo;
import com.carto.utils.Log;
import com.carto.vectorelements.Line;
import com.carto.vectorelements.Point;
import com.carto.vectorelements.Polygon;
import com.carto.vectorelements.VectorElement;
import com.carto.core.MapPosVector;

/**
 * A minimal sample displaying EditableVectorLayer usage.
 */
public class BasicEditableOverlayActivity extends BaseActivity {
		
	class MyEditEventListener extends VectorEditEventListener {
    	private PointStyle styleNormal;
    	private PointStyle styleVirtual;
    	private PointStyle styleSelected;
    	private final LocalVectorDataSource vectorDataSource;
    	
    	public MyEditEventListener(LocalVectorDataSource vectorDataSource) {
    		this.vectorDataSource = vectorDataSource;
    	}
    	
    	@Override
    	public boolean onElementSelect(VectorElement element) {
    		Log.debug("elementSelected");
    		return true;
    	}
    	
    	@Override
    	public void onElementDeselected(VectorElement element) {
    		Log.debug("elementDeselected");
    	}
    	
    	@Override
    	public void onElementModify(VectorElement element, Geometry geometry) {
    		if (element instanceof Point && geometry instanceof PointGeometry) {
    			((Point) element).setGeometry((PointGeometry) geometry);
    		}
    		if (element instanceof Line && geometry instanceof LineGeometry) {
    			((Line) element).setGeometry((LineGeometry) geometry);
    		}
    		if (element instanceof Polygon && geometry instanceof PolygonGeometry) {
    			((Polygon) element).setGeometry((PolygonGeometry) geometry);
    		}
    		Log.debug("modifyElement");
    	}
        
    	@Override
		public void onElementDelete(VectorElement element) {
    		Log.debug("deleteElement");
    		vectorDataSource.remove(element);
    	}

    	@Override
		public VectorElementDragResult onDragStart(VectorElementDragInfo dragInfo) {
    		Log.debug("dragStart");
    		return VectorElementDragResult.VECTOR_ELEMENT_DRAG_RESULT_MODIFY;
    	}

    	@Override
		public VectorElementDragResult onDragMove(VectorElementDragInfo dragInfo) {
    		return VectorElementDragResult.VECTOR_ELEMENT_DRAG_RESULT_MODIFY;
    	}

    	@Override
		public VectorElementDragResult onDragEnd(VectorElementDragInfo dragInfo) {
    		Log.debug("dragEnd");
    		return VectorElementDragResult.VECTOR_ELEMENT_DRAG_RESULT_MODIFY;
    	}

    	@Override
		public PointStyle onSelectDragPointStyle(VectorElement element, VectorElementDragPointStyle dragPointStyle) {
    		if (styleNormal == null) {
    			PointStyleBuilder builder = new PointStyleBuilder();
    			builder.setColor(new Color(0xa0ffffff));
    			builder.setSize(20.0f);
    			styleNormal = builder.buildStyle();
    			builder.setSize(15.0f);
    			styleVirtual = builder.buildStyle();
    			builder.setColor(new Color(0xc0ffffff));
    			builder.setSize(30.0f);
    			styleSelected = builder.buildStyle();
    		}
    		
    		switch (dragPointStyle) {
    		case VECTOR_ELEMENT_DRAG_POINT_STYLE_SELECTED:
    			return styleSelected;
    		case VECTOR_ELEMENT_DRAG_POINT_STYLE_VIRTUAL:
    			return styleVirtual;
    		default:
    			return styleNormal;
    		}
		}
		
	}
	
	class MyMapEventListener extends MapEventListener {
		private EditableVectorLayer vectorLayer;
		
		public MyMapEventListener(EditableVectorLayer vectorLayer) {
			this.vectorLayer = vectorLayer;
		}
		
		@Override
		public void onMapMoved() {
		}

		@Override
		public void onMapClicked(MapClickInfo mapClickInfo) {
			VectorElement e = vectorLayer.getSelectedVectorElement();
			if (e instanceof Polygon) {
				Polygon p = (Polygon) e;
				MapPosVector v = p.getPoses();
				v.add(mapClickInfo.getClickPos());
				p.setPoses(v);
			}
//			vectorLayer.setSelectedVectorElement(null);
		}
	}
	
	class MyVectorElementEventListener extends VectorElementEventListener {
		private EditableVectorLayer vectorLayer;
		
		public MyVectorElementEventListener(EditableVectorLayer vectorLayer) {
			this.vectorLayer = vectorLayer;
		}

		@Override
		public boolean onVectorElementClicked(VectorElementClickInfo vectorElementClickInfo) {
			vectorLayer.setSelectedVectorElement(vectorElementClickInfo.getVectorElement());
			return true;
		}		
	}
	
	void testEditable(MapView mapView) {
		Projection proj = new EPSG3857();
        
        final LocalVectorDataSource vectorDataSource = new LocalVectorDataSource(proj);
        MapPosVector mapPoses = new MapPosVector();

        mapPoses.add(new MapPos(-5000000, -900000));
        PointStyleBuilder pointStyleBuilder = new PointStyleBuilder();
        pointStyleBuilder.setColor(new Color(0xffff0000));
        Point point = new Point(mapPoses.get(0), pointStyleBuilder.buildStyle());
        vectorDataSource.add(point);

        mapPoses.clear();
        mapPoses.add(new MapPos(-6000000, -500000));
        mapPoses.add(new MapPos(-9000000, -500000));
        LineStyleBuilder lineStyleBuilder = new LineStyleBuilder();
        lineStyleBuilder.setColor(new Color(0xff0000ff));
        Line line = new Line(mapPoses, lineStyleBuilder.buildStyle());
        vectorDataSource.add(line);

        mapPoses.clear();
        mapPoses.add(new MapPos(-5000000, -5000000));
        mapPoses.add(new MapPos( 5000000, -5000000));
        mapPoses.add(new MapPos(       0, 10000000));
        PolygonStyleBuilder polygonStyleBuilder = new PolygonStyleBuilder();
        polygonStyleBuilder.setColor(new Color(0xff00ff00));
        Polygon polygon = new Polygon(mapPoses, polygonStyleBuilder.buildStyle());
        vectorDataSource.add(polygon);

        EditableVectorLayer editLayer = new EditableVectorLayer(vectorDataSource);
		mapView.getLayers().add(editLayer);
		
		editLayer.setVectorElementEventListener(new MyVectorElementEventListener(editLayer));
		editLayer.setVectorEditEventListener(new MyEditEventListener(vectorDataSource));
		
		mapView.setMapEventListener(new MyMapEventListener(editLayer));
	}
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        Log.setShowInfo(true);
        Log.setShowError(true);
        
        // Get your own license from developer.nutiteq.com
        MapView.registerLicense(getString(R.string.license_code), getApplicationContext());

        // Create map view 
        MapView mapView = (MapView) this.findViewById(R.id.map_view);

        // Set the base projection, that will be used for most MapView, MapEventListener and Options methods
        EPSG3857 proj = new EPSG3857();
        mapView.getOptions().setBaseProjection(proj); // note: EPSG3857 is the default, so this is actually not required
        
        // General options
        mapView.getOptions().setRotatable(true); // make map rotatable (this is also the default)
        mapView.getOptions().setTileThreadPoolSize(2); // use 2 download threads for tile downloading

        // Create base layer. Use registered Nutiteq API key and vector style from assets (osmbright.zip)
        VectorTileLayer baseLayer = new CartoOnlineVectorTileLayer(CartoBaseMapStyle.CARTO_BASEMAP_STYLE_GRAY);
        mapView.getLayers().add(baseLayer);

        // Create editable layer with listeners
        testEditable(mapView);
		
		mapView.setZoom(2, 0);
    }
}
