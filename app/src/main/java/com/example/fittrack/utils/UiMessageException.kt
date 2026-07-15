package com.example.fittrack.utils

import androidx.annotation.StringRes

/**
 * Classe auxiliar para erros com mensagem preparada para a interface.
 *
 * Comentário de manutenção: esta classe deve manter a lógica separada por responsabilidade,
 * para ser mais fácil alterar a app sem quebrar outros ecrãs.
 */
class UiMessageException(@StringRes val messageRes: Int) : Exception()
