package com.conacon.accesocongreso

import android.app.AlertDialog
import android.content.*
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.conacon.accesocongreso.Models.Asistente
import com.conacon.accesocongreso.Models.ListaPagadosModel
import com.conacon.accesocongreso.Models.TallerModel
import com.conacon.accesocongreso.databinding.ActivityMainBinding
import com.conacon.accesocongreso.responses.RestApi
import com.conacon.accesocongreso.responses.ServiceBuilder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {
    private var ctx = this
    private lateinit var Savings: SharedPreferences
    private var ListaPagos = ArrayList<ListaPagadosModel>()
    private var ListaAsistencias = ArrayList<Asistente>()
    private lateinit var binding: ActivityMainBinding
    private var currentEvent = 0;
    private  var eventsArray = ArrayList<String>()
    private var idsArray = ArrayList<String>()
    private  var talleresArray = ArrayList<String>()
    private var tallerclaveArray = ArrayList<String>()
    private var imageArray = ArrayList<String>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Savings = getPreferences(MODE_PRIVATE)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        binding.btnScanner.setOnClickListener{
            setVacios()
            initScanner()
        }
        binding.swiper.setOnRefreshListener {
            binding.swiper.isRefreshing = false
            //getListaPagados()
        }
        binding.btnExportar.setOnClickListener{
            exportarAsistencias()
        }
        //getListaPagados()
        cargarEventos()
        cargarAsistencias()
        binding.SpinnerEvento.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentEvent = position
                Picasso.get().load(imageArray.get(position)).into(binding.imagenEvento)
                cargarTalleres(idsArray.get(currentEvent))
            }

        }
    }

    private fun cargarTalleres(id_evento:String){
        talleresArray.add("Asistencia General")
        val adapter = ArrayAdapter(
            this,
            R.layout.spinner_texto, talleresArray
        )
        binding.SpinnerTalleres.adapter = adapter
    }

    private fun cargarEventos(){
        if(isNetworkAvailable()) {
            try {
                val retrofit = ServiceBuilder.buildService(RestApi::class.java)
                CoroutineScope(Dispatchers.Main).launch {
                    val cert = retrofit.consultaEventos(
                        action = "consulta_eventos_scanner"
                    )
                    eventsArray = ArrayList<String>()
                    idsArray = ArrayList<String>()
                    imageArray = ArrayList<String>()
                    ////Si la conexión fué exitosa
                    if (cert.isSuccessful && cert.body()!!.data != null) {
                        var datas = cert.body()!!.data!!
                        datas.forEach {
                            eventsArray.add(it.titulo)
                            val img = "https://moni.com.mx/assets/images/generales/flyers/"+it.imagen
                            imageArray.add(img)
                            idsArray.add(it.idEvento)
                        }
                    }//Si falla, hace la consulta local

                    val contxaux = this@MainActivity.ctx
                    if(contxaux != null) {
                        val adapter = ArrayAdapter(
                            contxaux,
                            R.layout.spinner_texto, eventsArray
                        )
                        binding.SpinnerEvento.adapter = adapter
                        cargarTalleres(idsArray.get(0))
                    }
                }
                //Si la respuesta no cumple con la estructura
            } catch (e: Exception) {
                print(e)
                //checkLocal(id_prospecto)
            } catch (e: UnknownHostException) {
                print(e)
                //checkLocal(id_prospecto)
            }
        }
    }
    private fun initScanner(){
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
        options.setPrompt("Escanear código")
        options.setCameraId(0) // Use a specific camera of the device
        options.setBeepEnabled(true)
        options.setBarcodeImageEnabled(true)
        barcodeLauncher.launch(options)
    }

    private val barcodeLauncher = registerForActivityResult(
        ScanContract()
    ) { result: ScanIntentResult ->
        if (result.contents == null) {
            Toast.makeText(this@MainActivity, "Lectura cancelada", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(
                this@MainActivity,
                "ID Prospecto: " + result.contents,
                Toast.LENGTH_LONG
            ).show()
            checkAsistencia(result.contents)
        }
    }

    private fun checkAsistencia(id_prospecto: String){
        setVacios()
        var ev = idsArray.get(currentEvent)
        if(isNetworkAvailable()) {
            try {
                val retrofit = ServiceBuilder.buildService(RestApi::class.java)
                CoroutineScope(Dispatchers.Main).launch {
                    val cert = retrofit.registroAsistencia(
                        id_afiliado = id_prospecto,
                        action = "check-congreso",
                        idEvento = idsArray.get(currentEvent)
                    )
                    ////Si la conexión fué exitosa
                    if (cert.isSuccessful && cert.body()!!.evento != null) {
                        var datas = cert.body()
                        ///Si tiene autorizado el acceso
                        if (datas!!.evento.acceso) {
                            setPositivo(
                                //datas.persona.instituciones[0].color,
                                datas.persona.nombre,
                                datas.persona.aPaterno,
                                datas.persona.aMaterno,
                                datas.evento.mensaje?: "Acceso correcto",
                                datas.persona.instituciones[0].color,
                                datas.persona.instituciones[0].nombre,
                                datas.persona.kit
                            )
                            guardaAsistencia(id_prospecto,ev,"1")
                        }///Si no tiene autorizado el acceso
                        else {
                            setnegativo(
                                //datas.persona.instituciones[0].color,
                                datas.persona.nombre,
                                datas.persona.aPaterno,
                                datas.persona.aMaterno,
                                //datas.persona.instituciones[0].nombre,
                                datas.evento.mensaje?: "Sin acceso permitido",
                                datas.persona.instituciones[0].color,
                                datas.persona.instituciones[0].nombre
                            )
                        }
                    }//Si falla, hace la consulta local
                    else {
                       // checkLocal(id_prospecto)
                        setnegativo("QR no válido",
                        "","","QR No Válido","#000000","Sin lectura de institución")
                    }
                }
                //Si la respuesta no cumple con la estructura
            } catch (e: Exception) {
                print(e)
                //checkLocal(id_prospecto)
            } catch (e: UnknownHostException) {
                print(e)
                //checkLocal(id_prospecto)
                guardaAsistencia(id_prospecto,ev,"0")
            }
        }
        else{
           // checkLocal(id_prospecto)
        }
    }

    fun setPositivo(nombre: String, apa: String, ama: String, mensaje: String, color: String, institucion: String, kit: String?){
        try {
            binding.imgAnuncio.setImageResource(R.drawable.ic_baseline_check_circle_24)
            binding.txtNombre.text = "${nombre} ${apa} ${ama}"
            binding.txtNombre.visibility = View.VISIBLE
            binding.txtInstitucion.text = institucion
            binding.txtInstitucion.visibility = View.VISIBLE
            binding.CirculoColor.setColorFilter(Color.parseColor(color))
            binding.txtMensaje.text = mensaje
            if(kit != null) {
                var mensaje = if(kit!! == "1")  "Entregar Kit" else "Kit ya entregado"
                mostrarDialogo(mensaje = mensaje)
            }
        }catch (e: Exception){
            Toast.makeText(this, "Acceso correcto, intente de nuevo para obtener el nombre", Toast.LENGTH_LONG).show()
        }
    }

    fun mostrarDialogo(mensaje: String){
        val builder = AlertDialog.Builder(this, androidx.appcompat.R.style.AlertDialog_AppCompat)
        builder.setMessage(mensaje)
            .setCancelable(false)
            .setPositiveButton("Aceptar") { dialog, id ->
                // Delete selected note from database
            }
        val alert = builder.create()
        alert.show()
    }

    fun setnegativo(nombre: String, apa: String, ama: String, mensaje: String, color: String, institucion: String){
        try {
            binding.imgAnuncio.setImageResource(R.drawable.ic_baseline_error_24)
            binding.txtNombre.text = "${nombre} ${apa} ${ama}"
            binding.txtInstitucion.text = institucion
            binding.txtInstitucion.visibility = View.VISIBLE
            binding.txtNombre.visibility = View.VISIBLE

            binding.CirculoColor.setColorFilter(Color.parseColor(color))
            binding.txtMensaje.text = mensaje
        }catch (e: Exception){
            Toast.makeText(this, "Acceso incorrecto, intente de nuevo para obtener el nombre", Toast.LENGTH_LONG).show()
        }
    }

    fun guardaAsistencia(id_prospecto: String, evento: String, validado: String){
        val prefsEditor: SharedPreferences.Editor = Savings.edit()
        val d = Date(Date().getTime())
        val s: String = SimpleDateFormat("dd/MM/yyyy hh:mm:ss").format(d)
        /*val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val formatted = current.format(formatter)*/
        val obj = Asistente(id_prospecto, s, evento, validado)
        ListaAsistencias.add(obj)
        val gson = Gson()
        val json = gson.toJson(ListaAsistencias)
        prefsEditor.putString("Asistencias", json)
        prefsEditor.commit()
    }

    fun checkLocal(id_prospecto: String){
        //cargarPagadosLocal()
        ListaPagos.forEach {
            if(it.id_pospecto == id_prospecto){
                setPositivo(it.nombre, it.apa, it.ama,  "Acceso correcto", it.color?:"#FFFFFF", it.Carrera?:"Vacio",null)
                //guardaAsistencia(it.id_pospecto)
                return
            }
        }
        setnegativo("Sin nombre", "", "",  "Acceso Incorrecto", "#FFFFFF", "Vacio")
    }
    fun guardarPagadosLocal(Lista: ArrayList<ListaPagadosModel>){
        val prefsEditor: SharedPreferences.Editor = Savings.edit()
        val gson = Gson()
        val json = gson.toJson(Lista)
        prefsEditor.putString("Pagados", json)
        prefsEditor.commit()
    }

    fun cargarPagadosLocal(){
        val gson = Gson()
        val json: String = Savings.getString("Pagados", "")!!
        val typeToken = object : TypeToken<ArrayList<ListaPagadosModel>>() {}.type
        try {
            ListaPagos = Gson().fromJson<ArrayList<ListaPagadosModel>>(json, typeToken)
        }catch(e: Exception){
            ListaPagos = ArrayList<ListaPagadosModel>()
        }
    }

    fun getListaPagados(){
        binding.swiper.isRefreshing = true
        ListaPagos = ArrayList<ListaPagadosModel>()
        if(isNetworkAvailable()){
            val retrofit = ServiceBuilder.buildService(RestApi::class.java)
            try {
                CoroutineScope(Dispatchers.Main).launch {
                    val cert = retrofit.getLista(
                        action = "asistentes_scae_match")
                    ////Si la conexión fué exitosa
                    if (cert.isSuccessful){
                        var datas = cert.body()
                        ///Si tiene autorizado el acceso
                        if (datas != null && datas.size>0){
                            datas.forEach{
                                ListaPagos.add(it)
                            }
                            //print(ListaPagos)
                            //guardarPagadosLocal(ListaPagos)
                            avisaerror("Lista Actualizada", "La lista se actualizó correctamente")
                        }///Si no tiene autorizado el acceso
                        else{
                            avisaerror("Error al actualziar lista", "El servidor retornouna lista vacía")
                            //cargarPagadosLocal()
                        }
                    }//Si falla, hace la consulta local
                    else{
                        avisaerror("Error al actualziar lista", "El servidor retorno un error 500")
                       // cargarPagadosLocal()
                    }
                }
                //Si la respuesta no cumple con la estructura
            }catch(e: Exception){
                avisaerror("Error al actualziar lista", "El servidor retorno un formato de JSON distinto")
               // cargarPagadosLocal()
            }
        }else{
            avisaerror("Error al actualziar lista", "El servidor retorno un error 500")
           // cargarPagadosLocal()
        }
        binding.swiper.isRefreshing = false
    }

    fun setVacios(){
        binding.txtNombre.text = ""
        binding.txtInstitucion.text = ""
        binding.txtTaller2.text = ""
        binding.txtTaller1.text = ""
        binding.CirculoColor.setColorFilter(Color.parseColor("#ffffff"))
    }

    fun avisaerror(titulo:String, mensaje: String){

    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }
    fun exportarAsistencias(){

        val json: String = Savings.getString("Asistencias", "")!!
        pasteTextFromClipboard(json)


       /* CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.IO) {
                val data: String = json
                val file = File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "asistencias.txt")
                val outputStream =
                    ctx.openFileOutput("asistencias.txt", Context.MODE_PRIVATE)
                outputStream.write(data.toByteArray())
                outputStream.close()

                //val uri = FileProvider.getUriForFile(ctx,BuildConfig.APPLICATION_ID, file)

                    val shareIntent = Intent().apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
                        putExtra(Intent.EXTRA_SUBJECT, "CAD")
                    }
                startActivity(
                    Intent.createChooser(
                        shareIntent,
                        "CAD"
                    )
                )
            }
        }*/
    }

    fun cargarAsistencias(){
        val json: String = Savings.getString("Asistencias", "")!!
        val typeToken = object : TypeToken<ArrayList<Asistente>>() {}.type
        try {
            ListaAsistencias = Gson().fromJson<ArrayList<Asistente>>(json, typeToken)
        }catch(e: Exception){
            ListaAsistencias = ArrayList<Asistente>()
        }
    }
    private fun pasteTextFromClipboard(texto: String) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("text", texto)
        clipboardManager.setPrimaryClip(clipData)
        Toast.makeText(this, "JSON copiado al portapapeles", Toast.LENGTH_LONG).show()
    }
}