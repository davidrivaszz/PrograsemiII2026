package com.example.myapplication;


import android.os.Bundle;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    TextView tempVal;
    Button btn;
    Spinner spn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn = findViewById(R.id.btnCalcular);
        btn.setOnClickListener(v -> calcular());
    }

    private void calcular() {
        try {
            // Leer num1 (Si está vacío, lo tomamos como 0.0 para evitar errores)
            tempVal = findViewById(R.id.txtNum1);
            String strNum1 = tempVal.getText().toString();
            double num1 = strNum1.isEmpty() ? 0.0 : Double.parseDouble(strNum1);

            // Leer num2 (Si está vacío, lo tomamos como 0.0)
            tempVal = findViewById(R.id.txtNum2);
            String strNum2 = tempVal.getText().toString();
            double num2 = strNum2.isEmpty() ? 0.0 : Double.parseDouble(strNum2);

            double respuesta = 0;

            spn = findViewById(R.id.cboOpciones);
            switch (spn.getSelectedItemPosition()) {
                case 0: // Suma
                    respuesta = num1 + num2;
                    break;
                case 1: // Resta
                    respuesta = num1 - num2;
                    break;
                case 2: // Multiplicacion
                    respuesta = num1 * num2;
                    break;
                case 3: // Division
                    if (num2 != 0) {
                        respuesta = num1 / num2;
                    } else {
                        Toast.makeText(this, "No se puede dividir entre 0", Toast.LENGTH_SHORT).show();
                        respuesta = 0;
                    }
                    break;
                case 4: // Factorial (Calculado en base a num1)
                    if (num1 < 0) {
                        Toast.makeText(this, "No hay factorial de negativos", Toast.LENGTH_SHORT).show();
                    } else {
                        respuesta = 1;
                        for (int i = 1; i <= (int) num1; i++) {
                            respuesta *= i;
                        }
                    }
                    break;
                case 5: // Porcentaje (El num1 % de num2)
                    respuesta = (num1 * num2) / 100.0;
                    break;
                case 6: // Exponenciacion (num1 elevado a num2)
                    respuesta = Math.pow(num1, num2);
                    break;
                case 7: // Raiz (num1 es la base, num2 es el indice de la raiz)
                    if (num2 == 0) {
                        Toast.makeText(this, "El índice de la raíz no puede ser 0", Toast.LENGTH_SHORT).show();
                    } else {
                        respuesta = Math.pow(num1, 1.0 / num2);
                    }
                    break;
            }

            // Mostrar el resultado
            tempVal = findViewById(R.id.lblRespuesta);
            tempVal.setText("Respuesta: " + respuesta);

        } catch (Exception e) {
            // Si algo sale mal (ej. texto no válido), mostramos un mensaje en lugar de que la app se cierre
            Toast.makeText(this, "Por favor, ingresa números válidos", Toast.LENGTH_SHORT).show();
        }
    }
}