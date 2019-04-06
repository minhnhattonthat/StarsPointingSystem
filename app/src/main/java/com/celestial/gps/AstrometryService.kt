package com.celestial.gps

import com.celestial.gps.AstrometryModel.JobResult
import com.celestial.gps.AstrometryModel.LoginResponse
import com.celestial.gps.AstrometryModel.SubmissionStatus
import com.celestial.gps.AstrometryModel.UploadResponse
import io.reactivex.Observable
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.OkHttpClient
import com.google.gson.GsonBuilder
import java.util.concurrent.TimeUnit


interface AstrometryService {

    @FormUrlEncoded
    @POST("login")
    fun login(@Field("request-json") apiKey: String): Observable<LoginResponse>

    @Multipart
    @POST("upload")
    fun upload(@Part("request-json") uploadRequest: RequestBody, @Part file: MultipartBody.Part): Observable<UploadResponse>

    @GET("submissions/{subId}")
    fun getSubmissionStatus(@Path("subId") subId: String): Observable<SubmissionStatus>

    @GET("jobs/{jobId}/info/")
    fun getJobResults(@Path("jobId") jobId: String): Observable<JobResult>

    companion object {
        fun create(): AstrometryService {
            val httpClient = OkHttpClient.Builder()
            httpClient.readTimeout(120, TimeUnit.SECONDS).writeTimeout(120, TimeUnit.SECONDS).connectTimeout(120, TimeUnit.SECONDS)
            val interceptor = HttpLoggingInterceptor()
            interceptor.level = HttpLoggingInterceptor.Level.BODY
            httpClient.interceptors().add(interceptor)

            val gson = GsonBuilder()
                .setLenient()
                .create()

            val retrofit = Retrofit.Builder()
                .addCallAdapterFactory(
                    RxJava2CallAdapterFactory.create()
                )
                .addConverterFactory(
                    GsonConverterFactory.create(gson)
                )
//                .client(httpClient.build())
                .baseUrl("http://nova.astrometry.net/api/")
                .build()

            return retrofit.create(AstrometryService::class.java)
        }
    }
}