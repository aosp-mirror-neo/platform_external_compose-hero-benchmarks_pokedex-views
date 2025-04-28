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

package com.skydoves.pokedex.core.repository.home

import androidx.annotation.WorkerThread
import com.skydoves.pokedex.core.database.entitiy.asDatabaseEntity
import com.skydoves.pokedex.core.database.entitiy.asPresentationModel
import com.skydoves.pokedex.core.service.PokedexClient
import com.skydoves.pokedex.core.database.PokemonDao
import kotlin.onSuccess
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import okhttp3.HttpUrl

class HomeRepositoryImpl(
    private val pokedexClient: PokedexClient,
    private val pokemonDao: PokemonDao,
    private val ioDispatcher: CoroutineDispatcher,
    private val apiUrl: HttpUrl
) : HomeRepository {

    @WorkerThread
    override fun fetchPokemonList(
        page: Int,
        onStart: () -> Unit,
        onComplete: () -> Unit,
        onError: (String?) -> Unit,
    ) =
        flow {
                // Start out by fetching cached data
                emit(pokemonDao.getPokemonList().asPresentationModel(apiUrl, page))
                // Afterwards, we'll make a request to the API to still get new data
                val networkPokemonResponse = pokedexClient.fetchPokemonList(page = page)
            println("networkPokemonResponse: $networkPokemonResponse")
                networkPokemonResponse
                    .onSuccess { data ->
                        val networkFetchedPokemons = data.results
                        pokemonDao.insertPokemonList(networkFetchedPokemons.asDatabaseEntity())
                        // We re-query the database to account for concurrent modifications
                        emit(pokemonDao.getAllPokemonList().asPresentationModel(apiUrl, page))
                    }
                    .onFailure { throwable -> onError(throwable.message) }
            }
            .onStart { onStart() }
            .onCompletion { onComplete() }
            .flowOn(ioDispatcher)
}
