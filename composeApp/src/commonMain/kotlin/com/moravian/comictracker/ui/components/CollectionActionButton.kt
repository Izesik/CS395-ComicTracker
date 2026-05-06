package com.moravian.comictracker.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.moravian.comictracker.ui.viewmodels.AddCollectionState
import comictracker.composeapp.generated.resources.Res
import comictracker.composeapp.generated.resources.add_to_collection
import comictracker.composeapp.generated.resources.added_to_collection
import comictracker.composeapp.generated.resources.loading
import comictracker.composeapp.generated.resources.remove_from_collection
import org.jetbrains.compose.resources.stringResource

private val AddedGreen = Color(0xFF2E7D32)
private val RemoveRed = Color(0xFFB71C1C)

/** Shared add/remove button for series and issue detail pages. */
@Composable
fun CollectionActionButton(
    addState: AddCollectionState,
    onAddToCollection: () -> Unit,
    onRemoveFromCollection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLoading =
        addState == AddCollectionState.Checking ||
            addState == AddCollectionState.Adding ||
            addState == AddCollectionState.Removing

    if (addState == AddCollectionState.InCollection) {
        Button(
            onClick = onRemoveFromCollection,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = RemoveRed,
                    contentColor = Color.White,
                ),
            modifier = modifier,
        ) {
            Text(stringResource(Res.string.remove_from_collection))
        }
    } else {
        Button(
            onClick = onAddToCollection,
            enabled = addState == AddCollectionState.Idle,
            colors =
                if (addState == AddCollectionState.Added) {
                    ButtonDefaults.buttonColors(
                        disabledContainerColor = AddedGreen,
                        disabledContentColor = Color.White,
                    )
                } else {
                    ButtonDefaults.buttonColors()
                },
            modifier = modifier,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.height(16.dp).width(16.dp),
                    strokeWidth = 2.dp,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            AnimatedContent(
                targetState = addState,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            ) { state ->
                Text(
                    when (state) {
                        AddCollectionState.Checking,
                        AddCollectionState.Adding,
                        AddCollectionState.Removing,
                        -> stringResource(Res.string.loading)

                        AddCollectionState.Added -> stringResource(Res.string.added_to_collection)

                        else -> stringResource(Res.string.add_to_collection)
                    },
                )
            }
        }
    }
}
