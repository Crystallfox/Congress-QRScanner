package com.conacon.accesocongreso.Models

import com.google.gson.annotations.SerializedName

class ResponseModel(
    @SerializedName("evento") val evento: EventoModel,
    @SerializedName("persona") val persona: PersonaModel
)

class EventoModel(
    @SerializedName("acceso") val acceso:Boolean,
    @SerializedName("mensaje") val mensaje:String?
)
class PersonaModel(
    @SerializedName("amaterno") val aMaterno: String,
    @SerializedName("apaterno") val aPaterno: String,
    @SerializedName("email") val email: String?,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("instituciones") val instituciones: List<InstitucionModel>
)

class InstitucionModel(
    @SerializedName("color_n1") val color: String,
    @SerializedName("nombre") val nombre: String
)

class TallerModel(
    @SerializedName("nombre") val nombre_taller: String,
    @SerializedName("fecha_tll") val fecha: String
)