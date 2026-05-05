package com.moravian.comictracker.ui.viewmodels

/**
 * Represents the current state of the add/remove collection button on detail screens.
 *
 * Transitions: [Checking] → [InCollection] or [Idle] → [Adding] → [Added] → [InCollection]
 *              [InCollection] → [Removing] → [Idle]
 */
sealed class AddCollectionState {
    /** Querying the database to determine whether the item is already saved. */
    data object Checking : AddCollectionState()
    /** Item is not in the collection; the add button is enabled. */
    data object Idle : AddCollectionState()
    /** A save operation is in progress. */
    data object Adding : AddCollectionState()
    /** The item was just added; the button shows a confirmation state briefly. */
    data object Added : AddCollectionState()
    /** The item is already in the collection; the remove button is shown. */
    data object InCollection : AddCollectionState()
    /** A delete operation is in progress. */
    data object Removing : AddCollectionState()
}
