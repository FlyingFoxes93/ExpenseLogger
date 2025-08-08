package com.example.expenselogger

import com.example.expenselogger.models.ActivityItem
import org.junit.Assert.assertEquals
import org.junit.Test

class ActivitiesAdapterTest {

    private val clickListener = object : ActivitiesAdapter.OnActivityClickListener {
        override fun onActivitySelected(activity: ActivityItem) {}
    }

    private val deleteListener = object : ActivitiesAdapter.OnActivityDeleteListener {
        override fun onActivityDelete(activity: ActivityItem) {}
    }

    @Test
    fun updateActivities_doesNotMutateSourceList() {
        val original = mutableListOf(
            ActivityItem(1, "Work"),
            ActivityItem(2, "Personal")
        )
        val adapter = ActivitiesAdapter(original, clickListener, deleteListener)

        // Filter down to a single activity and update the adapter
        adapter.updateActivities(listOf(original[0]))

        // Original list should remain unchanged
        assertEquals(2, original.size)
    }
}
