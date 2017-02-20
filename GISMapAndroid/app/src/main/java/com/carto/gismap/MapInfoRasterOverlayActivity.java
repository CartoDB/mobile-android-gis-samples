package com.carto.gismap;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Bundle;
import android.util.DisplayMetrics;

import com.carto.core.MapBounds;
import com.carto.core.MapPos;
import com.carto.core.MapRange;
import com.carto.core.ScreenBounds;
import com.carto.core.ScreenPos;
import com.carto.datasources.BitmapOverlayRasterTileDataSource;
import com.carto.datasources.LocalVectorDataSource;
import com.carto.gismap.base.BaseActivity;
import com.carto.gismap.utils.AssetCopy;
import com.carto.graphics.Bitmap;
import com.carto.layers.CartoBaseMapStyle;
import com.carto.layers.CartoOnlineVectorTileLayer;
import com.carto.layers.RasterTileLayer;
import com.carto.layers.TileSubstitutionPolicy;
import com.carto.layers.VectorLayer;
import com.carto.layers.VectorTileLayer;
import com.carto.projections.EPSG3857;
import com.carto.styles.MarkerStyle;
import com.carto.styles.MarkerStyleBuilder;
import com.carto.ui.MapView;
import com.carto.utils.BitmapUtils;
import com.carto.utils.Log;
import com.carto.vectorelements.Marker;
import com.carto.core.MapPosVector;
import com.carto.core.ScreenPosVector;
import com.carto.gismap.android.R;

public class MapInfoRasterOverlayActivity extends BaseActivity {
	
	MapView mapView;
	LocalVectorDataSource vectorDataSource;

	private void loadMapInfoOverlay(String tabFileName, boolean zoomIn, boolean addPin) {
        String localDir = getFilesDir().toString();
        try {
            AssetCopy.copyAssetToSDCard(getAssets(), tabFileName, localDir);
        } catch (IOException e) {
			e.printStackTrace();
		}

        // Parse the .tab file, extract image file name, control points and projection information
        String imageFileName = null;
		String projArgs = null;
		MapPosVector mapPoses = new MapPosVector();
		ScreenPosVector bitmapPoses = new ScreenPosVector();
		try {
			BufferedReader br = new BufferedReader(new FileReader(localDir + "/" + tabFileName));
			String line;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				
				// Match bitmap file name
				Matcher file = Pattern.compile("File\\s*\"([^\"]*)\"").matcher(line);
				if (file.matches()) {
					imageFileName = file.group(1);
				}
				
				// Match control points. Match only first 3. Though 4 can be used, the perspective transformation may look strange
				Matcher cpoint = Pattern.compile("\\(([0-9.]*),([0-9.]*)\\)\\s*\\(([0-9.]*),([0-9.]*)\\).*").matcher(line);
				if (cpoint.matches() && mapPoses.size() < 3) {
					MapPos mapPos = new MapPos(Double.parseDouble(cpoint.group(1)), Double.parseDouble(cpoint.group(2)));
					ScreenPos bitmapPos = new ScreenPos(Float.parseFloat(cpoint.group(3)), Float.parseFloat(cpoint.group(4)));
					mapPoses.add(mapPos);
					bitmapPoses.add(bitmapPos);
				}

				// Match EPSG4326/WGS84 projection
				Matcher coordSysWgs84 = Pattern.compile("CoordSys Earth Projection 1\\s*,\\s*104").matcher(line);
				if (coordSysWgs84.matches()) {
					projArgs = "";
				}

				// Match UTMxx/WGS84 projection
				Matcher coordSysUTM = Pattern.compile("CoordSys Earth Projection 8\\s*,\\s*104\\s*,(.*)").matcher(line);
				if (coordSysUTM.matches()) {
					String[] args = coordSysUTM.group(1).split(",");
					projArgs = "+proj=tmerc +ellps=WGS84 +x0=500000";
					projArgs += " +lon_0=" + args[1].trim() + " lat_0=" + args[2].trim();
					projArgs += " +k=" + args[3].trim();
					projArgs += " +x_0=" + args[4].trim() + " +y_0=" + args[5].trim();
				}
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();			
		}
		
		// Check if parsing was successful
		if (imageFileName != null && projArgs != null && mapPoses.size() >= 2 && mapPoses.size() <= 4) {
			Bitmap bitmap = BitmapUtils.loadBitmapFromAssets(imageFileName);

			// Reproject all control points to EPSG3857
			com.jhlabs.map.proj.Projection dataProj = null;
			if (projArgs.length() > 0) {
	        	dataProj = com.jhlabs.map.proj.ProjectionFactory.fromPROJ4Specification(projArgs.split(" "));
			}
	        EPSG3857 mapProj = new EPSG3857();
	        for (int i = 0; i < mapPoses.size(); i++) {
	        	MapPos posWgs84;
	        	if (dataProj != null) {
	        	    com.jhlabs.map.Point2D.Double src = new com.jhlabs.map.Point2D.Double(mapPoses.get(i).getX(), mapPoses.get(i).getY());
	        	    com.jhlabs.map.Point2D.Double dst = new com.jhlabs.map.Point2D.Double();
	        	    dataProj.inverseTransform(src, dst);
	        	    posWgs84 = new MapPos(dst.x, dst.y);
	        	} else {
	        		posWgs84 = new MapPos(mapPoses.get(i).getX(), mapPoses.get(i).getY());
	        	}
	        	MapPos mapPos = mapProj.fromWgs84(posWgs84);
	        	mapPoses.set(i, mapPos);
	        	
	        	// Flip vertical coordinate of the bitmap
	        	ScreenPos bitmapPos = bitmapPoses.get(i);
	        	bitmapPoses.set(i, new ScreenPos(bitmapPos.getX(), bitmap.getHeight() - 1 - bitmapPos.getY()));
	        }

	        // Create bitmap overlay layer. Use 22 as maximum zoom, but this may depend on actual data
	        BitmapOverlayRasterTileDataSource rasterDataSource = new BitmapOverlayRasterTileDataSource(0, 22, bitmap, mapProj, mapPoses, bitmapPoses);
	        RasterTileLayer rasterLayer = new RasterTileLayer(rasterDataSource);
	        rasterLayer.setTileSubstitutionPolicy(TileSubstitutionPolicy.TILE_SUBSTITUTION_POLICY_VISIBLE);
	        mapView.getLayers().add(rasterLayer); 
	        
	        // Calculate zoom level bias. This will result in optimal bitmap clarity on high-DPI screens
	        float zoomLevelBias = (float) (Math.log(mapView.getOptions().getDPI() / 160.0f) / Math.log(2));
	        rasterLayer.setZoomLevelBias(zoomLevelBias);
	        
	        // Limit visible zoom range, no need to display overlays at low zoom levels as the bitmap will be smaller than a pixel
	        rasterLayer.setVisibleZoomRange(new MapRange(12, 24));
	        
	        // Optionally add pin at the first vertex
	        if (addPin) {
	            // Create marker style
	            MarkerStyleBuilder markerStyleBuilder = new MarkerStyleBuilder();
	            markerStyleBuilder.setSize(20);
	            MarkerStyle markerStyle = markerStyleBuilder.buildStyle();
	            Marker marker = new Marker(mapPoses.get(0), markerStyle);
	            vectorDataSource.add(marker);
	        }
	        
	        // Optionally zoom-in
	        if (zoomIn) {
	        	MapBounds mapBounds = new MapBounds(mapPoses.get(0), mapPoses.get(1));
	            DisplayMetrics displaymetrics = new DisplayMetrics();
	            getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
	            int height = displaymetrics.heightPixels;
	            int width = displaymetrics.widthPixels;
	            ScreenBounds screenBounds = new ScreenBounds(new ScreenPos(0, 0), new ScreenPos(width, height));
	        	mapView.moveToFitBounds(mapBounds, screenBounds, false, 0.0f);
	        }
		} else {
			System.err.print("Image, projection or control points not correctly defined");
		}
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
        mapView = (MapView) this.findViewById(R.id.map_view);

        // Set the base projection, that will be used for most MapView, MapEventListener and Options methods
        EPSG3857 proj = new EPSG3857();
        mapView.getOptions().setBaseProjection(proj); // note: EPSG3857 is the default, so this is actually not required

        // General options
        mapView.getOptions().setRotatable(true); // make map rotatable (this is also the default)
        mapView.getOptions().setTileThreadPoolSize(3); // use 3 threads for tile downloading / raster overlay resampling

        // Create base layer. Use registered Nutiteq API key and vector style from assets (osmbright.zip)
        VectorTileLayer baseLayer = new CartoOnlineVectorTileLayer(CartoBaseMapStyle.CARTO_BASEMAP_STYLE_GRAY);
        mapView.getLayers().add(baseLayer);
        
        // Initialize an local vector data source
        vectorDataSource = new LocalVectorDataSource(proj);
        VectorLayer vectorLayer = new VectorLayer(vectorDataSource);
        // Add the previous vector layer to the map
        mapView.getLayers().add(vectorLayer);

        // Add MapInfo overlays
        loadMapInfoOverlay("0cf6e03a-baf0-4445-9b18-d4e66e32fdd7.tab", false, true);
        loadMapInfoOverlay("0e768fe2-4a51-4b64-993e-8b3cc757bd86.tab", false, true);
        loadMapInfoOverlay("650b2846-4324-49d4-ab59-d6c1c3de8156.tab", true, true);
    }
}
