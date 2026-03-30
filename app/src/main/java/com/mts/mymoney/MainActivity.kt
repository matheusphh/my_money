package com.mts.mymoney

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mts.mymoney.data.FinanceDatabase
import com.mts.mymoney.ui.theme.FinanceDarkColorScheme
import com.mts.mymoney.viewmodel.FinanceViewModel
import com.mts.mymoney.viewmodel.FinanceViewModelFactory


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val database = FinanceDatabase.getDatabase(this)
        val dao = database.financeDao()
        val viewModelFactory = FinanceViewModelFactory(dao)

        setContent {
            MaterialTheme(colorScheme = FinanceDarkColorScheme) {
                val viewModel: FinanceViewModel = viewModel(factory = viewModelFactory)
                FinanceApp(viewModel)
            }
        }
    }
}