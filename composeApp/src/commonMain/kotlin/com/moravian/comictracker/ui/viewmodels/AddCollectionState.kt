package com.moravian.comictracker.ui.viewmodels

sealed class AddCollectionState {
    data object Checking : AddCollectionState()
    data object Idle : AddCollectionState()
    data object Adding : AddCollectionState()
    data object Added : AddCollectionState()
    data object InCollection : AddCollectionState()
}
