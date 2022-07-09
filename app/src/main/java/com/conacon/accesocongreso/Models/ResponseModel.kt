package com.conacon.accesocongreso.Models

import com.google.gson.annotations.SerializedName

class ResponseModel(
    @SerializedName("evento") val evento: EventoModel,
    @SerializedName("persona") val persona: PersonaModel,
    @SerializedName("talleres") val talleres:Array<TallerModel>
)

class EventoModel(
    @SerializedName("acceso") val acceso:Boolean,
    @SerializedName("asistencia") val asistencia:Boolean,
    @SerializedName("mensaje") val mensaje:String
)
class PersonaModel(
    @SerializedName("aMaterno") val aMaterno: String,
    @SerializedName("aPaterno") val aPaterno: String,
    @SerializedName("email") val email: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("instituciones") val instituciones: Array<InstitucionModel>
)

class InstitucionModel(
    @SerializedName("color_n1") val color: String,
    @SerializedName("nombre") val nombre: String
)

class TallerModel(
    @SerializedName("nombre") val nombre_taller: String,
    @SerializedName("fecha_tll") val fecha: String
)