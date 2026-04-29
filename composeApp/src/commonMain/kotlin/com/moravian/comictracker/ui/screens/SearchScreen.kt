package com.moravian.comictracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moravian.comictracker.ui.viewmodels.SearchViewModel
import com.moravian.comictracker.ui.viewmodels.SeriesSearchCard
import comictracker.composeapp.generated.resources.Res
import comictracker.composeapp.generated.resources.no_results_found
import comictracker.composeapp.generated.resources.searching_comics
import org.jetbrains.compose.resources.stringResource


@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onComicClick: (Long) -> Unit
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
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    viewModel.performSearch()
                }
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(viewModel.searchResults) { series ->
                SeriesSearchCard(series, onClick = { onComicClick(series.id) })
            }
        }
    }
}