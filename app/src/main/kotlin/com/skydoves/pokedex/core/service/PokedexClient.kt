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

package com.skydoves.pokedex.core.service

import com.skydoves.pokedex.core.model.PokemonInfo
import com.skydoves.pokedex.core.model.PokemonResponse

class PokedexClient(private val pokedexService: PokedexService) {

    suspend fun fetchPokemonList(page: Int): Result<PokemonResponse> =
        kotlin.runCatching {
            pokedexService.fetchPokemonList(limit = PAGING_SIZE, offset = page * PAGING_SIZE)
        }

    suspend fun fetchPokemonInfo(name: String): Result<PokemonInfo> =
        kotlin.runCatching { pokedexService.fetchPokemonInfo(name = name) }

    companion object {
        private const val PAGING_SIZE = 48
    }
}
