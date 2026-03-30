package com.mts.mymoney.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mts.mymoney.data.AccountEntity
import com.mts.mymoney.data.FinanceDao
import com.mts.mymoney.data.TransactionEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FinanceViewModel(private val dao: FinanceDao) : ViewModel() {

    val accountsFlow: StateFlow<List<AccountEntity>> = dao.getAllAccountsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactionsFlow: StateFlow<List<TransactionEntity>> = dao.getAllTransactionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val currentAccounts = dao.getAllAccountsFlow().first()
            if (currentAccounts.isEmpty()) {
                dao.insertAccount(AccountEntity(name = "Carteira Física"))
            }
        }
    }

    fun addAccount(name: String, linkedPackage: String? = null) {
        viewModelScope.launch {
            dao.insertAccount(AccountEntity(name = name, linkedPackageName = linkedPackage))
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            dao.deleteTransactionsByAccount(account.id)
            dao.deleteAccount(account)
        }
    }

    fun addTransaction(accountId: String, description: String, amount: Double, isIncome: Boolean) {
        viewModelScope.launch {
            dao.insertTransaction(
                TransactionEntity(
                    accountId = accountId,
                    description = description.trim(),
                    amount = amount,
                    isIncome = isIncome
                )
            )
        }
    }
}

class FinanceViewModelFactory(private val dao: FinanceDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FinanceViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}