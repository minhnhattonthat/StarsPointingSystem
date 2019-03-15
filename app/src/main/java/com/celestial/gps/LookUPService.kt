package com.celestial.gps

import io.reactivex.Observable
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface LookUPService {

    @GET
    fun getStar(@Query("name") name: String): Observable<LookUPModel.Star>

    companion object {
        fun create(): LookUPService {

            val retrofit = Retrofit.Builder()
                .addCallAdapterFactory(
                    RxJava2CallAdapterFactory.create())
                .addConverterFactory(
                    GsonConverterFactory.create())
                .baseUrl("http://www.strudel.org.uk/lookUP/json/")
                .build()

            return retrofit.create(LookUPService::class.java)
        }
    }
}