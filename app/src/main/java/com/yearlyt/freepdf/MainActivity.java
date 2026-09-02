package com.yearlyt.freepdf;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.graphics.pdf.PdfRenderer;
import android.provider.OpenableColumns;
import android.database.Cursor;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;

public class MainActivity extends Activity {
    private static final int PICK_PDF = 41;
    private PdfRenderer renderer;
    private ParcelFileDescriptor descriptor;
    private ImageView pageImage;
    private TextView titleView, pageView, emptyTitle, emptyText;
    private LinearLayout emptyState, bottomBar;
    private ProgressBar progress;
    private int currentPage = 0;
    private float zoom = 1.25f;
    private final int blue = Color.rgb(39, 131, 222);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
        Uri incoming = getIntent() != null ? getIntent().getData() : null;
        if (incoming != null) openPdf(incoming);
    }

    private TextView text(String value, float sp, int color) {
        TextView v = new TextView(this); v.setText(value); v.setTextSize(sp); v.setTextColor(color);
        return v;
    }

    private Button button(String label) {
        Button b = new Button(this); b.setText(label); b.setTextSize(15); b.setAllCaps(false);
        b.setMinHeight(dp(48)); b.setTextColor(Color.WHITE); b.setBackground(roundRect(blue, 10));
        b.setPadding(dp(18), 0, dp(18), 0); return b;
    }

    private GradientDrawable roundRect(int color, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }

    private GradientDrawable outline() {
        GradientDrawable d = new GradientDrawable(); d.setColor(Color.WHITE); d.setStroke(dp(1), Color.rgb(230,229,227)); d.setCornerRadius(dp(10)); return d;
    }

    private void buildUi() {
        boolean dark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        int canvas = dark ? Color.rgb(25,25,25) : Color.WHITE;
        int surface = dark ? Color.rgb(32,32,32) : Color.rgb(249,248,247);
        int primary = dark ? Color.WHITE : Color.rgb(44,44,43);
        int secondary = dark ? Color.rgb(180,180,180) : Color.rgb(125,122,117);

        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(canvas);
        LinearLayout top = new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL); top.setPadding(dp(16),dp(12),dp(16),dp(12)); top.setBackgroundColor(canvas);
        TextView logo = text("▣", 25, blue); top.addView(logo, new LinearLayout.LayoutParams(dp(38), dp(48)));
        titleView = text("Free PDF Reader", 19, primary); titleView.setGravity(Gravity.CENTER_VERTICAL); titleView.setSingleLine(true);
        top.addView(titleView, new LinearLayout.LayoutParams(0, dp(48), 1));
        Button open = button("Open PDF"); open.setOnClickListener(v -> pickPdf()); top.addView(open, new LinearLayout.LayoutParams(-2, dp(48)));
        root.addView(top, new LinearLayout.LayoutParams(-1, dp(72)));

        LinearLayout stage = new LinearLayout(this); stage.setGravity(Gravity.CENTER); stage.setBackgroundColor(surface);
        emptyState = new LinearLayout(this); emptyState.setOrientation(LinearLayout.VERTICAL); emptyState.setGravity(Gravity.CENTER); emptyState.setPadding(dp(32),dp(32),dp(32),dp(32));
        TextView icon = text("PDF", 28, Color.WHITE); icon.setGravity(Gravity.CENTER); icon.setBackground(roundRect(blue, 12)); emptyState.addView(icon, new LinearLayout.LayoutParams(dp(82),dp(82)));
        emptyTitle = text("Open your first PDF", 22, primary); emptyTitle.setGravity(Gravity.CENTER); emptyTitle.setPadding(0,dp(24),0,dp(8)); emptyState.addView(emptyTitle);
        emptyText = text("Private, fast and completely free.\nYour files stay on your device.", 15, secondary); emptyText.setGravity(Gravity.CENTER); emptyText.setLineSpacing(0,1.25f); emptyState.addView(emptyText);
        Button choose = button("Choose a PDF"); LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-2,dp(50)); cp.topMargin=dp(24); emptyState.addView(choose,cp); choose.setOnClickListener(v -> pickPdf());
        stage.addView(emptyState, new LinearLayout.LayoutParams(-1,-1));

        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setPadding(dp(12),dp(12),dp(12),dp(12));
        pageImage = new ImageView(this); pageImage.setAdjustViewBounds(true); pageImage.setScaleType(ImageView.ScaleType.FIT_CENTER); pageImage.setBackgroundColor(Color.WHITE);
        scroll.addView(pageImage, new ScrollView.LayoutParams(-1,-2)); stage.addView(scroll, new LinearLayout.LayoutParams(-1,-1)); scroll.setVisibility(View.GONE); pageImage.setTag(scroll);
        progress = new ProgressBar(this); progress.setVisibility(View.GONE); stage.addView(progress);
        root.addView(stage, new LinearLayout.LayoutParams(-1,0,1));

        bottomBar = new LinearLayout(this); bottomBar.setGravity(Gravity.CENTER); bottomBar.setPadding(dp(12),dp(8),dp(12),dp(8)); bottomBar.setBackgroundColor(canvas); bottomBar.setVisibility(View.GONE);
        Button prev=secondaryButton("‹ Prev", primary), minus=secondaryButton("−", primary), plus=secondaryButton("+", primary), next=secondaryButton("Next ›", primary);
        pageView=text("1 / 1",15,primary); pageView.setGravity(Gravity.CENTER);
        bottomBar.addView(prev,new LinearLayout.LayoutParams(0,dp(48),1)); bottomBar.addView(minus,new LinearLayout.LayoutParams(dp(56),dp(48)));
        bottomBar.addView(pageView,new LinearLayout.LayoutParams(dp(86),dp(48))); bottomBar.addView(plus,new LinearLayout.LayoutParams(dp(56),dp(48))); bottomBar.addView(next,new LinearLayout.LayoutParams(0,dp(48),1));
        prev.setOnClickListener(v->{if(currentPage>0){currentPage--;render();}}); next.setOnClickListener(v->{if(renderer!=null&&currentPage<renderer.getPageCount()-1){currentPage++;render();}});
        minus.setOnClickListener(v->{zoom=Math.max(.75f,zoom-.25f);render();}); plus.setOnClickListener(v->{zoom=Math.min(3f,zoom+.25f);render();});
        root.addView(bottomBar,new LinearLayout.LayoutParams(-1,dp(64)));
        setContentView(root);
    }

    private Button secondaryButton(String label, int color) {
        Button b=new Button(this); b.setText(label); b.setTextSize(14); b.setAllCaps(false); b.setTextColor(color); b.setBackground(outline()); b.setMinWidth(0); return b;
    }

    private void pickPdf(){ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("application/pdf"); startActivityForResult(i,PICK_PDF); }

    @Override protected void onActivityResult(int request,int result,Intent data){ super.onActivityResult(request,result,data); if(request==PICK_PDF&&result==RESULT_OK&&data!=null){ Uri uri=data.getData(); if(uri!=null){ try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){} openPdf(uri); } } }

    private void openPdf(Uri uri){
        closePdf(); progress.setVisibility(View.VISIBLE);
        try {
            descriptor=getContentResolver().openFileDescriptor(uri,"r");
            if(descriptor==null) throw new IOException("Cannot open file");
            renderer=new PdfRenderer(descriptor); if(renderer.getPageCount()==0) throw new IOException("PDF is empty");
            currentPage=0; zoom=1.25f; titleView.setText(fileName(uri));
            emptyState.setVisibility(View.GONE); ((View)pageImage.getTag()).setVisibility(View.VISIBLE); bottomBar.setVisibility(View.VISIBLE); render();
        } catch(Exception e){ closePdf(); Toast.makeText(this,"Could not open this PDF",Toast.LENGTH_LONG).show(); }
        finally { progress.setVisibility(View.GONE); }
    }

    private void render(){ if(renderer==null)return; PdfRenderer.Page page=null; try{
        page=renderer.openPage(currentPage); float density=getResources().getDisplayMetrics().density; float safeZoom=Math.min(zoom, 4096f/Math.max(page.getWidth(),page.getHeight()));
        int w=Math.max(1,(int)(page.getWidth()*density*safeZoom)); int h=Math.max(1,(int)(page.getHeight()*density*safeZoom));
        Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888); bitmap.eraseColor(Color.WHITE); page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY); pageImage.setImageBitmap(bitmap);
        pageView.setText((currentPage+1)+" / "+renderer.getPageCount()+"   "+Math.round(zoom*100)+"%");
    }catch(Exception e){Toast.makeText(this,"Page could not be rendered",Toast.LENGTH_SHORT).show();}finally{if(page!=null)page.close();}}

    private String fileName(Uri uri){ String name="PDF document"; Cursor c=null; try{c=getContentResolver().query(uri,null,null,null,null); if(c!=null&&c.moveToFirst()){int i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME); if(i>=0)name=c.getString(i);}}catch(Exception ignored){}finally{if(c!=null)c.close();} return name; }
    private int dp(int value){return (int)(value*getResources().getDisplayMetrics().density+.5f);}
    private void closePdf(){ if(renderer!=null){renderer.close();renderer=null;} if(descriptor!=null){try{descriptor.close();}catch(IOException ignored){}descriptor=null;} }
    @Override protected void onDestroy(){closePdf();super.onDestroy();}
}
