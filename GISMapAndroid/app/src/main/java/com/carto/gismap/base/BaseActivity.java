package com.carto.gismap.base;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;

/**
 * Created by aareundo on 20/02/17.
 */

public class BaseActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        String title = this.getClass().getSimpleName().replace("Activity", "");
        setTitle(title);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
