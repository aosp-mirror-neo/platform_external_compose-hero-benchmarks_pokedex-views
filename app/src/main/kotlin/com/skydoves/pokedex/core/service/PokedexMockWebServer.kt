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

package com.skydoves.pokedex.core.service

import android.graphics.Bitmap
import androidx.compose.integration.hero.common.implementation.GradientBitmap
import com.skydoves.pokedex.core.model.AllPokemonNames
import com.skydoves.pokedex.core.model.fakePokemonInfo
import com.skydoves.pokedex.core.model.fakePokemonNames
import com.skydoves.pokedex.core.model.fakePokemonNetworkModels
import com.skydoves.pokedex.core.model.fakePokemonResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer

/**
 * A [okhttp3.mockwebserver.MockWebServer] with a [Dispatcher] that sends responses with fake data
 * for our API.
 */
fun pokedexMockWebServer(json: Json) =
    MockWebServer().apply { dispatcher = PokedexMockDispatcher(json) }

/** This [Dispatcher] provides fake responses for our API. */
private class PokedexMockDispatcher(private val json: Json) : Dispatcher() {
    private val pokemonEndpointRegex = Regex("/api/v2/pokemon(\\?(?<query>(.*)))")
    private val pokemonInfoEndpointRegex = Regex("/api/v2/pokemon/(?<name>\\w*)(/?)")
    private val pokemonImageEndpointRegex = Regex("/api/v2/pokemon/(?<name>.*)/image(/?)")

    override fun dispatch(request: RecordedRequest): MockResponse {
        val requestPath = request.path
        if (requestPath == null) return MockResponse().setResponseCode(404)
        val response =
            try {
                when {
                    pokemonEndpointRegex.matches(requestPath) -> pokemonHandler(request)
                    pokemonInfoEndpointRegex.matches(requestPath) -> pokemonInfoHandler(request)
                    pokemonImageEndpointRegex.matches(requestPath) -> pokemonImageHandler(request)
                    else -> MockResponse().setResponseCode(404)
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
                MockResponse()
                    .setResponseCode(500)
                    .setBody(exception.message ?: "Unknown Error Occurred")
            }
        return response
    }

    private fun pokemonHandler(request: RecordedRequest): MockResponse {
        val requestUrl = request.requestUrl
        if (requestUrl == null) return MockResponse().setResponseCode(404)
        val maxPokemon = requestUrl.queryParameter("limit")?.toInt() ?: 20
        val fetchingOffset = requestUrl.queryParameter("offset")?.toInt() ?: 0
        val response = fakePokemonResponse(
            pokemons = fakePokemonNetworkModels(
                pokemonNames = fakePokemonNames(
                    limit = maxPokemon,
                    offset = fetchingOffset
                )
            )
        )
        return MockResponse()
            .setResponseCode(200)
            .setBody(json.encodeToString(response))
    }

    private fun pokemonInfoHandler(request: RecordedRequest): MockResponse {
        val requestUrl = request.requestUrl
        if (requestUrl == null) return MockResponse().setResponseCode(404)
        val pokemonName = requestUrl.pathSegments.last()
        val fakePokemonInfo =
            json.encodeToString(
                fakePokemonInfo(id = AllPokemonNames.indexOf(pokemonName), name = pokemonName)
            )
        return MockResponse().setResponseCode(200).setBody(fakePokemonInfo)
    }

    private fun pokemonImageHandler(request: RecordedRequest): MockResponse {
        val requestUrl = request.requestUrl
        if (requestUrl == null) return MockResponse().setResponseCode(404)
        val pathSegments = requestUrl.pathSegments
        val pokemonName = pathSegments[pathSegments.size - 2]
        val image = GradientBitmap(width = 500, height = 500, seed = pokemonName.hashCode())
        val buffer = Buffer()
        image.compress(Bitmap.CompressFormat.PNG, 100, buffer.outputStream())
        return MockResponse().setResponseCode(200).setBody(buffer)
    }
}
