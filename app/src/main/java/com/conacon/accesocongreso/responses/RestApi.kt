package com.conacon.accesocongreso.responses

import com.conacon.accesocongreso.Models.ListaPagadosModel
import com.conacon.accesocongreso.Models.ResponseModel
import com.conacon.accesocongreso.Models.ResponseTallerModel
import com.conacon.accesocongreso.Models.eventoResponseModel
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface RestApi {
    /* @Headers("Content-Type: application/json")*/
    @FormUrlEncoded
    @POST("assets/data/Controller/alumnos/alumnosInstitucionesControl.php")
    suspend fun getLista(
        @Field("action") action: String
    ): Response<List<ListaPagadosModel>>

    @FormUrlEncoded
    @POST("assets/data/Controller/eventos/talleresControl.php")
    suspend fun registroAsistencia(
        @Field("jsonasistencia") id_afiliado: String,
        @Field("action") action: String,
        @Field("eventoid") idEvento: String
    ): Response<ResponseModel>

    @FormUrlEncoded
    @POST("consultas_android/Control.php")
    suspend fun consultaEventos(
        @Field("action")action: String
    ): Response<eventoResponseModel>

    @FormUrlEncoded
    @POST("consultas_android/Control.php")
    suspend fun getTalleres(
        @Field("action")action: String,
        @Field("id_evento")id_evento: String
    ): Response<ResponseTallerModel>
}