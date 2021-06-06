package com.ugb.myprimerproyecto;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class mostrarpostulados extends AppCompatActivity {

    TextView nombrel, duil;
    FloatingActionButton btnadd;
    DB miconexion;
    ListView ltspostulados;
    Cursor datospostuladoscursor = null;
    ArrayList<postulados> postuladosArrayList=new ArrayList<postulados>();
    ArrayList<postulados> postuladosArrayListCopy=new ArrayList<postulados>();
    postulados mispostulados;
    JSONArray jsonArrayDatospostulados;
    JSONObject jsonObjectDatospostulados;
    utilidades u;
    String lognombre,logdui,logtelefono,logmail,logpadss;
    detectarInternet di;
    FloatingActionButton btnChat;
    int position = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mostrarpostulados);

        nombrel = findViewById(R.id.nombre);
        duil = findViewById(R.id.duii);

        Bundle recibirparametros = getIntent().getExtras();
        lognombre = recibirparametros.getString("nombre");
        logdui = recibirparametros.getString("duii");
        logtelefono = recibirparametros.getString("telefono");
        logmail = recibirparametros.getString("mail");
        logpadss = recibirparametros.getString("padss");


        nombrel.setText(lognombre);
        duil.setText(logdui);

        di = new detectarInternet(getApplicationContext());
        btnadd = findViewById(R.id.btnagregar);
        btnadd.setOnClickListener(v->{
            Agregar("nuevo");
        });

        obtenerDatos();
       // Buscar();

        btnChat = findViewById(R.id.btnChat);
        btnChat.setOnClickListener(v -> {
            Intent chat = new Intent(getApplicationContext(), Chat.class);
            chat.putExtra("nombre", lognombre);
            startActivity(chat);
        });
    }

    private void Agregar(String accion) {
        Bundle parametros = new Bundle();
        parametros.putString("accion", accion);
        parametros.putString("nombre", lognombre);
        parametros.putString("duii", logdui);
        parametros.putString("telefono", logtelefono);
        parametros.putString("mail", logmail);
        parametros.putString("padss", logpadss);
        Intent i = new Intent(getApplicationContext(), agregarpostulados.class);
        i.putExtras(parametros);
        startActivity(i);

    }

    private void obtenerDatos() {
        if(di.hayConexionInternet()) {
            mensajes("Mostrando datos de votacion");
            obtenerDatosOnLine();
           } else {
            mensajes("No se pudo conectar con la base");
            }
    }

    private void obtenerDatosOnLine() {
        try {
            ConexionconServer conexionconServer = new ConexionconServer();
            String resp = conexionconServer.execute(u.urlmostrarpostulados, "GET").get();
            jsonObjectDatospostulados=new JSONObject(resp);
            jsonArrayDatospostulados = jsonObjectDatospostulados.getJSONArray("rows");
            mostrarDatos();
        }catch (Exception ex){
            mensajes(ex.getMessage());
        }
    }

    private void mostrarDatos() {
        try{
            ltspostulados = findViewById(R.id.list);
            postuladosArrayList.clear();
            postuladosArrayListCopy.clear();
            JSONObject jsonObject;
            if(di.hayConexionInternet()) {
                if(jsonArrayDatospostulados.length()>0) {
                    for (int i = 0; i < jsonArrayDatospostulados.length(); i++) {
                        jsonObject = jsonArrayDatospostulados.getJSONObject(i).getJSONObject("value");
                        mispostulados = new postulados(
                                jsonObject.getString("_id"),
                                jsonObject.getString("_rev"),
                                jsonObject.getString("nombre"),
                                jsonObject.getString("dui"),
                                jsonObject.getString("propuesta"),
                                jsonObject.getString("otro"),
                                jsonObject.getString("urlfoto"),
                                jsonObject.getString("urltriler")
                        );
                        postuladosArrayList.add(mispostulados);
                    }}
            }

            adaptadorImagenes adaptadorImagenes = new adaptadorImagenes(getApplicationContext(), postuladosArrayList);
            ltspostulados.setAdapter(adaptadorImagenes);
            registerForContextMenu(ltspostulados);
            postuladosArrayListCopy.addAll(postuladosArrayList);

        }catch (Exception e){
            mensajes(e.getMessage());
        }
    }
    private void mensajes(String msg){
        Toast.makeText(getApplicationContext(),msg,Toast.LENGTH_LONG).show();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_edit, menu);
        try {
            if(di.hayConexionInternet()) {
                AdapterView.AdapterContextMenuInfo adapterContextMenuInfo = (AdapterView.AdapterContextMenuInfo) menuInfo;
                position = adapterContextMenuInfo.position;
                menu.setHeaderTitle(jsonArrayDatospostulados.getJSONObject(position).getJSONObject("value").getString("nombre"));
            }
        }catch (Exception e){
            mensajes(e.getMessage());
        }
    }
    @Override

    public boolean onContextItemSelected(@NonNull MenuItem item) {
        try {
            switch (item.getItemId()) {
                case R.id.mxnAgregar:
                    Agregar("nuevo");
                    break;
                case R.id.mxnModificar:
                   Modificar ("modificar");
                    break;
                case R.id.mxnEliminar:
                    Eliminar();
                    break;
                case R.id.mxnVotar:
                  votar("votar");
                    break;
            }
        }catch (Exception ex){
            mensajes(ex.getMessage());
        }
        return super.onContextItemSelected(item);
    }

    private void Eliminar(){
        try {
            AlertDialog.Builder confirmacion = new AlertDialog.Builder(mostrarpostulados.this);
            confirmacion.setTitle("Esta seguro de eliminar?");

                jsonObjectDatospostulados = jsonArrayDatospostulados.getJSONObject(position).getJSONObject("value");
                confirmacion.setMessage(jsonObjectDatospostulados.getString("nombre"));

            confirmacion.setPositiveButton("Si", (dialog, which) -> {

                try {
                    if(di.hayConexionInternet()){
                        ConexionconServer objElimina = new ConexionconServer();
                        String resp =  objElimina.execute(u.urlagregarpostulados +
                                jsonObjectDatospostulados.getString("_id")+ "?rev="+
                                jsonObjectDatospostulados.getString("_rev"), "DELETE"
                        ).get();

                        JSONObject jsonRespEliminar = new JSONObject(resp);
                        if(jsonRespEliminar.getBoolean("ok")){
                            jsonArrayDatospostulados.remove(position);
                            mostrarDatos();
                        }
                    }

                    obtenerDatos();
                    mensajes("Registro eliminado");
                    dialog.dismiss();
                }catch (Exception e){
                }
            });
            confirmacion.setNegativeButton("No", (dialog, which) -> {
                mensajes("Eliminacion detendia");
                dialog.dismiss();
            });
            confirmacion.create().show();
        } catch (Exception ex){
            mensajes(ex.getMessage());
        }
    }


    private void Modificar(String accion){
        Bundle parametros = new Bundle();
        parametros.putString("accion", accion);
        parametros.putString("nombre", lognombre);
        parametros.putString("duii", logdui);
        parametros.putString("telefono", logtelefono);
        parametros.putString("mail", logmail);
        parametros.putString("padss", logpadss);
        jsonObjectDatospostulados = new JSONObject();
        JSONObject jsonValueObject = new JSONObject();
        if(di.hayConexionInternet())
        {
            try {
                if(jsonArrayDatospostulados.length()>0){
                    parametros.putString("datos", jsonArrayDatospostulados.getJSONObject(position).toString() );
                }
            }catch (Exception e){
                mensajes(e.getMessage());
            }
        }
        Intent i = new Intent(getApplicationContext(), agregarpostulados.class);
        i.putExtras(parametros);
        startActivity(i);
    }

    private void votar(String accion){
        Bundle parametros = new Bundle();
        parametros.putString("accion", accion);
        parametros.putString("nombre", lognombre);
        parametros.putString("duii", logdui);
        parametros.putString("telefono", logtelefono);
        parametros.putString("mail", logmail);
        parametros.putString("padss", logpadss);
        jsonObjectDatospostulados = new JSONObject();
        JSONObject jsonValueObject = new JSONObject();
        if(di.hayConexionInternet())
        {
            try {
                if(jsonArrayDatospostulados.length()>0){
                    parametros.putString("datos", jsonArrayDatospostulados.getJSONObject(position).toString() );
                }
            }catch (Exception e){
                mensajes(e.getMessage());
            }
        }
        Intent i = new Intent(getApplicationContext(), votar.class);
        i.putExtras(parametros);
        startActivity(i);
    }
}