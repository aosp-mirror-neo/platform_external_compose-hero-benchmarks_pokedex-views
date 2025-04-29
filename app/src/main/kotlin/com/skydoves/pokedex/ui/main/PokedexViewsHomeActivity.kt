/*
 * Designed and developed by 2022 skydoves (Jaewoong Eum)
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

package com.skydoves.pokedex.ui.main

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.skydoves.baserecyclerviewadapter.RecyclerViewPaginator
import com.skydoves.pokedex.R
import com.skydoves.pokedex.core.PokedexViewsViewModelProviderFactory
import com.skydoves.pokedex.core.di.ModuleLocator
import com.skydoves.pokedex.databinding.ActivityMainBinding
import com.skydoves.transformationlayout.onTransformationStartContainer
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class PokedexViewsHomeActivity : AppCompatActivity(R.layout.activity_main) {

    private lateinit var binding: ActivityMainBinding

    internal val viewModel: PokedexViewsHomeViewModel by viewModels {
        PokedexViewsViewModelProviderFactory(ModuleLocator.repositoryModule)
    }

    private val adapter = PokemonAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        ModuleLocator.attach { application }
        onTransformationStartContainer()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            this.adapter = this@PokedexViewsHomeActivity.adapter
            layoutManager = GridLayoutManager(this@PokedexViewsHomeActivity, 2)
        }
        RecyclerViewPaginator(
                recyclerView = binding.recyclerView,
                isLoading = { viewModel.isLoading.value },
                loadMore = { viewModel.fetchNextPokemonList() },
                onLast = { false },
            )
            .run { threshold = 8 }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.pokemonList.collect { pokemonList -> adapter.submitList(pokemonList) }
                }
                launch {
                    viewModel.toastMessage.filterNotNull().collect { message ->
                        println("MainActivity: $message")
                        Toast.makeText(this@PokedexViewsHomeActivity, message, Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressbar.visibility =
                            if (isLoading) {
                                View.VISIBLE
                            } else {
                                View.GONE
                            }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ModuleLocator.detach()
    }
}
