package com.ksj.lastletter.FastAPI

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Headers

// 서버에서 응답을 받을 데이터 클래스
data class TextRequest(val input_text: String)
data class TextResponse(val generated_text: String)

// Retrofit 인터페이스 정의
interface ApiService {
    @Headers("Content-Type: application/json")
    @POST("/generate")
    suspend fun generateText(@Body request: TextRequest): TextResponse
}

// Retrofit 클라이언트 설정
object RetrofitClient {
    private const val BASE_URL = "https://f913-34-74-83-198.ngrok-free.app" // 코랩 서버 주소

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}