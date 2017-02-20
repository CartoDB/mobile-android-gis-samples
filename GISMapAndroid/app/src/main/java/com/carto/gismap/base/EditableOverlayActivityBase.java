package com.carto.gismap.base;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Toast;

import com.carto.core.MapPos;
import com.carto.core.MapPosVector;
import com.carto.core.StringVariantMap;
import com.carto.core.Variant;
import com.carto.core.VariantType;
import com.carto.geometry.Geometry;
import com.carto.geometry.LineGeometry;
import com.carto.geometry.MultiGeometry;
import com.carto.geometry.PointGeometry;
import com.carto.geometry.PolygonGeometry;
import com.carto.gismap.android.R;
import com.carto.graphics.Color;
import com.carto.layers.CartoBaseMapStyle;
import com.carto.layers.CartoOnlineVectorTileLayer;
import com.carto.layers.EditableVectorLayer;
import com.carto.layers.Layer;
import com.carto.layers.VectorEditEventListener;
import com.carto.layers.VectorElementDragPointStyle;
import com.carto.layers.VectorElementDragResult;
import com.carto.layers.VectorElementEventListener;
import com.carto.layers.VectorTileLayer;
import com.carto.styles.PointStyle;
import com.carto.styles.PointStyleBuilder;
import com.carto.ui.MapClickInfo;
import com.carto.ui.MapEventListener;
import com.carto.ui.MapView;
import com.carto.ui.VectorElementClickInfo;
import com.carto.ui.VectorElementDragInfo;
import com.carto.utils.Log;
import com.carto.vectorelements.GeometryCollection;
import com.carto.vectorelements.Line;
import com.carto.vectorelements.Point;
import com.carto.vectorelements.Polygon;
import com.carto.vectorelements.VectorElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public abstract class EditableOverlayActivityBase extends BaseActivity {

	private class MyEditEventListener extends VectorEditEventListener {
    	private PointStyle styleNormal;
    	private PointStyle styleVirtual;
    	private PointStyle styleSelected;
    	private boolean dragging;
    	private Geometry dragGeometry;
    	
    	public MyEditEventListener() {
    	}
    	
    	@Override
    	public boolean onElementSelect(VectorElement element) {
    		Log.debug("elementSelected");
    		selectedElement = element;
            updateUIButtons();
    		return true;
    	}
    	
    	@Override
    	public void onElementDeselected(VectorElement element) {
    		Log.debug("elementDeselected");
    		selectedElement = null;
            updateUIButtons();
    	}
    	
    	@Override
    	public void onElementModify(VectorElement element, Geometry geometry) {
    		if (!dragging) {
    			saveElementState(element, dragGeometry != null ? dragGeometry : element.getGeometry(), geometry);
    			dragGeometry = null;
    		}
    		Log.debug("modifyElement");
    		updateElementGeometry(element, geometry);
    		updateUIButtons();
    	}
        
    	@Override
		public void onElementDelete(VectorElement element) {
    		if (!dragging) {
    			saveElementState(element, dragGeometry != null ? dragGeometry : element.getGeometry(), null);
    			dragGeometry = null;
    		}
    		Log.debug("deleteElement");
    		removeElement(element);
    		updateUIButtons();
    	}

    	@Override
		public VectorElementDragResult onDragStart(VectorElementDragInfo dragInfo) {
    		Log.debug("dragStart");
    		dragging = true;
    		dragGeometry = dragInfo.getVectorElement().getGeometry();
            addPointBtn.setVisibility(View.GONE);
            deletePointBtn.setVisibility(View.VISIBLE);
    		return VectorElementDragResult.VECTOR_ELEMENT_DRAG_RESULT_MODIFY;
    	}

    	@Override
		public VectorElementDragResult onDragMove(VectorElementDragInfo dragInfo) {
            Rect rect = new Rect();
            deletePointBtn.getHitRect(rect);
            boolean onDeleteBtn = rect.contains((int) dragInfo.getScreenPos().getX(), (int) dragInfo.getScreenPos().getY());
            if (onDeleteBtn) {
                deletePointBtn.setColorFilter(android.graphics.Color.argb(255, 255, 0, 0));
            } else {
                deletePointBtn.setColorFilter(null);
            }
    		return VectorElementDragResult.VECTOR_ELEMENT_DRAG_RESULT_MODIFY;
    	}

    	@Override
		public VectorElementDragResult onDragEnd(VectorElementDragInfo dragInfo) {
    		Log.debug("dragEnd");
    		dragging = false;
            Rect rect = new Rect();
            deletePointBtn.getHitRect(rect);
            deletePointBtn.setColorFilter(null);
            deletePointBtn.setVisibility(View.GONE);
            addPointBtn.setVisibility(dragInfo.getVectorElement() instanceof Line ? View.VISIBLE : View.GONE);
            boolean onDeleteBtn = rect.contains((int) dragInfo.getScreenPos().getX(), (int) dragInfo.getScreenPos().getY());
            return onDeleteBtn ? VectorElementDragResult.VECTOR_ELEMENT_DRAG_RESULT_DELETE : VectorElementDragResult.VECTOR_ELEMENT_DRAG_RESULT_MODIFY;
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

	private class MyMapEventListener extends MapEventListener {
		@Override
		public void onMapMoved() {
		}

		@Override
		public void onMapClicked(MapClickInfo mapClickInfo) {
			editableLayer.setSelectedVectorElement(null);
		}
	}
	
	private class MyVectorElementEventListener extends VectorElementEventListener {

		@Override
		public boolean onVectorElementClicked(VectorElementClickInfo vectorElementClickInfo) {
			editableLayer.setSelectedVectorElement(vectorElementClickInfo.getVectorElement());
			return true;
		}		
	}
	
	private static class Memento {
    	public final VectorElement vectorElement;
    	public final Geometry oldGeometry;
    	public final Geometry newGeometry;

        public Memento(VectorElement vectorElement, Geometry oldGeometry, Geometry newGeometry) {
        	this.vectorElement = vectorElement;
            this.oldGeometry = oldGeometry;
            this.newGeometry = newGeometry;
        }
    }

    protected MapView mapView;
    protected Layer baseLayer;
    protected EditableVectorLayer editableLayer;
    protected VectorElement selectedElement;

    private Stack<Memento> undoStack = new Stack<Memento>(); 
    private Stack<Memento> redoStack = new Stack<Memento>(); 

    private LinearLayout elementEditorLayout;
    private LinearLayout pointEditorLayout;
    private ImageButton createElementBtn;
    private ImageButton modifyElementBtn;
    private ImageButton deleteElementBtn;
    private ImageButton saveChangesBtn;
    private ImageButton discardChangesBtn;
    private ImageButton undoChangeBtn;
    private ImageButton redoChangeBtn;
    private ImageButton addPointBtn;
    private ImageButton deletePointBtn;

    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // enable logging for troubleshooting - optional
        Log.setShowDebug(true);
        Log.setShowInfo(true);
        Log.setTag("editablemap");

        // 1. Get the MapView from the Layout xml - mandatory
        mapView = (MapView) findViewById(R.id.map_view);

        // 3. Define map layer for basemap - mandatory.
        // Here we use MapQuest open tiles
        // Almost all online tiled maps use EPSG3857 projection.
        baseLayer = createBaseLayer();

        // Activate some mapview options to make it smoother - optional

        // 6. Set up editable layers
        editableLayer = createEditableLayer();
        createEditorListener();
        createUIButtons();
    }

    protected Layer createBaseLayer() {
        VectorTileLayer baseLayer = new CartoOnlineVectorTileLayer(CartoBaseMapStyle.CARTO_BASEMAP_STYLE_GRAY);
        mapView.getLayers().add(baseLayer);
        return baseLayer;
    }

    private void createEditorListener() {
    	editableLayer.setVectorElementEventListener(new MyVectorElementEventListener());
    	editableLayer.setVectorEditEventListener(new MyEditEventListener());
    	mapView.setMapEventListener(new MyMapEventListener());
    }

    private void createUIButtons() {
        elementEditorLayout = new LinearLayout(this);
        elementEditorLayout.setOrientation(LinearLayout.HORIZONTAL);
        elementEditorLayout.setGravity(Gravity.START | Gravity.CENTER_HORIZONTAL);

        pointEditorLayout = new LinearLayout(this);
        pointEditorLayout.setOrientation(LinearLayout.HORIZONTAL);
        pointEditorLayout.setGravity(Gravity.END | Gravity.CENTER_HORIZONTAL);

        // Create dialog
        createElementBtn = new ImageButton(this);
        createElementBtn.setImageResource(android.R.drawable.ic_menu_add);
        createElementBtn.setBackground(null);
        createElementBtn.setFocusable(false);
        createElementBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                VectorElement element = createElement();
                saveElementState(element, null, element.getGeometry());
                addElement(element);
                editableLayer.setSelectedVectorElement(element);
            }
        });
        elementEditorLayout.addView(createElementBtn);

        // Properties editor
        modifyElementBtn = new ImageButton(this);
        modifyElementBtn.setImageResource(android.R.drawable.ic_menu_edit);
        modifyElementBtn.setBackground(null);
        modifyElementBtn.setFocusable(false);
        modifyElementBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                modifyElementProperties(selectedElement);
            }
        });
        elementEditorLayout.addView(modifyElementBtn);

        // Delete element
        deleteElementBtn = new ImageButton(this);
        deleteElementBtn.setImageResource(android.R.drawable.ic_menu_delete);
        deleteElementBtn.setBackground(null);
        deleteElementBtn.setFocusable(false);
        deleteElementBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
    			saveElementState(selectedElement, selectedElement.getGeometry(), null);
                removeElement(selectedElement);
                editableLayer.setSelectedVectorElement(null);
            }
        });
        elementEditorLayout.addView(deleteElementBtn);

        // Save changes
        saveChangesBtn = new ImageButton(this);
        saveChangesBtn.setImageResource(android.R.drawable.ic_menu_save);
        saveChangesBtn.setBackground(null);
        saveChangesBtn.setFocusable(false);
        saveChangesBtn.setVisibility(View.GONE);
        saveChangesBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                saveChanges();
            }
        });
        elementEditorLayout.addView(saveChangesBtn);

        // Discard changes
        discardChangesBtn = new ImageButton(this);
        discardChangesBtn.setImageResource(android.R.drawable.ic_menu_revert);
        discardChangesBtn.setBackground(null);
        discardChangesBtn.setFocusable(false);
        discardChangesBtn.setVisibility(View.GONE);
        discardChangesBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                discardChanges();
            }
        });
        elementEditorLayout.addView(discardChangesBtn);

        // Redo last undo
        redoChangeBtn = new ImageButton(this);
        redoChangeBtn.setImageResource(android.R.drawable.ic_menu_revert);
        Bitmap image = BitmapFactory.decodeResource(this.getResources(), android.R.drawable.ic_menu_revert);
        if (image != null) {
            Matrix flipMatrix = new Matrix();
            flipMatrix.setScale(-1, 1, image.getWidth() / 2, image.getHeight() / 2);
            redoChangeBtn.setImageMatrix(flipMatrix);
        }
        redoChangeBtn.setScaleType(ScaleType.MATRIX);
        redoChangeBtn.setBackground(null);
        redoChangeBtn.setFocusable(false);
        redoChangeBtn.setVisibility(View.GONE);
        redoChangeBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                redoStateChanges();
            }
        });
        elementEditorLayout.addView(redoChangeBtn);

        // Undo last change
        undoChangeBtn = new ImageButton(this);
        undoChangeBtn.setImageResource(android.R.drawable.ic_menu_revert);
        undoChangeBtn.setBackground(null);
        undoChangeBtn.setFocusable(false);
        undoChangeBtn.setVisibility(View.GONE);
        undoChangeBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                undoStateChanges();
            }
        });
        elementEditorLayout.addView(undoChangeBtn);

        // Add single vertex
        addPointBtn = new ImageButton(this);
        addPointBtn.setImageResource(android.R.drawable.ic_menu_add);
        addPointBtn.setBackground(null);
        addPointBtn.setFocusable(false);
        addPointBtn.setVisibility(View.GONE);
        addPointBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addElementPoint(selectedElement);
            }
        });
        pointEditorLayout.addView(addPointBtn);

        // Delete single vertex
        deletePointBtn = new ImageButton(this);
        deletePointBtn.setImageResource(android.R.drawable.ic_menu_delete);
        deletePointBtn.setBackground(null);
        deletePointBtn.setFocusable(false);
        deletePointBtn.setVisibility(View.GONE);
        pointEditorLayout.addView(deletePointBtn);

        // Create content view
        addContentView(elementEditorLayout, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        addContentView(pointEditorLayout, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        updateUIButtons();
    }

    private void modifyElementProperties(final VectorElement element) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Properties");

        // Build the list of properties
        final StringVariantMap metaData = element.getMetaData();
        List<String> itemList = new ArrayList<String>();
        for (int i = 0; i < metaData.size(); i++) {
        	String key = metaData.get_key(i);
            Variant value = metaData.get(key);
            itemList.add(key + ": " + (value != null && value.getType() == VariantType.VARIANT_TYPE_STRING ? value.getString() : ""));
        }
        final String[] items = new String[itemList.size()];
        builder.setItems(itemList.toArray(items), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
            	final String key = metaData.get_key(item);
                AlertDialog.Builder propBuilder = new AlertDialog.Builder(EditableOverlayActivityBase.this);
                propBuilder.setTitle("Set property");
                propBuilder.setMessage("New value for " + key);

                final EditText input = new EditText(EditableOverlayActivityBase.this);
                propBuilder.setView(input);
                propBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int button) {
                    	metaData.set(key, new Variant(input.getEditableText().toString()));
                    	element.setMetaData(metaData);
                    }
                });
                propBuilder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int button) {
                        dialog.cancel();
                    }
                });

                AlertDialog propDialog = propBuilder.create();
                propDialog.show();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void updateUIButtons() {
        runOnUiThread(new Runnable() {
            public void run() {
                modifyElementBtn.setVisibility(selectedElement != null ? View.VISIBLE : View.GONE);
                deleteElementBtn.setVisibility(selectedElement != null ? View.VISIBLE : View.GONE);
                saveChangesBtn.setVisibility(hasEditableLayerChanges() ? View.VISIBLE : View.GONE);
                //discardChangesBtn.setVisibility(hasPendingChanges() ? View.VISIBLE : View.GONE);
                undoChangeBtn.setVisibility(!undoStack.isEmpty() ? View.VISIBLE : View.GONE);
                redoChangeBtn.setVisibility(!redoStack.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void saveElementState(VectorElement vectorElement, Geometry oldGeometry, Geometry newGeometry) {
        Memento memento = new Memento(vectorElement, oldGeometry, newGeometry);
        undoStack.push(memento);
        redoStack.clear();
    }

    private void undoStateChanges() {
        if (undoStack.empty()) {
            return;
        }
        Memento memento = undoStack.pop();      
        redoStack.push(new Memento(memento.vectorElement, memento.oldGeometry, memento.newGeometry));
        if (memento.oldGeometry == null) {
        	removeElement(memento.vectorElement);
        } else {
            if (memento.newGeometry == null) {
            	addElement(memento.vectorElement);
            }
            updateElementGeometry(memento.vectorElement, memento.oldGeometry);
        }
        editableLayer.setSelectedVectorElement(null);
        updateUIButtons();
    }

    private void redoStateChanges() {
        if (redoStack.empty()) {
            return;
        }
        Memento memento = redoStack.pop();        
        undoStack.push(new Memento(memento.vectorElement, memento.oldGeometry, memento.newGeometry));
        if (memento.newGeometry == null) {
        	removeElement(memento.vectorElement);
        } else {
            if (memento.oldGeometry == null) {
            	addElement(memento.vectorElement);
            }
            updateElementGeometry(memento.vectorElement, memento.newGeometry);        	
        }
        editableLayer.setSelectedVectorElement(null);
        updateUIButtons();
    }

    private void saveChanges() {
        editableLayer.setSelectedVectorElement(null);

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            ProgressDialog dialog;
            RuntimeException exception;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                dialog = new ProgressDialog(EditableOverlayActivityBase.this);
                dialog.setIndeterminate(false);
                dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                dialog.setCancelable(false);
                dialog.setMessage("Saving...");
                dialog.show();
            }

            @Override
            protected Void doInBackground(final Void... args) {
            	saveEditableLayerChanges();
                redoStack.clear(); // TODO: currently this is required, otherwise redo/undo will not work properly
                undoStack.clear();
                return null;
            }

            @Override
            protected void onPostExecute(final Void result) {
                super.onPostExecute(result);
                dialog.dismiss();
                if (exception != null) {
                    Toast.makeText(EditableOverlayActivityBase.this, "Failed to save: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                }
                updateUIButtons();
            }
        };
        task.execute();
    }

    private void discardChanges() {
        editableLayer.setSelectedVectorElement(null);
        discardEditableLayerChanges();
        redoStack.clear(); // TODO: currently this is required, otherwise redo/undo will not work properly
        undoStack.clear();
        updateUIButtons();
    }
    
    private void updateElementGeometry(VectorElement element, Geometry geometry) {
		if (element instanceof Point && geometry instanceof PointGeometry) {
			((Point) element).setGeometry((PointGeometry) geometry);
		}
		if (element instanceof Line && geometry instanceof LineGeometry) {
			((Line) element).setGeometry((LineGeometry) geometry);
		}
		if (element instanceof Polygon && geometry instanceof PolygonGeometry) {
			((Polygon) element).setGeometry((PolygonGeometry) geometry);
		}
		if (element instanceof GeometryCollection && geometry instanceof MultiGeometry) {
			((GeometryCollection) element).setGeometry((MultiGeometry) geometry);
		}
    }

    private void addElementPoint(VectorElement element) {
        if (element instanceof Line) {
            Line line = (Line) element;
            MapPosVector mapPoses = line.getPoses();
            if (mapPoses.size() >= 2) {
                MapPos p0 = mapPoses.get((int) mapPoses.size() - 2);
                MapPos p1 = mapPoses.get((int) mapPoses.size() - 1);
                mapPoses.add(new MapPos(p1.getX() + (p1.getX() - p0.getX()), p1.getY() + (p1.getY() - p0.getY())));
                line.setPoses(mapPoses);
            }
        }
    }
    
    protected abstract VectorElement createElement();

    protected abstract void addElement(VectorElement element);
    
    protected abstract void removeElement(VectorElement element);
    
    protected abstract EditableVectorLayer createEditableLayer();
    
    protected abstract void saveEditableLayerChanges();

    protected abstract void discardEditableLayerChanges();

    protected abstract boolean hasEditableLayerChanges();
}