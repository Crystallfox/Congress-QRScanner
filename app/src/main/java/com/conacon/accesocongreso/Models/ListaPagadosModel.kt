package com.conacon.accesocongreso.Models

import com.google.gson.annotations.SerializedName

data class ListaPagadosModel(@SerializedName("Carrera") val Carrera: String?,
                            @SerializedName("Celular") val Celular: String?,
                            @SerializedName("Correo") val Correo: String?,
                            @SerializedName("prospecto") val id_pospecto: String?,
                            @SerializedName("color")val color: String,
                            @SerializedName("nombrealumno") val nombre: String,
                            @SerializedName("paternoalumno") val apa: String,
                            @SerializedName("maternoalumno") val ama: String)
