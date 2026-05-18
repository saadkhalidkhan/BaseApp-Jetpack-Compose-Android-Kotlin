package com.merttoptas.composebase.features.screen.characters

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.merttoptas.composebase.data.model.dto.CharacterDto
import com.merttoptas.composebase.domain.usecase.characters.GetCharactersUseCase
import com.merttoptas.composebase.domain.usecase.favorite.UpdateFavoriteUseCase
import com.merttoptas.composebase.domain.viewstate.IViewEvent
import com.merttoptas.composebase.domain.viewstate.characters.CharactersViewState
import com.merttoptas.composebase.features.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by merttoptas on 13.03.2022
 */

@HiltViewModel
class CharactersViewModel @Inject constructor(
    private val getCharactersUseCase: GetCharactersUseCase,
    private val updateFavoriteUseCase: UpdateFavoriteUseCase
) : BaseViewModel<CharactersViewState, CharactersViewEvent>() {

    private val config = PagingConfig(pageSize = 20)

    init {
        getAllCharacters(isRefreshing = false)
    }

    private fun getAllCharacters(isRefreshing: Boolean = false) {
        viewModelScope.launch {
            try {
                if (isRefreshing) {
                    setState { currentState.copy(isRefreshing = true) }
                } else {
                    setState { currentState.copy(isLoading = true) }
                }
                val params = GetCharactersUseCase.Params(config, hashMapOf())
                val pagedFlow = getCharactersUseCase(params).cachedIn(scope = viewModelScope)
                delay(1000)
                if (isRefreshing) {
                    setState { currentState.copy(isRefreshing = false, pagedData = pagedFlow) }
                } else {
                    setState { currentState.copy(isLoading = false, pagedData = pagedFlow) }
                }
            } catch (e: Exception) {
                // Handle error: reset loading/refreshing state to false
                if (isRefreshing) {
                    setState { currentState.copy(isRefreshing = false) }
                } else {
                    setState { currentState.copy(isLoading = false) }
                }
            }
        }
    }

    private fun updateFavorite(dto: CharacterDto) = viewModelScope.launch {
        val params = UpdateFavoriteUseCase.Params(dto)
        call(updateFavoriteUseCase(params))
    }

    override fun createInitialState() = CharactersViewState()

    override fun onTriggerEvent(event: CharactersViewEvent) {
        viewModelScope.launch {
            when (event) {
                is CharactersViewEvent.UpdateFavorite -> updateFavorite(event.dto)
                is CharactersViewEvent.Refresh -> getAllCharacters(isRefreshing = true)
            }
        }
    }
}

sealed class CharactersViewEvent : IViewEvent {
    object Refresh : CharactersViewEvent()
    class UpdateFavorite(val dto: CharacterDto) : CharactersViewEvent()
}