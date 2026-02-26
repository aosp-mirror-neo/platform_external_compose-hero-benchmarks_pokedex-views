/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Designed and developed by 2024 skydoves (Jaewoong Eum)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.skydoves.pokedex.core.di

import com.skydoves.pokedex.core.service.PokedexClient
import com.skydoves.pokedex.core.service.PokedexService
import com.skydoves.pokedex.core.service.pokedexMockWebServer
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class NetworkModule(private val json: Json, private val networkCoroutineContext: CoroutineContext) {
    // Android O+ prohibits CLEARTEXT communication, even to localhost. We generate certificates
    // for our mock server and request client to authenticate them.
    val localhostCertificates by lazy {
        val rootCertificate =
            HeldCertificate.Builder().certificateAuthority(maxIntermediateCas = 0).build()
        val localhostCertificate =
            HeldCertificate.Builder()
                .addSubjectAlternativeName("localhost")
                .signedBy(rootCertificate)
                .build()
        HandshakeCertificates.Builder()
            .addTrustedCertificate(rootCertificate.certificate)
            .heldCertificate(localhostCertificate)
            .build()
    }

    val mockServer: MockWebServer by lazy {
        pokedexMockWebServer(json).apply {
            useHttps(localhostCertificates.sslSocketFactory(), false)
            // Starting the server requires a network operation. This can't happen on the main
            // thread, so we execute this in a blocking manner.
            runBlocking(networkCoroutineContext) { start() }
        }
    }
    val baseUrl: HttpUrl by lazy {
        // Prod URL: https://pokeapi.co/api/v2/
        // Calculating the URL requires a host lookup, which is a network operation. This can't
        // happen on the main thread, so for the initialization we block.
        runBlocking(networkCoroutineContext) { mockServer.url("/api/v2/") }
    }

    fun okHttpClientFactory(): OkHttpClient {
        return OkHttpClient.Builder()
            .addNetworkInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
            )
            .sslSocketFactory(
                sslSocketFactory = localhostCertificates.sslSocketFactory(),
                trustManager = localhostCertificates.trustManager,
            )
            .build()
    }

    val okHttpClient: OkHttpClient by lazy { okHttpClientFactory() }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(baseUrl)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    val pokedexService: PokedexService by lazy { retrofit.create(PokedexService::class.java) }

    val pokedexClient: PokedexClient by lazy { PokedexClient(pokedexService) }
}
