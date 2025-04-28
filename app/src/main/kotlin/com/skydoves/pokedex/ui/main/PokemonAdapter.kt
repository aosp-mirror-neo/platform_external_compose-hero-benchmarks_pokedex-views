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

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.ViewGroup
import com.skydoves.pokedex.core.model.Pokemon
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.skydoves.pokedex.databinding.ItemPokemonBinding
import com.skydoves.pokedex.ui.details.DetailActivity

class PokemonAdapter : ListAdapter<Pokemon, PokemonAdapter.PokemonViewHolder>(diffUtil) {

  private var onClickedAt = 0L

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PokemonViewHolder {
    val binding = ItemPokemonBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    return PokemonViewHolder(binding)
  }

  override fun onBindViewHolder(holder: PokemonViewHolder, position: Int) =
    holder.bind(getItem(position))

  inner class PokemonViewHolder(
    private val binding: ItemPokemonBinding
  ) : RecyclerView.ViewHolder(binding.root) {

    private val glideRequestListener = PokemonGlideRequestListener(
      onResourceReady = { drawable ->
        if (drawable !is BitmapDrawable) return@PokemonGlideRequestListener
        val bitmap = drawable.bitmap
        Palette.Builder(bitmap).generate { palette ->
          val rgb = palette?.dominantSwatch?.rgb
          if (rgb != null) {
            binding.cardView.setCardBackgroundColor(rgb)
          }
        }
      }
    )

    init {
      binding.root.setOnClickListener {
        val position = bindingAdapterPosition.takeIf { it != NO_POSITION }
          ?: return@setOnClickListener
        val currentClickedAt = SystemClock.elapsedRealtime()
        if (currentClickedAt - onClickedAt > binding.transformationLayout.duration) {
          DetailActivity.startActivity(binding.transformationLayout, getItem(position))
          onClickedAt = currentClickedAt
        }
      }
    }

    fun bind(pokemon: Pokemon) {
      Glide.with(binding.root.context)
        .load(pokemon.imageUrl)
        .listener(glideRequestListener)
        .into(binding.image)
      binding.name.text = pokemon.name
    }
  }

  companion object {
    private val diffUtil = object : DiffUtil.ItemCallback<Pokemon>() {

      override fun areItemsTheSame(oldItem: Pokemon, newItem: Pokemon): Boolean =
        oldItem.name == newItem.name

      override fun areContentsTheSame(oldItem: Pokemon, newItem: Pokemon): Boolean =
        oldItem == newItem
    }
  }
}

internal class PokemonGlideRequestListener(
  private val onLoadFailed: ((e: GlideException?) -> Unit)? = null,
  private val onResourceReady: ((resource: Drawable) -> Unit)? = null,
) : RequestListener<Drawable> {
  override fun onLoadFailed(
    e: GlideException?,
    model: Any?,
    target: Target<Drawable>,
    isFirstResource: Boolean,
  ): Boolean {
    e?.printStackTrace()
    onLoadFailed?.invoke(e)
    return false
  }

  override fun onResourceReady(
    resource: Drawable,
    model: Any,
    target: Target<Drawable>?,
    dataSource: DataSource,
    isFirstResource: Boolean,
  ): Boolean {
    onResourceReady?.invoke(resource)
    return false
  }
}
