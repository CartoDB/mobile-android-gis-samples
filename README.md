## GIS-specific Android samples of CARTO Mobile SDK

* Official docs: https://carto.com/docs/carto-engine/mobile-sdk/
* This project includes and requires prebuilt SDK version with GIS extensions (OGR/GDAL datasources), it does not work with standard SDK build.

## Sample structure

* OGR Overlay with a Shapefile
   * Shows how to display vector data using OGR data sources/connectors
* GDAL Overlay with a GeoTIFF file
   * Shows how to display raster data using GDAL data sources/connectors
* Basic Editable Overlay
   * Basic GIS editor that uses LocalVectorDataSource for non-persistent vector element storage
* Editable OGR Overlay with a Shapefile
   * More advanced GIS editor that uses OGRVectorDataSource for persistent feature storage
* MapInfo Overlay
   * Demonstrates how to read MapInfo files an display raster images using BitmapOverlayRasterTileDataSource class

## Other Samples of CARTO Mobile SDK

* Android samples (non-GIS): https://github.com/CartoDB/mobile-android-samples
* Xamarin (iOS, Android and Windows Phone samples in C#): https://github.com/CartoDB/mobile-dotnet-samples
* XCode (iOS samples in Objective-C and Swift): https://github.com/CartoDB/mobile-ios-samples
