package com.pixelcraft.editor.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.pixelcraft.editor.ui.tabs.ColorTabFragment
import com.pixelcraft.editor.ui.tabs.FiltersTabFragment
import com.pixelcraft.editor.ui.tabs.LightTabFragment
import com.pixelcraft.editor.ui.tabs.TransformTabFragment

/**
 * Supplies the four tool pages shown under the tab bar.
 *
 * Keeping the page list in one place means a contributor adding a fifth
 * tool only edits this file.
 */
class EditorPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> LightTabFragment()
        1 -> ColorTabFragment()
        2 -> FiltersTabFragment()
        else -> TransformTabFragment()
    }
}
