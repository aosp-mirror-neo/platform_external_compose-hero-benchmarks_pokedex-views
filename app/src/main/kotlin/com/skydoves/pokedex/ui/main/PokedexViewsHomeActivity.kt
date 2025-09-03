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
import androidx.tracing.trace
import com.skydoves.baserecyclerviewadapter.RecyclerViewPaginator
import com.skydoves.pokedex.R
import com.skydoves.pokedex.core.PokedexFeatureFlags
import com.skydoves.pokedex.core.PokedexFeatureFlags.Keys.POKEDEX_ENABLE_SHARED_ELEMENT_TRANSITIONS
import com.skydoves.pokedex.core.PokedexFeatureFlags.Keys.POKEDEX_ENABLE_TRANSFORMATION_LAYOUT
import com.skydoves.pokedex.core.PokedexViewsViewModelProviderFactory
import com.skydoves.pokedex.core.di.ModuleLocator
import com.skydoves.pokedex.core.model.Pokemon
import com.skydoves.pokedex.databinding.ActivityMainBinding
import com.skydoves.pokedex.ui.details.DetailActivity
import com.skydoves.pokedex.utils.requireBooleanExtra
import com.skydoves.transformationlayout.TransformationLayout
import com.skydoves.transformationlayout.onTransformationStartContainer
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class PokedexViewsHomeActivity : AppCompatActivity(R.layout.activity_main) {

    private lateinit var activityMainBinding: ActivityMainBinding

    internal val viewModel: PokedexViewsHomeViewModel by viewModels {
        PokedexViewsViewModelProviderFactory(ModuleLocator.repositoryModule)
    }

    private lateinit var adapter: PokemonAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        trace("PokedexActivity Setup") {
            PokedexFeatureFlags.EnableTransformationLayout =
                intent.requireBooleanExtra(POKEDEX_ENABLE_TRANSFORMATION_LAYOUT)
            PokedexFeatureFlags.EnableSharedElementTransitions =
                intent.requireBooleanExtra(POKEDEX_ENABLE_SHARED_ELEMENT_TRANSITIONS)
        }
        // onTransformationStartContainer sets up activity transitions and needs to run before
        //  the super's onCreate
        if (PokedexFeatureFlags.EnableSharedElementTransitions) {
            onTransformationStartContainer()
        }
        super.onCreate(savedInstanceState)

        trace("ModuleLocator#attach") { ModuleLocator.attach { application } }
        // Set window transitions listeners only after onTransformationStartContainer initializes
        //  them
        if (PokedexFeatureFlags.EnableSharedElementTransitions) {
            setupSharedElementTransitionListeners()
        }
        val onItemClicked: (Pokemon, TransformationLayout?) -> Unit =
            if (PokedexFeatureFlags.EnableSharedElementTransitions) {
                { pokemon, transformationLayout ->
                    DetailActivity.startActivityWithTransition(
                        transformationLayout =
                            requireNotNull(transformationLayout) {
                                "No TransformationLayout instance was passed back from PokedexAdapter"
                            },
                        pokemon = pokemon,
                    )
                }
            } else {
                { pokemon, _ -> DetailActivity.startActivity(this, pokemon) }
            }
        adapter = PokemonAdapter(onItemClicked = onItemClicked)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupSharedElementTransitionListeners() {
        val sharedElementTransitionListener =
            object : android.transition.Transition.TransitionListener {
                override fun onTransitionStart(transition: android.transition.Transition?) {
                    activityMainBinding.pokedexHomeTransitionStatus.text =
                        "pokedex-home-transition-active-true"
                }

                override fun onTransitionEnd(transition: android.transition.Transition?) {
                    activityMainBinding.pokedexHomeTransitionStatus.text =
                        "pokedex-home-transition-active-false"
                }

                override fun onTransitionCancel(transition: android.transition.Transition?) {}

                override fun onTransitionPause(transition: android.transition.Transition?) {}

                override fun onTransitionResume(transition: android.transition.Transition?) {}
            }
        window.sharedElementExitTransition.addListener(sharedElementTransitionListener)
        window.sharedElementReturnTransition.addListener(sharedElementTransitionListener)
    }

    private fun setupRecyclerView() {
        activityMainBinding.PokedexList.apply {
            this.adapter = this@PokedexViewsHomeActivity.adapter
            layoutManager = GridLayoutManager(this@PokedexViewsHomeActivity, 2)
        }
        RecyclerViewPaginator(
                recyclerView = activityMainBinding.PokedexList,
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
                        activityMainBinding.progressbar.visibility =
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
