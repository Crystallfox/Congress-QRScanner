package com.conacon.accesocongreso.Models

import com.google.gson.annotations.SerializedName

class eventoResponseModel(@SerializedName("estatus") val estatus: String,
                          @SerializedName("data") val data: List<EventosModel>?)
class EventosModel (@SerializedName("idEvento") val idEvento: String,
                    @SerializedName("tipo") val tipo: String,
                    @SerializedName("titulo") val titulo: String,
                    @SerializedName("imagen") val imagen: String,
                    @SerializedName("fechaE") val fecha: String)