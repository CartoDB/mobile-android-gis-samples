package com.carto.gismap;

import android.os.Bundle;
import android.util.DisplayMetrics;

import com.carto.core.MapBounds;
import com.carto.core.MapRange;
import com.carto.core.ScreenBounds;
import com.carto.core.ScreenPos;
import com.carto.core.StringVariantMap;
import com.carto.datasources.LocalVectorDataSource;
import com.carto.datasources.OGRVectorDataSource;
import com.carto.gismap.android.R;
import com.carto.gismap.base.BaseActivity;
import com.carto.gismap.utils.AssetCopy;
import com.carto.graphics.Color;
import com.carto.layers.CartoBaseMapStyle;
import com.carto.layers.CartoOnlineVectorTileLayer;
import com.carto.layers.VectorElementEventListener;
import com.carto.layers.VectorLayer;
import com.carto.layers.VectorTileLayer;
import com.carto.layers.VectorTileRenderOrder;
import com.carto.projections.EPSG3857;
import com.carto.projections.Projection;
import com.carto.styles.BalloonPopupStyleBuilder;
import com.carto.styles.PolygonStyle;
import com.carto.styles.PolygonStyleBuilder;
import com.carto.styles.StyleSelector;
import com.carto.styles.StyleSelectorBuilder;
import com.carto.styles.TextStyleBuilder;
import com.carto.ui.MapClickInfo;
import com.carto.ui.MapEventListener;
import com.carto.ui.MapView;
import com.carto.ui.VectorElementClickInfo;
import com.carto.utils.Log;
import com.carto.vectorelements.BalloonPopup;

import java.io.IOException;

/**
 * A sample showing how to use OGRVectorDataSource, style selectors and custom element meta data.
 */
public class ShapeFileOgrOverlayActivity extends BaseActivity {
	
    static {
        try {
            // force Java to load PROJ.4 library. Needed as we don't call it directly, but 
            // OGR datasource reading may need it.
            System.loadLibrary("proj");
        } catch (Throwable t) {
            System.err.println("Unable to load proj: " + t);
        }
    }
    
	// Listener that displays vector element meta data as popups
	class ActivityMapEventListener extends MapEventListener {
	    
		@Override
		public void onMapMoved() {
		}

		@Override
		public void onMapClicked(MapClickInfo mapClickInfo) {	
		}
	}
	
	class ActivityVectorElementEventListener extends VectorElementEventListener {

		@Override
		public boolean onVectorElementClicked(VectorElementClickInfo vectorElementClickInfo) {
			popupDataSource.clear();
			StringVariantMap stringMap = vectorElementClickInfo.getVectorElement().getMetaData();
			if (stringMap.size() > 0) {
				StringBuilder msgBuilder = new StringBuilder();
				for (int i = 0; i < stringMap.size(); i++) {
				    Log.debug(""+stringMap.get_key(i)+" = "+stringMap.get(stringMap.get_key(i)));
					msgBuilder.append(stringMap.get_key(i));
					msgBuilder.append("=");
					msgBuilder.append(stringMap.get(stringMap.get_key(i)));
					msgBuilder.append("\n");
				}
				BalloonPopupStyleBuilder styleBuilder = new BalloonPopupStyleBuilder();
				BalloonPopup clickPopup = new BalloonPopup(
						vectorElementClickInfo.getClickPos(),
						styleBuilder.buildStyle(),
						"",
						msgBuilder.toString());
				popupDataSource.add(clickPopup);
			}
			return true;
		}
	}
	
	private LocalVectorDataSource popupDataSource;

	void testVector(MapView mapView) {
		Projection proj = new EPSG3857();
        
        // 2. Add a pin marker to map
        // Initialize a local vector data source
        LocalVectorDataSource vectorDataSource1 = new LocalVectorDataSource(proj);
        
        // Create a vector layer with the previously created data source
        VectorLayer vectorLayer1 = new VectorLayer(vectorDataSource1);
        // Add the previous vector layer to the map
        mapView.getLayers().add(vectorLayer1);
        // Set visible zoom range for the vector layer
        vectorLayer1.setVisibleZoomRange(new MapRange(0, 24)); // this is optional, by default layer is visible for all zoom levels


        // 3. Add Shapefile as layer
        // Copy sample shape file from assets folder to SDCard

        String shpName = "new-delhi_osm_buildings";
        String localDir = getFilesDir().toString();
        try {
            
            AssetCopy.copyAssetToSDCard(getAssets(), shpName+".shp", localDir);
            AssetCopy.copyAssetToSDCard(getAssets(), shpName+".dbf", localDir);
            AssetCopy.copyAssetToSDCard(getAssets(), shpName+".prj", localDir);
            AssetCopy.copyAssetToSDCard(getAssets(), shpName+".shx", localDir);
            AssetCopy.copyAssetToSDCard(getAssets(), shpName+".qix", localDir);
            AssetCopy.copyAssetToSDCard(getAssets(), shpName+".cpg", localDir);
        } catch (IOException e) {
			e.printStackTrace();
		}

        // Create sample point styles, one for cafes/restaurants, the other for all other POIs
//      PointStyleBuilder pointStyleBuilder = new PointStyleBuilder();
//      pointStyleBuilder.setColor(new Color(0xffff0000)); // fully opaque, red
//      pointStyleBuilder.setSize(5.0f);
//      PointStyle pointStyleBig = pointStyleBuilder.buildStyle();
//      pointStyleBuilder.setColor(new Color(0x7f7f0000)); // half-transparent, red
//      pointStyleBuilder.setSize(3.0f);
//      PointStyle pointStyleSmall = pointStyleBuilder.buildStyle();
        
        // Create line style
//      LineStyleBuilder lineStyleBuilder = new LineStyleBuilder();
//      lineStyleBuilder.setColor(new Color(0xff00ff00));
//      lineStyleBuilder.setWidth(2.0f);
//      LineStyle lineStyle = lineStyleBuilder.buildStyle();
                
//      lineStyleBuilder.setColor(new Color(0xffff0000));
//      LineStyle lineStyle2 = lineStyleBuilder.buildStyle();
        
//      lineStyleBuilder.setColor(new Color(0xffffff00));
//      LineStyle lineStyle3 = lineStyleBuilder.buildStyle();
        
        // Create polygon style2
        PolygonStyleBuilder polygonStyleBuilder = new PolygonStyleBuilder();
        polygonStyleBuilder.setColor(new Color(0xff006000));
        PolygonStyle polygonStyleGreen = polygonStyleBuilder.buildStyle();

        polygonStyleBuilder.setColor(new Color(0xff600000));
        PolygonStyle polygonStyleRed = polygonStyleBuilder.buildStyle();

        // Create style selector.
        // Style selectors allow to assign styles based on element attributes and view parameters (zoom, for example)
        // Style filter expressions are given in a simple SQL-like language.
        StyleSelectorBuilder styleSelectorBuilder = new StyleSelectorBuilder();
//      styleSelectorBuilder.addRule("type='cafe' OR type='restaurant'", pointStyleBig) // 'type' is a member of geometry meta data
//      styleSelectorBuilder.addRule(pointStyleSmall)
//      styleSelectorBuilder.addRule("ROADTYPE = 1", lineStyle)
//      styleSelectorBuilder.addRule("ROADTYPE = 2", lineStyle2)
//      styleSelectorBuilder.addRule("ROADTYPE = 3", lineStyle3)
        styleSelectorBuilder.addRule("type != 'yes'", polygonStyleRed);
        styleSelectorBuilder.addRule(polygonStyleGreen);
        StyleSelector styleSelector = styleSelectorBuilder.buildSelector();
        
        // Create data source. Use constructed style selector and copied shape file containing points.
        // OGRVectorDataSource.setConfigOption("SHAPE_ENCODING", "ISO8859_1");

        OGRVectorDataSource ogrDataSource = new OGRVectorDataSource(proj, styleSelector, localDir + "/"+shpName+".shp");

//      ogrDataSource.setCodePage("CP1254");

        // Create vector layer using OGR data source
        VectorLayer ogrLayer = new VectorLayer(ogrDataSource);
        ogrLayer.setVectorElementEventListener(new ActivityVectorElementEventListener());
        mapView.getLayers().add(ogrLayer);

        // 4. Add 'name' field as labels from same Shapefile, as another layer

        TextStyleBuilder textStyleBuilder = new TextStyleBuilder();
        textStyleBuilder.setTextField("name"); // field in the metadata
        textStyleBuilder.setHideIfOverlapped(false);
//        textStyleBuilder.setFontSize(20);
//        textStyleBuilder.setStrokeColor(new Color(0xff000060));


        StyleSelectorBuilder textStyleSelectorBuilder = new StyleSelectorBuilder();
        textStyleSelectorBuilder.addRule(textStyleBuilder.buildStyle());

        OGRVectorDataSource ogrDataSourceText = new OGRVectorDataSource(proj, textStyleSelectorBuilder.buildSelector(), localDir + "/"+shpName+".shp");

        // Create vector layer using OGR data source
        VectorLayer ogrLayerText = new VectorLayer(ogrDataSourceText);
        // set zoom range for the layer
        ogrLayerText.setVisibleZoomRange(new MapRange(14,20));

        mapView.getLayers().add(ogrLayerText);

        // 5. Find data bounds and zoom there
        MapBounds bounds = ogrDataSource.getDataExtent();

        Log.debug("features:" + ogrDataSource.getFeatureCount());
        Log.debug("bounds:"+bounds.toString());


        // Fit to bounds
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int height = displaymetrics.heightPixels;
        int width = displaymetrics.widthPixels;

        mapView.moveToFitBounds(bounds,
                new ScreenBounds(new ScreenPos(0, 0), new ScreenPos(width, height)),false, 0);
        mapView.setZoom(mapView.getZoom()+2, 0.5f); // zoom in 2 steps

        // 6. Create layer for popups and attach event listener
        popupDataSource = new LocalVectorDataSource(proj);
        VectorLayer popupLayer = new VectorLayer(popupDataSource);
        mapView.getLayers().add(popupLayer);
        mapView.setMapEventListener(new ActivityMapEventListener());		
	}
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        Log.setShowInfo(true);
        Log.setShowError(true);
        
        // Get your own license from developer.nutiteq.com
        MapView.registerLicense(getString(R.string.license_code), getApplicationContext());

        // 1. Basic map setup
        // Create map view 
        MapView mapView = (MapView) this.findViewById(R.id.map_view);

        // Set the base projection, that will be used for most MapView, MapEventListener and Options methods
        EPSG3857 proj = new EPSG3857();
        mapView.getOptions().setBaseProjection(proj); // note: EPSG3857 is the default, so this is actually not required
        
        // General options
        mapView.getOptions().setRotatable(true); // make map rotatable (this is also the default)
        mapView.getOptions().setTileThreadPoolSize(2); // use 2 download threads for tile downloading

        // Create base layer. Use registered Nutiteq API key and vector style from assets (osmbright.zip)
        VectorTileLayer baseLayer = new CartoOnlineVectorTileLayer(CartoBaseMapStyle.CARTO_BASEMAP_STYLE_POSITRON);

        // hide labels from base map
        baseLayer.setLabelRenderOrder(VectorTileRenderOrder.VECTOR_TILE_RENDER_ORDER_HIDDEN);

        mapView.getLayers().add(baseLayer);

        testVector(mapView);
    }
}
