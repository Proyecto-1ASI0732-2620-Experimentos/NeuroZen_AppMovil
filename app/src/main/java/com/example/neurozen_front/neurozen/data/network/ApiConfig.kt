package com.example.neurozen_front.neurozen.data.network

object ApiConfig {
    // 1. SI USAS EMULADOR: Deja "10.0.2.2"
    // 2. SI USAS CELULAR FÍSICO: Pon la IP de tu PC (ej: "192.168.0.15")
    // IMPORTANTE: PC y Celular deben estar en el mismo Wi-Fi.
    
    private const val CURRENT_IP = "192.168.0.90" // IP de la PC para dispositivo físico
    private const val PORT = "5059"
    
    const val BASE_URL = "http://$CURRENT_IP:$PORT/api/v1/"
}
