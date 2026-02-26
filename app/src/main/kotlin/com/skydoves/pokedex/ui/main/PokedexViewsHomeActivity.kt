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
import android.transition.Transition
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.tracing.Trace
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
import com.skydoves.pokedex.ui.details.DetailActivity.Companion.NAVIGATION_TRANSITION_TRACE_NAME
import com.skydoves.pokedex.utils.requireBooleanExtra
import com.skydoves.transformationlayout.TransformationLayout
import com.skydoves.transformationlayout.onTransformationStartContainer
import kotlin.random.Random
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class PokedexViewsHomeActivity : AppCompatActivity(R.layout.activity_main) {

    private lateinit var activityMainBinding: ActivityMainBinding

    internal val viewModel: PokedexViewsHomeViewModel by viewModels {
        PokedexViewsViewModelProviderFactory(ModuleLocator.repositoryModule)
    }

    private lateinit var adapter: PokemonAdapter

    /**
     * Cookie for the trace block of the home <-> details transition. Should be set when starting a
     * transition and reset when it ends.
     */
    private var transitionTraceCookie = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        trace("PokedexActivity Setup") {
            PokedexFeatureFlags.EnableTransformationLayout =
                intent.requireBooleanExtra(POKEDEX_ENABLE_TRANSFORMATION_LAYOUT)
            PokedexFeatureFlags.EnableSharedElementTransitions =
                intent.requireBooleanExtra(POKEDEX_ENABLE_SHARED_ELEMENT_TRANSITIONS)
            if (PokedexFeatureFlags.EnableSharedElementTransitions) {
                // onTransformationStartContainer sets up activity transitions and needs to run
                // before
                //  the super's onCreate
                onTransformationStartContainer()
                // Set window transitions listeners only after onTransformationStartContainer
                // initializes
                //  them
                setupSharedElementTransitionListeners()
            }
        }
        super.onCreate(savedInstanceState)

        trace("ModuleLocator#attach") { ModuleLocator.attach { application } }
        val onItemClicked: (Pokemon, TransformationLayout?) -> Unit =
            if (PokedexFeatureFlags.EnableSharedElementTransitions) {
                { pokemon, transformationLayout ->
                    transitionTraceCookie = startDetailActivityTransitionTrace()
                    DetailActivity.startActivityWithTransition(
                        transformationLayout =
                            requireNotNull(transformationLayout) {
                                "No TransformationLayout instance was passed back from PokedexAdapter"
                            },
                        pokemon = pokemon,
                        traceCookie = transitionTraceCookie,
                    )
                }
            } else {
                { pokemon, _ ->
                    transitionTraceCookie = startDetailActivityTransitionTrace()
                    DetailActivity.startActivity(this, pokemon, traceCookie = transitionTraceCookie)
                }
            }
        adapter = PokemonAdapter(onItemClicked = onItemClicked)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)

        ViewCompat.setOnApplyWindowInsetsListener(activityMainBinding.root) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            activityMainBinding.appBarLayout.updatePadding(top = insets.top)
            activityMainBinding.PokedexList.updatePadding(bottom = insets.bottom)
            WindowInsetsCompat.toWindowInsetsCompat(windowInsets.toWindowInsets()!!)
        }

        setupRecyclerView()
        observeViewModel()
    }

    /**
     * Start an async trace block with a new cookie for the details transition.
     *
     * @return The cookie for the trace block.
     */
    private fun startDetailActivityTransitionTrace(traceCookie: Int = Random.nextInt()): Int {
        Trace.beginAsyncSection(NAVIGATION_TRANSITION_TRACE_NAME, traceCookie)
        return traceCookie
    }

    private fun endDetailsTransitionTrace() {
        if (transitionTraceCookie != -1) {
            Trace.endAsyncSection(NAVIGATION_TRANSITION_TRACE_NAME, transitionTraceCookie)
            transitionTraceCookie = -1
        }
    }

    override fun onEnterAnimationComplete() {
        Log.d("PokedexViewsHomeActivity", "onEnterAnimationComplete")
        // onEnterAnimationComplete can get called multiple times when we have more than one
        //  transition, which is the case for shared element. For shared element, we rely on the
        //  transition listeners to update the status instead.
        if (!PokedexFeatureFlags.EnableSharedElementTransitions) {
            endDetailsTransitionTrace()
            activityMainBinding.pokedexHomeTransitionStatus.text = HOME_TRANSITION_INACTIVE
        }
    }

    private fun setupSharedElementTransitionListeners() {
        val sharedElementTransitionListener =
            object : Transition.TransitionListener {
                override fun onTransitionStart(transition: Transition) {
                    Log.d("PokedexViewsHomeActivity", "onTransitionStart $transition")
                    activityMainBinding.pokedexHomeTransitionStatus.text = HOME_TRANSITION_ACTIVE
                }

                override fun onTransitionEnd(transition: Transition) {
                    Log.d("PokedexViewsHomeActivity", "onTransitionEnd $transition")
                    activityMainBinding.pokedexHomeTransitionStatus.text = HOME_TRANSITION_INACTIVE
                    endDetailsTransitionTrace()
                }

                override fun onTransitionCancel(transition: Transition?) {}

                override fun onTransitionPause(transition: Transition?) {}

                override fun onTransitionResume(transition: Transition?) {}
            }

        window.sharedElementReturnTransition?.addListener(sharedElementTransitionListener)
        window.sharedElementExitTransition?.addListener(sharedElementTransitionListener)
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
            // threshold is more like an anti-threshold. The higher it is, the earlier we will
            // fetch more data, even if it's not actually needed. The threshold should ideally be
            // aligned with how many items can be visible on screen at once.
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

private const val HOME_TRANSITION_ACTIVE = "pokedex-home-transition-active-true"
private const val HOME_TRANSITION_INACTIVE = "pokedex-home-transition-active-false"
