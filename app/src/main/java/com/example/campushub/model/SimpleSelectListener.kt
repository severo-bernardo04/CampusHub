package com.example.campushub

import android.view.View
import android.widget.AdapterView

class SimpleSelectListener(
    private val onSelect: () -> Unit
) : AdapterView.OnItemSelectedListener {
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        onSelect()
    }
    override fun onNothingSelected(parent: AdapterView<*>?) {}
}