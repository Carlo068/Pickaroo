package com.example.pikaroo.ui.ordenar.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pikaroo.ui.ordenar.viewmodel.OrdenarViewModel

@Composable
fun OrdenarView(
    viewModel: OrdenarViewModel = viewModel()
) {
    val state = viewModel.state

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (state.isLoading) {
            CircularProgressIndicator()
        } else {
            Text(text = state.title)
        }
    }
}
