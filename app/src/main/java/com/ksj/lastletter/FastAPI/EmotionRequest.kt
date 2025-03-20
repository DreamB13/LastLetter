package com.ksj.lastletter.FastAPI

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

// 요청 데이터 모델
data class EmotionRequest(val text: String)

// 응답 데이터 모델
data class EmotionResponse(val emotion: String)

interface ApiService2 {
    @POST("analyze/")
    suspend fun analyzeText(@Body request: EmotionRequest): EmotionResponse
}

// Retrofit 인스턴스 생성
object RetrofitInstance2 {
    private const val BASE_URL = "https://3a98-34-86-104-42.ngrok-free.app/" // Colab에서 받은 ngrok 주소

    val api: ApiService2 by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService2::class.java)
    }
}