package com.example.spacirkovnik

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.spacirkovnik.ComponentType.TWO_BUTTONS

data class DataHolder(val displayText: String,
                      val firstButtonText: String = "Späť",
                      val secondButtonText: String = "Pokračovať",
                      val fontSize: TextUnit = 16.sp,
                      val componentType: ComponentType = TWO_BUTTONS,
                      val answers: List<Answer> = emptyList())