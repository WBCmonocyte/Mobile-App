package com.example.easysport.ui.signup

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.easysport.data.model.User

/**
 * Luồng đăng ký 2 bước. onComplete nhận user đã tạo thành công.
 */
@Composable
fun SignUpRoute(
    onComplete: (User) -> Unit,
    onSignIn: () -> Unit,
    onTerms: () -> Unit = {},
    onPrivacy: () -> Unit = {},
    viewModel: SignUpViewModel = viewModel(factory = SignUpViewModelFactory()),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BackHandler(enabled = state.step == 2) { viewModel.back() }
    BackHandler(enabled = state.step == 1) {
        viewModel.reset()
        onSignIn()
    }

    when (state.step) {
        1 -> SignUpDetailsScreen(
            form = state.form,
            errors = state.errors,
            onFormChange = viewModel::onFormChange,
            onNext = viewModel::next,
            onSignIn = {
                viewModel.reset()
                onSignIn()
            },
        )
        else -> SignUpPasswordScreen(
            form = state.form,
            errors = state.errors,
            onFormChange = viewModel::onFormChange,
            onComplete = { viewModel.complete(onComplete) },
            onBack = viewModel::back,
            onTerms = onTerms,
            onPrivacy = onPrivacy,
            isLoading = state.isLoading,
            submitError = state.submitError,
        )
    }
}
