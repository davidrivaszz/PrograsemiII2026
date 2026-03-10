package com.example.myapplication;



import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    TextView tempVal;
    Spinner spn;
    Button btn;
    Double valores[][] = {
            {1.0, 0.85, 7.67, 26.42, 36.80, 495.77}, //moendas
            {1.0, 1000.0, 100.0, 39.3701, 3.280841666667, 1.1963081929167, 1.09361}, //longitud
            {1.0, 1000.0, 0.001, 0.264172, 4.22675, 1.05669, 33.814}, //volumen
            {1.0, 1000.0, 1000000.0, 0.001, 35.274, 2.2046}, //masa
            {1.0, 0.125, 0.0001220703125, 0.00000011920928955, 0.0000000001164153218, 0.0000000000001136868}, //almacenamiento
            {1.0, 60.0, 3600.0, 0.0417, 0.00595, 0.00139, 0.000114, 3600000.0}, //tiempo
            {1.0, 0.001, 0.000001, 0.000000001, 0.000000000001}, //Tramsferemcoa de datos
    };
    String[][] etiquetas = {
            {"Dolar", "Euro", "Quetzal", "Lempira", "Cordoba", "Colon CR"}, //monedas
            {"Mts", "Ml", "Cm", "Pulgada", "Pies", "Vara", "Yarda"}, //Longitud
            {"L", "mL", "Metro cubico", "Galon", "Taza", "Cuarto", "Onza Liquida"},  //volumen
            {"kg", "g", "mg", "t", "oz", "lb"},  //masa
            {"b", "B", "KB", "MB", "GB", "TB"},  //almacenamiento
            {"h", "min", "s", "dia", "sem", "mes", "año", "ms"},  //tiempo
            {"bps", "Kbps", "Mbps", "Gbps", "Tbps"},  //Transferencia de datos
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn = findViewById(R.id.btnConvertir);
        btn.setOnClickListener(v->convertir());

        cambiarEtiqueta(0);//valores predeterminaods

        spn = findViewById(R.id.spnTipo);
        spn.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                cambiarEtiqueta(i);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }
    private void cambiarEtiqueta(int posicion){
        ArrayAdapter<String> aaEtiquetas = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                etiquetas[posicion]
        );
        aaEtiquetas.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spn = findViewById(R.id.spnDe);
        spn.setAdapter(aaEtiquetas);

        spn = findViewById(R.id.spnA);
        spn.setAdapter(aaEtiquetas);
    }
    private void convertir(){
        spn = findViewById(R.id.spnTipo);
        int tipo = spn.getSelectedItemPosition();

        spn = findViewById(R.id.spnDe);
        int de = spn.getSelectedItemPosition();

        spn = findViewById(R.id.spnA);
        int a = spn.getSelectedItemPosition();

        tempVal = findViewById(R.id.txtCantidad);
        double cantidad = Double.parseDouble(tempVal.getText().toString());
        double respuesta = conversor(tipo, de, a, cantidad);

        tempVal = findViewById(R.id.lblRespuesta);
        tempVal.setText("Respuesta: "+ respuesta);
    }
    double conversor(int tipo, int de, int a, double cantidad){
        return valores[tipo][a]/valores[tipo][de] * cantidad;
    }
}