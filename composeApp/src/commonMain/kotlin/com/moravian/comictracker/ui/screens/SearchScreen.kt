package com.moravian.comictracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.moravian.comictracker.ui.viewmodels.SearchUiState
import com.moravian.comictracker.ui.viewmodels.SearchViewModel
import com.moravian.comictracker.ui.viewmodels.SeriesSearchCard
import comictracker.composeapp.generated.resources.Res
import comictracker.composeapp.generated.resources.no_results_found
import comictracker.composeapp.generated.resources.searching_comics
import org.jetbrains.compose.resources.stringResource

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onComicClick: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = viewModel.searchQuery,
            onValueChange = { viewModel.onQueryChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            label = { Text(stringResource(Res.string.searching_comics)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = { viewModel.performSearch() }
            )
        )

        when (val state = viewModel.uiState) {
            is SearchUiState.Idle -> Unit
            is SearchUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is SearchUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.message,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            is SearchUiState.Success -> {
                if (state.results.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(Res.string.no_results_found, viewModel.searchQuery),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.results) { result ->
                            SeriesSearchCard(result, onClick = { onComicClick(result.id) })
                        }
                    }
                }
            }
        }
    }
}
