package com.example.myapplication;


import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    TabHost tbh;
    TextView tempVal;
    Spinner spn;
    Button btn;
    Double valores[] = new Double[] {1.0, 0.85, 7.67, 26.42, 36.80, 495.77};
    Double longitudes[] = new Double[] {1.0, 1000.0, 100.0, 39.3701, 3.280841666667, 1.1963081929167, 1.09361};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tbh = findViewById(R.id.tbhConversores);
        tbh.setup();

        tbh.addTab(tbh.newTabSpec("Monedas").setContent(R.id.tabMonedas).setIndicator("", getDrawable(R.drawable.monedas)));
        tbh.addTab(tbh.newTabSpec("Longitud").setContent(R.id.tabLongitud).setIndicator("", getResources().getDrawable(R.drawable.longitud)));
        tbh.addTab(tbh.newTabSpec("Volumen").setContent(R.id.tabVolumen).setIndicator("", getDrawable(R.drawable.volumen)));
        tbh.addTab(tbh.newTabSpec("Masa").setContent(R.id.tabMasa).setIndicator("", getDrawable(R.drawable.masa)));

        btn = findViewById(R.id.btnMonedasConvertir);
        btn.setOnClickListener(v->convertirMonedas());

        btn = findViewById(R.id.btnLongitudConvertir);
        btn.setOnClickListener(v->convertirLongitud());
    }
    private void convertirLongitud(){
        spn = findViewById(R.id.spnLongitudDe);
        int de = spn.getSelectedItemPosition();

        spn = findViewById(R.id.spnLongitudA);
        int a = spn.getSelectedItemPosition();

        tempVal = findViewById(R.id.txtLongitudCantidad);
        double cantidad = Double.parseDouble(tempVal.getText().toString());
        double respuesta = conversorLongitud(de, a, cantidad);

        tempVal = findViewById(R.id.lblLongitudRespuesta);
        tempVal.setText("Respuesta: "+ respuesta);
    }
    private void convertirMonedas(){
        spn = findViewById(R.id.spnMonedasDe);
        int de = spn.getSelectedItemPosition();

        spn = findViewById(R.id.spnMonedasA);
        int a = spn.getSelectedItemPosition();

        tempVal = findViewById(R.id.txtMonedasCantidad);
        double cantidad = Double.parseDouble(tempVal.getText().toString());
        double respuesta = conversor(de, a, cantidad);

        tempVal = findViewById(R.id.lblMonedasRespuesta);
        tempVal.setText("Respuesta: "+ respuesta);
    }
    double conversor(int de, int a, double cantidad){
        return valores[a]/valores[de] * cantidad;
    }
    double conversorLongitud(int de, int a, double cantidad){
        return longitudes[a]/longitudes[de] * cantidad;
    }
}