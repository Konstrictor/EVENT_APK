package bi.konstrictor.ikirori;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private Menu menu;
    TextView lbl_guest_place, lbl_guest_type, lbl_guest_solde, lbl_guest_fullname,
            lbl_guest_others,lbl_guest_inscr_date, lbl_guest_phone, lbl_guest_email;
    ImageView img_guest_profile;
    ProgressBar progress_fetching_data;
    MainActivity activity;
    private ArrayList<Product> products = new ArrayList<>();
    private Profile profile;
    private ArrayList<String> products_str = new ArrayList<>();
    private Member user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        lbl_guest_place = findViewById(R.id.lbl_guest_place);
        lbl_guest_type = findViewById(R.id.lbl_guest_type);
        lbl_guest_solde = findViewById(R.id.lbl_guest_solde);
        lbl_guest_fullname = findViewById(R.id.lbl_guest_fullname);
        lbl_guest_others = findViewById(R.id.lbl_guest_others);
        lbl_guest_inscr_date = findViewById(R.id.lbl_guest_inscr_date);
        lbl_guest_email = findViewById(R.id.lbl_guest_email);
        lbl_guest_phone = findViewById(R.id.lbl_guest_phone);
        img_guest_profile = findViewById(R.id.img_guest_profile);
        progress_fetching_data = findViewById(R.id.progress_fetching_data);

        user = Host.extractUser(Host.getSessionValue(this, "user_session", "token"));

        if(user.hasService("service")){
            loadProducts();
        }
        activity = this;
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu) {
        getMenuInflater().inflate( R.menu.user_menu, menu);
        this.menu = menu;
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.action_logout){
            Host.logout(this);
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadProducts() {
        OkHttpClient client = new OkHttpClient();
        HttpUrl.Builder urlBuilder = HttpUrl.parse(Host.getURL(this)+"/api/product/").newBuilder();

        String url = urlBuilder.build().toString();
        final String token = Host.getSessionValue(this, "user_session","token");
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + token)
                .get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Pas d'access réseau", Toast.LENGTH_SHORT).show();
                        progress_fetching_data.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String json = response.body().string();
                try {
                    JSONArray json_array = new JSONArray(json);
                    for (int i=0; i<json_array.length(); i++){
                        JSONObject json_object = json_array.getJSONObject(i);
                        Product product = new Product(
                                json_object.getString("id"),
                                json_object.getString("name"),
                                json_object.getDouble("price")
                        );
                        products.add(product);
                        products_str.add(product.name);
                    }
                } catch (Exception e) {
                    try {
                        if(Host.tokenExpired(token)){
                            Host.refreshToken(MainActivity.this);
                            loadProducts();
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    final String message = e.getMessage();
                    Log.i("==== MAIN ACT JSON ====", json);
                    Log.i("==== MAIN ACT ERR ====", e.getMessage());
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "got incorrect products", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }
    public void scanQr(View view) {
        Host.logoutIfNoSession(this);
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
        integrator.setPrompt("Scan a barcode");
        integrator.setCameraId(0);  // Use a specific camera of the device
        integrator.setBeepEnabled(false);
        integrator.setBarcodeImageEnabled(true);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result!=null){
            if(result.getContents()==null){
                Toast.makeText(this, "you cancelled the scan", Toast.LENGTH_LONG).show();
            } else {
                setUserQR(result.getContents());
                Toast.makeText(this, "fetching guest data...", Toast.LENGTH_SHORT).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void setUserQR(final String qr_data) {
        progress_fetching_data.setVisibility(View.VISIBLE);

        OkHttpClient client = new OkHttpClient();
        HttpUrl.Builder urlBuilder = HttpUrl.parse(Host.getURL(this)+"/api/profile/scanqr/"+qr_data+"/").newBuilder();

        String url = urlBuilder.build().toString();
        final String token = Host.getSessionValue(this, "user_session","token");
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + token)
                .get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Pas d'access réseau", Toast.LENGTH_SHORT).show();
                        progress_fetching_data.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String json = response.body().string();
                try {
                    JSONArray json_array = new JSONArray(json);
                    if (json_array.length()>0){
                        JSONObject json_object = json_array.getJSONObject(0);
                        final Profile profile = new Profile(
                            json_object.getString("id"),
                            json_object.getString("fullname"),
                            json_object.getString("avatar"),
                            json_object.getString("phone"),
                            json_object.getString("email"),
                            json_object.getString("date"),
                            json_object.getString("autres"),
                            json_object.getString("qr"),
                            json_object.getJSONObject("ticket")
                        );
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                chargerProfile(profile);
                            }
                        });
                    }
                } catch (Exception e) {
                    try {
                        if(Host.tokenExpired(token)){
                            Host.refreshToken(MainActivity.this);
                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    setUserQR(qr_data);
                                }
                            });
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    final String message = e.getMessage();
                    Log.i("==== MAIN ACT JSON ====", json);
                    Log.i("==== MAIN ACT ERR ====", e.getMessage());
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "incorrect guest infos", Toast.LENGTH_LONG).show();
                            progress_fetching_data.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });
    }

    public void chargerProfile(Profile profile) {
        this.profile = profile;
        lbl_guest_place.setText(profile.ticket.id);
        lbl_guest_type.setText(profile.ticket.name);
        lbl_guest_solde.setText(profile.ticket.consommable.toString());
        lbl_guest_fullname.setText(profile.fullname);
        lbl_guest_others.setText(profile.autres);
        lbl_guest_inscr_date.setText(profile.date);
        lbl_guest_email.setText(profile.email);
        lbl_guest_phone.setText(profile.phone);

        String avatar = Host.getURL(this)+profile.avatar;
        Glide.with(this).load(avatar).into(img_guest_profile);
        progress_fetching_data.setVisibility(View.GONE);
    }

    public void openService(View view) {
        if ((products.size()>0)&(profile!=null)){
            ServicesForm servicesForm = new ServicesForm(this, profile, products, products_str);
            servicesForm.show();
        }
    }
}
