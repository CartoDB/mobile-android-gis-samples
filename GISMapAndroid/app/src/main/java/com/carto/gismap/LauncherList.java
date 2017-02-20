package com.carto.gismap;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.carto.gismap.android.R;

/**
 * Shows list of demo Activities. Enables to open pre-launch activity for file picking.
 * This is the "main" of samples
 */
public class LauncherList extends ListActivity {

    // list of demos: MapActivity, ParameterSelectorActivity (can be null)
    // if parameter selector is given, then this is launched first to get a parameter (file path)
    
    private Class<?>[] samples= {
            ShapeFileOgrOverlayActivity.class,
            GeoTiffGdalOverlayActivity.class,
            BasicEditableOverlayActivity.class,
            EditableLocalOverlayActivity.class,
            EditableShapeFileOgrOverlayActivity.class,
            MapInfoRasterOverlayActivity.class,
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.list);

        ListView lv = this.getListView();
        lv.setAdapter(new ArrayAdapter<String>(
                this, 
                android.R.layout.simple_list_item_1, 
                getStringArray()));
    }
    
    private String[] getStringArray() {
        String[] sampleNames = new String[samples.length];
        for(int i=0; i < samples.length; i++) {
            sampleNames[i] = ""+ (i + 1 ) + ". " + (samples[i].getSimpleName().replace("Activity", ""));
        }
        return sampleNames;
    }

    public void onListItemClick(ListView parent, View v, int position, long id) {

        Intent myIntent = new Intent(LauncherList.this, samples[position]);
        this.startActivity(myIntent);
    }
    
}
