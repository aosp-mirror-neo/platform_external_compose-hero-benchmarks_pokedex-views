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

package com.skydoves.pokedex.core.repository.details

import androidx.annotation.WorkerThread
import com.skydoves.pokedex.core.database.PokemonInfoDao
import com.skydoves.pokedex.core.database.entitiy.asDatabaseEntity
import com.skydoves.pokedex.core.database.entitiy.asPresentationModel
import com.skydoves.pokedex.core.service.PokedexClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion

class DetailsRepositoryImpl(
    private val pokedexClient: PokedexClient,
    private val pokemonInfoDao: PokemonInfoDao,
    private val ioDispatcher: CoroutineDispatcher,
) : DetailsRepository {

    @WorkerThread
    override fun fetchPokemonInfo(
        name: String,
        onComplete: () -> Unit,
        onError: (String?) -> Unit,
    ) =
        flow {
                val pokemonInfo = pokemonInfoDao.getPokemonInfo(name)
                if (pokemonInfo == null) {
                    val response = pokedexClient.fetchPokemonInfo(name = name)
                    response
                        .onSuccess { data ->
                            pokemonInfoDao.insertPokemonInfo(data.asDatabaseEntity())
                            emit(data)
                        }
                        .onFailure { throwable -> onError(throwable.message) }
                } else {
                    emit(pokemonInfo.asPresentationModel())
                }
            }
            .onCompletion { onComplete() }
            .flowOn(ioDispatcher)
}
