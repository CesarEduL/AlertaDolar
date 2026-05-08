package com.waytolearn.alertadolar

/** Tipo de fila en el historial interno (persistido en JSON). */
enum class InternalNotificationType(val storageKey: String) {
    PRICE_CHANGE("CHANGE"),
    PRICE_STABLE_ABOVE("STABLE_ABOVE"),
    PRICE_BELOW_THRESHOLD("BELOW_THRESHOLD"),
    ERROR("ERROR");

    companion object {
        fun fromStorageOrInfer(
            key: String?,
            message: String,
            priceChangeFallback: Boolean?
        ): InternalNotificationType {
            if (!key.isNullOrBlank()) {
                entries.find { it.storageKey == key }?.let { return it }
            }
            return inferFromLegacy(message, priceChangeFallback)
        }

        /** Entradas antiguas sin campo `type`. */
        private fun inferFromLegacy(
            message: String,
            priceChangeFallback: Boolean?
        ): InternalNotificationType {
            val m = message.lowercase()
            if (m.contains("error") || m.contains("no se pudo obtener")) return ERROR
            if (priceChangeFallback == true ||
                message.contains(" cambió de ") ||
                message.contains("(cambió)")
            ) {
                return PRICE_CHANGE
            }
            if (message.contains("ya bajó de")) return PRICE_BELOW_THRESHOLD
            if (message.contains("aún no baja de")) return PRICE_STABLE_ABOVE
            return PRICE_STABLE_ABOVE
        }
    }
}
