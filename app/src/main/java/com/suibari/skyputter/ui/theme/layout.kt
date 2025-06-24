package com.suibari.skyputter.ui.theme

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

val Modifier.screenPadding: Modifier
    get() = this.padding(horizontal = 16.dp, vertical = 8.dp)

val Modifier.contentPadding: Modifier
    get() = this.padding(16.dp)

val Modifier.cardPadding: Modifier
    get() = this.padding(12.dp)

val Modifier.itemPadding: Modifier
    get() = this.padding(vertical = 4.dp, horizontal = 8.dp)

val Modifier.spacePadding: Modifier
    get() = this.padding(start = 8.dp)