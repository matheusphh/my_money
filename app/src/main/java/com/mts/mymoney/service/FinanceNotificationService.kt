package com.mts.mymoney.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.mts.mymoney.data.FinanceDatabase
import com.mts.mymoney.data.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class FinanceNotificationService : NotificationListenerService() {

    // Para evitar notificações duplicadas
    companion object {
        private val processedCache = mutableSetOf<String>()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        sbn?.let {
            val packageName = it.packageName
            val extras = it.notification.extras
            val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
            val text = extras.getString(Notification.EXTRA_TEXT) ?: ""

            val fullText = "$title $text".lowercase()

            // Cria uma assinatura baseada no app e no texto da notificação
            val signature = "${packageName}_${fullText}"

            // Se já processamos essa notificação, IGNORA e sai!
            if (processedCache.contains(signature)) {
                return
            }

            // Lógica para identificar se é uma transação financeira
            if (fullText.contains("r$") || fullText.contains("pix") || fullText.contains("transferência") || fullText.contains("compra")) {

                val amountRegex = Regex("""r\$\s*([\d.,]+)""")
                val match = amountRegex.find(fullText)

                if (match != null) {
                    val amountStr = match.groupValues[1].replace(".", "").replace(",", ".")
                    val amount = amountStr.toDoubleOrNull()

                    if (amount != null) {
                        val isIncome = fullText.contains("recebeu") || fullText.contains("recebida") || fullText.contains("chegou") || fullText.contains("te enviou") || fullText.contains("creditado")

                        // Se chegou até aqui, é uma notificação válida!
                        processedCache.add(signature)

                        // Evita que o trave o celular
                        if (processedCache.size > 50) {
                            processedCache.clear()
                        }

                        // Salva no banco de dados
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = FinanceDatabase.getDatabase(applicationContext)
                            val dao = database.financeDao()

                            val accounts = dao.getAllAccountsFlow().firstOrNull() ?: emptyList()
                            val targetAccount = accounts.find { acc -> acc.linkedPackageName == packageName }

                            if (targetAccount != null) {
                                dao.insertTransaction(
                                    TransactionEntity(
                                        accountId = targetAccount.id,
                                        description = "Auto: $title",
                                        amount = amount,
                                        isIncome = isIncome
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}