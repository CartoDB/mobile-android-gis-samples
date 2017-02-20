package com.carto.gismap;

import java.io.IOException;

import android.os.Bundle;
import android.util.DisplayMetrics;

import com.carto.core.MapBounds;
import com.carto.core.ScreenBounds;
import com.carto.core.ScreenPos;
import com.carto.datasources.GDALRasterTileDataSource;
import com.carto.gismap.android.R;
import com.carto.gismap.base.BaseActivity;
import com.carto.gismap.utils.AssetCopy;
import com.carto.layers.CartoBaseMapStyle;
import com.carto.layers.CartoOnlineVectorTileLayer;
import com.carto.layers.RasterTileLayer;
import com.carto.layers.VectorTileRenderOrder;
import com.carto.projections.EPSG3857;
import com.carto.ui.MapView;
import com.carto.utils.Log;

/**
 * A sample showing how to display GeoTiff overlay using GDALRasterTileDataSource.
 */
public class GeoTiffGdalOverlayActivity extends BaseActivity {
	
    static {
        try {
            // force Java to load PROJ.4 library. Needed as we don't call it directly, but 
            // OGR datasource reading may need it.
            System.loadLibrary("proj");
        } catch (Throwable t) {
            System.err.println("Unable to load proj: " + t);
        }
    }
    
	void testRaster(MapView mapView) {
        String localDir = getFilesDir().toString();
        try {
            AssetCopy.copyAssetToSDCard(getAssets(), "chicago.tif", localDir);
        } catch (IOException e) {
			e.printStackTrace();
		}
        
        mapView.getOptions().setTileThreadPoolSize(2); // faster tile loading

        // Create GDAL raster tile layer
		GDALRasterTileDataSource dataSource;
		try {
			dataSource = new GDALRasterTileDataSource(0, 23, localDir + "/chicago.tif", "EPSG:26771");
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		RasterTileLayer rasterLayer = new RasterTileLayer(dataSource);
		mapView.getLayers().add(rasterLayer);
		
		// Calculate zoom bias, basically this is needed to 'undo' automatic DPI scaling, we will display original raster with close to 1:1 pixel density
		double zoomLevelBias = Math.log(mapView.getOptions().getDPI() / 160) / Math.log(2);
		rasterLayer.setZoomLevelBias((float) zoomLevelBias);

		// Find GDAL layer bounds
		MapBounds bounds = dataSource.getDataExtent();

        // Fit to bounds
        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int height = displaymetrics.heightPixels;
        int width = displaymetrics.widthPixels;

        mapView.moveToFitBounds(bounds, new ScreenBounds(new ScreenPos(0, 0), new ScreenPos(width, height)), false, 0.0f);
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

        // Create base layer.
        CartoOnlineVectorTileLayer baseLayer = new CartoOnlineVectorTileLayer(CartoBaseMapStyle.CARTO_BASEMAP_STYLE_GRAY);
        // set labels on top
        baseLayer.setLabelRenderOrder(VectorTileRenderOrder.VECTOR_TILE_RENDER_ORDER_LAST);
        mapView.getLayers().add(baseLayer);

        testRaster(mapView);
    }
}
