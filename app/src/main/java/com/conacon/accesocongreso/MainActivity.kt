package com.conacon.accesocongreso

import android.content.*
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.FileProvider
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.UnknownHostException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class MainActivity : AppCompatActivity() {
    private var ctx = this
    private lateinit var Savings: SharedPreferences
    private var ListaPagos = ArrayList<ListaPagadosModel>()
    private var ListaAsistencias = ArrayList<Asistente>()
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Savings = getPreferences(MODE_PRIVATE)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        binding.btnScanner.setOnClickListener {
            setVacios()
            initScanner()
        }
        binding.swiper.setOnRefreshListener {
            binding.swiper.isRefreshing = true
            getListaPagados()
        }
        binding.btnExportar.setOnClickListener {
            exportarAsistencias()
        }
        getListaPagados()
        cargarAsistencias()
    }
    private fun initScanner(){
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
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
        if(isNetworkAvailable()) {
            try {
                val retrofit = ServiceBuilder.buildService(RestApi::class.java)
                CoroutineScope(Dispatchers.Main).launch {
                    val cert = retrofit.registroAsistencia(
                        id_afiliado = id_prospecto,
                        action = "consultar_alumno",
                        idEvento = "72"
                    )
                    ////Si la conexión fué exitosa
                    if (cert.isSuccessful) {
                        var datas = cert.body()
                        ///Si tiene autorizado el acceso
                        if (datas!!.evento.acceso) {
                            setPositivo(
                                datas.persona.instituciones[0].color,
                                datas.persona.nombre,
                                datas.persona.aPaterno,
                                datas.persona.aMaterno,
                                datas.persona.instituciones[0].nombre,
                                datas.talleres,
                                datas.evento.mensaje
                            )
                            guardaAsistencia(id_prospecto)
                        }///Si no tiene autorizado el acceso
                        else {
                            setnegativo(
                                datas.persona.instituciones[0].color,
                                datas.persona.nombre,
                                datas.persona.aPaterno,
                                datas.persona.aMaterno,
                                datas.persona.instituciones[0].nombre,
                                datas.talleres,
                                datas.evento.mensaje
                            )
                        }
                    }//Si falla, hace la consulta local
                    else {
                        checkLocal(id_prospecto)
                    }
                }
                //Si la respuesta no cumple con la estructura
            } catch (e: Exception) {
                print(e)
                checkLocal(id_prospecto)
            } catch (e: UnknownHostException) {
                print(e)
                checkLocal(id_prospecto)
            }
        }
        else{
            checkLocal(id_prospecto)
        }
    }

    fun setPositivo(color: String, nombre: String, apa: String, ama: String, institucion: String, talleres: Array<TallerModel>?, mensaje: String){
        binding.imgAnuncio.setImageResource(R.drawable.ic_baseline_check_circle_24)
        binding.txtNombre.text = "${nombre} ${apa} ${ama}"
        binding.txtInstitucion.text = institucion
        if(talleres != null && talleres.size==1){
            binding.txtTaller1.text = talleres[0].nombre_taller
            binding.txtTaller2.visibility = View.GONE
        }else{
            if(talleres !=null && talleres.size==2){
                binding.txtTaller2.visibility = View.VISIBLE
                binding.txtTaller1.text = talleres[0].nombre_taller
                binding.txtTaller2.text = talleres[1].nombre_taller
            }
            else{
                binding.txtTaller1.text = "Sin talleres registrados"
                binding.txtTaller2.visibility = View.GONE
            }
        }
        binding.CirculoColor.setColorFilter(Color.parseColor(color))
        binding.txtMensaje.text = mensaje
    }

    fun setnegativo(color: String, nombre: String, apa: String, ama: String, institucion: String, talleres: Array<TallerModel>?, mensaje: String){
        binding.imgAnuncio.setImageResource(R.drawable.ic_baseline_error_24)
        binding.txtNombre.text = "${nombre} ${apa} ${ama}"
        binding.txtInstitucion.text = institucion
        binding.txtInstitucion.visibility = View.VISIBLE
        binding.txtNombre.visibility = View.VISIBLE
        if(talleres != null && talleres.size==1){
            binding.txtTaller1.text = talleres[0].nombre_taller
            binding.txtTaller2.visibility = View.GONE
        }else{
            if(talleres != null && talleres.size==2){
                binding.txtTaller1.visibility = View.VISIBLE
                binding.txtTaller2.visibility = View.VISIBLE
                binding.txtTaller1.text = talleres[0].nombre_taller
                binding.txtTaller2.text = talleres[1].nombre_taller
            }
            else{
                binding.txtTaller1.text = "Sin talleres registrados"
                binding.txtTaller2.visibility = View.GONE
            }
        }
        binding.CirculoColor.setColorFilter(Color.parseColor(color))
        binding.txtMensaje.text = mensaje
    }

    fun guardaAsistencia(id_prospecto: String){
        val prefsEditor: SharedPreferences.Editor = Savings.edit()
        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val formatted = current.format(formatter)
        val obj = Asistente(id_prospecto, formatted)
        ListaAsistencias.add(obj)
        val gson = Gson()
        val json = gson.toJson(ListaAsistencias)
        prefsEditor.putString("Asistencias", json)
        prefsEditor.commit()
    }

    fun checkLocal(id_prospecto: String){
        cargarPagadosLocal()
        ListaPagos.forEach {
            if(it.id_pospecto == id_prospecto){
                setPositivo(it.color,it.nombre, it.apa, it.ama, "", null, "Acceso correcto")
                guardaAsistencia(it.id_pospecto)
                return
            }
        }
        setnegativo("#ffffff","Sin nombre", "", "", "", null, "Acceso Incorrecto")
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
        ListaPagos = Gson().fromJson<ArrayList<ListaPagadosModel>>(json, typeToken)
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
                            guardarPagadosLocal(ListaPagos)
                            avisaerror("Lista Actualizada", "La lista se actualizó correctamente")
                        }///Si no tiene autorizado el acceso
                        else{
                            avisaerror("Error al actualziar lista", "El servidor retornouna lista vacía")
                            cargarPagadosLocal()
                        }
                    }//Si falla, hace la consulta local
                    else{
                        avisaerror("Error al actualziar lista", "El servidor retorno un error 500")
                        cargarPagadosLocal()
                    }
                }
                //Si la respuesta no cumple con la estructura
            }catch(e: Exception){
                avisaerror("Error al actualziar lista", "El servidor retorno un formato de JSON distinto")
                cargarPagadosLocal()
            }
        }else{
            avisaerror("Error al actualziar lista", "El servidor retorno un error 500")
            cargarPagadosLocal()
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
        ListaAsistencias = Gson().fromJson<ArrayList<Asistente>>(json, typeToken)
    }
    private fun pasteTextFromClipboard(texto: String) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("text", texto)
        clipboardManager.setPrimaryClip(clipData)
        Toast.makeText(this, "JSON copiado al portapapeles", Toast.LENGTH_LONG).show()
    }
}